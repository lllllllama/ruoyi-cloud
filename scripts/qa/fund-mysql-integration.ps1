param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$MySqlExe = 'D:\ruoyi\dev-tools\mysql-5.7.44-winx64\bin\mysql.exe',
    [string]$MySqlPassword = 'password'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-http-common.ps1')
Add-Type -AssemblyName System.Net.Http

$groupA = 991000001
$groupB = 991000002
$script:Results = @()
$stamp = Get-Date -Format 'yyyyMMddHHmmssfff'

function Add-QaCase([string]$Name, [bool]$Passed, [string]$Actual) {
    $script:Results += [pscustomobject]@{
        Case = $Name
        Status = $(if ($Passed) { 'PASS' } else { 'FAIL' })
        Actual = $Actual
    }
}

function Get-DbScalar([string]$Database, [string]$Sql) {
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        $output = & $MySqlExe --host=127.0.0.1 --port=3307 --user=root `
            "--database=$Database" --batch --skip-column-names "--execute=$Sql"
        return ($output | Select-Object -Last 1)
    }
    finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Get-AllocationPlan([string]$Name, [long]$GroupId = $groupA) {
    $response = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get `
        -Path "/ruoyi-fund/allocation/plan/list?topicId=$GroupId&pageNum=1&pageSize=200" `
        -Token $adminToken
    $body = Assert-QaSuccess -Response $response -Label 'allocation list'
    return @($body.rows) | Where-Object { $_.allocationName -eq $Name } | Select-Object -First 1
}

function Add-AllocationPlan([object]$Amount, [string]$Name, [long]$GroupId = $groupA,
        [long]$AllocationDeptId = 103, [long]$ReceiveDeptId = 104, [object]$ResponsibleUserId = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/allocation/plan' `
        -Token $adminToken -Body @{
            topicId = $GroupId
            allocationName = $Name
            allocationDeptId = $AllocationDeptId
            receiveDeptId = $ReceiveDeptId
            planAmount = $Amount
            planTime = '2026-08-28 10:00:00'
            fundDesc = 'QA MySQL integration'
            responsibleUserId = $ResponsibleUserId
        }
}

function Remove-AllocationPlan([long]$PlanId) {
    $response = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Delete `
        -Path "/ruoyi-fund/allocation/plan/$PlanId" -Token $adminToken
    Assert-QaSuccess -Response $response -Label "delete allocation $PlanId" | Out-Null
}

function Get-UsePlan([string]$Name, [long]$GroupId = $groupA, [string]$Token = $leaderAToken) {
    $response = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get `
        -Path "/ruoyi-fund/use/plan/list?topicId=$GroupId&pageNum=1&pageSize=200" -Token $Token
    $body = Assert-QaSuccess -Response $response -Label 'use list'
    return @($body.rows) | Where-Object { $_.useName -eq $Name } | Select-Object -First 1
}

function Add-UsePlan([object]$Amount, [string]$Name, [long]$GroupId = $groupA,
        [long]$ResponsibleUserId = 9103, [string]$Token = $leaderAToken) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/use/plan' `
        -Token $Token -Body @{
            topicId = $GroupId
            useName = $Name
            planAmount = $Amount
            responsibleUserId = $ResponsibleUserId
            planTime = '2026-08-28 11:00:00'
            fundDesc = 'QA MySQL integration'
        }
}

