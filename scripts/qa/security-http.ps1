param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$MySqlExe = 'D:\ruoyi\dev-tools\mysql-5.7.44-winx64\bin\mysql.exe',
    [string]$MySqlPassword = 'password'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-http-common.ps1')

$groupA = 991000001
$script:Results = @()
$frameworkName = 'SEC-XSS-FRAMEWORK'
$taskPayload = '<img src=x onerror="window.__qaXss=1">SEC-XSS-TASK'
$submissionPayload = '<svg onload="window.__qaXss=2">SEC-XSS-SUBMISSION'
$descriptionPayload = '<script>window.__qaXss=3</script>SEC-XSS-DESCRIPTION'

function Add-QaCase([string]$Name, [bool]$Passed, [string]$Actual) {
    $script:Results += [pscustomobject]@{
        Case = $Name
        Status = $(if ($Passed) { 'PASS' } else { 'FAIL' })
        Actual = $Actual
    }
}

function Test-Rejected([object]$Response) {
    return -not (Test-QaSuccessResponse $Response)
}

function Invoke-Research([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method `
        -Path ('/ruoyi-research' + $Path) -Token $Token -Body $Body
}

function Invoke-Fund([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method `
        -Path ('/ruoyi-fund' + $Path) -Token $Token -Body $Body
}

function Get-DbScalar([string]$Database, [string]$Sql) {
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        $output = & $MySqlExe --host=127.0.0.1 --port=3307 --user=root `
            "--database=$Database" --batch --skip-column-names "--execute=$Sql"
        if ($LASTEXITCODE -ne 0) { throw "MySQL query failed: $Sql" }
        return ($output | Select-Object -Last 1)
    }
    finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

Write-Output '[1/7] Reset isolated QA fixtures and login security roles'
$previousPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $MySqlPassword
    $fixturePath = (Join-Path $PSScriptRoot 'qa-fixtures.sql').Replace('\', '/')
    & $MySqlExe --host=127.0.0.1 --port=3307 --user=root `
        --default-character-set=utf8mb4 "--execute=source $fixturePath" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw 'QA fixture reset failed' }
}
finally {
    $env:MYSQL_PWD = $previousPassword
}

$adminToken = Get-QaToken -BaseUrl $BaseUrl -Username 'admin'
$leaderToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_leader'
$coreToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_core'
$memberToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_member'
$leaderBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_leader'
$outsiderToken = Get-QaToken -BaseUrl $BaseUrl -Username 'outsider'

Write-Output '[2/7] Seed an XSS-shaped task and pending submission through normal APIs'
$frameworkCreate = Invoke-Research Post '/framework' $leaderToken @{
    groupId = $groupA; frameworkName = $frameworkName; year = 2026; leadDeptId = 103
    overallGoal = 'Security rendering regression'; status = '0'; sort = 1; units = @()
}
Assert-QaSuccess -Response $frameworkCreate -Label 'security framework create' | Out-Null
$frameworkList = Invoke-Research Get ('/framework/list?frameworkName=' + [uri]::EscapeDataString($frameworkName) + '&pageNum=1&pageSize=10') $leaderToken
$framework = @((Assert-QaSuccess -Response $frameworkList -Label 'security framework list').rows) | Where-Object frameworkName -eq $frameworkName | Select-Object -First 1

$taskCreate = Invoke-Research Post '/task' $leaderToken @{
    frameworkId = $framework.frameworkId; groupId = $groupA; parentId = 0; level = 1
    taskName = $taskPayload; taskType = 'SECURITY'; description = $descriptionPayload
    deadline = '2026-12-31'; sort = 1
}
Assert-QaSuccess -Response $taskCreate -Label 'security task create' | Out-Null
$taskList = Invoke-Research Get ("/task/list?frameworkId=$($framework.frameworkId)") $leaderToken
$task = @((Assert-QaSuccess -Response $taskList -Label 'security task list').data) | Where-Object taskName -eq $taskPayload | Select-Object -First 1
Add-QaCase 'XSS-TASK-STORED-AS-LITERAL-DATA' ($task.taskName -eq $taskPayload -and $task.description -eq $descriptionPayload) $task.taskName

$deliverableCreate = Invoke-Research Post '/deliverable' $leaderToken @{
    groupId = $groupA; taskId = $task.taskId; deliverableName = 'SEC-XSS-DELIVERABLE'
    requirement = 'Security test'; requiredNum = 1; deadline = '2026-12-31'; isRequired = '1'; sort = 1
}
$deliverable = (Assert-QaSuccess -Response $deliverableCreate -Label 'security deliverable create').data
$submissionCreate = Invoke-Research Post '/submission' $coreToken @{
    deliverableId = $deliverable.deliverableId
    submissionName = $submissionPayload
    submissionDesc = $descriptionPayload
}
$submission = (Assert-QaSuccess -Response $submissionCreate -Label 'security submission create').data
Add-QaCase 'XSS-SUBMISSION-STORED-AS-LITERAL-DATA' ($submission.submissionName -eq $submissionPayload -and $submission.submissionDesc -eq $descriptionPayload) $submission.submissionName

