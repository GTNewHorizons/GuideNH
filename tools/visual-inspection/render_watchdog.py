#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Render Watchdog — wrapper for headless render commands to prevent orphaned JVM processes.

Usage:
    py -3 tools/visual-inspection/render_watchdog.py --timeout 480 -- ./gradlew runClient25 -Dguidenh.headlessRender=true ...

This script:
  1. Takes a pre-snapshot of all java processes.
  2. Launches the wrapped command with stdout/stderr redirected to a log file.
  3. Monitors the process every 5 s, provides heartbeats every 30 s.
  4. On timeout: kills the process tree (/T /F) and any orphaned JVMs matching
     patterns (run/client_new, guidenh, launchwrapper, plus --kill-extra regex).
  5. On normal exit: checks for orphaned JVMs, waits 10 s, kills survivors.
"""

import argparse
import os
import re
import subprocess
import sys
import time


def parse_args():
    """Parse command line arguments.

    Returns (args, command_list).  Exits with code 2 on error or --help.
    """
    parser = argparse.ArgumentParser(
        description='Watchdog wrapper for headless render commands.',
        add_help=False,
    )
    parser.add_argument('--timeout', type=int, default=480,
                        help='Timeout in seconds (default: 480)')
    parser.add_argument('--log', type=str,
                        default='C:/Temp/opencode/watchdog_last.log',
                        help='Log file path (default: C:/Temp/opencode/watchdog_last.log)')
    parser.add_argument('--kill-extra', type=str, default='',
                        help='Additional regex pattern for matching orphan JVMs')

    # --help / -h → print usage and exit non-zero
    if '--help' in sys.argv or '-h' in sys.argv:
        parser.print_help()
        print()
        print('Usage:')
        print('  py -3 render_watchdog.py [--timeout SEC] [--log PATH] [--kill-extra REGEX] -- COMMAND...')
        sys.exit(2)

    # Locate the -- separator
    try:
        dash_idx = sys.argv.index('--')
    except ValueError:
        print('Error: missing "--" separator before command', file=sys.stderr)
        parser.print_help()
        sys.exit(2)

    # Parse options before --
    args, _ = parser.parse_known_args(sys.argv[1:dash_idx])

    # Everything after -- is the command
    command = sys.argv[dash_idx + 1:]

    if not command:
        print('Error: empty command after "--"', file=sys.stderr)
        sys.exit(2)

    return args, command


def enum_java_procs():
    """Enumerate all java processes returning {pid: cmdline}.

    Tries WMIC first, falls back to PowerShell.  Returns {} on total failure.
    """
    result = {}

    # -- attempt WMIC ---------------------------------------------------------
    try:
        cmd = [
            'wmic', 'process', 'where', "name like 'java%'",
            'get', 'processid,commandline', '/format:csv',
        ]
        proc = subprocess.run(cmd, capture_output=True, text=True, timeout=15)
        if proc.returncode == 0:
            for line in proc.stdout.strip().splitlines():
                line = line.strip()
                if not line or line.startswith('Node,ProcessId'):
                    continue
                # CSV columns: Node,ProcessId,CommandLine
                parts = line.split(',', 2)
                if len(parts) >= 2:
                    pid_str = parts[1].strip()
                    cmdline = parts[2].strip() if len(parts) > 2 else ''
                    if pid_str.isdigit():
                        result[int(pid_str)] = cmdline
            if result:
                return result
    except Exception as exc:
        print(f'[watchdog] WARNING: WMIC enumeration failed: {exc}',
              file=sys.stderr)

    # -- fallback PowerShell --------------------------------------------------
    try:
        ps_cmd = [
            'powershell', '-NoProfile', '-Command',
            'Get-CimInstance Win32_Process -Filter "Name like \'java%\'" | '
            'ForEach-Object { $_.ProcessId.ToString() + \'|\' + $_.CommandLine }',
        ]
        proc = subprocess.run(ps_cmd, capture_output=True, text=True, timeout=30)
        if proc.returncode == 0:
            for line in proc.stdout.strip().splitlines():
                line = line.strip()
                if not line or '|' not in line:
                    continue
                pid_str, _, cmdline = line.partition('|')
                if pid_str.strip().isdigit():
                    result[int(pid_str.strip())] = cmdline.strip()
        else:
            print(f'[watchdog] WARNING: PowerShell enumeration failed '
                  f'(rc={proc.returncode}): {proc.stderr.strip()}',
                  file=sys.stderr)
    except Exception as exc:
        print(f'[watchdog] WARNING: PowerShell enumeration failed: {exc}',
              file=sys.stderr)

    return result


def matches_orphan_pattern(cmdline, extra_regex):
    """Check if *cmdline* matches any orphan JVM pattern.

    *extra_regex* may be a compiled ``re.Pattern`` or *None*.
    """
    keywords = ['run/client_new', 'guidenh', 'launchwrapper']
    for kw in keywords:
        if kw in cmdline:
            return True
    if extra_regex is not None and extra_regex.search(cmdline):
        return True
    return False


def find_orphan_javas(before_procs, extra_pattern=None):
    """Return [(pid, cmdline), …] for java processes after *before_procs*.

    Only processes whose command line matches one of the orphan patterns
    (run/client_new / guidenh / launchwrapper, plus the optional regex)
    are included.
    """
    extra_regex = re.compile(extra_pattern) if extra_pattern else None
    after_procs = enum_java_procs()
    return [
        (pid, cmdline)
        for pid, cmdline in after_procs.items()
        if pid not in before_procs and matches_orphan_pattern(cmdline, extra_regex)
    ]


def taskkill_tree(pid):
    """Kill process tree *pid* with ``taskkill /T /F``.

    Errors are printed as warnings (non-fatal).
    """
    try:
        subprocess.run(
            ['taskkill', '/PID', str(pid), '/T', '/F'],
            capture_output=True, text=True, timeout=10,
        )
    except Exception as exc:
        print(f'[watchdog] WARNING: taskkill tree PID {pid} failed: {exc}',
              file=sys.stderr)


def kill_pid(pid):
    """Kill a single process by *pid* with ``taskkill /F``.

    Errors are printed as warnings (non-fatal).
    """
    try:
        subprocess.run(
            ['taskkill', '/PID', str(pid), '/F'],
            capture_output=True, text=True, timeout=10,
        )
    except Exception as exc:
        print(f'[watchdog] WARNING: taskkill PID {pid} failed: {exc}',
              file=sys.stderr)


def read_last_lines(filepath, n=15):
    """Return the last *n* lines of *filepath* as a list of strings.

    On any error a single-element list with an error message is returned.
    """
    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as fh:
            lines = fh.readlines()
        return lines[-n:] if len(lines) >= n else lines
    except Exception as exc:
        return [f'<error reading log: {exc}>']


def print_summary(retcode, elapsed, log_path):
    """Print final summary (exit code, elapsed, log path, last 15 lines)."""
    print(f'[watchdog] exit code: {retcode}')
    print(f'[watchdog] elapsed: {elapsed:.1f}s')
    print(f'[watchdog] log: {log_path}')
    print('--- last 15 log lines ---')
    for line in read_last_lines(log_path, 15):
        sys.stdout.write(line.rstrip('\r\n') + '\n')


def main():
    """Entry point."""
    # Ensure UTF-8 output on Windows
    if hasattr(sys.stdout, 'reconfigure'):
        sys.stdout.reconfigure(encoding='utf-8')
    if hasattr(sys.stderr, 'reconfigure'):
        sys.stderr.reconfigure(encoding='utf-8')

    args, command = parse_args()

    timeout = args.timeout
    log_path = args.log
    extra_pattern = args.kill_extra  # empty string means no extra pattern

    # Create log directory if needed
    log_dir = os.path.dirname(log_path)
    if log_dir:
        os.makedirs(log_dir, exist_ok=True)

    # ---- pre-snapshot -------------------------------------------------------
    before_procs = enum_java_procs()
    if not before_procs:
        print('[watchdog] WARNING: pre-snapshot returned no java processes '
              '(enumeration may have failed)', file=sys.stderr)

    # ---- launch wrapped command ---------------------------------------------
    start_time = time.time()
    print(f'[watchdog] starting: {" ".join(command)}')
    print(f'[watchdog] timeout: {timeout}s, log: {log_path}')

    try:
        with open(log_path, 'w', encoding='utf-8') as log_fh:
            proc = subprocess.Popen(command, stdout=log_fh, stderr=log_fh)
    except Exception as exc:
        print(f'[watchdog] ERROR: failed to start process: {exc}',
              file=sys.stderr)
        sys.exit(2)

    last_heartbeat = 0.0
    timed_out = False

    # ---- monitoring loop ----------------------------------------------------
    while True:
        time.sleep(5)
        ret = proc.poll()
        elapsed = time.time() - start_time

        # -- process exited ---------------------------------------------------
        if ret is not None:
            if not timed_out:
                # Normal exit → orphan check
                orphans = find_orphan_javas(before_procs, extra_pattern)
                if orphans:
                    print(f'[watchdog] WARNING: {len(orphans)} orphan JVM(s) '
                          f'detected after process exit, waiting 10 s…')
                    time.sleep(10)
                    current_procs = enum_java_procs()
                    still_alive = [
                        (pid, cmdline)
                        for pid, cmdline in orphans
                        if pid in current_procs
                    ]
                    for pid, cmdline in still_alive:
                        print(f'[watchdog] killing orphan JVM PID {pid}: '
                              f'{cmdline[:120]}')
                        kill_pid(pid)
                    print(f'[watchdog] killed {len(still_alive)} orphan JVM(s)')
                print_summary(ret, elapsed, log_path)
            sys.exit(ret)

        # -- timeout ----------------------------------------------------------
        if elapsed > timeout and not timed_out:
            timed_out = True
            print(f'[watchdog] TIMEOUT after {int(elapsed)}s, starting cleanup…')
            # 4a. Kill entire process tree
            taskkill_tree(proc.pid)
            # 4b. Wait and mop up orphaned JVMs
            time.sleep(5)
            orphans = find_orphan_javas(before_procs, extra_pattern)
            for pid, cmdline in orphans:
                print(f'[watchdog] killing orphan JVM PID {pid}: {cmdline[:120]}')
                kill_pid(pid)
            print(f'[watchdog] TIMEOUT after {int(elapsed)}s, killed tree + '
                  f'{len(orphans)} orphan JVM(s)')
            sys.exit(124)

        # -- heartbeat --------------------------------------------------------
        if elapsed - last_heartbeat >= 30:
            log_size = 0
            try:
                log_size = os.path.getsize(log_path)
            except OSError:
                pass
            print(f'[watchdog] heartbeat: {int(elapsed)}s elapsed, '
                  f'log size {log_size} bytes')
            last_heartbeat = elapsed


if __name__ == '__main__':
    main()
