param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$MySqlPassword = 'password'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-http-common.ps1')

$toolsRoot = 'D:\ruoyi\dev-tools'
$projectRoot = 'D:\ruoyi\ruoyi-cloud'
$runtimeRoot = Join-Path $toolsRoot 'runtime'
$javaExe = Join-Path $toolsRoot 'jdk1.8.0_504\bin\java.exe'
$mysqlExe = Join-Path $toolsRoot 'mysql-5.7.44-winx64\bin\mysql.exe'
$mysqlAdminExe = Join-Path $toolsRoot 'mysql-5.7.44-winx64\bin\mysqladmin.exe'
$mysqlServerExe = Join-Path $toolsRoot 'mysql-5.7.44-winx64\bin\mysqld.exe'
$mysqlConfig = Join-Path $toolsRoot 'mysql57-instance\my.ini'
$nacosHome = Join-Path $toolsRoot 'nacos'
$groupA = 991000001
$script:Results = @()
$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'

function Add-QaCase([string]$Name, [bool]$Passed, [string]$Actual) {
    $script:Results += [pscustomobject]@{
        Case = $Name
        Status = $(if ($Passed) { 'PASS' } else { 'FAIL' })
        Actual = $Actual
    }
}

function Add-QaBlocked([string]$Name, [string]$Actual) {
    $script:Results += [pscustomobject]@{
        Case = $Name
        Status = 'BLOCKED'
        Actual = $Actual
    }
}

function Test-LocalPort([int]$Port) {
    try {
        $client = [Net.Sockets.TcpClient]::new()
        $result = $client.BeginConnect('127.0.0.1', $Port, $null, $null)
        $connected = $result.AsyncWaitHandle.WaitOne(500) -and $client.Connected
        $client.Dispose()
        return $connected
    }
    catch { return $false }
}

function Wait-Port([int]$Port, [bool]$Expected, [int]$Seconds = 60) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        if ((Test-LocalPort $Port) -eq $Expected) { return }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    throw "Port $Port did not reach expected state $Expected within $Seconds seconds"
}

function Get-ListenerPid([int]$Port) {
    $line = netstat -ano | Select-String ":$Port\s+.*LISTENING\s+" | Select-Object -First 1
    if ($null -eq $line) { return $null }
    return [int](($line.ToString() -split '\s+')[-1])
}

function Stop-App([string]$Name, [int]$Port) {
    $pidPath = Join-Path $runtimeRoot "pids\$Name.pid"
    $expectedPid = [int](Get-Content -LiteralPath $pidPath)
    $listenerPid = Get-ListenerPid $Port
    if ($listenerPid -ne $expectedPid) {
        throw "$Name PID file $expectedPid does not match port $Port listener $listenerPid"
    }
    Stop-Process -Id $expectedPid -Force
    Wait-Port $Port $false 30
}

function Start-App([string]$Name, [int]$Port, [string]$RelativeJar) {
    $jarPath = Join-Path $projectRoot $RelativeJar
    $process = Start-Process -FilePath $javaExe `
        -ArgumentList @('-Dfile.encoding=UTF-8', '-Xms128m', '-Xmx384m', '-jar', $jarPath) `
        -WorkingDirectory (Split-Path -Parent $jarPath) `
        -RedirectStandardOutput (Join-Path $runtimeRoot "logs\$Name.out.log") `
        -RedirectStandardError (Join-Path $runtimeRoot "logs\$Name.err.log") `
        -WindowStyle Hidden -PassThru
    Set-Content -LiteralPath (Join-Path $runtimeRoot "pids\$Name.pid") -Value $process.Id -Encoding ascii
    Wait-Port $Port $true 90
    Start-Sleep -Seconds 5
}

function Stop-PortProcess([int]$Port) {
    $listenerPid = Get-ListenerPid $Port
    if ($null -eq $listenerPid) { throw "No listener found on port $Port" }
    Stop-Process -Id $listenerPid -Force
    Wait-Port $Port $false 30
}

function Start-Nacos {
    $startup = Join-Path $nacosHome 'bin\startup.cmd'
    $arguments = "/d /c set JAVA_HOME=$(Join-Path $toolsRoot 'jdk1.8.0_504')&& `"$startup`" -m standalone"
    Start-Process -FilePath $env:ComSpec -ArgumentList $arguments `
        -WorkingDirectory (Join-Path $nacosHome 'bin') -WindowStyle Hidden
    Wait-Port 8848 $true 90
    Start-Sleep -Seconds 8
}

function Start-MySql {
    Start-Process -FilePath $mysqlServerExe -ArgumentList "--defaults-file=$mysqlConfig" -WindowStyle Hidden
    Wait-Port 3307 $true 60
    Start-Sleep -Seconds 5
}

function Get-DbScalar([string]$Database, [string]$Sql) {
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        $output = & $mysqlExe --host=127.0.0.1 --port=3307 --user=root `
            "--database=$Database" --batch --skip-column-names "--execute=$Sql"
        if ($LASTEXITCODE -ne 0) { throw "MySQL query failed: $Sql" }
        return ($output | Select-Object -Last 1)
    }
    finally { $env:MYSQL_PWD = $previousPassword }
}