function Remove-UsePlan([long]$PlanId, [string]$Token = $leaderAToken) {
    $response = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Delete `
        -Path "/ruoyi-fund/use/plan/$PlanId" -Token $Token
    Assert-QaSuccess -Response $response -Label "delete use $PlanId" | Out-Null
}

function Invoke-ConcurrentQaRequests([object[]]$Requests) {
    $client = New-Object System.Net.Http.HttpClient
    $taskEntries = @()
    try {
        foreach ($request in $Requests) {
            $message = New-Object System.Net.Http.HttpRequestMessage
            $message.Method = New-Object System.Net.Http.HttpMethod($request.Method)
            $message.RequestUri = $BaseUrl.TrimEnd('/') + $request.Path
            $message.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $request.Token)
            if ($null -ne $request.Body) {
                $json = $request.Body | ConvertTo-Json -Depth 20 -Compress
                $message.Content = New-Object System.Net.Http.StringContent($json, [Text.Encoding]::UTF8, 'application/json')
            }
            $taskEntries += [pscustomobject]@{
                Message = $message
                Task = $client.SendAsync($message)
            }
        }

        [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]@($taskEntries.Task))
        $responses = @()
        foreach ($entry in $taskEntries) {
            $httpResponse = $entry.Task.Result
            $raw = $httpResponse.Content.ReadAsStringAsync().Result
            $body = $null
            try { $body = $raw | ConvertFrom-Json } catch { }
            $responses += [pscustomobject]@{
                HttpStatus = [int]$httpResponse.StatusCode
                Body = $body
                Raw = $raw
            }
        }
        return $responses
    }
    finally {
        foreach ($entry in $taskEntries) {
            if ($null -ne $entry.Message) { $entry.Message.Dispose() }
        }
        $client.Dispose()
    }
}

Write-Output '[1/8] Login through Gateway'
$adminToken = Get-QaToken -BaseUrl $BaseUrl -Username 'admin'
$leaderAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_leader'
$leaderBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_leader'
$coreBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_core'

Write-Output '[2/8] Create exact DECIMAL budgets'
$invalidBudget = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/budget' -Token $adminToken -Body @{
    topicId = $groupA; totalAmount = [decimal]'100.001'; planEndTime = '2026-12-31 23:59:59'; fundDesc = 'must reject scale 3'
}
$invalidBudgetRejected = -not (Test-QaSuccessResponse $invalidBudget)
Add-QaCase 'BUDGET-INVALID-SCALE-3' $invalidBudgetRejected $invalidBudget.Raw
if (-not $invalidBudgetRejected) {
    $createdBudget = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/budget/topic/$groupA" -Token $adminToken) -Label 'get invalid budget').data
    Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Delete -Path "/ruoyi-fund/budget/$($createdBudget.budgetId)" -Token $adminToken) -Label 'cleanup invalid budget' | Out-Null
}
$budgetA = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/budget' -Token $adminToken -Body @{
    topicId = $groupA; totalAmount = [decimal]'100.00'; planEndTime = '2026-12-31 23:59:59'; fundDesc = 'QA boundary budget'
}
Add-QaCase 'DB-BUDGET-A-100.00' (Test-QaSuccessResponse $budgetA) $budgetA.Raw
$budgetB = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/budget' -Token $adminToken -Body @{
    topicId = $groupB; totalAmount = [decimal]'1000000.01'; planEndTime = '2026-12-31 23:59:59'; fundDesc = 'QA precision budget'
}
Add-QaCase 'DB-BUDGET-B-1000000.01' (Test-QaSuccessResponse $budgetB) $budgetB.Raw
$storedBudgets = Get-DbScalar 'ry-fund' "SELECT GROUP_CONCAT(CAST(total_amount AS CHAR) ORDER BY topic_id SEPARATOR ',') FROM fund_project_budget WHERE topic_id IN ($groupA,$groupB)"
Add-QaCase 'DB-DECIMAL-STORAGE' ($storedBudgets -eq '100.00,1000000.01') $storedBudgets

Write-Output '[3/8] Allocation boundary matrix'
$allocationScenarios = @(
    @{ Name = 'A-0+100'; First = $null; Second = [decimal]'100.00'; SecondExpected = $true },
    @{ Name = 'A-60+40'; First = [decimal]'60.00'; Second = [decimal]'40.00'; SecondExpected = $true },
    @{ Name = 'A-60+40.01'; First = [decimal]'60.00'; Second = [decimal]'40.01'; SecondExpected = $false },
    @{ Name = 'A-99.99+0.01'; First = [decimal]'99.99'; Second = [decimal]'0.01'; SecondExpected = $true },
    @{ Name = 'A-100+0.01'; First = [decimal]'100.00'; Second = [decimal]'0.01'; SecondExpected = $false }
)
foreach ($scenario in $allocationScenarios) {
    $created = @()
    if ($null -ne $scenario.First) {
        $firstName = "$($scenario.Name)-first-$stamp"
        $first = Add-AllocationPlan -Amount $scenario.First -Name $firstName
        if (Test-QaSuccessResponse $first) { $created += (Get-AllocationPlan $firstName).planId }
    }
    $secondName = "$($scenario.Name)-second-$stamp"
    $second = Add-AllocationPlan -Amount $scenario.Second -Name $secondName
    $actual = Test-QaSuccessResponse $second
    Add-QaCase "ALLOC-$($scenario.Name)" ($actual -eq $scenario.SecondExpected) $second.Raw
    if ($actual) { $created += (Get-AllocationPlan $secondName).planId }
    foreach ($id in $created) { Remove-AllocationPlan $id }
}

Write-Output '[4/8] Use-plan boundary matrix'
$useScenarios = @(
    @{ Name = 'U-0+100'; First = $null; Second = [decimal]'100.00'; SecondExpected = $true },
    @{ Name = 'U-60+40'; First = [decimal]'60.00'; Second = [decimal]'40.00'; SecondExpected = $true },
    @{ Name = 'U-60+40.01'; First = [decimal]'60.00'; Second = [decimal]'40.01'; SecondExpected = $false },
    @{ Name = 'U-99.99+0.01'; First = [decimal]'99.99'; Second = [decimal]'0.01'; SecondExpected = $true },
    @{ Name = 'U-100+0.01'; First = [decimal]'100.00'; Second = [decimal]'0.01'; SecondExpected = $false }
)
foreach ($scenario in $useScenarios) {
    $created = @()
    if ($null -ne $scenario.First) {
        $firstName = "$($scenario.Name)-first-$stamp"
        $first = Add-UsePlan -Amount $scenario.First -Name $firstName
        if (Test-QaSuccessResponse $first) { $created += (Get-UsePlan $firstName).usePlanId }
    }
    $secondName = "$($scenario.Name)-second-$stamp"
    $second = Add-UsePlan -Amount $scenario.Second -Name $secondName
    $actual = Test-QaSuccessResponse $second
    Add-QaCase "USE-$($scenario.Name)" ($actual -eq $scenario.SecondExpected) $second.Raw
    if ($actual) { $created += (Get-UsePlan $secondName).usePlanId }
    foreach ($id in $created) { Remove-UsePlan $id }
}

Write-Output '[5/8] Invalid amount rejection and precision'
$invalidAmounts = @(
    @{ Label = 'NULL'; Value = $null },
    @{ Label = 'ZERO'; Value = [decimal]'0' },
    @{ Label = 'NEGATIVE'; Value = [decimal]'-1' },
    @{ Label = 'NEGATIVE-CENT'; Value = [decimal]'-0.01' },
    @{ Label = 'SCALE-3'; Value = [decimal]'1.001' },
    @{ Label = 'OVERFLOW'; Value = '10000000000000000.00' }
)
foreach ($invalid in $invalidAmounts) {
    $allocationName = "invalid-allocation-$($invalid.Label)-$stamp"
    $allocation = Add-AllocationPlan -Amount $invalid.Value -Name $allocationName
    $allocationRejected = -not (Test-QaSuccessResponse $allocation)
    Add-QaCase "ALLOC-INVALID-$($invalid.Label)" $allocationRejected $allocation.Raw
    if (-not $allocationRejected) {
        $plan = Get-AllocationPlan $allocationName
        if ($null -ne $plan) { Remove-AllocationPlan $plan.planId }
    }

    $useName = "invalid-use-$($invalid.Label)-$stamp"
    $use = Add-UsePlan -Amount $invalid.Value -Name $useName
    $useRejected = -not (Test-QaSuccessResponse $use)
    Add-QaCase "USE-INVALID-$($invalid.Label)" $useRejected $use.Raw
    if (-not $useRejected) {
        $plan = Get-UsePlan $useName
        if ($null -ne $plan) { Remove-UsePlan $plan.usePlanId }
    }
}

$invalidRecordAllocationName = "invalid-record-allocation-$stamp"
Assert-QaSuccess -Response (Add-AllocationPlan -Amount 10.00 -Name $invalidRecordAllocationName -GroupId $groupB -AllocationDeptId 105 -ReceiveDeptId 105 -ResponsibleUserId 9105) -Label 'create invalid allocation record plan' | Out-Null
$invalidRecordAllocationId = (Get-AllocationPlan $invalidRecordAllocationName $groupB).planId
$invalidAllocationRecord = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/allocation/record' -Token $leaderBToken -Body @{
    planId=$invalidRecordAllocationId; allocationName='invalid scale record'; amount=1.001; allocationTime='2026-08-28 11:30:00'
}
$invalidAllocationRecordRejected = -not (Test-QaSuccessResponse $invalidAllocationRecord)
Add-QaCase 'ALLOC-RECORD-INVALID-SCALE-3' $invalidAllocationRecordRejected $invalidAllocationRecord.Raw
if (-not $invalidAllocationRecordRejected) {
    $records = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/allocation/plan/$invalidRecordAllocationId/records" -Token $leaderBToken) -Label 'invalid allocation records').data
    foreach ($record in @($records)) {
        Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Delete -Path "/ruoyi-fund/allocation/record/$($record.recordId)" -Token $leaderBToken) -Label 'cleanup invalid allocation record' | Out-Null
    }
}
Remove-AllocationPlan $invalidRecordAllocationId

$invalidRecordUseName = "invalid-record-use-$stamp"
Assert-QaSuccess -Response (Add-UsePlan -Amount 10.00 -Name $invalidRecordUseName -GroupId $groupB -ResponsibleUserId 9106 -Token $leaderBToken) -Label 'create invalid use record plan' | Out-Null
$invalidRecordUseId = (Get-UsePlan $invalidRecordUseName $groupB $leaderBToken).usePlanId
$invalidUseRecord = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/use/record' -Token $coreBToken -Body @{
    usePlanId=$invalidRecordUseId; useName='invalid scale record'; amount=1.001; useTime='2026-08-28 11:30:00'
}
$invalidUseRecordRejected = -not (Test-QaSuccessResponse $invalidUseRecord)
Add-QaCase 'USE-RECORD-INVALID-SCALE-3' $invalidUseRecordRejected $invalidUseRecord.Raw
if (-not $invalidUseRecordRejected) {
    $records = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/use/plan/$invalidRecordUseId/records" -Token $leaderBToken) -Label 'invalid use records').data
    foreach ($record in @($records)) {
        Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Delete -Path "/ruoyi-fund/use/record/$($record.useRecordId)" -Token $coreBToken) -Label 'cleanup invalid use record' | Out-Null
    }
}
Remove-UsePlan $invalidRecordUseId $leaderBToken

$precisionName = "precision-0.10-$stamp"
$precision = Add-AllocationPlan -Amount ([decimal]'0.10') -Name $precisionName -GroupId $groupB -AllocationDeptId 105 -ReceiveDeptId 105
$precisionPlan = if (Test-QaSuccessResponse $precision) { Get-AllocationPlan $precisionName $groupB } else { $null }
$precisionStored = if ($null -ne $precisionPlan) { Get-DbScalar 'ry-fund' "SELECT CAST(plan_amount AS CHAR) FROM fund_allocation_plan WHERE plan_id=$($precisionPlan.planId)" } else { 'NOT_CREATED' }
Add-QaCase 'ALLOC-PRECISION-0.10' ($precisionStored -eq '0.10') $precisionStored
if ($null -ne $precisionPlan) { Remove-AllocationPlan $precisionPlan.planId }

Write-Output '[6/8] Concurrent 60 + 60 plan creation'
$concurrentAllocation = Invoke-ConcurrentQaRequests @(
    @{ Method = 'POST'; Path = '/ruoyi-fund/allocation/plan'; Token = $adminToken; Body = @{ topicId=$groupA; allocationName="concurrent-a1-$stamp"; allocationDeptId=103; receiveDeptId=104; planAmount=60.00; planTime='2026-08-28 12:00:00' } },
    @{ Method = 'POST'; Path = '/ruoyi-fund/allocation/plan'; Token = $adminToken; Body = @{ topicId=$groupA; allocationName="concurrent-a2-$stamp"; allocationDeptId=103; receiveDeptId=104; planAmount=60.00; planTime='2026-08-28 12:00:00' } }
)
$allocationSuccesses = @($concurrentAllocation | Where-Object { Test-QaSuccessResponse $_ }).Count
$allocationRows = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/allocation/plan/list?topicId=$groupA&pageNum=1&pageSize=200" -Token $adminToken) -Label 'post-concurrency allocation list').rows
$allocationConcurrentPlans = @($allocationRows | Where-Object { $_.allocationName -like "concurrent-a?-$stamp" })
$allocationSum = ($allocationConcurrentPlans | Measure-Object -Property planAmount -Sum).Sum
Add-QaCase 'CON-ALLOC-60+60' ($allocationSuccesses -eq 1 -and [decimal]$allocationSum -eq 60.00) "success=$allocationSuccesses,sum=$allocationSum"
foreach ($plan in $allocationConcurrentPlans) { Remove-AllocationPlan $plan.planId }

$concurrentUse = Invoke-ConcurrentQaRequests @(
    @{ Method = 'POST'; Path = '/ruoyi-fund/use/plan'; Token = $leaderAToken; Body = @{ topicId=$groupA; useName="concurrent-u1-$stamp"; planAmount=60.00; responsibleUserId=9103; planTime='2026-08-28 12:00:00' } },
    @{ Method = 'POST'; Path = '/ruoyi-fund/use/plan'; Token = $leaderAToken; Body = @{ topicId=$groupA; useName="concurrent-u2-$stamp"; planAmount=60.00; responsibleUserId=9103; planTime='2026-08-28 12:00:00' } }
)
$useSuccesses = @($concurrentUse | Where-Object { Test-QaSuccessResponse $_ }).Count
$useRows = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/use/plan/list?topicId=$groupA&pageNum=1&pageSize=200" -Token $leaderAToken) -Label 'post-concurrency use list').rows
$useConcurrentPlans = @($useRows | Where-Object { $_.useName -like "concurrent-u?-$stamp" })
$useSum = ($useConcurrentPlans | Measure-Object -Property planAmount -Sum).Sum
Add-QaCase 'CON-USE-60+60' ($useSuccesses -eq 1 -and [decimal]$useSum -eq 60.00) "success=$useSuccesses,sum=$useSum"
foreach ($plan in $useConcurrentPlans) { Remove-UsePlan $plan.usePlanId }

Write-Output '[7/8] Record-versus-finish races'
$raceAllocationName = "race-allocation-$stamp"
Assert-QaSuccess -Response (Add-AllocationPlan -Amount 100.00 -Name $raceAllocationName -GroupId $groupB -AllocationDeptId 105 -ReceiveDeptId 105 -ResponsibleUserId 9105) -Label 'create allocation race plan' | Out-Null
$raceAllocationId = (Get-AllocationPlan $raceAllocationName $groupB).planId
$raceAllocation = Invoke-ConcurrentQaRequests @(
    @{ Method='PUT'; Path="/ruoyi-fund/allocation/plan/$raceAllocationId/finish"; Token=$leaderBToken; Body=@{ confirmDifference=$true; reason='QA concurrent close' } },
    @{ Method='POST'; Path='/ruoyi-fund/allocation/record'; Token=$leaderBToken; Body=@{ planId=$raceAllocationId; allocationName='QA concurrent record'; amount=20.00; allocationTime='2026-08-28 12:30:00' } }
)
$raceAllocationFinishSuccess = Test-QaSuccessResponse $raceAllocation[0]
$raceAllocationAfter = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/allocation/plan/$raceAllocationId" -Token $adminToken) -Label 'allocation after race').data
Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/allocation/plan/$raceAllocationId/records" -Token $adminToken) -Label 'allocation records after race' | Out-Null
$raceAllocationSum = Get-DbScalar 'ry-fund' "SELECT COALESCE(SUM(amount),0) FROM fund_allocation_record WHERE plan_id=$raceAllocationId"
$postFinishAllocation = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/allocation/record' -Token $leaderBToken -Body @{ planId=$raceAllocationId; allocationName='must fail after close'; amount=1.00; allocationTime='2026-08-28 12:31:00' }
Add-QaCase 'CON-ALLOC-RECORD-VS-FINISH' ($raceAllocationFinishSuccess -and $raceAllocationAfter.status -eq '1' -and [decimal]$raceAllocationAfter.actualAmount -eq [decimal]$raceAllocationSum -and -not (Test-QaSuccessResponse $postFinishAllocation)) "status=$($raceAllocationAfter.status),actual=$($raceAllocationAfter.actualAmount),sum=$raceAllocationSum"

$raceUseName = "race-use-$stamp"
Assert-QaSuccess -Response (Add-UsePlan -Amount 100.00 -Name $raceUseName -GroupId $groupB -ResponsibleUserId 9106 -Token $leaderBToken) -Label 'create use race plan' | Out-Null
$raceUseId = (Get-UsePlan $raceUseName $groupB $leaderBToken).usePlanId
$raceUse = Invoke-ConcurrentQaRequests @(
    @{ Method='PUT'; Path="/ruoyi-fund/use/plan/$raceUseId/finish"; Token=$coreBToken; Body=@{ confirmDifference=$true; reason='QA concurrent close' } },
    @{ Method='POST'; Path='/ruoyi-fund/use/record'; Token=$coreBToken; Body=@{ usePlanId=$raceUseId; useName='QA concurrent record'; amount=20.00; useTime='2026-08-28 12:30:00' } }
)
$raceUseFinishSuccess = Test-QaSuccessResponse $raceUse[0]
$raceUseAfter = (Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/use/plan/$raceUseId" -Token $leaderBToken) -Label 'use after race').data
Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path "/ruoyi-fund/use/plan/$raceUseId/records" -Token $leaderBToken) -Label 'use records after race' | Out-Null
$raceUseSum = Get-DbScalar 'ry-fund' "SELECT COALESCE(SUM(amount),0) FROM fund_use_record WHERE use_plan_id=$raceUseId"
$postFinishUse = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/use/record' -Token $coreBToken -Body @{ usePlanId=$raceUseId; useName='must fail after close'; amount=1.00; useTime='2026-08-28 12:31:00' }
Add-QaCase 'CON-USE-RECORD-VS-FINISH' ($raceUseFinishSuccess -and $raceUseAfter.status -eq '1' -and [decimal]$raceUseAfter.actualAmount -eq [decimal]$raceUseSum -and -not (Test-QaSuccessResponse $postFinishUse)) "status=$($raceUseAfter.status),actual=$($raceUseAfter.actualAmount),sum=$raceUseSum"

Write-Output '[8/8] Double finish and single audit log'
$doubleName = "double-finish-allocation-$stamp"
Assert-QaSuccess -Response (Add-AllocationPlan -Amount 100.00 -Name $doubleName -GroupId $groupB -AllocationDeptId 105 -ReceiveDeptId 105 -ResponsibleUserId 9105) -Label 'create double finish allocation' | Out-Null
$doubleId = (Get-AllocationPlan $doubleName $groupB).planId
Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/allocation/record' -Token $leaderBToken -Body @{ planId=$doubleId; allocationName='exact record'; amount=100.00; allocationTime='2026-08-28 13:00:00' }) -Label 'create exact allocation record' | Out-Null
$doubleResponses = Invoke-ConcurrentQaRequests @(
    @{ Method='PUT'; Path="/ruoyi-fund/allocation/plan/$doubleId/finish"; Token=$leaderBToken; Body=@{ confirmDifference=$false; reason='first close' } },
    @{ Method='PUT'; Path="/ruoyi-fund/allocation/plan/$doubleId/finish"; Token=$leaderBToken; Body=@{ confirmDifference=$false; reason='second close' } }
)
$doubleSuccesses = @($doubleResponses | Where-Object { Test-QaSuccessResponse $_ }).Count
$doubleLogs = Get-DbScalar 'ry-fund' "SELECT COUNT(*) FROM fund_operation_log WHERE business_type='ALLOCATION_PLAN' AND business_id=$doubleId AND operation_type='CLOSE_ALLOCATION'"
Add-QaCase 'CON-ALLOC-DOUBLE-FINISH' ($doubleSuccesses -eq 1 -and [int]$doubleLogs -eq 1) "success=$doubleSuccesses,closeLogs=$doubleLogs"

$doubleUseName = "double-finish-use-$stamp"
Assert-QaSuccess -Response (Add-UsePlan -Amount 100.00 -Name $doubleUseName -GroupId $groupB -ResponsibleUserId 9106 -Token $leaderBToken) -Label 'create double finish use' | Out-Null
$doubleUseId = (Get-UsePlan $doubleUseName $groupB $leaderBToken).usePlanId
Assert-QaSuccess -Response (Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/ruoyi-fund/use/record' -Token $coreBToken -Body @{ usePlanId=$doubleUseId; useName='exact record'; amount=100.00; useTime='2026-08-28 13:00:00' }) -Label 'create exact use record' | Out-Null
$doubleUseResponses = Invoke-ConcurrentQaRequests @(
    @{ Method='PUT'; Path="/ruoyi-fund/use/plan/$doubleUseId/finish"; Token=$coreBToken; Body=@{ confirmDifference=$false; reason='first close' } },
    @{ Method='PUT'; Path="/ruoyi-fund/use/plan/$doubleUseId/finish"; Token=$coreBToken; Body=@{ confirmDifference=$false; reason='second close' } }
)
$doubleUseSuccesses = @($doubleUseResponses | Where-Object { Test-QaSuccessResponse $_ }).Count
$doubleUseLogs = Get-DbScalar 'ry-fund' "SELECT COUNT(*) FROM fund_operation_log WHERE business_type='USE_PLAN' AND business_id=$doubleUseId AND operation_type='CLOSE_USE'"
Add-QaCase 'CON-USE-DOUBLE-FINISH' ($doubleUseSuccesses -eq 1 -and [int]$doubleUseLogs -eq 1) "success=$doubleUseSuccesses,closeLogs=$doubleUseLogs"

$script:Results | Format-Table -AutoSize -Wrap
$failed = @($script:Results | Where-Object { $_.Status -eq 'FAIL' })
[pscustomobject]@{
    Status = $(if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' })
    Total = $script:Results.Count
    Passed = $script:Results.Count - $failed.Count
    Failed = $failed.Count
    FailedCases = @($failed | ForEach-Object { $_.Case })
} | ConvertTo-Json -Depth 5

if ($failed.Count -gt 0) { exit 1 }
