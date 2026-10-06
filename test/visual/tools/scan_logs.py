#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Log scanner (scan_logs)

Purpose:
  Scans the client log of a render round. A round is judged failed as soon as a
  warning pattern such as `glyph atlas full` or `OutOfMemory` is hit:

    py -3 test/visual/tools/scan_logs.py \\
        [--logs run/client_new/logs] \\
        [--pattern "glyph atlas full"] [--pattern "OutOfMemory"] \\
        [--self-test]

  Exit code: 0 when there is no hit; 1 on any hit.
  Supports *.log, *.log.gz (streamed gzip decompression) and latest.log.

Dependencies: Python 3 standard library only
(argparse / os / gzip / sys / tempfile).
Do not add any third-party package.
"""

import argparse
import gzip
import os
import sys
import tempfile

# Built-in default warning patterns (log hygiene for render rounds)
DEFAULT_PATTERNS = ['glyph atlas full', 'OutOfMemory']


def eprint(*args, **kwargs):
    """Print to stderr"""
    print(*args, file=sys.stderr, **kwargs)


def find_log_files(logs_dir):
    """Collect log files recursively: *.log, *.log.gz and latest.log.
    Returns a sorted list of paths."""
    files = []
    if not os.path.isdir(logs_dir):
        eprint(f'[fatal] --logs directory does not exist: {logs_dir}')
        sys.exit(1)
    for root, _dirs, names in os.walk(logs_dir):
        for name in names:
            if (name.endswith('.log') or name.endswith('.log.gz')
                    or name == 'latest.log'):
                files.append(os.path.join(root, name))
    return sorted(files)


def scan_file(path, patterns):
    """Scan a single log file (*.gz is decompressed as a gzip stream).

    Returns (hits, error):
      hits: [(lineno, pattern, line_text), ...]
      error: description of a read failure (None means normal)
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
    """Run the scan, print TAP-style output, return the exit code."""
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
            eprint(f'[warn] cannot scan {path}: {err}')

    print(f'SUMMARY: files={n_files} hits={n_hits} errors={len(errors)}')
    return 1 if n_hits or errors else 0


def self_test():
    """Built-in smoke test: build sample logs (both patterns plus noise) and
    verify the hits and the exit code."""
    results = []  # (case_name, passed, detail)

    def check(name, cond, detail=''):
        results.append((name, bool(cond), detail))

    with tempfile.TemporaryDirectory() as tmp:
        # ---- 1) plain .log with glyph atlas full plus noise ----
        plain = os.path.join(tmp, 'plain.log')
        with open(plain, 'w', encoding='utf-8') as f:
            f.write('[main] INFO clean startup line\n')
            f.write('[main] WARN glyph atlas full, dropping glyph key=foo '
                    '(978x1110)\n')
            f.write('[main] INFO normal text mentioning atlas in prose\n')

        hits, err = scan_file(plain, DEFAULT_PATTERNS)
        check('plain log hit count', err is None and len(hits) == 1,
              f'hits={len(hits)} err={err}')

        # ---- 2) .log.gz containing OutOfMemory ----
        gz_path = os.path.join(tmp, 'archive.log.gz')
        with gzip.open(gz_path, 'wt', encoding='utf-8') as f:
            f.write('[main] WARN OutOfMemoryError during render stage\n')
            f.write('[main] INFO another normal line\n')

        hits, err = scan_file(gz_path, DEFAULT_PATTERNS)
        check('gzip OutOfMemory hit', err is None and len(hits) == 1
              and hits[0][1] == 'OutOfMemory',
              f'hits={len(hits)} err={err}')

        # ---- 3) file without hits ----
        clean = os.path.join(tmp, 'clean.log')
        with open(clean, 'w', encoding='utf-8') as f:
            f.write('[main] INFO clean render completed\n')

        hits, err = scan_file(clean, DEFAULT_PATTERNS)
        check('clean log no hits', err is None and len(hits) == 0,
              f'hits={len(hits)} err={err}')

        # ---- 4) both patterns hit inside the same file ----
        both = os.path.join(tmp, 'both.log')
        with open(both, 'w', encoding='utf-8') as f:
            f.write('handling OutOfMemoryError gracefully\n')
            f.write('glyph atlas full, dropping glyph key=big (900x1000)\n')

        hits, err = scan_file(both, DEFAULT_PATTERNS)
        check('both patterns matched', err is None and len(hits) == 2,
              f'hits={len(hits)} err={err}')

        # ---- 5) directory-level run(): a file with hits exists -> exit 1 ----
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
    # UTF-8 console
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8', errors='replace')
        sys.stderr.reconfigure(encoding='utf-8', errors='replace')

    parser = argparse.ArgumentParser(
        description='log scanner - fails a render round whose client log '
                    'contains a warning pattern such as glyph atlas full or '
                    'OutOfMemory')
    parser.add_argument('--logs', default='run/client_new/logs',
                        help='log directory (default run/client_new/logs; '
                             'scans *.log / *.log.gz / latest.log)')
    parser.add_argument('--pattern', action='append', dest='patterns',
                        help='extra or overriding warning pattern '
                             '(substring match, may be given several times; '
                             'without it the two built-in patterns are used)')
    parser.add_argument('--self-test', action='store_true',
                        help='run the built-in smoke test and exit')
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
