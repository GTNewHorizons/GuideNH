#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
视觉初筛工具 (Visual Inspection Screener)
三层功能：geometric / vlm / report
依赖：Pillow + Python 3 标准库
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

# ============================================================
# 常量 —— 提示词可在此处迭代
# ============================================================

VLM_SYSTEM_PROMPT = """你是一个排版缺陷检测员。分析图像中的排版与渲染问题。

请检查以下问题类别：
1. 文字重叠 - 不同文本块互相重叠
2. 文字溢出容器 - 文本超出其背景容器边界
3. 元素错位 - 元素未对齐或位置异常
4. 异常空白 - 不应有的空白区域
5. 图像或3D场景破损 - 图片/3D渲染显示异常(花屏/黑块/缺失)
6. 字形渲染异常 - 文字显示不全、乱码或字形错误
7. 裁剪截断 - 内容被边缘截断
8. 其他 - 其他排版缺陷

严格要求只输出JSON格式，不要包含任何其他文字说明。
输出格式: {"findings": [{"bbox":[x,y,w,h], "class":"问题类别", "severity":"error|warn|info", "confidence":0-1, "evidence":"一句话描述"}]}
无问题则输出: {"findings": []}
注意：bbox坐标使用图像内的像素坐标。"""

VLM_USER_TEXT = "请检测此图像区域的排版和渲染缺陷。"

# ---- 几何检测常量 ----
# sibling_intersection 排除规则：LytDocumentFloat 与文本类块对的合法环绕重叠不报告
FLOAT_CLASS = "LytDocumentFloat"
FLOAT_EXCLUDED_TEXT_CLASSES = ["LytParagraph", "LytHeading", "LytListBlock"]

# zero_size 规则降级为 info 的已知良性类（首轮实测数据支撑）
ZERO_SIZE_BENIGN_CLASSES = ["LytThematicBreak", "LytItemImage"]
ZERO_SIZE_BENIGN_EVIDENCE_SUFFIX = " (已知良性类，待校准确认)"

# ============================================================
# 工具函数
# ============================================================

def eprint(*args, **kwargs):
    """打印到 stderr"""
    print(*args, file=sys.stderr, **kwargs)


def load_json(path):
    """安全加载 JSON 文件"""
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (json.JSONDecodeError, FileNotFoundError, IOError) as e:
        eprint(f"[warn] 跳过无法读取的 JSON: {path} — {e}")
        return None


def save_json(path, data):
    """保存 JSON 文件"""
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


def iou(a, b):
    """计算两个 bbox [x,y,w,h] 的 IoU"""
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


# ============================================================
# .env 解析
# ============================================================

