param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$Username = 'admin',
    [string]$Password = 'admin123',
    [string]$RedisHost = '127.0.0.1',
    [int]$RedisPort = 6379
)

$ErrorActionPreference = 'Stop'
$script:AccessToken = $null

function Get-CaptchaAnswer([string]$Uuid) {
    $key = 'captcha_codes:' + $Uuid
    $client = New-Object System.Net.Sockets.TcpClient($RedisHost, $RedisPort)
    try {
        $stream = $client.GetStream()
        $command = "*2`r`n`$3`r`nGET`r`n`$$($key.Length)`r`n$key`r`n"
        $bytes = [Text.Encoding]::UTF8.GetBytes($command)
        $stream.Write($bytes, 0, $bytes.Length)
        $buffer = New-Object byte[] 1024
        $read = $stream.Read($buffer, 0, $buffer.Length)
        $reply = [Text.Encoding]::UTF8.GetString($buffer, 0, $read)
        $match = [regex]::Match($reply, '"([^\"]+)"')
        if (-not $match.Success) {
            throw "Unable to read captcha answer from Redis: $reply"
        }
        return $match.Groups[1].Value
    }
    finally {
        $client.Close()
    }
}

function Invoke-SmokeApi(
    [string]$Method,
    [string]$Path,
    [object]$Body = $null,
    [switch]$Anonymous
) {
    $params = @{
        Method = $Method
        Uri = $BaseUrl + $Path
        ContentType = 'application/json'
    }
    if (-not $Anonymous) {
        $params.Headers = @{ Authorization = 'Bearer ' + $script:AccessToken }
    }
    if ($null -ne $Body) {
        $params.Body = $Body | ConvertTo-Json -Depth 12
    }
    $response = Invoke-RestMethod @params
    if ($null -ne $response.code -and [int]$response.code -ne 200) {
        throw "$Method $Path failed: code=$($response.code), msg=$($response.msg)"
    }
    return $response
}

function Require-Value([object]$Value, [string]$Message) {
    if ($null -eq $Value) {
        throw $Message
    }
    return $Value
}

Write-Output '[1/8] Login through gateway'
$captcha = Invoke-SmokeApi -Method Get -Path '/code' -Anonymous
$captchaAnswer = Get-CaptchaAnswer $captcha.uuid
$login = Invoke-SmokeApi -Method Post -Path '/auth/login' -Anonymous -Body @{
    username = $Username
    password = $Password
    code = $captchaAnswer
    uuid = $captcha.uuid
}
$script:AccessToken = Require-Value $login.data.access_token 'Login response did not contain an access token'

$stamp = Get-Date -Format 'yyyyMMddHHmmss'
$groupCode = 'SMOKE-' + $stamp
$groupName = 'T69 smoke group ' + $stamp
$leadDeptId = 103
$participantDeptId = 104
$adminUserId = 1

Write-Output '[2/8] Create research group, units and leader member'
Invoke-SmokeApi -Method Post -Path '/ruoyi-research/group' -Body @{
    groupCode = $groupCode
    groupName = $groupName
    leadDeptId = $leadDeptId
    description = 'Created by the repeatable T69 smoke test'
    status = '0'
    sort = 0
    units = @(
        @{ deptId = $leadDeptId; unitType = 'LEAD'; managerUserId = $adminUserId; status = '0' },
        @{ deptId = $participantDeptId; unitType = 'PARTICIPANT'; status = '0' }
    )
} | Out-Null
$groupList = Invoke-SmokeApi -Method Get -Path ('/ruoyi-research/group/list?groupCode=' + [uri]::EscapeDataString($groupCode) + '&pageNum=1&pageSize=10')
$group = @($groupList.rows) | Where-Object { $_.groupCode -eq $groupCode } | Select-Object -First 1
$groupId = (Require-Value $group 'Created research group was not returned by list API').groupId
Invoke-SmokeApi -Method Post -Path "/ruoyi-research/group/$groupId/member" -Body @{
    userId = $adminUserId
    deptId = $leadDeptId
    memberRole = 'LEADER'
    status = '0'
} | Out-Null
$members = Invoke-SmokeApi -Method Get -Path "/ruoyi-research/group/$groupId/member/list"
if (-not (@($members.data) | Where-Object { $_.userId -eq $adminUserId -and $_.memberRole -eq 'LEADER' })) {
    throw 'Leader membership was not persisted'
}

