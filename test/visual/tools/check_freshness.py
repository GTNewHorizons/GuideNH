#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Screenshot freshness validator (check_freshness)

Purpose:
  Guards against stale screenshot list false positives: it checks whether every
  page in a page list has a screenshot in the most recent batch and whether the
  screenshots are fresh. Used together with headless render batches:

    py -3 test/visual/tools/check_freshness.py \\
        --shots run/client_new/screenshots_visualtest \\
        --list <page-list.txt> \\
        [--stale-min 10]

  Exit code: 0 when everything is OK; 1 on any MISSING / STALE.

Dependencies: Python 3 standard library only
(argparse / os / re / sys / tempfile / datetime).
Do not add any third-party package.
"""

import argparse
import os
import re
import sys
import tempfile
from datetime import datetime


# Screenshot file name regex (same idea as assert_bounds.py TIMESTAMP_RE,
# extended to .png)
SHOT_RE = re.compile(r'^(.+)_(\d{4}-\d{2}-\d{2}_\d{6})\.png$')

# Page list prefix: guidenh:visualtest/<path>.md
PAGE_PREFIX = 'guidenh:visualtest/'


def eprint(*args, **kwargs):
    """Print to stderr"""
    print(*args, file=sys.stderr, **kwargs)


def page_id_to_stem(page_id):
    """Page id -> screenshot stem.

    guidenh:visualtest/layout/details.md -> visualtest_layout_details.md
    Inputs without the prefix, or with path separators already translated, are
    normalized the same way.
    """
    s = page_id.strip()
    if s.startswith(PAGE_PREFIX):
        s = s[len(PAGE_PREFIX):]
    # path separator / -> _
    s = s.replace('/', '_')
    if not s.startswith('visualtest_'):
        s = 'visualtest_' + s
    return s


def load_page_list(list_path):
    """Read the page list. Returns [(line_no, page_id), ...]; lines starting
    with '#' and blank lines are ignored."""
    pages = []
    with open(list_path, 'r', encoding='utf-8', errors='replace') as f:
        for lineno, line in enumerate(f, 1):
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            pages.append((lineno, line))
    return pages


def scan_shots(shots_dir):
    """Scan the screenshot directory, grouped by stem.
    Returns {stem: [(ts_str, path), ...]}."""
    by_stem = {}
    try:
        entries = os.listdir(shots_dir)
    except OSError as e:
        eprint(f'[fatal] cannot read screenshot directory {shots_dir}: {e}')
        sys.exit(1)
    for name in entries:
        m = SHOT_RE.match(name)
        if not m:
            continue
        path = os.path.join(shots_dir, name)
        if not os.path.isfile(path):
            continue
        stem, ts = m.group(1), m.group(2)
        by_stem.setdefault(stem, []).append((ts, path))
    return by_stem


def parse_ts(ts_str):
    """File name timestamp 'YYYY-MM-DD_HHMMSS' -> datetime
    (returns None when it cannot be parsed)."""
    try:
        return datetime.strptime(ts_str, '%Y-%m-%d_%H%M%S')
    except ValueError:
        return None


def evaluate(shots_dir, list_path, stale_min):
    """Core decision.

    Returns (results, n_missing, n_stale):
      results: [(page_id, status, detail), ...], status in OK / MISSING / STALE
    """
    pages = load_page_list(list_path)
    by_stem = scan_shots(shots_dir)

    try:
        list_mtime = os.path.getmtime(list_path)
    except OSError as e:
        eprint(f'[fatal] cannot read list file {list_path}: {e}')
        sys.exit(1)

    results = []
    n_missing = 0
    n_stale = 0

    for lineno, page_id in pages:
        stem = page_id_to_stem(page_id)
        entries = by_stem.get(stem, [])
        if not entries:
            results.append((page_id, 'MISSING',
                            f'no screenshots for stem {stem} '
                            f'(line {lineno})'))
            n_missing += 1
            continue

        # Same rule as assert_bounds.py discover_pages: newest file name
        # timestamp wins
        latest_ts, latest_path = max(entries, key=lambda e: e[0])
        shot_mtime = os.path.getmtime(latest_path)

        age_min = (list_mtime - shot_mtime) / 60.0
        if age_min > stale_min:
            results.append((
                page_id, 'STALE',
                f'latest shot {latest_ts} is {age_min:.1f} min older than '
                f'list (stale-min {stale_min} min)'))
            n_stale += 1
        else:
            results.append((page_id, 'OK', f'latest shot {latest_ts}'))

    return results, n_missing, n_stale


def run(shots_dir, list_path, stale_min):
    """Run the validation, print TAP-style output, return the exit code."""
    results, n_missing, n_stale = evaluate(shots_dir, list_path, stale_min)

    n = 0
    for page_id, status, detail in results:
        n += 1
        if status == 'OK':
            print(f'ok {n} - {page_id}: OK ({detail})')
        elif status == 'MISSING':
            print(f'not ok {n} - {page_id}: MISSING ({detail})')
        else:  # STALE
            print(f'not ok {n} - {page_id}: STALE ({detail})')

    print(f'SUMMARY: pages={len(results)} ok={len(results) - n_missing - n_stale} '
          f'missing={n_missing} stale={n_stale}')

    return 1 if (n_missing + n_stale) > 0 else 0


def self_test():
    """Built-in smoke test: build fake screenshots and lists in a temporary
    directory and verify the OK/MISSING/STALE states."""
    results = []  # (case_name, passed, detail)

    def check(name, cond, detail=''):
        results.append((name, bool(cond), detail))

    with tempfile.TemporaryDirectory() as tmp:
        shots = os.path.join(tmp, 'shots')
        os.makedirs(shots)

        # ---- 1) stem conversion ----
        cases = [
            ('guidenh:visualtest/layout/details.md', 'visualtest_layout_details.md'),
            ('guidenh:visualtest/mermaid/mindmap.md', 'visualtest_mermaid_mindmap.md'),
            ('guidenh:visualtest/foo.md', 'visualtest_foo.md'),
        ]
        for page_id, want in cases:
            got = page_id_to_stem(page_id)
            check(f'stem conversion {page_id}', got == want,
                  f'got {got}, want {want}')

        # ---- 2) OK state ----
        ok_shot = os.path.join(shots,
                               'visualtest_layout_details.md_2026-08-02_120000.png')
        with open(ok_shot, 'wb') as f:
            f.write(b'x')
        list_ok = os.path.join(tmp, 'list_ok.txt')
        with open(list_ok, 'w', encoding='utf-8') as f:
            f.write('# comment line\n')
            f.write('guidenh:visualtest/layout/details.md\n')
        # Screenshot mtime older, list mtime slightly newer (< stale-min) -> OK
        os.utime(ok_shot, (1000000, 1000000))
        os.utime(list_ok, (1000000 + 60, 1000000 + 60))

        # ---- 3) STALE state: same screenshot but the list mtime is much newer ----
        stale_min = 10
        list_stale = os.path.join(tmp, 'list_stale.txt')
        with open(list_stale, 'w', encoding='utf-8') as f:
            f.write('guidenh:visualtest/layout/details.md\n')
        # List mtime 30 minutes later than the screenshot > stale-min 10 minutes
        os.utime(list_stale, (1000000 + 60 * 30, 1000000 + 60 * 30))

        # ---- 4) MISSING state: the list has a page but there is no screenshot ----
        list_missing = os.path.join(tmp, 'list_missing.txt')
        with open(list_missing, 'w', encoding='utf-8') as f:
            f.write('guidenh:visualtest/charts/pie.md\n')

        # Run the three decisions
        results_ok, nm, ns = evaluate(shots, list_ok, stale_min)
        check('OK detection', (nm, ns) == (0, 0)
              and results_ok[0][1] == 'OK',
              f'missing={nm} stale={ns} status={results_ok[0][1]}')

        results_stale, nm, ns = evaluate(shots, list_stale, stale_min)
        check('STALE detection', (nm, ns) == (0, 1)
              and results_stale[0][1] == 'STALE',
              f'missing={nm} stale={ns} status={results_stale[0][1]}')

        results_missing, nm, ns = evaluate(shots, list_missing, stale_min)
        check('MISSING detection', (nm, ns) == (1, 0)
              and results_missing[0][1] == 'MISSING',
              f'missing={nm} stale={ns} status={results_missing[0][1]}')

        # ---- 5) comment lines and blank lines are ignored ----
        list_comment = os.path.join(tmp, 'list_comment.txt')
        with open(list_comment, 'w', encoding='utf-8') as f:
            f.write('# only a comment\n\n')
            f.write('guidenh:visualtest/layout/details.md\n')
        results_comment, nm, ns = evaluate(shots, list_comment, stale_min)
        check('comment/blank handling', len(results_comment) == 1,
              f'{len(results_comment)} entries, want 1')

    n_fail = 0
    for i, (name, passed, detail) in enumerate(results, 1):
        if not passed:
            n_fail += 1
        print(f'{"ok" if passed else "not ok"} {i} - self-test: {name}'
              f' ({"OK" if passed else detail})')

    print(f'SUMMARY: selftest ok={len(results) - n_fail} failed={n_fail}')
    return 1 if n_fail else 0


def main():
    # UTF-8 console
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='screenshot freshness validator '
                    '(guards against stale page-list false positives)')
    parser.add_argument('--shots',
                        help='screenshot directory '
                             '(holding <page_stem>_YYYY-MM-DD_HHMMSS.png)')
    parser.add_argument('--list',
                        help='page list file (one guidenh:visualtest/<path>.md '
                             'per line, lines starting with # are comments)')
    parser.add_argument('--stale-min', type=int, default=10,
                        help='report STALE when a screenshot is more than N '
                             'minutes older than the list (default 10)')
    parser.add_argument('--self-test', action='store_true',
                        help='run the built-in smoke test '
                             '(OK/MISSING/STALE states) and exit')
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    if not args.shots or not args.list:
        eprint('[fatal] --shots and --list are required '
               '(or use --self-test)')
        return 2

    return run(args.shots, args.list, args.stale_min)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        eprint('[info] interrupted by user')
        sys.exit(130)
    except Exception as e:
        eprint(f'[fatal] unhandled exception: {e}')
        sys.exit(1)