def load_env(env_path):
    """
    手写 .env 解析，支持 key=value，忽略 # 注释和空行。
    返回 dict。禁止 python-dotenv。
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
            # 去掉可能包裹的引号
            if len(val) >= 2 and val[0] == val[-1] and val[0] in ('"', "'"):
                val = val[1:-1]
            env[key] = val
    return env


# ============================================================
# 几何检测 (geometric)
# ============================================================

def build_parent_map(blocks):
    """
    从 depth 有序的块列表构建 parent -> children 映射。
    块按深度优先/先序遍历排列，parent 是最近的 depth-1 祖先。
    返回: {parent_idx: [child_block, ...]}，parent_idx=-1 表示根
    """
    # 维护每个 depth 的最近祖先
    ancestors = {}  # depth -> block
    parent_map = {}  # parent_idx -> [children]

    for blk in blocks:
        d = blk.get('depth', 1)
        pid = blk.get('i', -1)

        # 确定 parent
        parent = None
        if d > 1:
            parent = ancestors.get(d - 1)

        parent_key = parent['i'] if parent is not None else -1
        parent_map.setdefault(parent_key, []).append(blk)
        ancestors[d] = blk

    return parent_map


def run_geometric(shots_dir, page_width, out_path):
    """
    执行几何检测：
    a) 溢出页宽 (x+w > page_width + 2)
    b) 零尺寸 (w<=0 or h<=0)
    c) 出页边界 (x<0 or y<0)
    d) 兄弟相交 (同 parent 的直接子节点 IoU > 0.05)
    """
    # 搜集所有 PNG 文件
    png_files = []
    for fname in os.listdir(shots_dir):
        if fname.lower().endswith('.png'):
            base = os.path.splitext(fname)[0]
            json_path = os.path.join(shots_dir, base + '.json')
            if os.path.isfile(json_path):
                png_files.append((os.path.join(shots_dir, fname), json_path, base))

    eprint(f"[info] 找到 {len(png_files)} 个带 bounds JSON 的 PNG")

    all_pages = []
    total_findings = 0

    for png_path, json_path, base_name in png_files:
        blocks = load_json(json_path)
        if blocks is None or not isinstance(blocks, list):
            eprint(f"[warn] 跳过 {base_name}: 无效的 bounds JSON")
            continue

        # 页面名 = 不含时间戳的部分
        # 格式如 "__mediawiki_category_charts--636861727473.md_2026-07-28_004636"
        # 尝试剥离末尾 _YYYY-MM-DD_HHMMSS
        page_name = re.sub(r'_\d{4}-\d{2}-\d{2}_\d{6}$', '', base_name)

        page_findings = []

        # --- 检查 (a)(b)(c)：逐块 ---
        for blk in blocks:
            x = blk.get('x', 0)
            y = blk.get('y', 0)
            w = blk.get('w', 0)
            h = blk.get('h', 0)
            cls = blk.get('cls', '')
            idx = blk.get('i', -1)

            # (a) 溢出页宽
            if x + w > page_width + 2:
                page_findings.append({
                    "page": page_name,
                    "rule": "overflow_width",
                    "bbox": [x, y, w, h],
                    "severity": "error",
                    "evidence": f"块 #{idx} ({cls}) x+w={x+w} > page_width={page_width}"
                })

            # (b) 零尺寸
            if w <= 0 or h <= 0:
                severity = "error"
                evidence = f"块 #{idx} ({cls}) 尺寸 w={w}, h={h}"
                if cls in ZERO_SIZE_BENIGN_CLASSES:
                    severity = "info"
                    evidence += ZERO_SIZE_BENIGN_EVIDENCE_SUFFIX
                page_findings.append({
                    "page": page_name,
                    "rule": "zero_size",
                    "bbox": [x, y, w, h],
                    "severity": severity,
                    "evidence": evidence
                })

            # (c) 出页边界
            if x < 0 or y < 0:
                page_findings.append({
                    "page": page_name,
                    "rule": "off_page",
                    "bbox": [x, y, w, h],
                    "severity": "warn",
                    "evidence": f"块 #{idx} ({cls}) 坐标 x={x}, y={y}"
                })

        # --- (d) 兄弟相交 ---
        parent_map = build_parent_map(blocks)
        for parent_key, siblings in parent_map.items():
            n = len(siblings)
            for i in range(n):
                for j in range(i + 1, n):
                    a = siblings[i]
                    b = siblings[j]
                    bbox_a = [a['x'], a['y'], a['w'], a['h']]
                    bbox_b = [b['x'], b['y'], b['w'], b['h']]
                    # 跳过零尺寸
                    if a['w'] <= 0 or a['h'] <= 0 or b['w'] <= 0 or b['h'] <= 0:
                        continue
                    # 跳过一方为 LytDocumentFloat 且另一方为文本类块的合法环绕重叠
                    a_cls = a.get('cls', '')
                    b_cls = b.get('cls', '')
                    if (a_cls == FLOAT_CLASS and b_cls in FLOAT_EXCLUDED_TEXT_CLASSES) or \
                       (b_cls == FLOAT_CLASS and a_cls in FLOAT_EXCLUDED_TEXT_CLASSES):
                        continue
                    overlap = iou(bbox_a, bbox_b)
                    if overlap > 0.05:
                        # 按块 id 排序，避免 A∩B/B∩A 重复报告
                        if a['i'] <= b['i']:
                            primary, secondary = a, b
                        else:
                            primary, secondary = b, a
                        page_findings.append({
                            "page": page_name,
                            "rule": "sibling_intersection",
                            "bbox": [primary['x'], primary['y'], primary['w'], primary['h']],
                            "severity": "warn",
                            "evidence": f"块 #{primary['i']} ({primary['cls']}) 与 #{secondary['i']} ({secondary['cls']}) 相交 IoU={overlap:.3f}"
                        })

        total_findings += len(page_findings)
        all_pages.append({
            "page": page_name,
            "source": "geometric",
            "findings": page_findings
        })

    # 构造输出
    output = {
        "tool": "visual-inspection/screen.py",
        "subcommand": "geometric",
        "page_width": page_width,
        "total_pages": len(all_pages),
        "total_findings": total_findings,
        "pages": all_pages
    }

    save_json(out_path, output)
    eprint(f"[info] geometric 检测完成: {len(all_pages)} 页, {total_findings} 条 findings")
    eprint(f"[info] 输出 -> {out_path}")

    # stdout 打印摘要
    print(json.dumps({
        "pages_processed": len(all_pages),
        "findings_count": total_findings,
        "output": out_path
    }))

    return 0


# ============================================================
# VLM 检测
# ============================================================

def encode_image_png(pil_image):
    """PIL Image -> base64 PNG"""
    buf = BytesIO()
    pil_image.save(buf, format='PNG')
    return base64.b64encode(buf.getvalue()).decode('ascii')


def tile_image(pil_image, tile_h, overlap):
    """
    将图像切为宽全幅、高 tile_h、重叠 overlap 的瓦片。
    返回: [(tile_index, tile_pil_image, offset_y)]
    """
    if overlap >= tile_h:
        raise ValueError(
            f"overlap（{overlap}）必须小于 tile_h（{tile_h}），否则会导致死循环"
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


def call_vlm_api(api_key, base_url, model, image_b64, timeout, prompt_text):
    """
    调用 OpenAI 兼容的 VLM API。
    返回: 解析后的 JSON dict，或 None（失败时）
    """
    url = f"{base_url}/chat/completions".rstrip('/')
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }
    payload = {
        "model": model,
        "messages": [
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

    retries = [1, 4, 16]  # 指数退避
    last_err = None

    for attempt in range(1 + len(retries)):
        try:
            req = urllib.request.Request(url, data=data, headers=headers, method='POST')
            resp = urllib.request.urlopen(req, timeout=timeout)
            resp_body = resp.read().decode('utf-8')
            resp_json = json.loads(resp_body)
            # 提取 content
            choices = resp_json.get('choices', [])
            if not choices:
                eprint(f"[warn] API 返回无 choices")
                return None
            content = choices[0].get('message', {}).get('content', '')
            return extract_json_block(content)
        except (urllib.error.HTTPError, urllib.error.URLError, OSError,
                json.JSONDecodeError, TimeoutError) as e:
            last_err = e
            code = getattr(e, 'code', 0)
            if code == 429 or code >= 500 or isinstance(e, (TimeoutError, urllib.error.URLError)):
                if attempt < len(retries):
                    wait = retries[attempt]
                    eprint(f"[warn] API 调用失败 (attempt {attempt+1}): {e}，等待 {wait}s 重试")
                    time.sleep(wait)
                    continue
            eprint(f"[error] API 调用最终失败: {e}")
            return None

    if last_err:
        eprint(f"[error] API 调用最终失败: {last_err}")
    return None


def extract_json_block(text):
    """
    宽容提取首个完整 JSON 对象块。
    模型可能包裹 ```json ... ``` 标记。
    """
    if not text:
        return None

    # 尝试提取 ```json ... ``` 块
    m = re.search(r'```(?:json)?\s*\n?({.*?})\s*\n?```', text, re.DOTALL)
    if m:
        try:
            return json.loads(m.group(1))
        except json.JSONDecodeError:
            pass

    # 尝试提取首个 { ... }
    brace_start = text.find('{')
    brace_end = text.rfind('}')
    if brace_start >= 0 and brace_end > brace_start:
        try:
            return json.loads(text[brace_start:brace_end + 1])
        except json.JSONDecodeError:
            pass

    return None


def process_tile(args):
    """处理单个瓦片（供线程池使用）"""
    api_key, base_url, model, timeout, prompt_text = args['cfg']
    tile_img = args['tile_img']
    tile_idx = args['tile_idx']
    offset_y = args['offset_y']
    page_name = args['page_name']

    try:
        b64 = encode_image_png(tile_img)
        result = call_vlm_api(api_key, base_url, model, b64, timeout, prompt_text)
        if result is None:
            return {
                "page": page_name,
                "source": "vlm",
                "tile_index": tile_idx,
                "findings": [],
                "error": f"tile {tile_idx} API 调用失败"
            }

        findings = result.get('findings', [])
        # 坐标换算：瓦片内 y -> 整页 y
        converted = []
        for f in findings:
            bbox = f.get('bbox', [0, 0, 0, 0])
            bbox[1] += offset_y  # y 加瓦片偏移
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
        eprint(f"[error] tile {tile_idx} 处理异常: {e}")
        return {
            "page": page_name,
            "source": "vlm",
            "tile_index": tile_idx,
            "findings": [],
            "error": str(e)
        }


def deduplicate_findings(findings, iou_threshold=0.5):
    """
    跨瓦片去重：同 class 且 bbox IoU > threshold 合并保留 confidence 高者。
    """
    if not findings:
        return []

    # 按 class 分组
    by_class = {}
    for f in findings:
        cls = f.get('class', 'unknown')
        by_class.setdefault(cls, []).append(f)

    deduped = []
    for cls, items in by_class.items():
        # 按 confidence 降序
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
    执行 VLM 初筛。
    """
    env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), '.env')
    env = load_env(env_path)

    if env is None:
        eprint("[fatal] 未找到 .env 文件！")
        eprint(f"[fatal] 请复制 {os.path.dirname(os.path.abspath(__file__))}/.env.example 为 .env 并填入 DASHSCOPE_API_KEY")
        sys.exit(1)

    api_key = env.get('DASHSCOPE_API_KEY', '').strip()

    # dry-run 模式不需要真实 key
    if not dry_run:
        if not api_key or api_key.startswith('sk-在这里') or api_key == 'dummy':
            eprint("[fatal] DASHSCOPE_API_KEY 未正确配置！")
            eprint(f"[fatal] 请编辑 {env_path} 填入真实 API Key")
            sys.exit(1)

    base_url = env.get('VLM_BASE_URL', 'https://dashscope.aliyuncs.com/compatible-mode/v1').rstrip('/')
    model = model_override or env.get('VLM_MODEL', 'qwen2.5-vl-32b-instruct')
    concurrency = int(env.get('VLM_CONCURRENCY', '4'))
    timeout = int(env.get('VLM_TIMEOUT', '120'))

    # 瓦片参数防卫（避免死循环）
    if overlap >= tile_h:
        eprint(f"[fatal] overlap（{overlap}）必须小于 tile_h（{tile_h}），否则会导致死循环")
        sys.exit(1)

    # 搜集文件
    png_files = []
    for fname in os.listdir(shots_dir):
        if fname.lower().endswith('.png'):
            png_path = os.path.join(shots_dir, fname)
            base = os.path.splitext(fname)[0]
            # 页面名 = 不含时间戳
            page_name = re.sub(r'_\d{4}-\d{2}-\d{2}_\d{6}$', '', base)

            if pages_filter:
                if page_name not in pages_filter:
                    continue

            png_files.append((png_path, page_name))

    if not png_files:
        eprint("[fatal] 没有匹配的 PNG 文件")
        sys.exit(1)

    # Dry-run: 只打印信息
    if dry_run:
        eprint(f"[dry-run] 模型: {model}")
        eprint(f"[dry-run] Base URL: {base_url}")
        prompt_len = len(VLM_USER_TEXT) + len(VLM_SYSTEM_PROMPT)
        eprint(f"[dry-run] 提示词长度: {prompt_len} 字符")
        eprint(f"[dry-run] 并发度: {concurrency}, 超时: {timeout}s")
        eprint(f"[dry-run] 匹配到 {len(png_files)} 页:")

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
                        "messages": [{"role": "user", "content": [
                            {"type": "text", "text": VLM_USER_TEXT},
                            {"type": "image_url", "image_url": {"url": f"data:image/png;base64,..."}}
                        ]}],
                        "max_tokens": 1024,
                        "temperature": 0.1
                    })
                    eprint(f"[dry-run]   页 {page_name} 瓦片 {idx}: y={off_y}-{off_y+tile.size[1]}, "
                           f"请求体 ~{len(req_body)} bytes, base64 ~{b64_size} bytes")
            except Exception as e:
                eprint(f"[warn] 无法打开 {png_path}: {e}")

        eprint(f"[dry-run] 总计: {len(png_files)} 页, {total_tiles} 瓦片")
        print(json.dumps({"dry_run": True, "pages": len(png_files), "tiles": total_tiles}))
        return 0

    # 正式运行
    from PIL import Image

    all_findings = []
    all_errors = []

    for png_path, page_name in png_files:
        try:
            img = Image.open(png_path)
        except Exception as e:
            eprint(f"[warn] 无法打开 {png_path}: {e}")
            all_errors.append({
                "page": page_name,
                "source": "vlm",
                "error": f"无法打开图片: {e}"
            })
            continue

        tiles = tile_image(img, tile_h, overlap)
        eprint(f"[info] 处理页 {page_name}: {len(tiles)} 瓦片")

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

        # 去重
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
        "tool": "visual-inspection/screen.py",
        "subcommand": "vlm",
        "model": model,
        "total_pages": len(png_files),
        "total_findings": len(all_findings),
        "findings": all_findings,
        "errors": all_errors
    }

    save_json(out_path, output)
    eprint(f"[info] VLM 检测完成: {len(all_findings)} 条 findings, {len(all_errors)} 条错误")
    eprint(f"[info] 输出 -> {out_path}")

    print(json.dumps({
        "pages_processed": len(png_files),
        "findings_count": len(all_findings),
        "errors_count": len(all_errors),
        "output": out_path
    }))

    return 0