Write-Output '[3/8] Create budget and complete allocation plan'
Invoke-SmokeApi -Method Post -Path '/ruoyi-fund/budget' -Body @{
    topicId = $groupId
    totalAmount = 100.00
    planEndTime = '2026-12-31 23:59:59'
    fundDesc = 'T69 smoke budget'
} | Out-Null
$budgetResponse = Invoke-SmokeApi -Method Get -Path "/ruoyi-fund/budget/topic/$groupId"
$budgetId = (Require-Value $budgetResponse.data 'Budget was not persisted').budgetId
$allocationName = 'T69 allocation ' + $stamp
Invoke-SmokeApi -Method Post -Path '/ruoyi-fund/allocation/plan' -Body @{
    topicId = $groupId
    allocationName = $allocationName
    allocationDeptId = $leadDeptId
    receiveDeptId = $participantDeptId
    planAmount = 40.00
    planTime = '2026-08-28 10:00:00'
    fundDesc = 'T69 smoke allocation plan'
    responsibleUserId = $adminUserId
} | Out-Null
$allocationList = Invoke-SmokeApi -Method Get -Path ("/ruoyi-fund/allocation/plan/list?topicId=$groupId&pageNum=1&pageSize=50")
$allocationPlan = @($allocationList.rows) | Where-Object { $_.allocationName -eq $allocationName } | Select-Object -First 1
$allocationPlanId = (Require-Value $allocationPlan 'Allocation plan was not persisted').planId
Invoke-SmokeApi -Method Post -Path '/ruoyi-fund/allocation/record' -Body @{
    planId = $allocationPlanId
    allocationName = 'T69 allocation record'
    amount = 40.00
    allocationTime = '2026-08-28 10:30:00'
    fundDesc = 'T69 exact allocation'
} | Out-Null
Invoke-SmokeApi -Method Put -Path "/ruoyi-fund/allocation/plan/$allocationPlanId/finish" -Body @{
    confirmDifference = $false
    reason = 'T69 exact amount completion'
} | Out-Null
$finishedAllocation = (Invoke-SmokeApi -Method Get -Path "/ruoyi-fund/allocation/plan/$allocationPlanId").data
if ($finishedAllocation.status -ne '1' -or [decimal]$finishedAllocation.actualAmount -ne 40.00) {
    throw 'Allocation plan did not finish with the expected actual amount'
}

Write-Output '[4/8] Create and complete use plan'
$useName = 'T69 use ' + $stamp
Invoke-SmokeApi -Method Post -Path '/ruoyi-fund/use/plan' -Body @{
    topicId = $groupId
    useName = $useName
    planAmount = 30.00
    responsibleUserId = $adminUserId
    planTime = '2026-08-28 11:00:00'
    fundDesc = 'T69 smoke use plan'
} | Out-Null
$useList = Invoke-SmokeApi -Method Get -Path ("/ruoyi-fund/use/plan/list?topicId=$groupId&pageNum=1&pageSize=50")
$usePlan = @($useList.rows) | Where-Object { $_.useName -eq $useName } | Select-Object -First 1
$usePlanId = (Require-Value $usePlan 'Use plan was not persisted').usePlanId
Invoke-SmokeApi -Method Post -Path '/ruoyi-fund/use/record' -Body @{
    usePlanId = $usePlanId
    useName = 'T69 use record'
    amount = 30.00
    useTime = '2026-08-28 11:30:00'
    fundDesc = 'T69 exact use'
} | Out-Null
Invoke-SmokeApi -Method Put -Path "/ruoyi-fund/use/plan/$usePlanId/finish" -Body @{
    confirmDifference = $false
    reason = 'T69 exact amount completion'
} | Out-Null
$finishedUse = (Invoke-SmokeApi -Method Get -Path "/ruoyi-fund/use/plan/$usePlanId").data
if ($finishedUse.status -ne '1' -or [decimal]$finishedUse.actualAmount -ne 30.00) {
    throw 'Use plan did not finish with the expected actual amount'
}

Write-Output '[5/8] Create annual framework and three-level task tree'
$frameworkName = 'T69 framework ' + $stamp
Invoke-SmokeApi -Method Post -Path '/ruoyi-research/framework' -Body @{
    groupId = $groupId
    frameworkName = $frameworkName
    year = 2026
    leadDeptId = $leadDeptId
    overallGoal = 'Verify the complete research task workflow'
    status = '0'
    sort = 0
    units = @(@{ deptId = $participantDeptId })
} | Out-Null
$frameworks = Invoke-SmokeApi -Method Get -Path ("/ruoyi-research/framework/list?groupId=$groupId&pageNum=1&pageSize=50")
$framework = @($frameworks.rows) | Where-Object { $_.frameworkName -eq $frameworkName } | Select-Object -First 1
$frameworkId = (Require-Value $framework 'Annual framework was not persisted').frameworkId