function Invoke-Fund([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method -Path ('/ruoyi-fund' + $Path) -Token $Token -Body $Body
}

function Invoke-Research([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method -Path ('/ruoyi-research' + $Path) -Token $Token -Body $Body
}

function Wait-QaSuccess([scriptblock]$Request, [int]$Seconds = 60) {
    $deadline = (Get-Date).AddSeconds($Seconds)
    do {
        $response = & $Request
        if (Test-QaSuccessResponse $response) { return $response }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    return $response
}

Write-Output '[1/7] Reset fixtures and create persistent open/completed plans'
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $MySqlPassword
    $fixturePath = (Join-Path $PSScriptRoot 'qa-fixtures.sql').Replace('\', '/')
    & $mysqlExe --host=127.0.0.1 --port=3307 --user=root --default-character-set=utf8mb4 `
        "--execute=source $fixturePath" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'QA fixture reset failed' }
}
finally { $env:MYSQL_PWD = $previousPassword }

$adminToken = Get-QaToken -BaseUrl $BaseUrl -Username 'admin'
$leaderToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_leader'
$memberToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_member'
Assert-QaSuccess -Response (Invoke-Fund Post '/budget' $adminToken @{
    topicId=$groupA; totalAmount=1000.00; planEndTime='2026-12-31 23:59:59'; fundDesc='failure recovery budget'
}) -Label 'recovery budget' | Out-Null

$openName = "FAIL-OPEN-$stamp"
$completedName = "FAIL-COMPLETED-$stamp"
foreach ($plan in @(
    @{ Name=$openName; Amount=200.00 },
    @{ Name=$completedName; Amount=100.00 }
)) {
    Assert-QaSuccess -Response (Invoke-Fund Post '/use/plan' $leaderToken @{
        topicId=$groupA; useName=$plan.Name; planAmount=$plan.Amount; responsibleUserId=9103
        planTime='2026-08-28 10:00:00'; fundDesc='failure recovery plan'
    }) -Label "create $($plan.Name)" | Out-Null
}
$openPlanId = [long](Get-DbScalar 'ry-fund' "select use_plan_id from fund_use_plan where use_name='$openName'")
$completedPlanId = [long](Get-DbScalar 'ry-fund' "select use_plan_id from fund_use_plan where use_name='$completedName'")
Assert-QaSuccess -Response (Invoke-Fund Post '/use/record' $memberToken @{
    usePlanId=$completedPlanId; useName='exact completed record'; amount=100.00; useTime='2026-08-28 10:10:00'
}) -Label 'completed plan record' | Out-Null
Assert-QaSuccess -Response (Invoke-Fund Put "/use/plan/$completedPlanId/finish" $memberToken @{
    confirmDifference=$false; reason='normal completion'
}) -Label 'complete persistent plan' | Out-Null

Write-Output '[2/7] FAIL-01 Research down makes Fund permission checks fail closed'
Stop-App 'research' 9204
$closedResponse = Invoke-Fund Post '/use/record' $memberToken @{
    usePlanId=$openPlanId; useName='must fail while research down'; amount=10.00; useTime='2026-08-28 10:20:00'
}
Add-QaCase 'FAIL-01-RESEARCH-DOWN-FUND-FAIL-CLOSED' (-not (Test-QaSuccessResponse $closedResponse)) $closedResponse.Raw
Start-App 'research' 9204 'ruoyi-modules\ruoyi-research\target\ruoyi-modules-research.jar'
$recoveredRecord = Wait-QaSuccess { Invoke-Fund Post '/use/record' $memberToken @{
    usePlanId=$openPlanId; useName='research recovered record'; amount=10.00; useTime='2026-08-28 10:21:00'
} }
Add-QaCase 'FAIL-01-RESEARCH-RECOVERY' (Test-QaSuccessResponse $recoveredRecord) $recoveredRecord.Raw

Write-Output '[3/7] FAIL-02 Fund restart preserves open and completed plan state'
Stop-App 'fund' 9205
$fundDown = Invoke-Fund Get "/use/plan/$openPlanId" $memberToken
Add-QaCase 'FAIL-02-FUND-DOWN-NOT-ROUTABLE' (-not (Test-QaSuccessResponse $fundDown)) $fundDown.Raw
Start-App 'fund' 9205 'ruoyi-modules\ruoyi-fund\target\ruoyi-modules-fund.jar'
$openAfter = Wait-QaSuccess { Invoke-Fund Get "/use/plan/$openPlanId" $memberToken }
$completedAfter = Wait-QaSuccess { Invoke-Fund Get "/use/plan/$completedPlanId" $memberToken }
$completedWrite = Invoke-Fund Post '/use/record' $memberToken @{
    usePlanId=$completedPlanId; useName='must remain locked'; amount=1.00; useTime='2026-08-28 10:30:00'
}
$statesOk = (Test-QaSuccessResponse $openAfter) -and $openAfter.Body.data.status -eq '0' `
    -and (Test-QaSuccessResponse $completedAfter) -and $completedAfter.Body.data.status -eq '1' `
    -and -not (Test-QaSuccessResponse $completedWrite)
Add-QaCase 'FAIL-02-FUND-STATE-PERSISTED' $statesOk "open=$($openAfter.Body.data.status),completed=$($completedAfter.Body.data.status),locked=$(-not (Test-QaSuccessResponse $completedWrite))"

Write-Output '[4/7] FAIL-06 File service outage does not create attachment metadata'
$attachmentCountBefore = [int](Get-DbScalar 'ry-fund' "select count(*) from fund_attachment where group_id=$groupA")
Stop-App 'file' 9300
$failedUpload = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $memberToken -FileName 'file-down.txt' -Content ([Text.Encoding]::UTF8.GetBytes('file down')) -ContentType 'text/plain'
$attachmentCountAfter = [int](Get-DbScalar 'ry-fund' "select count(*) from fund_attachment where group_id=$groupA")
Add-QaCase 'FAIL-06-FILE-DOWN-NO-FALSE-METADATA' (-not (Test-QaSuccessResponse $failedUpload) -and $attachmentCountAfter -eq $attachmentCountBefore) "upload=$($failedUpload.Raw),before=$attachmentCountBefore,after=$attachmentCountAfter"
Start-App 'file' 9300 'ruoyi-modules\ruoyi-file\target\ruoyi-modules-file.jar'
$uploadAfter = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $memberToken -FileName 'file-recovered.txt' -Content ([Text.Encoding]::UTF8.GetBytes('file recovered')) -ContentType 'text/plain'
Add-QaCase 'FAIL-06-FILE-RECOVERY' (Test-QaSuccessResponse $uploadAfter) $uploadAfter.Raw

Write-Output '[5/7] FAIL-03 Redis restart permits a fresh login after recovery'
$beforeRedis = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/getInfo' -Token $memberToken
try {
    Stop-Service -Name Redis -Force
    Wait-Port 6379 $false 30
    $duringRedis = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/getInfo' -Token $memberToken
    Start-Service -Name Redis
    Wait-Port 6379 $true 60
    Start-Sleep -Seconds 3
    $newMemberToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_member'
    $afterRedis = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/getInfo' -Token $newMemberToken
    Add-QaCase 'FAIL-03-REDIS-RESTART-LOGIN-RECOVERY' ((Test-QaSuccessResponse $beforeRedis) -and -not (Test-QaSuccessResponse $duringRedis) -and (Test-QaSuccessResponse $afterRedis)) "before=$($beforeRedis.Raw),during=$($duringRedis.Raw),after=$($afterRedis.Raw)"
    $memberToken = $newMemberToken
}
catch {
    Add-QaBlocked 'FAIL-03-REDIS-RESTART-LOGIN-RECOVERY' ("Windows service control denied: " + $_.Exception.Message)
    if (-not (Test-LocalPort 6379)) {
        try { Start-Service -Name Redis } catch { }
        Wait-Port 6379 $true 60
    }
}

Write-Output '[6/7] FAIL-04 Nacos outage does not crash already running services'
Stop-PortProcess 8848
$systemAliveWhileNacosDown = Test-LocalPort 9201
$fundAliveWhileNacosDown = Test-LocalPort 9205
$fundWhileNacosDown = Invoke-Fund Get "/use/plan/$openPlanId" $memberToken
Add-QaCase 'FAIL-04-NACOS-DOWN-SERVICES-STAY-UP' ($systemAliveWhileNacosDown -and $fundAliveWhileNacosDown) "systemPort=$systemAliveWhileNacosDown,fundPort=$fundAliveWhileNacosDown,fundGateway=$($fundWhileNacosDown.Raw)"
Start-Nacos
$registry = Invoke-QaRawRequest -BaseUrl 'http://127.0.0.1:8848' -Method Get -Path '/nacos/v1/ns/instance/list?serviceName=ruoyi-research&healthyOnly=true'
$registryHosts = if ($null -ne $registry.Body.hosts) { @($registry.Body.hosts).Count } else { 0 }
Add-QaCase 'FAIL-04-NACOS-REGISTRATION-RECOVERY' ($registry.HttpStatus -eq 200 -and $registryHosts -gt 0) "hosts=$registryHosts,$($registry.Raw)"

Write-Output '[7/7] FAIL-05 Interrupted MySQL transaction rolls back completely'
$transactionBusinessId = 999990001
$transactionSql = "START TRANSACTION; INSERT INTO fund_operation_log(group_id,business_type,business_id,operation_type,reason,operator_id,operation_time) VALUES($groupA,'QA_TX',$transactionBusinessId,'MYSQL_INTERRUPTION','must rollback',1,NOW()); SELECT SLEEP(30); COMMIT;"
$previousPassword = $env:MYSQL_PWD
$transactionOut = Join-Path $runtimeRoot 'logs\mysql-interruption.out.log'
$transactionErr = Join-Path $runtimeRoot 'logs\mysql-interruption.err.log'
try {
    $env:MYSQL_PWD = $MySqlPassword
    $transaction = Start-Process -FilePath $mysqlExe -ArgumentList @(
        '--host=127.0.0.1', '--port=3307', '--user=root', '--database=ry-fund', "--execute=$transactionSql"
    ) -RedirectStandardOutput $transactionOut -RedirectStandardError $transactionErr -WindowStyle Hidden -PassThru
    Start-Sleep -Seconds 2
    & $mysqlAdminExe --host=127.0.0.1 --port=3307 --user=root shutdown
    Wait-Port 3307 $false 30
    $transaction.WaitForExit(30000) | Out-Null
    # The listener closes before InnoDB has fully released its files. Starting a
    # replacement immediately can race the old process during final shutdown.
    Start-Sleep -Seconds 8
}
finally { $env:MYSQL_PWD = $previousPassword }
Start-MySql
$rollbackCount = [int](Get-DbScalar 'ry-fund' "select count(*) from fund_operation_log where business_type='QA_TX' and business_id=$transactionBusinessId")
$fundAfterMySql = Wait-QaSuccess { Invoke-Fund Get "/use/plan/$openPlanId" $memberToken } 90
Add-QaCase 'FAIL-05-MYSQL-TRANSACTION-ROLLBACK' ($rollbackCount -eq 0 -and (Test-QaSuccessResponse $fundAfterMySql)) "rows=$rollbackCount,fund=$($fundAfterMySql.Raw)"

$script:Results | Format-Table -AutoSize -Wrap
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
$blocked = @($script:Results | Where-Object Status -eq 'BLOCKED')
[pscustomobject]@{
    Status = $(if ($failed.Count -gt 0) { 'FAIL' } elseif ($blocked.Count -gt 0) { 'BLOCKED' } else { 'PASS' })
    Total = $script:Results.Count
    Passed = $script:Results.Count - $failed.Count - $blocked.Count
    Failed = $failed.Count
    Blocked = $blocked.Count
    FailedCases = @($failed | ForEach-Object Case)
    BlockedCases = @($blocked | ForEach-Object Case)
} | ConvertTo-Json -Depth 5

if ($failed.Count -gt 0) { exit 1 }
if ($blocked.Count -gt 0) { exit 2 }
