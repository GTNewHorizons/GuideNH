#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
bounds-JSON assertion ratchet (assert_bounds)

Purpose:
  Verify that the bounds JSON produced by a headless render satisfies the
  expected structural assertions (node counts, centering, width ratios, height
  caps, parent containment, and so on). CI integration:

    # step 1: render -> bounds JSON (done by the upstream tooling)
    # step 2: assert
    py -3 test/visual/tools/assert_bounds.py \\
        --shots run/client_new/screenshots_visualtest \\
        --assertions test/visual/ratchet/assertions.json \\
        [--junit build/test-results/assert-bounds.xml]

  Exit code: 0 when everything passes; 1 on any failure, rule error or missing
  page.

Dependencies: Python 3 standard library only
(json / argparse / xml.etree / pathlib / re / sys).
Do not add any third-party package.
"""

import argparse
import json
import os
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


# Utility functions

def eprint(*args, **kwargs):
    """Print to stderr"""
    print(*args, file=sys.stderr, **kwargs)


def load_json_safe(path):
    """Load a JSON file safely; returns None on failure"""
    try:
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except (json.JSONDecodeError, FileNotFoundError, IOError) as e:
        eprint(f"[warn] cannot read JSON: {path} - {e}")
        return None


# Page discovery

# Regex: extract the stem and the timestamp
TIMESTAMP_RE = re.compile(r'^(.+)_(\d{4}-\d{2}-\d{2}_\d{6})\.json$')


def discover_pages(shots_dir, page_filters):
    """
    Scan shots_dir, group by page stem and take the JSON with the newest
    timestamp.
    page_filters: list of substrings (an empty list means no restriction).
    Returns: {stem: json_path}
    """
    all_by_stem = {}  # stem -> [(ts_str, path), ...]

    shots_path = Path(shots_dir)
    if not shots_path.is_dir():
        eprint(f"[fatal] --shots directory does not exist: {shots_dir}")
        sys.exit(1)

    for fname in os.listdir(shots_dir):
        m = TIMESTAMP_RE.match(fname)
        if not m:
            continue
        stem = m.group(1)
        ts = m.group(2)
        path = shots_path / fname
        # make sure it is a file
        if not path.is_file():
            continue
        # --page substring filter
        if page_filters:
            if not any(sub.lower() in stem.lower() for sub in page_filters):
                continue
        all_by_stem.setdefault(stem, []).append((ts, str(path)))

    # take the newest timestamp per stem
    result = {}
    for stem, entries in all_by_stem.items():
        entries.sort(key=lambda x: x[0], reverse=True)
        result[stem] = entries[0][1]

    return result


# Parent chain reconstruction

def build_parent_map(blocks):
    """
    Build the parent index for a list of nodes.
    parent_of[i] = the parent node object, or None for a root.
    """
    # ancestors[depth] = the most recent node at that depth
    ancestors = {}
    parent_of = [None] * len(blocks)

    for idx, blk in enumerate(blocks):
        d = blk.get('depth', 1)
        if d > 1:
            parent_of[idx] = ancestors.get(d - 1)
        else:
            parent_of[idx] = None
        ancestors[d] = blk

    return parent_of


# Assertion engine

def default_msg(rule_type, params):
    """Build the default message text from the rule type and its parameters"""
    cls = params.get('cls', '?')
    if rule_type == 'count':
        for op in ('eq', 'ge', 'le'):
            if op in params:
                return f"expect {op}={params[op]} {cls}"
        return f"expect count {cls} with constraint"
    elif rule_type == 'exists':
        return f"expect {cls} to exist"
    elif rule_type == 'absent':
        return f"expect {cls} to be absent"
    elif rule_type == 'centered':
        tol = params.get('tol', 0)
        return f"expect {cls} centered within tol={tol}"
    elif rule_type == 'width_ratio':
        of = params.get('of', 'parent')
        for op in ('ge', 'le'):
            if op in params:
                return f"expect {cls} width_ratio of {of} {op}={params[op]}"
        return f"expect {cls} width_ratio constraint"
    elif rule_type == 'max_height':
        return f"expect max_height of {cls} le={params.get('le', '?')}"
    elif rule_type == 'max_width':
        return f"expect max_width of {cls} le={params.get('le', '?')}"
    elif rule_type == 'inside_parent':
        pad = params.get('pad', 0)
        return f"expect {cls} inside parent with pad={pad}"
    elif rule_type == 'attr':
        field = params.get('field', '?')
        for op in ('gt', 'ge', 'eq', 'le', 'lt'):
            if op in params:
                return f"expect {cls}.{field} {op}={params[op]} for every node"
        return f"expect {cls}.{field} constraint"
    return f"rule {rule_type} on {cls}"


def check_count(nodes_of_cls, params):
    """count: {cls, eq|ge|le: N}"""
    actual = len(nodes_of_cls)
    for op in ('eq', 'ge', 'le'):
        if op in params:
            expected = params[op]
            if op == 'eq' and actual != expected:
                return (False, f"expected eq={expected} {params['cls']}, actual {actual}")
            elif op == 'ge' and actual < expected:
                return (False, f"expected >= {expected} {params['cls']}, actual {actual}")
            elif op == 'le' and actual > expected:
                return (False, f"expected <= {expected} {params['cls']}, actual {actual}")
    return (True, None)


def check_exists(nodes_of_cls, params):
    """exists: {cls}"""
    if not nodes_of_cls:
        return (False, f"expected {params['cls']} to exist, not found")
    return (True, None)


def check_absent(nodes_of_cls, params):
    """absent: {cls}"""
    if nodes_of_cls:
        return (False, f"expected {params['cls']} to be absent, found {len(nodes_of_cls)}")
    return (True, None)


def check_centered(blocks, nodes_of_cls, parent_of, params):
    """centered: {cls, tol: T} - for every C node
    |center_x - center_x(parent)| <= T"""
    cls = params['cls']
    tol = params['tol']
    for idx, node in nodes_of_cls:
        parent = parent_of[idx]
        if parent is None:
            return (False, f"RULE ERROR: {cls} node #{idx} has no parent, cannot check centered")
        node_cx = node['x'] + node['w'] / 2.0
        parent_cx = parent['x'] + parent['w'] / 2.0
        diff = abs(node_cx - parent_cx)
        if diff > tol:
            return (False, f"{cls} node #{idx} center_x diff {diff:.1f} exceeds tol {tol}")
    return (True, None)


def check_width_ratio(blocks, nodes_of_cls, parent_of, params):
    """width_ratio: {cls, of: 'parent', ge|le: R}"""
    cls = params['cls']
    op = 'ge' if 'ge' in params else 'le' if 'le' in params else None
    threshold = params.get(op, 0) if op else 0
    if op is None:
        return (False, f"RULE ERROR: width_ratio missing ge/le for {cls}")

    for idx, node in nodes_of_cls:
        parent = parent_of[idx]
        if parent is None:
            return (False, f"RULE ERROR: {cls} node #{idx} has no parent, cannot check width_ratio")
        parent_w = parent['w']
        if parent_w == 0:
            return (False, f"RULE ERROR: parent of {cls} node #{idx} has width 0")
        ratio = node['w'] / float(parent_w)
        if op == 'ge' and ratio < threshold:
            return (False, f"{cls} node #{idx} width_ratio {ratio:.3f} < {threshold}")
        elif op == 'le' and ratio > threshold:
            return (False, f"{cls} node #{idx} width_ratio {ratio:.3f} > {threshold}")
    return (True, None)


def check_max_height(blocks, nodes_of_cls, parent_of, params):
    """max_height: {cls, le: H}"""
    cls = params['cls']
    limit = params['le']
    for idx, node in nodes_of_cls:
        h = node['h']
        if h > limit:
            return (False, f"{cls} node #{idx} h={h} > limit {limit}")
    return (True, None)


def check_max_width(blocks, nodes_of_cls, parent_of, params):
    """max_width: {cls, le: W}"""
    cls = params['cls']
    limit = params['le']
    for idx, node in nodes_of_cls:
        w = node['w']
        if w > limit:
            return (False, f"{cls} node #{idx} w={w} > limit {limit}")
    return (True, None)


def check_inside_parent(blocks, nodes_of_cls, parent_of, params):
    """inside_parent: {cls, pad: P}"""
    cls = params['cls']
    pad = params.get('pad', 0)
    for idx, node in nodes_of_cls:
        parent = parent_of[idx]
        if parent is None:
            return (False, f"RULE ERROR: {cls} node #{idx} has no parent, cannot check inside_parent")
        overflow_left = node['x'] - parent['x']
        overflow_top = node['y'] - parent['y']
        overflow_right = (node['x'] + node['w']) - (parent['x'] + parent['w'])
        overflow_bottom = (node['y'] + node['h']) - (parent['y'] + parent['h'])
        if overflow_left < -pad:
            return (False, f"{cls} node #{idx} overflows parent left by {-overflow_left} px (pad={pad})")
        if overflow_top < -pad:
            return (False, f"{cls} node #{idx} overflows parent top by {-overflow_top} px (pad={pad})")
        if overflow_right > pad:
            return (False, f"{cls} node #{idx} overflows parent right by {overflow_right} px (pad={pad})")
        if overflow_bottom > pad:
            return (False, f"{cls} node #{idx} overflows parent bottom by {overflow_bottom} px (pad={pad})")
    return (True, None)


# Single page evaluation

def check_attr(nodes_of_cls, params):
    """attr: {cls, field: x|y|w|h, gt|ge|eq|le|lt: N} - the numeric field of
    every C node must satisfy the comparison; no node at all counts as
    failure"""
    cls = params['cls']
    field = params['field']
    if not nodes_of_cls:
        return (False, f"expected {cls} nodes for attr check, none found")
    for idx, node in nodes_of_cls:
        actual = node.get(field)
        if actual is None:
            return (False, f"{cls} node #{idx} missing field '{field}'")
        for op in ('gt', 'ge', 'eq', 'le', 'lt'):
            if op not in params:
                continue
            expected = params[op]
            ok = {'gt': actual > expected, 'ge': actual >= expected, 'eq': actual == expected,
                  'le': actual <= expected, 'lt': actual < expected}[op]
            if not ok:
                return (False, f"{cls} node #{idx} {field}={actual} violates {op}={expected}")
    return (True, None)


def validate_rule_structure(rule_obj):
    """
    Validate the structure of a rule object.
    rule_obj should be {rule_type: params} or {rule_type: params, msg: ...}.
    Returns: (True, rule_type, params, msg) or (False, error_msg, None, None)
    """
    if not isinstance(rule_obj, dict):
        return (False, f"rule is not a dict", None, None)

    # find the rule_type key (skipping metadata such as msg and _readme)
    known_keys = {'count', 'exists', 'absent', 'centered',
                  'width_ratio', 'max_height', 'max_width', 'inside_parent', 'attr'}
    rule_type = None
    params = None
    for k, v in rule_obj.items():
        if k == 'msg' or k == '_readme' or k.startswith('_'):
            continue
        if k in known_keys:
            if rule_type is not None:
                return (False, f"multiple rule keys in one object: {rule_type} and {k}", None, None)
            rule_type = k
            params = v
        else:
            return (False, f"unknown rule key '{k}'", None, None)

    if rule_type is None:
        return (False, "no known rule key found in rule object", None, None)

    if not isinstance(params, dict):
        return (False, f"params for {rule_type} is not a dict", None, None)

    if 'cls' not in params and rule_type in ('count', 'exists', 'absent', 'centered',
                                              'width_ratio', 'max_height', 'max_width', 'inside_parent', 'attr'):
        return (False, f"missing 'cls' in {rule_type} params", None, None)

    # count needs an operator
    if rule_type == 'count':
        has_op = any(op in params for op in ('eq', 'ge', 'le'))
        if not has_op:
            return (False, "count rule missing eq/ge/le operator", None, None)

    # width_ratio needs an operator and of
    if rule_type == 'width_ratio':
        has_op = any(op in params for op in ('ge', 'le'))
        if not has_op:
            return (False, "width_ratio rule missing ge/le operator", None, None)
        if params.get('of') != 'parent':
            return (False, "width_ratio only supports of='parent'", None, None)

    # max_height/max_width need le
    if rule_type in ('max_height', 'max_width'):
        if 'le' not in params:
            return (False, f"{rule_type} rule missing 'le' threshold", None, None)

    # centered needs tol
    if rule_type == 'centered':
        if 'tol' not in params:
            return (False, "centered rule missing 'tol'", None, None)

    # attr needs a valid field and at least one comparison operator
    if rule_type == 'attr':
        if params.get('field') not in ('x', 'y', 'w', 'h'):
            return (False, "attr rule field must be one of x/y/w/h", None, None)
        if not any(op in params for op in ('gt', 'ge', 'eq', 'le', 'lt')):
            return (False, "attr rule missing gt/ge/eq/le/lt operator", None, None)

    msg = rule_obj.get('msg')
    if msg is None or not isinstance(msg, str) or msg.strip() == '':
        msg = default_msg(rule_type, params)

    return (True, rule_type, params, msg)


def evaluate_page(page_stem, page_json_path, assertions_list):
    """
    Run the assertions for a single page.
    Returns: [(n, page_stem, msg, passed, failure_text), ...]
    n: assertion number (global)
    failure_text: failure detail, None when it passed
    """
    blocks = load_json_safe(page_json_path)
    if blocks is None or not isinstance(blocks, list) or len(blocks) == 0:
        # invalid JSON - every assertion counts as failed
        results = []
        for idx, rule_obj in enumerate(assertions_list):
            ok_struct, err, _, _ = validate_rule_structure(rule_obj)
            if not ok_struct:
                msg = err
            else:
                _, _, _, msg = validate_rule_structure(rule_obj)
            results.append((page_stem, msg, False,
                           f"page bounds JSON invalid or empty: {page_json_path}"))
        return results

    parent_of = build_parent_map(blocks)

    results = []
    for rule_obj in assertions_list:
        ok_struct, rule_type, params, msg = validate_rule_structure(rule_obj)
        if not ok_struct:
            results.append((page_stem, msg, False,
                           f"RULE ERROR: {ok_struct}"))
            continue

        cls = params.get('cls', '')
        # collect every node of that class (as (idx, node) pairs)
        nodes_of_cls = [(idx, b) for idx, b in enumerate(blocks) if b.get('cls') == cls]

        passed = False
        failure_text = None

        try:
            if rule_type == 'count':
                passed, failure_text = check_count(nodes_of_cls, params)
            elif rule_type == 'exists':
                passed, failure_text = check_exists(nodes_of_cls, params)
            elif rule_type == 'absent':
                passed, failure_text = check_absent(nodes_of_cls, params)
            elif rule_type == 'centered':
                passed, failure_text = check_centered(blocks, nodes_of_cls, parent_of, params)
            elif rule_type == 'width_ratio':
                passed, failure_text = check_width_ratio(blocks, nodes_of_cls, parent_of, params)
            elif rule_type == 'max_height':
                passed, failure_text = check_max_height(blocks, nodes_of_cls, parent_of, params)
            elif rule_type == 'max_width':
                passed, failure_text = check_max_width(blocks, nodes_of_cls, parent_of, params)
            elif rule_type == 'inside_parent':
                passed, failure_text = check_inside_parent(blocks, nodes_of_cls, parent_of, params)
            elif rule_type == 'attr':
                passed, failure_text = check_attr(nodes_of_cls, params)
            else:
                passed = False
                failure_text = f"RULE ERROR: unknown rule type '{rule_type}'"
        except Exception as e:
            passed = False
            failure_text = f"RULE ERROR: {type(e).__name__}: {e}"

        results.append((page_stem, msg, passed, failure_text))

    return results


# JUnit XML output

def build_junit(all_results):
    """
    Build the JUnit XML tree.
    all_results: [(page_stem, msg, passed, failure_text), ...]
    """
    # group by page_stem
    by_page = {}
    for page_stem, msg, passed, failure_text in all_results:
        by_page.setdefault(page_stem, []).append((msg, passed, failure_text))

    ts_elements = []
    for page_stem in sorted(by_page.keys()):
        entries = by_page[page_stem]
        suite = ET.Element('testsuite')
        suite.set('name', page_stem)
        suite.set('tests', str(len(entries)))

        failures = sum(1 for _, p, _ in entries if not p)
        suite.set('failures', str(failures))

        for msg, passed, failure_text in entries:
            tc = ET.SubElement(suite, 'testcase')
            tc.set('name', msg)
            tc.set('classname', f'assert_bounds.{page_stem}')
            if not passed:
                fail_elem = ET.SubElement(tc, 'failure')
                fail_elem.set('message', failure_text)
                fail_elem.text = failure_text

        ts_elements.append(suite)

    # root element
    if ts_elements:
        root = ET.Element('testsuites')
        total = sum(len(v) for v in by_page.values())
        failed_count = 0
        for v in by_page.values():
            for _, p, _ in v:
                if not p:
                    failed_count += 1
        root.set('tests', str(total))
        root.set('failures', str(failed_count))
        for suite in ts_elements:
            root.append(suite)
        return root
    else:
        root = ET.Element('testsuites')
        root.set('tests', '0')
        root.set('failures', '0')
        return root


# Main run

def main():
    # UTF-8 console
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='assertion ratchet for visual test bounds JSON'
    )
    parser.add_argument('--shots', required=True,
                        help='screenshot directory (holding '
                             '<page>_YYYY-MM-DD_HHMMSS.json files)')
    parser.add_argument('--assertions', required=True,
                        help='path to the assertions JSON file')
    parser.add_argument('--junit',
                        help='JUnit XML output path (optional)')
    parser.add_argument('--page', action='append', dest='page_filters', default=[],
                        help='page name substring filter (may be given several times)')
    args = parser.parse_args()

    # load the assertions file
    assertions_data = load_json_safe(args.assertions)
    if assertions_data is None:
        eprint("[fatal] cannot read the assertions file")
        sys.exit(1)

    pages_assertions = assertions_data.get('pages', {})
    if not pages_assertions:
        eprint("[fatal] the assertions file is missing the 'pages' key or it is empty")
        sys.exit(1)

    # page discovery
    discovered = discover_pages(args.shots, args.page_filters)
    if not discovered:
        eprint("[warn] no matching JSON file found in the --shots directory")

    total_assertions = 0
    total_failed = 0
    all_results = []  # [(n, page_stem, msg, passed, failure_text)]
    n = 0

    # process every page stem of the assertions JSON in declaration order
    for page_stem, assertion_list in pages_assertions.items():
        if not isinstance(assertion_list, list):
            eprint(f"[warn] page '{page_stem}' assertions is not an array, skipping")
            continue

        json_path = discovered.get(page_stem)

        if json_path is None:
            # page missing
            for rule_obj in assertion_list:
                n += 1
                total_assertions += 1
                # try to get msg, fall back to the default
                ok_struct, rule_type, params, msg = validate_rule_structure(rule_obj)
                if not ok_struct:
                    msg = rule_obj.get('msg', 'assertion')

                all_results.append((page_stem, msg, False,
                                   "page bounds JSON missing"))
                total_failed += 1
                print(f"not ok {n} - {page_stem}: {msg} (page bounds JSON missing)")
            continue

        # evaluate the page
        page_results = evaluate_page(page_stem, json_path, assertion_list)
        for ps, msg, passed, failure_text in page_results:
            n += 1
            total_assertions += 1
            all_results.append((ps, msg, passed, failure_text))
            if passed:
                print(f"ok {n} - {ps}: {msg}")
            else:
                total_failed += 1
                print(f"not ok {n} - {ps}: {msg} ({failure_text})")

    # SUMMARY
    total_pages = len(pages_assertions)
    print(f"SUMMARY: pages={total_pages} assertions={total_assertions} failed={total_failed}")

    # JUnit output
    if args.junit:
        junit_root = build_junit(all_results)
        tree = ET.ElementTree(junit_root)
        # make sure the output directory exists
        junit_path = Path(args.junit)
        junit_path.parent.mkdir(parents=True, exist_ok=True)
        tree.write(str(junit_path), encoding='utf-8', xml_declaration=True)

    # exit code
    return 1 if total_failed > 0 else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        eprint("[info] interrupted by user")
        sys.exit(130)
    except Exception as e:
        eprint(f"[fatal] unhandled exception: {e}")
        sys.exit(1)