# ============================================================
# Report 合并
# ============================================================

SEVERITY_ORDER = {'error': 0, 'warn': 1, 'info': 2}


def severity_key(f):
    return SEVERITY_ORDER.get(f.get('severity', 'info'), 99)


def run_report(inputs, out_path):
    """
    合并多来源 findings，按 page 分组，按 severity 排序。
    """
    all_findings = []
    total_inputs = 0

    for inp in inputs:
        data = load_json(inp)
        if data is None:
            eprint(f"[warn] 跳过无法读取的输入文件: {inp}")
            continue
        total_inputs += 1

        # 支持两种格式：{pages: [...]} 或 {findings: [...]} 或直接顶层 findings 数组
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

    # 按 page 分组
    by_page = {}
    for f in all_findings:
        page = f.get('page', 'unknown')
        by_page.setdefault(page, []).append(f)

    # 按 severity 排序
    for page in by_page:
        by_page[page].sort(key=severity_key)

    # 构建输出
    summary = {}
    for page, findings in by_page.items():
        counts = {'error': 0, 'warn': 0, 'info': 0}
        for f in findings:
            sev = f.get('severity', 'info')
            counts[sev] = counts.get(sev, 0) + 1
        summary[page] = counts

    output = {
        "tool": "visual-inspection/screen.py",
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

    # stdout 打印每页计数表
    print(f"Report — 合并 {len(inputs)} 个来源")
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


# ============================================================
# 主入口
# ============================================================

def main():
    # Windows GBK 控制台中文乱码修复
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='视觉初筛工具 — Visual Inspection Screener'
    )
    subparsers = parser.add_subparsers(dest='command', help='子命令')

    # --- geometric ---
    p_geo = subparsers.add_parser('geometric', help='第 0 层机械检测')
    p_geo.add_argument('--shots', required=True, help='截图目录（含 *.png + 同名 *.json bounds）')
    p_geo.add_argument('--page-width', type=int, default=1800, help='页宽参考值（px）')
    p_geo.add_argument('--out', required=True, help='输出 JSON 路径')

    # --- vlm ---
    p_vlm = subparsers.add_parser('vlm', help='第 1 层多模态初筛')
    p_vlm.add_argument('--shots', required=True, help='截图目录')
    p_vlm.add_argument('--pages', help='过滤页名（逗号分隔）')
    p_vlm.add_argument('--model', help='覆盖 .env 中的 VLM_MODEL')
    p_vlm.add_argument('--tile-h', type=int, default=1400, help='瓦片高度（px）')
    p_vlm.add_argument('--overlap', type=int, default=200, help='瓦片重叠（px）')
    p_vlm.add_argument('--dry-run', action='store_true', help='不发起 HTTP，仅打印信息')
    p_vlm.add_argument('--out', required=True, help='输出 JSON 路径')

    # --- report ---
    p_rep = subparsers.add_parser('report', help='合并报告')
    p_rep.add_argument('--inputs', required=True, help='输入 JSON 路径（逗号分隔）')
    p_rep.add_argument('--out', required=True, help='输出 triage JSON 路径')

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
        eprint("[info] 用户中断")
        sys.exit(130)
    except Exception as e:
        eprint(f"[fatal] 未处理的异常: {e}")
        sys.exit(1)
