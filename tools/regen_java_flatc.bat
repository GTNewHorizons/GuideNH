@echo off
setlocal EnableDelayedExpansion
chcp 65001 >nul 2>&1

rem ============================================================
rem  GuideNH Java FlatBuffers regenerator  (tools/regen_java_flatc.bat)
rem
rem  Pipeline: locate flatc -> verify version 23.5.26
rem            -> flatc --java into temp dir -> diff checked-in classes
rem            -> (apply mode) copy over src/main/java -> hint to run gate
rem
rem  Modes:
rem    (no args)  apply       full regen, overwrite src/main/java
rem    --dry-run  dry         generate + diff report only, never overwrite
rem    --check-only  check    diff only, exit 1 if out of sync (CI)
rem    --help / -h            usage
rem ============================================================

for %%I in ("%~dp0..") do set "REPO_ROOT=%%~fI"
set "SCHEMA=%REPO_ROOT%\layout-engine\schema\guidenh_layout.fbs"
set "JAVA_DST=%REPO_ROOT%\src\main\java\com\hfstudio\guidenh\guide\layout\flatbuffers"
set "SCRIPT_NAME=%~nx0"

rem ---- argument parsing ----
set "MODE=apply"
:parse_args
if "%~1"=="" goto args_done
if /i "%~1"=="--help"       goto usage
if /i "%~1"=="-h"           goto usage
if /i "%~1"=="--dry-run" (
  set "MODE=dry"
  shift
  goto parse_args
)
if /i "%~1"=="--check-only" (
  set "MODE=check"
  shift
  goto parse_args
)
echo [ERROR] Unknown option: %~1
echo Hint: run !SCRIPT_NAME! --help for usage.
exit /b 2

:args_done
echo.
echo ============================================================
echo  GuideNH Java FlatBuffers regenerator (flatc 23.5.26)
echo  schema : !SCHEMA!
echo  dst    : !JAVA_DST!
echo  mode   : !MODE!    ^(apply=overwrite / dry-run=preview / check-only=CI check^)
echo  usage  : !SCRIPT_NAME! [--dry-run ^| --check-only]   no-arg = overwrite regen
echo ============================================================
echo.

rem ---- 1) locate flatc ----
set "FLATC_BIN="
if defined FLATC (
  set "FLATC_BIN=!FLATC!"
) else (
  for %%R in (E:\build_out\guide_nh_rust\debug\build E:\build_out\guide_nh_rust\release\build) do (
    if not defined FLATC_BIN (
      for /f "delims=" %%D in ('dir /b /ad /o-d "%%~R\flatc-*" 2^>nul') do (
        if not defined FLATC_BIN (
          if exist "%%~R\%%~D\out\bin\flatc.exe" set "FLATC_BIN=%%~R\%%~D\out\bin\flatc.exe"
        )
      )
    )
  )
)
if not defined FLATC_BIN (
  echo [ERROR] flatc.exe not found.
  echo   Option 1: build Rust side first to produce flatc:
  echo            cd layout-engine ^&^& cargo build --release
  echo            ^(expect it under E:\build_out\guide_nh_rust\{debug,release}\build\flatc-*\out\bin\flatc.exe^)
  echo   Option 2: set env var FLATC to point at flatc.exe
  exit /b 1
)
echo [ok] flatc located : !FLATC_BIN!

rem ---- 2) version check ----
set "FLATC_VERSION="
for /f "delims=" %%V in ('"!FLATC_BIN!" --version 2^>nul') do set "FLATC_VERSION=%%V"
echo !FLATC_VERSION! | findstr /c:"23.5.26" >nul
if errorlevel 1 (
  echo [ERROR] flatc version must be 23.5.26 ^(got: !FLATC_VERSION!^)
  echo         Java generated classes carry a Constants.FLATBUFFERS_23_5_26 version
  echo         guard and must match the flatbuffers-java runtime version.
  echo         See layout-engine/schema/README.md section 4 for upgrade policy.
  exit /b 1
)
echo [ok] flatc version : !FLATC_VERSION!

