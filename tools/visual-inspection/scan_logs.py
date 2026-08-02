#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
日志扫描器 (scan_logs)

用途（架构审计 A2 - P5，落实 WORKFLOW §3.1.5 强制项）:
  扫描渲染轮次的客户端日志，命中 `glyph atlas full` / `OutOfMemory` 等
  告警模式即视为失败轮次：

    py -3 tools/visual-inspection/scan_logs.py \\
        [--logs run/client_new/logs] \\
        [--pattern "glyph atlas full"] [--pattern "OutOfMemory"] \\
        [--self-test]

  退出码：无命中 0；任何命中 = 1。
  支持 *.log、*.log.gz（gzip 流式解压）与 latest.log。

依赖：仅 Python 3 标准库（argparse / os / gzip / sys / tempfile）。
禁止引入任何第三方包。
"""

import argparse
import gzip
import os
import sys
import tempfile

# 内置默认告警模式（WORKFLOW §3.1.5 / §5 Stage 3 强制项）
DEFAULT_PATTERNS = ['glyph atlas full', 'OutOfMemory']


def eprint(*args, **kwargs):
    """打印到 stderr"""
    print(*args, file=sys.stderr, **kwargs)


def find_log_files(logs_dir):
    """递归收集日志文件：*.log、*.log.gz 与 latest.log。返回排序路径列表。"""
    files = []
    if not os.path.isdir(logs_dir):
        eprint(f'[fatal] --logs 目录不存在: {logs_dir}')
        sys.exit(1)
    for root, _dirs, names in os.walk(logs_dir):
        for name in names:
            if (name.endswith('.log') or name.endswith('.log.gz')
                    or name == 'latest.log'):
                files.append(os.path.join(root, name))
    return sorted(files)


def scan_file(path, patterns):
    """扫描单个日志文件（*.gz 用 gzip 流式解压）。

    返回 (hits, error)：
      hits: [(lineno, pattern, line_text), ...]
      error: 读取失败时的描述（None 表示正常）
    """
    hits = []
    error = None
    lower_patterns = [p.lower() for p in patterns]

    try:
        opener = gzip.open if path.endswith('.gz') else open
        with opener(path, 'rt', encoding='utf-8', errors='replace') as f:
            for lineno, line in enumerate(f, 1):
                low = line.lower()
                for pat, low_pat in zip(patterns, lower_patterns):
                    if low_pat in low:
                        hits.append((lineno, pat, line.rstrip('\r\n')))
                        break
    except gzip.BadGzipFile as e:
        error = f'bad gzip file: {e}'
    except OSError as e:
        error = f'read error: {e}'
    except Exception as e:
        error = f'{type(e).__name__}: {e}'

    return hits, error


def run(logs_dir, patterns):
    """执行扫描，打印 TAP 风格输出，返回退出码。"""
    files = find_log_files(logs_dir)
    n = 0
    n_hits = 0
    n_files = 0
    errors = []

    for path in files:
        hits, err = scan_file(path, patterns)
        n_files += 1
        if err:
            errors.append((path, err))
            n += 1
            print(f'not ok {n} - {path}: ERROR ({err})')
            continue
        if not hits:
            n += 1
            print(f'ok {n} - {path}: clean')
            continue
        n_hits += len(hits)
        for lineno, pat, text in hits:
            n += 1
            print(f'not ok {n} - {path}:{lineno} matches "{pat}": {text}')

    if errors:
        for path, err in errors:
            eprint(f'[warn] 无法扫描 {path}: {err}')

    print(f'SUMMARY: files={n_files} hits={n_hits} errors={len(errors)}')
    return 1 if n_hits or errors else 0


def self_test():
    """内置冒烟测试：构造样本日志（含两种模式 + 噪声），验证命中与退出码。"""
    results = []  # (case_name, passed, detail)

    def check(name, cond, detail=''):
        results.append((name, bool(cond), detail))

    with tempfile.TemporaryDirectory() as tmp:
        # ---- 1) 普通 .log 含 glyph atlas full + 噪声 ----
        plain = os.path.join(tmp, 'plain.log')
        with open(plain, 'w', encoding='utf-8') as f:
            f.write('[main] INFO clean startup line\n')
            f.write('[main] WARN glyph atlas full, dropping glyph key=foo '
                    '(978x1110)\n')
            f.write('[main] INFO normal text mentioning atlas in prose\n')

        hits, err = scan_file(plain, DEFAULT_PATTERNS)
        check('plain log hit count', err is None and len(hits) == 1,
              f'hits={len(hits)} err={err}')

        # ---- 2) .log.gz 含 OutOfMemory ----
        gz_path = os.path.join(tmp, 'archive.log.gz')
        with gzip.open(gz_path, 'wt', encoding='utf-8') as f:
            f.write('[main] WARN OutOfMemoryError during render stage\n')
            f.write('[main] INFO another normal line\n')

        hits, err = scan_file(gz_path, DEFAULT_PATTERNS)
        check('gzip OutOfMemory hit', err is None and len(hits) == 1
              and hits[0][1] == 'OutOfMemory',
              f'hits={len(hits)} err={err}')

        # ---- 3) 无命中文件 ----
        clean = os.path.join(tmp, 'clean.log')
        with open(clean, 'w', encoding='utf-8') as f:
            f.write('[main] INFO clean render completed\n')

        hits, err = scan_file(clean, DEFAULT_PATTERNS)
        check('clean log no hits', err is None and len(hits) == 0,
              f'hits={len(hits)} err={err}')

        # ---- 4) 两种模式在同一文件内均命中 ----
        both = os.path.join(tmp, 'both.log')
        with open(both, 'w', encoding='utf-8') as f:
            f.write('handling OutOfMemoryError gracefully\n')
            f.write('glyph atlas full, dropping glyph key=big (900x1000)\n')

        hits, err = scan_file(both, DEFAULT_PATTERNS)
        check('both patterns matched', err is None and len(hits) == 2,
              f'hits={len(hits)} err={err}')

        # ---- 5) 目录级 run()：命中文件存在 -> exit 1 ----
        import io
        import contextlib
        with contextlib.redirect_stdout(io.StringIO()):
            rc = run(tmp, DEFAULT_PATTERNS)
        check('run() exit code with hits', rc == 1, f'rc={rc}')

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
        description='日志扫描 — client log scanner (A2/P5, WORKFLOW '
                    '§3.1.5 log-hygiene mandate)')
    parser.add_argument('--logs', default='run/client_new/logs',
                        help='日志目录（默认 run/client_new/logs，扫描 '
                             '*.log / *.log.gz / latest.log）')
    parser.add_argument('--pattern', action='append', dest='patterns',
                        help='额外/覆盖告警模式（子串匹配，可多次给出；'
                             '缺省用内置两条）')
    parser.add_argument('--self-test', action='store_true',
                        help='运行内置冒烟测试后退出')
    args = parser.parse_args()

    if args.self_test:
        return self_test()

    patterns = args.patterns if args.patterns else DEFAULT_PATTERNS
    return run(args.logs, patterns)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        eprint('[info] interrupted by user')
        sys.exit(130)
    except Exception as e:
        eprint(f'[fatal] unhandled exception: {e}')
        sys.exit(1)
