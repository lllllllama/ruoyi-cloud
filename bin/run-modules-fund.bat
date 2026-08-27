@echo off
echo.
echo [info] Starting Modules-Fund.
echo.

cd %~dp0
cd ../ruoyi-modules/ruoyi-fund/target

set JAVA_OPTS=-Xms256m -Xmx512m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m

java -Dfile.encoding=utf-8 %JAVA_OPTS% -jar ruoyi-modules-fund.jar

cd bin
pause
