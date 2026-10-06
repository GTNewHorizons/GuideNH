#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Visual Inspection Screener (screen)

Three layers: geometric / vlm / report
Dependencies: Pillow + the Python 3 standard library
"""

import argparse
import base64
import io
import json
import os
import re
import sys
import time
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor, as_completed
from io import BytesIO

# Constants - the prompts can be iterated on here

VLM_SYSTEM_PROMPT = """You are a layout defect inspector. Analyse the layout and rendering problems in the image.

This is one vertical slice of a long page. Text, images or scenes cut off at the top or bottom edge of the slice are the slice's doing, so do not report them. Report "clipping" only when content is cut off by a container edge inside the slice.

[Expected elements of this documentation system - do not report]
- Angle-bracket tags such as <GameScene> or <Entity> appearing in body text in monospace code style are normal layout.
- A solid black bar (possibly with slight noise) is the expected rendering of a Spoiler mask.
- The bilingual TEST GOAL / INVARIANTS block at the top of a page is test annotation, not a content defect.

[Judging basis] The small grey text "Expected: ..." on a page is the official description of the intended result for the content below it. Use it as the comparison baseline: report only when the actual rendering does not match the Expected description; when it matches, do not report. Paragraphs without an Expected line are judged by general layout standards. Do not report anything merely because of the presence or the wording of the Expected text itself.

Check the following problem categories:
1. Text overlap - different text blocks overlap each other, or an icon/image sits on top of text
2. Text overflowing its container - text extends beyond its background container's bounds
3. Misplaced elements - elements are misaligned or positioned abnormally (an icon drifting off its line, pressing into an adjacent line, or landing outside its block belongs here too)
4. Abnormal blank space - blank areas that should not be there
5. Broken images or 3D scenes - images/3D rendering display abnormally (corruption / black blocks / missing)
6. Glyph rendering problems - text is incomplete, garbled or wrong glyphs
7. Clipping - content cut off by an edge
8. Font rendering path inconsistency - body text and text inside graphics (chart/flowchart/mindmap nodes, scene labels) on the same page should use one and the same smooth font rendering; when text inside graphics looks like a pixel bitmap and clearly differs from the smooth body font, report this category
9. Other - any other layout defect

[What not to report, besides the expected elements above]
- In a tree or directory-tree structure, aligned connector lines (such as the box-drawing forms or | |-- \\--) mean the indentation is correct; do not infer parent-child nesting from the text semantics.
- Lines styled like "CSV with ..." or "## ..." are section headings, not unrendered configuration text.
- Text running to the right edge of the page or a container without being cut in half is normal layout; report "clipping" only when text is visibly cut in half.

Strictly output JSON only, with no other text.
Output format: {"findings": [{"bbox":[x,y,w,h], "class":"problem category", "severity":"error|warn|info", "confidence":0-1, "evidence":"one-sentence description"}]}
The keys must be exactly bbox/class/severity/confidence/evidence; other key names such as type or description are forbidden.
When there is no problem output: {"findings": []}
Note: bbox coordinates are pixel coordinates inside the image."""

VLM_USER_TEXT = "Detect layout and rendering defects in this image region."

# Neutral system prompt used by the ask subcommand (unlike the first-pass
# VLM_SYSTEM_PROMPT)
ASK_SYSTEM_PROMPT = """You are a visual question answering assistant. Answer strictly the specific question the user asks; do not describe the whole image in general terms.