rem ---- 3) generate into temp dir ----
if not exist "!SCHEMA!" (
  echo [ERROR] schema not found: !SCHEMA!
  exit /b 1
)
set "OUT_DIR=%TEMP%\guidenh_flatc_regen_!RANDOM!!RANDOM!"
if exist "!OUT_DIR!" rmdir /s /q "!OUT_DIR!" >nul 2>&1
mkdir "!OUT_DIR!" >nul 2>&1
if errorlevel 1 (
  echo [ERROR] cannot create temp dir: !OUT_DIR!
  exit /b 1
)
set "GEN_DIR=!OUT_DIR!\com\hfstudio\guidenh\guide\layout\flatbuffers"
echo [gen] !FLATC_BIN! --java -o "!OUT_DIR!" "!SCHEMA!"
"!FLATC_BIN!" --java -o "!OUT_DIR!" "!SCHEMA!"
if errorlevel 1 (
  echo [ERROR] flatc --java generation failed.
  rmdir /s /q "!OUT_DIR!" >nul 2>&1
  exit /b 1
)
if not exist "!GEN_DIR!\*.java" (
  echo [ERROR] no .java generated ^(expected dir: !GEN_DIR!^)
  rmdir /s /q "!OUT_DIR!" >nul 2>&1
  exit /b 1
)

rem ---- 4) diff against checked-in classes ----
if not exist "!JAVA_DST!" mkdir "!JAVA_DST!" >nul 2>&1
set "CHANGED=0"
set "NEW_COUNT=0"
set "MOD_COUNT=0"
for %%F in ("!GEN_DIR!\*.java") do (
  set "BASE=%%~nxF"
  if not exist "!JAVA_DST!\!BASE!" (
    set /a CHANGED+=1
    set /a NEW_COUNT+=1
    echo   [NEW]     !BASE!
    if "!MODE!"=="apply" copy /y "%%F" "!JAVA_DST!\!BASE!" >nul
  ) else (
    fc /b "%%F" "!JAVA_DST!\!BASE!" >nul 2>&1
    if errorlevel 1 (
      set /a CHANGED+=1
      set /a MOD_COUNT+=1
      echo   [CHANGED] !BASE!
      if "!MODE!"=="apply" copy /y "%%F" "!JAVA_DST!\!BASE!" >nul
    )
  )
)
set "STALE_COUNT=0"
for %%F in ("!JAVA_DST!\*.java") do (
  set "BASE=%%~nxF"
  if not exist "!GEN_DIR!\!BASE!" (
    set /a STALE_COUNT+=1
    echo   [STALE]   !BASE!   ^(no such type in schema; wire-compat is append-only, script will not delete^)
  )
)

rem ---- 5) report ----
echo.
if !CHANGED!==0 (
  echo 无变更：生成类与检入类完全一致。
) else (
  echo [summary] out-of-sync files: !CHANGED!  ^(new !NEW_COUNT! / modified !MOD_COUNT!^)
  if "!MODE!"=="apply" (
    echo [done] copied changed files into src\main\java\com\hfstudio\guidenh\guide\layout\flatbuffers\
    echo [next] run the gate to verify:  .\gradlew compileJava compileTestJava test runLayoutDump
  ) else if "!MODE!"=="dry" (
    echo [dry-run] src\main\java untouched. Re-run without --dry-run to apply.
  ) else (
    echo [check-only] schema and checked-in classes are OUT OF SYNC, exiting 1.
  )
)
if !STALE_COUNT! GTR 0 echo [warn] !STALE_COUNT! checked-in class(es) no longer in schema; please confirm manually ^(wire-compat keeps them^).
rmdir /s /q "!OUT_DIR!" >nul 2>&1

rem NOTE: chcp 65001 makes cmd lose errorlevel for exit /b inside 2+ nested
rem blocks, so we set a flag inside and exit at top level.
set "FAIL=0"
if "!MODE!"=="check" (
  if !CHANGED! GTR 0 set "FAIL=1"
  if !STALE_COUNT! GTR 0 set "FAIL=1"
)
if !FAIL!==1 exit /b 1
exit /b 0

:usage
echo.
echo Usage: !SCRIPT_NAME! [--dry-run ^| --check-only]
echo.
echo   no-arg        full regen: locate flatc - verify version - generate - diff,
echo                 overwrite src\main\java on mismatch, then hint the gate.
echo   --dry-run     generate into temp dir and diff-report only; never overwrite.
echo   --check-only  diff only; exit 1 if out of sync ^(CI / manual sync check^).
echo   --help / -h   show this help.
echo.
echo Prerequisites:
echo   - flatc: env var FLATC wins; otherwise searched newest-first under
echo     E:\build_out\guide_nh_rust\{debug,release}\build\flatc-*\out\bin\flatc.exe
echo     ^(produced by cargo build --release in layout-engine via the flatc crate^)
echo   - flatc version must be 23.5.26 ^(= flatbuffers-java runtime; generated classes
echo     carry the FLATBUFFERS_23_5_26 version guard^)
echo   - schema: layout-engine\schema\guidenh_layout.fbs
echo.
echo See layout-engine\schema\README.md for change flow / version / wire-compat policy.
exit /b 0