$proofBytes = [Text.Encoding]::UTF8.GetBytes('security attachment proof')
$upload = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $coreToken -FileName 'security-proof.txt' -Content $proofBytes -ContentType 'text/plain'
$proofUrl = (Assert-QaSuccess -Response $upload -Label 'security proof upload').data.url
$attachmentCreate = Invoke-Research Post "/submission/$($submission.submissionId)/attachments" $coreToken @{
    fileName = 'security-proof.txt'; originalName = 'security-proof.txt'; fileUrl = $proofUrl
    fileSize = $proofBytes.Length; fileType = 'text/plain'
}
Assert-QaSuccess -Response $attachmentCreate -Label 'security attachment relation' | Out-Null
$attachmentId = [long](Get-DbScalar 'ry-research' "select max(attachment_id) from task_attachment where submission_id=$($submission.submissionId) and del_flag='0'")
Assert-QaSuccess -Response (Invoke-Research Put "/submission/$($submission.submissionId)/submit" $coreToken) -Label 'security submit' | Out-Null

Write-Output '[3/7] Verify anonymous requests and cross-group IDOR fail closed'
$anonymousCases = @(
    @{ Name='ANON-RESEARCH-TASK'; Response=(Invoke-Research Get "/task/$($task.taskId)" $null) },
    @{ Name='ANON-RESEARCH-SUBMISSION'; Response=(Invoke-Research Get "/submission/$($submission.submissionId)" $null) },
    @{ Name='ANON-RESEARCH-ATTACHMENT'; Response=(Invoke-Research Get "/submission/attachment/$attachmentId/download" $null) },
    @{ Name='ANON-FUND-USE'; Response=(Invoke-Fund Get "/use/plan/list?topicId=$groupA&pageNum=1&pageSize=10" $null) }
)
foreach ($case in $anonymousCases) {
    Add-QaCase $case.Name (Test-Rejected $case.Response) "HTTP=$($case.Response.HttpStatus),$($case.Response.Raw)"
}
$idorCases = @(
    @{ Name='IDOR-B-TASK'; Response=(Invoke-Research Get "/task/$($task.taskId)" $leaderBToken) },
    @{ Name='IDOR-B-SUBMISSION'; Response=(Invoke-Research Get "/submission/$($submission.submissionId)" $leaderBToken) },
    @{ Name='IDOR-B-ATTACHMENT'; Response=(Invoke-Research Get "/submission/attachment/$attachmentId/download" $leaderBToken) },
    @{ Name='IDOR-OUTSIDER-TASK'; Response=(Invoke-Research Get "/task/$($task.taskId)" $outsiderToken) },
    @{ Name='IDOR-OUTSIDER-ATTACHMENT'; Response=(Invoke-Research Get "/submission/attachment/$attachmentId/download" $outsiderToken) }
)
foreach ($case in $idorCases) {
    Add-QaCase $case.Name (Test-Rejected $case.Response) "HTTP=$($case.Response.HttpStatus),$($case.Response.Raw)"
}

Write-Output '[4/7] Verify privilege escalation attempts are rejected'
$privilegeCases = @(
    @{ Name='MEMBER-CREATE-FRAMEWORK'; Response=(Invoke-Research Post '/framework' $memberToken @{ groupId=$groupA; frameworkName='forged'; year=2026; leadDeptId=103 }) },
    @{ Name='MEMBER-ASSIGN-DELIVERABLE'; Response=(Invoke-Research Put "/deliverable/$($deliverable.deliverableId)/assignees" $memberToken @{ userIds=@(9103) }) },
    @{ Name='MEMBER-APPROVE-SUBMISSION'; Response=(Invoke-Research Put "/submission/$($submission.submissionId)/approve" $memberToken @{ opinion='forged approval' }) },
    @{ Name='MEMBER-CREATE-BUDGET'; Response=(Invoke-Fund Post '/budget' $memberToken @{ topicId=$groupA; totalAmount=100; planEndTime='2026-12-31 23:59:59' }) },
    @{ Name='OUTSIDER-CREATE-BUDGET'; Response=(Invoke-Fund Post '/budget' $outsiderToken @{ topicId=$groupA; totalAmount=100; planEndTime='2026-12-31 23:59:59' }) }
)
foreach ($case in $privilegeCases) {
    Add-QaCase $case.Name (Test-Rejected $case.Response) $case.Response.Raw
}

