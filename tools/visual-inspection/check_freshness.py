#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
截图新鲜度校验器 (check_freshness)

用途（架构审计 A2 - P4）:
  防"陈旧截图清单误报"（F7c 教训）—— 校验页清单中每页是否都有最新批次
  截图、截图是否新鲜。配合无头渲染批次使用：

    py -3 tools/visual-inspection/check_freshness.py \\
        --shots run/client_new/screenshots_visualtest \\
        --list <page-list.txt> \\
        [--stale-min 10]

  退出码：全 OK 0；任何 MISSING / STALE = 1。

依赖：仅 Python 3 标准库（argparse / os / re / sys / tempfile / datetime）。
禁止引入任何第三方包。
"""

import argparse
import os
import re
import sys
import tempfile
from datetime import datetime


# 截图文件名正则（借鉴 assert_bounds.py TIMESTAMP_RE，扩展为 .png）
SHOT_RE = re.compile(r'^(.+)_(\d{4}-\d{2}-\d{2}_\d{6})\.png$')

# 页清单前缀：guidenh:visualtest/<path>.md
PAGE_PREFIX = 'guidenh:visualtest/'


def eprint(*args, **kwargs):
    """打印到 stderr"""
    print(*args, file=sys.stderr, **kwargs)


def page_id_to_stem(page_id):
    """页 ID → 截图 stem。

    guidenh:visualtest/layout/details.md -> visualtest_layout_details.md
    无前缀或路径分隔符已转译的输入同样被规范化。
    """
    s = page_id.strip()
    if s.startswith(PAGE_PREFIX):
        s = s[len(PAGE_PREFIX):]
    # 路径分隔符 / -> _
    s = s.replace('/', '_')
    if not s.startswith('visualtest_'):
        s = 'visualtest_' + s
    return s


def load_page_list(list_path):
    """读页清单。返回 [(line_no, page_id), ...]，'#' 开头与空行忽略。"""
    pages = []
    with open(list_path, 'r', encoding='utf-8', errors='replace') as f:
        for lineno, line in enumerate(f, 1):
            line = line.strip()
            if not line or line.startswith('#'):
                continue
            pages.append((lineno, line))
    return pages


def scan_shots(shots_dir):
    """扫描截图目录，按 stem 分组。返回 {stem: [(ts_str, path), ...]}。"""
    by_stem = {}
    try:
        entries = os.listdir(shots_dir)
    except OSError as e:
        eprint(f'[fatal] 无法读取截图目录 {shots_dir}: {e}')
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
    """文件名时间戳 'YYYY-MM-DD_HHMMSS' -> datetime（无法解析返回 None）。"""
    try:
        return datetime.strptime(ts_str, '%Y-%m-%d_%H%M%S')
    except ValueError:
        return None


def evaluate(shots_dir, list_path, stale_min):
    """核心判定。

    返回 (results, n_missing, n_stale)：
      results: [(page_id, status, detail), ...]，status ∈ OK / MISSING / STALE
    """
    pages = load_page_list(list_path)
    by_stem = scan_shots(shots_dir)

    try:
        list_mtime = os.path.getmtime(list_path)
    except OSError as e:
        eprint(f'[fatal] 无法读取清单文件 {list_path}: {e}')
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

        # 与 assert_bounds.py discover_pages 一致：按文件名时间戳取最新
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
    """执行校验，打印 TAP 风格输出，返回退出码。"""
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
    """内置冒烟测试：临时目录构造假截图/清单，验证 OK/MISSING/STALE 三态。"""
    results = []  # (case_name, passed, detail)

    def check(name, cond, detail=''):
        results.append((name, bool(cond), detail))

    with tempfile.TemporaryDirectory() as tmp:
        shots = os.path.join(tmp, 'shots')
        os.makedirs(shots)

        # ---- 1) stem 转换 ----
        cases = [
            ('guidenh:visualtest/layout/details.md', 'visualtest_layout_details.md'),
            ('guidenh:visualtest/mermaid/mindmap.md', 'visualtest_mermaid_mindmap.md'),
            ('guidenh:visualtest/foo.md', 'visualtest_foo.md'),
        ]
        for page_id, want in cases:
            got = page_id_to_stem(page_id)
            check(f'stem conversion {page_id}', got == want,
                  f'got {got}, want {want}')

        # ---- 2) OK 态 ----
        ok_shot = os.path.join(shots,
                               'visualtest_layout_details.md_2026-08-02_120000.png')
        with open(ok_shot, 'wb') as f:
            f.write(b'x')
        list_ok = os.path.join(tmp, 'list_ok.txt')
        with open(list_ok, 'w', encoding='utf-8') as f:
            f.write('# comment line\n')
            f.write('guidenh:visualtest/layout/details.md\n')
        # 截图 mtime 较旧、清单 mtime 稍新（< stale-min）-> OK
        os.utime(ok_shot, (1000000, 1000000))
        os.utime(list_ok, (1000000 + 60, 1000000 + 60))

        # ---- 3) STALE 态：同一张截图但清单 mtime 更新很多 ----
        stale_min = 10
        list_stale = os.path.join(tmp, 'list_stale.txt')
        with open(list_stale, 'w', encoding='utf-8') as f:
            f.write('guidenh:visualtest/layout/details.md\n')
        # 清单 mtime 比截图晚 30 分钟 > stale-min 10 分钟
        os.utime(list_stale, (1000000 + 60 * 30, 1000000 + 60 * 30))

        # ---- 4) MISSING 态：清单里有页但无截图 ----
        list_missing = os.path.join(tmp, 'list_missing.txt')
        with open(list_missing, 'w', encoding='utf-8') as f:
            f.write('guidenh:visualtest/charts/pie.md\n')

        # 执行三组判定
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

        # ---- 5) 注释行与空行忽略 ----
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
    # 控制台 UTF-8
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='截图新鲜度校验 — screenshot freshness validator '
                    '(A2/P4, guards against stale page-list false positives)')
    parser.add_argument('--shots',
                        help='截图目录（含 <page_stem>_YYYY-MM-DD_HHMMSS.png）')
    parser.add_argument('--list',
                        help='页清单文件（每行一个 guidenh:visualtest/<path>.md，'
                             '# 开头为注释）')
    parser.add_argument('--stale-min', type=int, default=10,
                        help='截图比清单旧超过 N 分钟即报 STALE（默认 10）')
    parser.add_argument('--self-test', action='store_true',
                        help='运行内置冒烟测试（OK/MISSING/STALE 三态）后退出')
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    if not args.shots or not args.list:
        eprint('[fatal] --shots 与 --list 为必填参数（或用 --self-test）')
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