Requirements:
- Answer only from evidence visible in the image; do not guess or invent information that is not in the image.
- Answer directly, concretely and briefly, staying on the question itself.
- When the user asks for structured output (for example a specified JSON format or key names), output exactly that format with no extra explanatory text.
- When the user cares about one region only, focus the answer on that region and do not discuss other regions."""

# Geometric detection constants
# sibling_intersection exclusion rule: legitimate float wrapping between
# LytDocumentFloat and a specific set of blocks is not reported.
# LytFloatAwareBlock wraps at full width, as the CSS float model intends;
# LytDocumentFloat.getBounds returns the inner visible bounds while its flow
# height is 0.
FLOAT_CLASS = "LytDocumentFloat"
FLOAT_EXCLUDED_CLASSES = ["LytParagraph", "LytHeading", "LytListBlock", "LytFloatAwareBlock"]

# Known benign classes for which the zero_size rule is demoted to info
# (supported by first-round measurement data)
ZERO_SIZE_BENIGN_CLASSES = ["LytThematicBreak"]  # LytItemImage removed 2026-07-29: zero-size was the R2-2 defect (D3), not benign
ZERO_SIZE_BENIGN_EVIDENCE_SUFFIX = " (known benign class, pending calibration)"

# geometric subcommand: file name timestamp extraction regex
TIMESTAMP_RE = re.compile(r'^(.+)_(\d{4}-\d{2}-\d{2}_\d{6})\.(png|json)$')

# Utility functions

def eprint(*args, **kwargs):
    """Print to stderr"""
    print(*args, file=sys.stderr, **kwargs)


def load_json(path):
    """Load a JSON file safely"""
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (json.JSONDecodeError, FileNotFoundError, IOError) as e:
        eprint(f"[warn] skipping unreadable JSON: {path} - {e}")
        return None


def save_json(path, data):
    """Save a JSON file"""
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def iou(a, b):
    """Compute the IoU of two bboxes [x,y,w,h]"""
    ax1, ay1, aw, ah = a
    bx1, by1, bw, bh = b
    ax2, ay2 = ax1 + aw, ay1 + ah
    bx2, by2 = bx1 + bw, by1 + bh

    ix1 = max(ax1, bx1)
    iy1 = max(ay1, by1)
    ix2 = min(ax2, bx2)
    iy2 = min(ay2, by2)

    iw = max(0, ix2 - ix1)
    ih = max(0, iy2 - iy1)
    inter = iw * ih

    area_a = aw * ah
    area_b = bw * bh
    union = area_a + area_b - inter
    if union <= 0:
        return 0.0
    return inter / union


def _contains(outer, inner):
    """Check whether outer bbox [x,y,w,h] fully contains inner
    (touching edges count as contained)"""
    ox, oy, ow, oh = outer
    ix, iy, iw, ih = inner
    return (ox <= ix and oy <= iy and
            ox + ow >= ix + iw and
            oy + oh >= iy + ih)


# .env parsing

def load_env(env_path):
    """
    Hand-written .env parsing: supports key=value, ignores # comments and
    blank lines. Returns a dict. python-dotenv is not allowed.
    """
    if not os.path.isfile(env_path):
        return None
    env = {}
    with open(env_path, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            if '=' not in line:
                continue
            key, _, val = line.partition('=')
            key = key.strip()
            val = val.strip()
            # strip optional surrounding quotes
            if len(val) >= 2 and val[0] == val[-1] and val[0] in ('"', "'"):
                val = val[1:-1]
            env[key] = val
    return env


# Geometric detection (geometric)

def build_parent_map(blocks):
    """
    Build a parent -> children map from a depth-ordered block list.
    Blocks are ordered depth-first / pre-order, so the parent is the closest
    depth-1 ancestor.
    Returns: {parent_idx: [child_block, ...]}, parent_idx=-1 means root
    """
    # keep the most recent ancestor per depth
    ancestors = {}  # depth -> block
    parent_map = {}  # parent_idx -> [children]

    for blk in blocks:
        d = blk.get('depth', 1)
        pid = blk.get('i', -1)

        # determine the parent
        parent = None
        if d > 1:
            parent = ancestors.get(d - 1)

        parent_key = parent['i'] if parent is not None else -1
        parent_map.setdefault(parent_key, []).append(blk)
        ancestors[d] = blk

    return parent_map


def _has_non_zero_descendant(blocks, pos):
    """
    Check whether blocks[pos] has a descendant with a non-zero bbox
    (w>0 and h>0).
    Descendant test: the consecutive run of nodes after pos whose depth is
    greater than blocks[pos].depth.
    """
    if pos >= len(blocks) - 1:
        return False
    parent_depth = blocks[pos].get('depth', 1)
    for i in range(pos + 1, len(blocks)):
        child_depth = blocks[i].get('depth', 1)
        if child_depth <= parent_depth:
            break
        w = blocks[i].get('w', 0)
        h = blocks[i].get('h', 0)
        if w > 0 and h > 0:
            return True
    return False


def run_geometric(shots_dir, page_width, out_path):
    """
    Run the geometric checks:
    a) overflow beyond page width (x+w > page_width + 2)
    b) zero size (w<=0 or h<=0)
    c) off-page bounds (x<0 or y<0)
    d) sibling intersection (IoU > 0.05 between direct children of one parent)
    """
    # collect all PNG/JSON files, grouped by stem with the newest timestamp
    png_by_stem = {}  # stem -> (path, ts)
    json_by_stem = {}  # stem -> (path, ts)
    for fname in os.listdir(shots_dir):
        m = TIMESTAMP_RE.match(fname)
        if not m:
            continue
        stem = m.group(1)
        ts = m.group(2)
        ext = m.group(3)
        path = os.path.join(shots_dir, fname)
        if ext == 'png':
            # take the PNG with the newest timestamp (compare index [1], the
            # ts, not index [0], the path string)
            if stem not in png_by_stem or ts > png_by_stem[stem][1]:
                png_by_stem[stem] = (path, ts)
        else:
            if stem not in json_by_stem or ts > json_by_stem[stem][1]:
                json_by_stem[stem] = (path, ts)

    # pair by stem
    png_files = []
    for stem in png_by_stem:
        if stem not in json_by_stem:
            eprint(f"[warn] skipping {stem}: no matching JSON")
            continue
        png_files.append((png_by_stem[stem][0], json_by_stem[stem][0], stem))

    eprint(f"[info] found {len(png_files)} PNGs with a bounds JSON (deduplicated by stem)")

    all_pages = []
    total_findings = 0

    for png_path, json_path, page_name in png_files:
        blocks = load_json(json_path)
        if blocks is None or not isinstance(blocks, list):
            eprint(f"[warn] skipping {page_name}: invalid bounds JSON")
            continue

        page_findings = []

        # checks (a)(b)(c): block by block
        for pos, blk in enumerate(blocks):
            x = blk.get('x', 0)
            y = blk.get('y', 0)
            w = blk.get('w', 0)
            h = blk.get('h', 0)
            cls = blk.get('cls', '')
            idx = blk.get('i', -1)

            # (a) overflow beyond the page width
            if x + w > page_width + 2:
                page_findings.append({
                    "page": page_name,
                    "rule": "overflow_width",
                    "bbox": [x, y, w, h],
                    "severity": "error",
                    "evidence": f"block #{idx} ({cls}) x+w={x+w} > page_width={page_width}"
                })

            # (b) zero size
            if w <= 0 or h <= 0:
                severity = "error"
                evidence = f"block #{idx} ({cls}) size w={w}, h={h}"
                if cls in ZERO_SIZE_BENIGN_CLASSES:
                    severity = "info"
                    evidence += ZERO_SIZE_BENIGN_EVIDENCE_SUFFIX
                elif not _has_non_zero_descendant(blocks, pos):
                    # no non-zero descendant -> legitimate empty container,
                    # do not report
                    continue
                page_findings.append({
                    "page": page_name,
                    "rule": "zero_size",
                    "bbox": [x, y, w, h],
                    "severity": severity,
                    "evidence": evidence
                })

            # (c) off-page bounds
            if x < 0 or y < 0:
                page_findings.append({
                    "page": page_name,
                    "rule": "off_page",
                    "bbox": [x, y, w, h],
                    "severity": "warn",
                    "evidence": f"block #{idx} ({cls}) coordinates x={x}, y={y}"
                })

        # (d) sibling intersection
        parent_map = build_parent_map(blocks)
        for parent_key, siblings in parent_map.items():
            n = len(siblings)
            for i in range(n):
                for j in range(i + 1, n):
                    a = siblings[i]
                    b = siblings[j]
                    bbox_a = [a['x'], a['y'], a['w'], a['h']]
                    bbox_b = [b['x'], b['y'], b['w'], b['h']]
                    # skip zero-size blocks
                    if a['w'] <= 0 or a['h'] <= 0 or b['w'] <= 0 or b['h'] <= 0:
                        continue
                    # skip legitimate float wrapping where one side is
                    # LytDocumentFloat and the other is a text class block
                    a_cls = a.get('cls', '')
                    b_cls = b.get('cls', '')
                    if (a_cls == FLOAT_CLASS and b_cls in FLOAT_EXCLUDED_CLASSES) or \
                       (b_cls == FLOAT_CLASS and a_cls in FLOAT_EXCLUDED_CLASSES):
                        continue
                    # ancestor-descendant exemption: when the shallower block
                    # fully contains the deeper one they are ancestor and
                    # descendant (intermediate wrappers are skipped), so skip
                    # and do not report
                    a_depth = a.get('depth', 1)
                    b_depth = b.get('depth', 1)
                    if a_depth != b_depth:
                        if (_contains(bbox_a, bbox_b) and a_depth < b_depth) or \
                           (_contains(bbox_b, bbox_a) and b_depth < a_depth):
                            continue
                    overlap = iou(bbox_a, bbox_b)
                    if overlap > 0.05:
                        # sort by block id to avoid reporting A intersect B and
                        # B intersect A twice
                        if a['i'] <= b['i']:
                            primary, secondary = a, b
                        else:
                            primary, secondary = b, a
                        page_findings.append({
                            "page": page_name,
                            "rule": "sibling_intersection",
                            "bbox": [primary['x'], primary['y'], primary['w'], primary['h']],
                            "severity": "warn",
                            "evidence": f"block #{primary['i']} ({primary['cls']}) intersects #{secondary['i']} ({secondary['cls']}) IoU={overlap:.3f}"
                        })

        total_findings += len(page_findings)
        all_pages.append({
            "page": page_name,
            "source": "geometric",
            "findings": page_findings
        })

    # build the output
    output = {
        "tool": "test/visual/tools/screen.py",
        "subcommand": "geometric",
        "page_width": page_width,
        "total_pages": len(all_pages),
        "total_findings": total_findings,
        "pages": all_pages
    }

    save_json(out_path, output)
    eprint(f"[info] geometric detection done: {len(all_pages)} pages, {total_findings} findings")
    eprint(f"[info] output -> {out_path}")

    # print the summary to stdout
    print(json.dumps({
        "pages_processed": len(all_pages),
        "findings_count": total_findings,
        "output": out_path
    }))

    return 0


# VLM detection

def encode_image_png(pil_image):
    """PIL Image -> base64 PNG"""
    buf = BytesIO()
    pil_image.save(buf, format='PNG')
    return base64.b64encode(buf.getvalue()).decode('ascii')


def tile_image(pil_image, tile_h, overlap):
    """
    Slice the image into full-width tiles of height tile_h with overlap
    overlap.
    Returns: [(tile_index, tile_pil_image, offset_y)]
    """
    if overlap >= tile_h:
        raise ValueError(
            f"overlap ({overlap}) must be smaller than tile_h ({tile_h}), "
            f"otherwise the loop never ends"
        )
    width, height = pil_image.size
    tiles = []
    y = 0
    idx = 0
    while y < height:
        top = y
        bottom = min(y + tile_h, height)
        tile = pil_image.crop((0, top, width, bottom))
        tiles.append((idx, tile, top))
        idx += 1
        y += tile_h - overlap
        if y >= height:
            break
    return tiles


def call_vlm_api(api_key, base_url, model, image_b64, timeout, prompt_text,
                 system_prompt=VLM_SYSTEM_PROMPT, raw_out=None, require_json=True):
    """
    Call an OpenAI-compatible VLM API.
    Returns: (result, error_detail). On success result is the parsed JSON dict
    and error_detail is None; on failure result is None and error_detail is the
    error description string (distinguishing an API call failure from a response
    parse failure).

    Optional parameters (used by the ask subcommand; the defaults keep the
    original vlm behaviour unchanged):
    - system_prompt: overrides the system prompt (default VLM_SYSTEM_PROMPT)
    - raw_out: when a dict is passed, the verbatim model answer is written to
      raw_out['content']
    - require_json=False: a non-JSON answer is not treated as a failure
      (returns (None, None)); the verbatim text is still written to raw_out
    """
    url = f"{base_url}/chat/completions".rstrip('/')
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": system_prompt},
            {
                "role": "user",
                "content": [
                    {"type": "text", "text": prompt_text},
                    {"type": "image_url", "image_url": {
                        "url": f"data:image/png;base64,{image_b64}"
                    }}
                ]
            }
        ],
        "max_tokens": 1024,
        "temperature": 0.1
    }
    data = json.dumps(payload).encode('utf-8')

    retries = [1, 4, 16]  # exponential backoff, only for 429, 5xx and network errors

    for attempt in range(1 + len(retries)):
        try:
            req = urllib.request.Request(url, data=data, headers=headers, method='POST')
            resp = urllib.request.urlopen(req, timeout=timeout)
            resp_body = resp.read().decode('utf-8')
            resp_json = json.loads(resp_body)
            # extract content
            choices = resp_json.get('choices', [])
            if not choices:
                preview = resp_body[:200]
                eprint(f"[warn] API returned no choices")
                return (None, f"API call failed: response has no choices, raw response: {preview}")
            content = choices[0].get('message', {}).get('content', '')
            if raw_out is not None:
                raw_out['content'] = content
            result = extract_json_block(content)
            if result is None:
                if require_json:
                    preview = content[:200]
                    return (None, f"response parse failed: {preview}")
                # require_json=False: a non-JSON answer is not a failure (ask
                # case); the verbatim text is already in raw_out
                return (None, None)
            return (result, None)
        except urllib.error.HTTPError as e:
            code = e.code
            body_preview = ""
            try:
                body_preview = e.read().decode('utf-8', errors='replace')[:200]
            except Exception:
                pass
            err_msg = f"API call failed: HTTP {code} {body_preview}"
            # retry only 429 and 5xx; fail other 4xx immediately
            if code == 429 or code >= 500:
                if attempt < len(retries):
                    wait = retries[attempt]
                    eprint(f"[warn] API call failed (attempt {attempt+1}): {e}, waiting {wait}s to retry")
                    time.sleep(wait)
                    continue
                eprint(f"[error] {err_msg}")
                return (None, err_msg)
            eprint(f"[error] {err_msg}")
            return (None, err_msg)
        except (urllib.error.URLError, TimeoutError, OSError) as e:
            err_msg = f"API call failed: {e}"
            if attempt < len(retries):
                wait = retries[attempt]
                eprint(f"[warn] API call failed (attempt {attempt+1}): {e}, waiting {wait}s to retry")
                time.sleep(wait)
                continue
            eprint(f"[error] {err_msg}")
            return (None, err_msg)
        except json.JSONDecodeError as e:
            err_msg = f"API call failed: response body is not valid JSON: {e}"
            eprint(f"[error] {err_msg}")
            return (None, err_msg)

    return (None, "API call failed: unknown error")


def extract_json_block(text):
    """
    Leniently extract the first complete JSON object block.
    The model may wrap it in ```json ... ``` markers.
    """
    if not text:
        return None

    # try to extract a ```json ... ``` block
    m = re.search(r'```(?:json)?\s*\n?({.*?})\s*\n?```', text, re.DOTALL)
    if m:
        try:
            return json.loads(m.group(1))
        except json.JSONDecodeError:
            pass

    # try to extract the first { ... }
    brace_start = text.find('{')
    brace_end = text.rfind('}')
    if brace_start >= 0 and brace_end > brace_start:
        try:
            return json.loads(text[brace_start:brace_end + 1])
        except json.JSONDecodeError:
            preview = text[:200]
            eprint(f"[warn] tile response JSON parse failed: {preview}")
            pass

    return None


def _normalize_finding(f):
    """
    Normalize the finding key names returned by the model.
    Measured behaviour: qwen models occasionally drift on key names
    (type->class, description->evidence, bbox_2d->bbox, and so on).
    Mapping rule: map only when the target key is absent; a bbox_2d in
    four-point form is converted to [x,y,w,h].
    """
    f = dict(f)  # do not modify the original dict

    # type -> class
    if 'class' not in f and 'type' in f:
        f['class'] = f.pop('type')

    # description -> evidence
    if 'evidence' not in f and 'description' in f:
        f['evidence'] = f.pop('description')

    # box -> bbox
    if 'bbox' not in f and 'box' in f:
        f['bbox'] = f.pop('box')

    # bbox_2d -> bbox (four-point [x1,y1,x2,y2] converted to [x,y,w,h])
    if 'bbox' not in f and 'bbox_2d' in f:
        raw = f.pop('bbox_2d')
        if isinstance(raw, (list, tuple)) and len(raw) == 4:
            x1, y1, x2, y2 = raw
            if x2 > x1 and y2 > y1:
                f['bbox'] = [x1, y1, x2 - x1, y2 - y1]
            else:
                f['bbox'] = list(raw)
        else:
            f['bbox'] = raw

    return f


def process_tile(args):
    """Process one tile (used by the thread pool)"""
    api_key, base_url, model, timeout, prompt_text = args['cfg']
    tile_img = args['tile_img']
    tile_idx = args['tile_idx']
    offset_y = args['offset_y']
    page_name = args['page_name']

    try:
        b64 = encode_image_png(tile_img)
        result, error_detail = call_vlm_api(api_key, base_url, model, b64, timeout, prompt_text)
        if error_detail:
            return {
                "page": page_name,
                "source": "vlm",
                "tile_index": tile_idx,
                "findings": [],
                "error": f"tile {tile_idx} {error_detail}"
            }

        findings = result.get('findings', [])
        # normalize key names: defend against the model ignoring the key
        # constraints (qwen models drift on key names occasionally)
        findings = [_normalize_finding(f) for f in findings]
        # coordinate conversion: y inside the tile -> y on the whole page
        converted = []
        for f in findings:
            bbox = f.get('bbox', [0, 0, 0, 0])
            bbox[1] += offset_y  # add the tile offset to y
            converted.append({
                "bbox": bbox,
                "class": f.get('class', 'unknown'),
                "severity": f.get('severity', 'info'),
                "confidence": f.get('confidence', 0.0),
                "evidence": f.get('evidence', ''),
                "tile_index": tile_idx
            })

        return {
            "page": page_name,
            "source": "vlm",
            "tile_index": tile_idx,
            "findings": converted,
            "error": None
        }
    except Exception as e:
        eprint(f"[error] tile {tile_idx} processing exception: {e}")
        return {
            "page": page_name,
            "source": "vlm",
            "tile_index": tile_idx,
            "findings": [],
            "error": str(e)
        }


def deduplicate_findings(findings, iou_threshold=0.5):
    """
    Cross-tile deduplication: same class and bbox IoU > threshold are merged,
    keeping the one with the higher confidence.
    """
    if not findings:
        return []

    # group by class
    by_class = {}
    for f in findings:
        cls = f.get('class', 'unknown')
        by_class.setdefault(cls, []).append(f)

    deduped = []
    for cls, items in by_class.items():
        # sort by confidence descending
        items.sort(key=lambda x: x.get('confidence', 0), reverse=True)
        kept = []
        for item in items:
            is_dup = False
            for k in kept:
                if iou(item['bbox'], k['bbox']) > iou_threshold:
                    is_dup = True
                    break
            if not is_dup:
                kept.append(item)
        deduped.extend(kept)

    return deduped


def run_vlm(shots_dir, pages_filter, model_override, tile_h, overlap, dry_run, out_path):
    """
    Run the VLM first pass.
    """
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
    env = load_env(env_path)

    if env is None:
        eprint("[fatal] no .env file found")
        eprint(f"[fatal] copy {os.path.dirname(os.path.abspath(__file__))}/.env.example to .env and fill in DASHSCOPE_API_KEY")
        sys.exit(1)

    api_key = env.get('DASHSCOPE_API_KEY', '').strip()

    # dry-run mode does not need a real key
    if not dry_run:
        if not api_key or api_key.startswith('sk-your-key') or api_key == 'dummy':
            eprint("[fatal] DASHSCOPE_API_KEY is not configured properly")
            eprint(f"[fatal] edit {env_path} and fill in a real API key")
            sys.exit(1)

    base_url = env.get('VLM_BASE_URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1').rstrip('/')
    model = model_override or env.get('VLM_MODEL', 'qwen2.5-vl-32b-instruct')
    concurrency = int(env.get('VLM_CONCURRENCY', '4'))
    timeout = int(env.get('VLM_TIMEOUT', '120'))

    # tile parameter guard (avoid an endless loop)
    if overlap >= tile_h:
        eprint(f"[fatal] overlap ({overlap}) must be smaller than tile_h ({tile_h}), otherwise the loop never ends")
        sys.exit(1)

    # collect files
    png_files = []
    for fname in os.listdir(shots_dir):
        if fname.lower().endswith('.png'):
            png_path = os.path.join(shots_dir, fname)
            base = os.path.splitext(fname)[0]
            # page name = the name without the timestamp
            page_name = re.sub(r'_\d{4}-\d{2}-\d{2}_\d{6}$', '', base)

            if pages_filter:
                if page_name not in pages_filter:
                    continue

            png_files.append((png_path, page_name))

    if not png_files:
        eprint("[fatal] no matching PNG file")
        sys.exit(1)

    # Dry-run: only print information
    if dry_run:
        eprint(f"[dry-run] model: {model}")
        eprint(f"[dry-run] Base URL: {base_url}")
        prompt_len = len(VLM_USER_TEXT) + len(VLM_SYSTEM_PROMPT)
        eprint(f"[dry-run] prompt length: {prompt_len} characters")
        eprint(f"[dry-run] concurrency: {concurrency}, timeout: {timeout}s")
        eprint(f"[dry-run] matched {len(png_files)} pages:")

        total_tiles = 0
        for png_path, page_name in png_files:
            try:
                from PIL import Image
                img = Image.open(png_path)
                width, height = img.size
                tiles = tile_image(img, tile_h, overlap)
                total_tiles += len(tiles)
                for idx, tile, off_y in tiles:
                    buf = BytesIO()
                    tile.save(buf, format='PNG')
                    b64_size = len(base64.b64encode(buf.getvalue()).decode('ascii'))
                    req_body = json.dumps({
                        "model": model,
                        "messages": [
                            {"role": "system", "content": VLM_SYSTEM_PROMPT},
                            {"role": "user", "content": [
                                {"type": "text", "text": VLM_USER_TEXT},
                                {"type": "image_url", "image_url": {"url": f"data:image/png;base64,..."}}
                            ]}
                        ],
                        "max_tokens": 1024,
                        "temperature": 0.1
                    })
                    eprint(f"[dry-run]   page {page_name} tile {idx}: y={off_y}-{off_y+tile.size[1]}, "
                           f"request body ~{len(req_body)} bytes, base64 ~{b64_size} bytes")
            except Exception as e:
                eprint(f"[warn] cannot open {png_path}: {e}")

        eprint(f"[dry-run] total: {len(png_files)} pages, {total_tiles} tiles")
        print(json.dumps({"dry_run": True, "pages": len(png_files), "tiles": total_tiles}))
        return 0

    # actual run
    from PIL import Image

    all_findings = []
    all_errors = []

    for png_path, page_name in png_files:
        try:
            img = Image.open(png_path)
        except Exception as e:
            eprint(f"[warn] cannot open {png_path}: {e}")
            all_errors.append({
                "page": page_name,
                "source": "vlm",
                "error": f"cannot open image: {e}"
            })
            continue

        tiles = tile_image(img, tile_h, overlap)
        eprint(f"[info] processing page {page_name}: {len(tiles)} tiles")

        cfg = (api_key, base_url, model, timeout, VLM_USER_TEXT)

        tasks = []
        for idx, tile, off_y in tiles:
            tasks.append({
                'cfg': cfg,
                'tile_img': tile,
                'tile_idx': idx,
                'offset_y': off_y,
                'page_name': page_name
            })

        page_findings = []
        with ThreadPoolExecutor(max_workers=concurrency) as executor:
            futures = {executor.submit(process_tile, t): t for t in tasks}
            for fut in as_completed(futures):
                result = fut.result()
                if result['error']:
                    all_errors.append({
                        "page": result['page'],
                        "source": "vlm",
                        "tile_index": result['tile_index'],
                        "error": result['error']
                    })
                page_findings.extend(result['findings'])

        # deduplicate
        deduped = deduplicate_findings(page_findings, iou_threshold=0.5)
        for f in deduped:
            all_findings.append({
                "page": page_name,
                "source": "vlm",
                "rule": f.get('class', 'unknown'),
                "bbox": f['bbox'],
                "severity": f.get('severity', 'info'),
                "confidence": f.get('confidence', 0.0),
                "evidence": f.get('evidence', '')
            })

    output = {
        "tool": "test/visual/tools/screen.py",
        "subcommand": "vlm",
        "model": model,
        "total_pages": len(png_files),
        "total_findings": len(all_findings),
        "findings": all_findings,
        "errors": all_errors
    }

    save_json(out_path, output)
    eprint(f"[info] VLM detection done: {len(all_findings)} findings, {len(all_errors)} errors")
    eprint(f"[info] output -> {out_path}")

    print(json.dumps({
        "pages_processed": len(png_files),
        "findings_count": len(all_findings),
        "errors_count": len(all_errors),
        "output": out_path
    }))

    return 0


# Ask - lightweight targeted visual question answering

def run_ask(files, prompt, model_override, dry_run, out_path):
    """
    Lightweight targeted visual question answering: name specific PNG files
    plus a custom question, feed the whole image straight to the VLM without
    tiling.
    The fundamental differences from the vlm first pass: whole image fed
    directly / custom prompt / free-form answer with best-effort structure.
    """
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
    env = load_env(env_path)

    if env is None:
        eprint("[fatal] no .env file found")
        eprint(f"[fatal] copy {os.path.dirname(os.path.abspath(__file__))}/.env.example to .env and fill in DASHSCOPE_API_KEY")
        sys.exit(1)

    api_key = env.get('DASHSCOPE_API_KEY', '').strip()

    # dry-run mode does not need a real key (same key validation logic as run_vlm)
    if not dry_run:
        if not api_key or api_key.startswith('sk-your-key') or api_key == 'dummy':
            eprint("[fatal] DASHSCOPE_API_KEY is not configured properly")
            eprint(f"[fatal] edit {env_path} and fill in a real API key")
            sys.exit(1)

    base_url = env.get('VLM_BASE_URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1').rstrip('/')
    model = model_override or env.get('VLM_MODEL', 'qwen2.5-vl-32b-instruct')
    timeout = int(env.get('VLM_TIMEOUT', '120'))

    if not files:
        eprint("[fatal] no file specified")
        sys.exit(1)

    from PIL import Image

    # Dry-run: only print the files that would be called and the request body
    # size, no HTTP request
    if dry_run:
        eprint(f"[dry-run] model: {model}")
        eprint(f"[dry-run] Base URL: {base_url}")
        prompt_len = len(prompt) + len(ASK_SYSTEM_PROMPT)
        eprint(f"[dry-run] prompt length: {prompt_len} characters (system {len(ASK_SYSTEM_PROMPT)} + user {len(prompt)})")
        eprint(f"[dry-run] matched {len(files)} files:")

        for path in files:
            try:
                img = Image.open(path)
                width, height = img.size
                area = width * height
                if area > 6_000_000:
                    eprint(f"[warn] {path} image area {width}x{height}={area} pixels exceeds 6,000,000; "
                           f"detail may suffer, consider cropping a region or using the vlm first pass")
                buf = BytesIO()
                img.save(buf, format='PNG')
                b64_size = len(base64.b64encode(buf.getvalue()).decode('ascii'))
                req_body = json.dumps({
                    "model": model,
                    "messages": [
                        {"role": "system", "content": ASK_SYSTEM_PROMPT},
                        {"role": "user", "content": [
                            {"type": "text", "text": prompt},
                            {"type": "image_url", "image_url": {"url": "data:image/png;base64,..."}}
                        ]}
                    ],
                    "max_tokens": 1024,
                    "temperature": 0.1
                })
                eprint(f"[dry-run]   {path}: size {width}x{height}, area {area}, "
                       f"request body ~{len(req_body)} bytes, base64 ~{b64_size} bytes")
            except Exception as e:
                eprint(f"[warn] cannot open {path}: {e}")

        print(json.dumps({"dry_run": True, "files": len(files)}))
        return 0

    # actual run: each file is called independently and processed sequentially
    # (no concurrency)
    answers = []
    errors = []

    for path in files:
        try:
            img = Image.open(path)
        except Exception as e:
            err = f"cannot open image: {e}"
            eprint(f"[warn] {err}: {path}")
            answers.append({"file": path, "answer": None, "findings": [], "error": err})
            errors.append({"file": path, "error": err})
            continue

        width, height = img.size
        area = width * height
        if area > 6_000_000:
            eprint(f"[warn] {path} image area {width}x{height}={area} pixels exceeds 6,000,000; "
                   f"detail may suffer, consider cropping a region or using the vlm first pass")

        eprint(f"[info] processing file {path} ({width}x{height})")
        try:
            b64 = encode_image_png(img)
        except Exception as e:
            err = f"image encoding failed: {e}"
            eprint(f"[warn] {err}: {path}")
            answers.append({"file": path, "answer": None, "findings": [], "error": err})
            errors.append({"file": path, "error": err})
            continue

        raw_out = {}
        result, error_detail = call_vlm_api(
            api_key, base_url, model, b64, timeout, prompt,
            system_prompt=ASK_SYSTEM_PROMPT, raw_out=raw_out, require_json=False
        )
        if error_detail:
            eprint(f"[warn] {path} call failed: {error_detail}")
            answers.append({"file": path, "answer": None, "findings": [], "error": error_detail})
            errors.append({"file": path, "error": error_detail})
            continue

        answer = raw_out.get('content', '')
        findings = []
        # best effort: take the findings array out of the result extracted by
        # extract_json_block
        # when the model produced structured JSON with findings as requested it
        # is extracted; otherwise the array stays empty
        if isinstance(result, dict):
            extracted = result.get('findings', [])
            if isinstance(extracted, list):
                findings = [_normalize_finding(f) for f in extracted if isinstance(f, dict)]
            # result is JSON but has no findings key: the whole JSON is already
            # preserved verbatim in answer, so findings stays empty

        answers.append({"file": path, "answer": answer, "findings": findings, "error": None})

    output = {
        "tool": "test/visual/tools/screen.py",
        "subcommand": "ask",
        "model": model,
        "files": list(files),
        "prompt": prompt,
        "answers": answers,
        "errors": errors
    }

    if out_path:
        save_json(out_path, output)
        eprint(f"[info] ask done: {len(answers)} files, {len(errors)} errors")
        eprint(f"[info] output -> {out_path}")
        print(json.dumps({
            "files_processed": len(files),
            "errors_count": len(errors),
            "output": out_path
        }))
    else:
        print(json.dumps(output, ensure_ascii=False, indent=2))

    return 0


# Report merging

SEVERITY_ORDER = {'error': 0, 'warn': 1, 'info': 2}


def severity_key(f):
    return SEVERITY_ORDER.get(f.get('severity', 'info'), 99)


def run_report(inputs, out_path):
    """
    Merge findings from several sources, group by page and sort by severity.
    """
    all_findings = []
    total_inputs = 0

    for inp in inputs:
        data = load_json(inp)
        if data is None:
            eprint(f"[warn] skipping unreadable input file: {inp}")
            continue
        total_inputs += 1

        # two supported shapes: {pages: [...]} or {findings: [...]}, or a
        # top-level findings array
        pages = data.get('pages', None)
        if pages is not None:
            for p in pages:
                pf = p.get('findings', [])
                for f in pf:
                    f['_source_file'] = inp
                    all_findings.append(f)
        else:
            findings = data.get('findings', None)
            if findings is not None:
                for f in findings:
                    f['_source_file'] = inp
                    all_findings.append(f)
            elif isinstance(data, list):
                for f in data:
                    if isinstance(f, dict) and 'page' in f:
                        f['_source_file'] = inp
                        all_findings.append(f)

    # group by page
    by_page = {}
    for f in all_findings:
        page = f.get('page', 'unknown')
        by_page.setdefault(page, []).append(f)

    # sort by severity
    for page in by_page:
        by_page[page].sort(key=severity_key)

    # build the output
    summary = {}
    for page, findings in by_page.items():
        counts = {'error': 0, 'warn': 0, 'info': 0}
        for f in findings:
            sev = f.get('severity', 'info')
            counts[sev] = counts.get(sev, 0) + 1
        summary[page] = counts

    output = {
        "tool": "test/visual/tools/screen.py",
        "subcommand": "report",
        "sources": inputs,
        "total_pages": len(by_page),
        "total_findings": len(all_findings),
        "summary": summary,
        "pages": {page: {
            "findings": by_page[page],
            "counts": summary[page]
        } for page in by_page}
    }

    save_json(out_path, output)

    # print the per-page count table to stdout
    print(f"Report - merged {len(inputs)} sources")
    print(f"{'Page':<45} {'Error':>6} {'Warn':>6} {'Info':>6}")
    print("-" * 65)
    sorted_pages = sorted(by_page.keys())
    for page in sorted_pages:
        c = summary[page]
        display = page[:44] if len(page) > 44 else page
        print(f"{display:<45} {c['error']:>6} {c['warn']:>6} {c['info']:>6}")
    print("-" * 65)
    print(f"{'TOTAL':<45} "
          f"{sum(c['error'] for c in summary.values()):>6} "
          f"{sum(c['warn'] for c in summary.values()):>6} "
          f"{sum(c['info'] for c in summary.values()):>6}")

    print(json.dumps({
        "total_pages": len(by_page),
        "total_findings": len(all_findings),
        "output": out_path
    }))

    return 0


# Main entry point

def main():
    # Fix garbled CJK output on the Windows GBK console
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='Visual Inspection Screener'
    )
    subparsers = parser.add_subparsers(dest='command', help='subcommand')

    # --- geometric ---
    p_geo = subparsers.add_parser('geometric', help='layer 0 mechanical checks')
    p_geo.add_argument('--shots', required=True, help='screenshot directory (holding *.png plus the same-named *.json bounds)')
    p_geo.add_argument('--page-width', type=int, default=1800, help='reference page width (px)')
    p_geo.add_argument('--out', required=True, help='output JSON path')

    # --- vlm ---
    p_vlm = subparsers.add_parser('vlm', help='layer 1 multimodal first pass')
    p_vlm.add_argument('--shots', required=True, help='screenshot directory')
    p_vlm.add_argument('--pages', help='page name filter (comma separated)')
    p_vlm.add_argument('--model', help='overrides VLM_MODEL from .env')
    p_vlm.add_argument('--tile-h', type=int, default=1400, help='tile height (px)')
    p_vlm.add_argument('--overlap', type=int, default=200, help='tile overlap (px)')
    p_vlm.add_argument('--dry-run', action='store_true', help='no HTTP request, only print information')
    p_vlm.add_argument('--out', required=True, help='output JSON path')

    # --- ask ---
    p_ask = subparsers.add_parser('ask', help='lightweight targeted visual question answering: named files plus a custom question, whole image fed directly')
    p_ask.add_argument('--files', required=True, action='append',
                       help='PNG file paths (repeat the flag or separate several with commas)')
    p_ask.add_argument('--prompt', required=True, help='targeted question text (chosen by the caller)')
    p_ask.add_argument('--out', help='output JSON path (printed to stdout when omitted)')
    p_ask.add_argument('--model', help='overrides VLM_MODEL from .env')
    p_ask.add_argument('--dry-run', action='store_true', help='no HTTP request, only print information')

    # --- report ---
    p_rep = subparsers.add_parser('report', help='merged report')
    p_rep.add_argument('--inputs', required=True, help='input JSON paths (comma separated)')
    p_rep.add_argument('--out', required=True, help='output triage JSON path')

    args = parser.parse_args()

    if args.command == 'geometric':
        return run_geometric(args.shots, args.page_width, args.out)

    elif args.command == 'vlm':
        pages_filter = None
        if args.pages:
            pages_filter = {p.strip() for p in args.pages.split(',') if p.strip()}
        return run_vlm(
            args.shots, pages_filter, args.model,
            args.tile_h, args.overlap, args.dry_run, args.out
        )

    elif args.command == 'ask':
        files = []
        for part in args.files:
            for f in part.split(','):
                f = f.strip()
                if f:
                    files.append(f)
        return run_ask(files, args.prompt, args.model, args.dry_run, args.out)

    elif args.command == 'report':
        inputs = [s.strip() for s in args.inputs.split(',') if s.strip()]
        return run_report(inputs, args.out)

    else:
        parser.print_help()
        return 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        eprint("[info] interrupted by user")
        sys.exit(130)
    except Exception as e:
        eprint(f"[fatal] unhandled exception: {e}")
        sys.exit(1)