Write-Output '[5/7] Exercise SQL-injection-shaped search and audit inputs'
$injection = "' OR 1=1 --"
$encoded = [uri]::EscapeDataString($injection)
$groupInjection = Invoke-Research Get "/group/list?groupName=$encoded&pageNum=1&pageSize=100" $adminToken
$groupRows = @((Assert-QaSuccess -Response $groupInjection -Label 'group injection search').rows)
Add-QaCase 'SQLI-GROUP-NAME-NO-BYPASS' ($groupRows.Count -eq 0) "rows=$($groupRows.Count)"
$taskInjection = Invoke-Research Get "/task/list?frameworkId=$($framework.frameworkId)&taskName=$encoded" $leaderToken
$taskRows = @((Assert-QaSuccess -Response $taskInjection -Label 'task injection search').data)
Add-QaCase 'SQLI-TASK-NAME-NO-BYPASS' ($taskRows.Count -eq 0) "rows=$($taskRows.Count)"
$submissionInjection = Invoke-Research Get "/submission/list?groupId=$groupA&submissionName=$encoded&pageNum=1&pageSize=100" $leaderToken
$submissionRows = @((Assert-QaSuccess -Response $submissionInjection -Label 'submission injection search').rows)
Add-QaCase 'SQLI-SUBMISSION-NAME-NO-BYPASS' ($submissionRows.Count -eq 0) "rows=$($submissionRows.Count)"
$rejectInjection = Invoke-Research Put "/submission/$($submission.submissionId)/reject" $leaderToken @{ opinion=$injection }
Assert-QaSuccess -Response $rejectInjection -Label 'literal audit opinion' | Out-Null
$auditResponse = Invoke-Research Get "/submission/$($submission.submissionId)/audits" $leaderToken
$audits = @((Assert-QaSuccess -Response $auditResponse -Label 'audit history after injection').data)
$lastAudit = $audits | Select-Object -Last 1
Add-QaCase 'SQLI-AUDIT-OPINION-STORED-LITERALLY' ($lastAudit.auditOpinion -eq $injection -and $lastAudit.action -eq 'REJECT') "$($lastAudit.action):$($lastAudit.auditOpinion)"
Assert-QaSuccess -Response (Invoke-Research Put "/submission/$($submission.submissionId)/resubmit" $coreToken @{ opinion='restore pending state for browser XSS test' }) -Label 'restore pending XSS submission' | Out-Null

Write-Output '[6/7] Verify hostile filenames cannot escape the configured upload path'
$fileCases = @('../escape.txt', '..\escape.txt', 'fake.exe.pdf', 'double.pdf.exe', '空 格@#$%.txt')
$uploadedUrls = @()
foreach ($fileName in $fileCases) {
    try {
        $response = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $coreToken -FileName $fileName -Content $proofBytes -ContentType 'application/octet-stream'
        $safe = Test-Rejected $response
        $actual = $response.Raw
        if (Test-QaSuccessResponse $response) {
            $url = $response.Body.data.url
            $uploadedUrls += $url
            $uri = [uri]$url
            $safe = -not $uri.AbsolutePath.Contains('..') -and -not $uri.AbsolutePath.Contains('escape')
            $actual = $url
        }
        Add-QaCase ("FILE-NAME-SAFE-" + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($fileName))) $safe $actual
    }
    catch {
        Add-QaCase ("FILE-NAME-CLIENT-REJECTED-" + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($fileName))) $true $_.Exception.Message
    }
}
$duplicate1 = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $coreToken -FileName 'duplicate.pdf' -Content $proofBytes -ContentType 'application/pdf'
$duplicate2 = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $coreToken -FileName 'duplicate.pdf' -Content $proofBytes -ContentType 'application/pdf'
$duplicateUrl1 = (Assert-QaSuccess -Response $duplicate1 -Label 'duplicate upload 1').data.url
$duplicateUrl2 = (Assert-QaSuccess -Response $duplicate2 -Label 'duplicate upload 2').data.url
Add-QaCase 'FILE-DUPLICATE-NAME-UNIQUE-STORAGE' ($duplicateUrl1 -ne $duplicateUrl2) "$duplicateUrl1 != $duplicateUrl2"

Write-Output '[7/7] Security HTTP summary'
$script:Results | Format-Table -AutoSize -Wrap
$failed = @($script:Results | Where-Object Status -eq 'FAIL')
[pscustomobject]@{
    Status = $(if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' })
    Total = $script:Results.Count
    Passed = $script:Results.Count - $failed.Count
    Failed = $failed.Count
    FailedCases = @($failed | ForEach-Object Case)
    FrameworkName = $frameworkName
    TaskPayload = $taskPayload
    SubmissionPayload = $submissionPayload
} | ConvertTo-Json -Depth 5

if ($failed.Count -gt 0) { exit 1 }
