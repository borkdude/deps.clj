@echo off

Rem set GRAALVM_HOME=C:\Users\IEUser\Downloads\graalvm\graalvm-ce-19.2.1
Rem set PATH=%PATH%;C:\Users\IEUser\bin

if "%GRAALVM_HOME%"=="" (
    echo Please set GRAALVM_HOME
    exit /b
)
set JAVA_HOME=%GRAALVM_HOME%\bin
set PATH=%PATH%;%GRAALVM_HOME%\bin

set /P DEPS_CLJ_VERSION=< resources\DEPS_CLJ_VERSION
echo Building deps.exe %DEPS_CLJ_VERSION%

call lein do clean, uberjar
if %errorlevel% neq 0 exit /b %errorlevel%

call %GRAALVM_HOME%\bin\native-image.cmd ^
  "-jar" "target/deps.clj-%DEPS_CLJ_VERSION%-standalone.jar" ^
  "-H:Name=deps" ^
  "-H:+ReportExceptionStackTraces" ^
  "-J-Dclojure.spec.skip-macros=true" ^
  "-J-Dclojure.compiler.direct-linking=true" ^
  "--initialize-at-build-time"  ^
  "-H:Log=registerResource:" ^
  "-H:EnableURLProtocols=http,https" ^
  "--enable-all-security-services" ^
  "--no-fallback" ^
  "--verbose" ^
  "-J-Xmx3g"

if %errorlevel% neq 0 exit /b %errorlevel%

echo Creating zip archive
jar -cMf deps.clj-%DEPS_CLJ_VERSION%-windows-amd64.zip deps.exe

echo Test run
call deps.exe -Spath -Sdeps "{:deps {borkdude/deps.clj {:mvn/version ""0.0.1""}}}"