$rootName = 'T69 root ' + $stamp
$middleName = 'T69 middle ' + $stamp
$leafName = 'T69 leaf ' + $stamp
Invoke-SmokeApi -Method Post -Path '/ruoyi-research/task' -Body @{
    frameworkId = $frameworkId; groupId = $groupId; parentId = 0; level = 1
    taskName = $rootName; taskType = 'ROOT'; description = 'T69 level 1'; sort = 1
} | Out-Null
$tasks = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/list?frameworkId=$frameworkId").data
$rootTask = @($tasks) | Where-Object { $_.taskName -eq $rootName } | Select-Object -First 1
$rootTaskId = (Require-Value $rootTask 'Root task was not persisted').taskId
Invoke-SmokeApi -Method Post -Path '/ruoyi-research/task' -Body @{
    frameworkId = $frameworkId; groupId = $groupId; parentId = $rootTaskId; level = 2
    taskName = $middleName; taskType = 'MIDDLE'; description = 'T69 level 2'; sort = 1
} | Out-Null
$tasks = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/list?frameworkId=$frameworkId").data
$middleTask = @($tasks) | Where-Object { $_.taskName -eq $middleName } | Select-Object -First 1
$middleTaskId = (Require-Value $middleTask 'Middle task was not persisted').taskId
Invoke-SmokeApi -Method Post -Path '/ruoyi-research/task' -Body @{
    frameworkId = $frameworkId; groupId = $groupId; parentId = $middleTaskId; level = 3
    taskName = $leafName; taskType = 'LEAF'; description = 'T69 level 3'; sort = 1
} | Out-Null
$tasks = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/list?frameworkId=$frameworkId").data
$leafTask = @($tasks) | Where-Object { $_.taskName -eq $leafName } | Select-Object -First 1
$leafTaskId = (Require-Value $leafTask 'Leaf task was not persisted').taskId

Write-Output '[6/8] Create deliverable and assign responsible user'
$deliverableResponse = Invoke-SmokeApi -Method Post -Path '/ruoyi-research/deliverable' -Body @{
    groupId = $groupId
    taskId = $leafTaskId
    deliverableName = 'T69 deliverable ' + $stamp
    requirement = 'One archived smoke-test submission'
    requiredNum = 1
    deadline = '2026-12-31'
    isRequired = '1'
    sort = 1
}
$deliverableId = (Require-Value $deliverableResponse.data 'Deliverable was not persisted').deliverableId
Invoke-SmokeApi -Method Put -Path "/ruoyi-research/deliverable/$deliverableId/assignees" -Body @{
    userIds = @($adminUserId)
} | Out-Null
$canSubmit = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/deliverable/$deliverableId/can-submit").data
if ($canSubmit -ne $true) {
    throw 'Assigned responsible user cannot submit the deliverable'
}
Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/framework/$frameworkId/validate" | Out-Null

Write-Output '[7/8] Submit, approve and archive deliverable'
$submissionResponse = Invoke-SmokeApi -Method Post -Path '/ruoyi-research/submission' -Body @{
    deliverableId = $deliverableId
    submissionName = 'T69 submission ' + $stamp
    submissionDesc = 'T69 smoke submission'
}
$submissionId = (Require-Value $submissionResponse.data 'Submission draft was not persisted').submissionId
Invoke-SmokeApi -Method Put -Path "/ruoyi-research/submission/$submissionId/submit" | Out-Null
Invoke-SmokeApi -Method Put -Path "/ruoyi-research/submission/$submissionId/approve" -Body @{
    opinion = 'T69 smoke approval'
} | Out-Null
$submission = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/submission/$submissionId").data
if ($submission.status -ne '3') {
    throw 'Submission was not archived after approval'
}

Write-Output '[8/8] Verify deliverable and recursive task completion'
$deliverable = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/deliverable/$deliverableId").data
$leafTask = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/$leafTaskId").data
$middleTask = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/$middleTaskId").data
$rootTask = (Invoke-SmokeApi -Method Get -Path "/ruoyi-research/task/$rootTaskId").data
if ($deliverable.status -ne '2' -or [int]$deliverable.archivedNum -ne 1) {
    throw 'Deliverable completion counters are incorrect'
}
if ($leafTask.status -ne '2' -or $middleTask.status -ne '2' -or $rootTask.status -ne '2') {
    throw 'Task completion did not propagate through the three-level tree'
}

[pscustomobject]@{
    status = 'PASS'
    groupId = $groupId
    budgetId = $budgetId
    allocationPlanId = $allocationPlanId
    usePlanId = $usePlanId
    frameworkId = $frameworkId
    rootTaskId = $rootTaskId
    middleTaskId = $middleTaskId
    leafTaskId = $leafTaskId
    deliverableId = $deliverableId
    submissionId = $submissionId
} | ConvertTo-Json
