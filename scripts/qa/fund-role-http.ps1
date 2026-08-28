param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$MySqlExe = 'D:\ruoyi\dev-tools\mysql-5.7.44-winx64\bin\mysql.exe',
    [string]$MySqlPassword = 'password'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-http-common.ps1')

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

function Test-Rejected([object]$Response) {
    return -not (Test-QaSuccessResponse $Response)
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

function Invoke-Fund([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method `
        -Path ('/ruoyi-fund' + $Path) -Token $Token -Body $Body
}

function Add-AllocationRecord([long]$PlanId, [string]$Name, [string]$Token,
        [decimal]$Amount = [decimal]'10.00', [string]$VoucherUrls = $null) {
    return Invoke-Fund Post '/allocation/record' $Token @{
        planId = $PlanId
        allocationName = $Name
        amount = $Amount
        allocationTime = '2026-08-28 10:00:00'
        fundDesc = 'QA role matrix'
        voucherUrls = $VoucherUrls
        submitUserId = 1
        submitUserName = 'admin'
    }
}

function Add-UseRecord([long]$UsePlanId, [string]$Name, [string]$Token,
        [decimal]$Amount = [decimal]'10.00', [string]$VoucherUrls = $null) {
    return Invoke-Fund Post '/use/record' $Token @{
        usePlanId = $UsePlanId
        useName = $Name
        amount = $Amount
        useTime = '2026-08-28 10:00:00'
        fundDesc = 'QA role matrix'
        voucherUrls = $VoucherUrls
        submitUserId = 1
        submitUserName = 'admin'
    }
}

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

Write-Output '[1/7] Login fixed accounts through Gateway'
$adminToken = Get-QaToken -BaseUrl $BaseUrl -Username 'admin'
$leaderAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_leader'
$coreAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_core'
$memberAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_member'
$expertAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_expert'
$leaderBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_leader'
$outsiderToken = Get-QaToken -BaseUrl $BaseUrl -Username 'outsider'
$managerToken = Get-QaToken -BaseUrl $BaseUrl -Username 'alloc_manager'
$allocationUserToken = Get-QaToken -BaseUrl $BaseUrl -Username 'alloc_user'
$otherUnitToken = Get-QaToken -BaseUrl $BaseUrl -Username 'other_unit_user'

Write-Output '[2/7] Create budget, allocation plans and use plan'
$budgetResponse = Invoke-Fund Post '/budget' $adminToken @{
    topicId = $groupA
    totalAmount = [decimal]'1000.00'
    planEndTime = '2026-12-31 23:59:59'
    fundDesc = 'QA role matrix budget'
}
Add-QaCase 'SETUP-BUDGET-CREATE' (Test-QaSuccessResponse $budgetResponse) $budgetResponse.Raw

$assignedPlanName = "QA-ALLOC-ASSIGNED-$stamp"
$assignedPlanResponse = Invoke-Fund Post '/allocation/plan' $adminToken @{
    topicId = $groupA
    allocationName = $assignedPlanName
    allocationDeptId = 104
    receiveDeptId = 103
    planAmount = [decimal]'300.00'
    planTime = '2026-08-28 10:00:00'
    fundDesc = 'assigned role matrix'
}
Add-QaCase 'SETUP-ASSIGNED-ALLOCATION-PLAN-CREATE' (Test-QaSuccessResponse $assignedPlanResponse) $assignedPlanResponse.Raw
$assignedPlanId = [long](Get-DbScalar 'ry-fund' "select plan_id from fund_allocation_plan where allocation_name='$assignedPlanName' and del_flag='0'")

$unassignedPlanName = "QA-ALLOC-UNASSIGNED-$stamp"
$unassignedPlanResponse = Invoke-Fund Post '/allocation/plan' $adminToken @{
    topicId = $groupA
    allocationName = $unassignedPlanName
    allocationDeptId = 103
    receiveDeptId = 104
    planAmount = [decimal]'300.00'
    planTime = '2026-08-28 10:00:00'
    fundDesc = 'unassigned role matrix'
}
Add-QaCase 'SETUP-UNASSIGNED-ALLOCATION-PLAN-CREATE' (Test-QaSuccessResponse $unassignedPlanResponse) $unassignedPlanResponse.Raw
$unassignedPlanId = [long](Get-DbScalar 'ry-fund' "select plan_id from fund_allocation_plan where allocation_name='$unassignedPlanName' and del_flag='0'")

$usePlanName = "QA-USE-$stamp"
$usePlanResponse = Invoke-Fund Post '/use/plan' $leaderAToken @{
    topicId = $groupA
    useName = $usePlanName
    planAmount = [decimal]'300.00'
    responsibleUserId = 9103
    planTime = '2026-08-28 10:00:00'
    fundDesc = 'QA role matrix use plan'
}
Add-QaCase 'SETUP-USE-PLAN-CREATE' (Test-QaSuccessResponse $usePlanResponse) $usePlanResponse.Raw
$usePlanId = [long](Get-DbScalar 'ry-fund' "select use_plan_id from fund_use_plan where use_name='$usePlanName' and del_flag='0'")

Write-Output '[3/7] Verify allocation public-read boundary'
$publicReadTokens = @(
    @{ Name = 'A-LEADER'; Token = $leaderAToken },
    @{ Name = 'B-LEADER'; Token = $leaderBToken },
    @{ Name = 'OUTSIDER'; Token = $outsiderToken }
)
foreach ($entry in $publicReadTokens) {
    $list = Invoke-Fund Get "/allocation/plan/list?topicId=$groupA&pageNum=1&pageSize=100" $entry.Token
    Add-QaCase "ALLOC-PUBLIC-LIST-$($entry.Name)" (Test-QaSuccessResponse $list) $list.Raw
    $detail = Invoke-Fund Get "/allocation/plan/$assignedPlanId" $entry.Token
    Add-QaCase "ALLOC-PUBLIC-DETAIL-$($entry.Name)" (Test-QaSuccessResponse $detail) $detail.Raw
    $records = Invoke-Fund Get "/allocation/plan/$assignedPlanId/records" $entry.Token
    Add-QaCase "ALLOC-PUBLIC-RECORDS-$($entry.Name)" (Test-QaSuccessResponse $records) $records.Raw
    $overview = Invoke-Fund Get "/allocation/overview/$groupA" $entry.Token
    Add-QaCase "ALLOC-PUBLIC-OVERVIEW-$($entry.Name)" (Test-QaSuccessResponse $overview) $overview.Raw
}
$anonymousAllocation = Invoke-Fund Get "/allocation/plan/$assignedPlanId" $null
Add-QaCase 'ALLOC-ANONYMOUS-DENIED' (Test-Rejected $anonymousAllocation) $anonymousAllocation.Raw

Write-Output '[4/7] Verify use data is restricted to the current research group'
$useMembers = @(
    @{ Name = 'LEADER'; Token = $leaderAToken },
    @{ Name = 'CORE'; Token = $coreAToken },
    @{ Name = 'MEMBER'; Token = $memberAToken },
    @{ Name = 'EXPERT'; Token = $expertAToken }
)
foreach ($entry in $useMembers) {
    $list = Invoke-Fund Get "/use/plan/list?topicId=$groupA&pageNum=1&pageSize=100" $entry.Token
    Add-QaCase "USE-GROUP-LIST-$($entry.Name)" (Test-QaSuccessResponse $list) $list.Raw
    $detail = Invoke-Fund Get "/use/plan/$usePlanId" $entry.Token
    Add-QaCase "USE-GROUP-DETAIL-$($entry.Name)" (Test-QaSuccessResponse $detail) $detail.Raw
}
$bUseDetail = Invoke-Fund Get "/use/plan/$usePlanId" $leaderBToken
Add-QaCase 'USE-B-LEADER-CROSS-GROUP-DENIED' (Test-Rejected $bUseDetail) $bUseDetail.Raw
$outsiderUseDetail = Invoke-Fund Get "/use/plan/$usePlanId" $outsiderToken
Add-QaCase 'USE-OUTSIDER-DETAIL-DENIED' (Test-Rejected $outsiderUseDetail) $outsiderUseDetail.Raw
$outsiderUseList = Invoke-Fund Get "/use/plan/list?topicId=$groupA&pageNum=1&pageSize=100" $outsiderToken
Add-QaCase 'USE-OUTSIDER-LIST-DENIED' (Test-Rejected $outsiderUseList) $outsiderUseList.Raw
$outsiderUseOverview = Invoke-Fund Get "/use/overview/$groupA" $outsiderToken
Add-QaCase 'USE-OUTSIDER-OVERVIEW-DENIED' (Test-Rejected $outsiderUseOverview) $outsiderUseOverview.Raw

Write-Output '[5/7] Verify allocation assignment and record permissions'
$outsiderAssignment = Invoke-Fund Put "/allocation/plan/$assignedPlanId/assign" $managerToken @{ responsibleUserId = 9107 }
Add-QaCase 'ALLOC-MANAGER-CANNOT-ASSIGN-OUTSIDER' (Test-Rejected $outsiderAssignment) $outsiderAssignment.Raw
$assignResponsible = Invoke-Fund Put "/allocation/plan/$assignedPlanId/assign" $managerToken @{ responsibleUserId = 9109 }
Add-QaCase 'ALLOC-UNIT-MANAGER-ASSIGN-SUCCESS' (Test-QaSuccessResponse $assignResponsible) $assignResponsible.Raw
$ordinaryAssign = Invoke-Fund Put "/allocation/plan/$assignedPlanId/assign" $allocationUserToken @{ responsibleUserId = 9109 }
Add-QaCase 'ALLOC-ORDINARY-UNIT-MEMBER-ASSIGN-DENIED' (Test-Rejected $ordinaryAssign) $ordinaryAssign.Raw

$assignedRecord = Add-AllocationRecord $assignedPlanId "QA-ALLOC-RECORD-$stamp" $allocationUserToken ([decimal]'10.00') '/profile/upload/qa/allocation-proof.txt'
Add-QaCase 'ALLOC-RESPONSIBLE-RECORD-SUCCESS' (Test-QaSuccessResponse $assignedRecord) $assignedRecord.Raw
$assignedRecordId = [long](Get-DbScalar 'ry-fund' "select max(record_id) from fund_allocation_record where plan_id=$assignedPlanId and del_flag='0'")
$allocationIdentity = Get-DbScalar 'ry-fund' "select submit_user_id from fund_allocation_record where record_id=$assignedRecordId"
Add-QaCase 'ALLOC-RECORD-IDENTITY-FROM-TOKEN' ($allocationIdentity -eq '9109') $allocationIdentity

$otherAssignedRecord = Add-AllocationRecord $assignedPlanId "QA-ALLOC-OTHER-DENY-$stamp" $otherUnitToken
Add-QaCase 'ALLOC-OTHER-UNIT-ASSIGNED-RECORD-DENIED' (Test-Rejected $otherAssignedRecord) $otherAssignedRecord.Raw
$outsiderAssignedRecord = Add-AllocationRecord $assignedPlanId "QA-ALLOC-OUTSIDER-DENY-$stamp" $outsiderToken
Add-QaCase 'ALLOC-OUTSIDER-RECORD-DENIED' (Test-Rejected $outsiderAssignedRecord) $outsiderAssignedRecord.Raw

$unassignedLegalRecord = Add-AllocationRecord $unassignedPlanId "QA-ALLOC-UNIT-RECORD-$stamp" $leaderAToken
Add-QaCase 'ALLOC-UNASSIGNED-UNIT-MEMBER-RECORD-SUCCESS' (Test-QaSuccessResponse $unassignedLegalRecord) $unassignedLegalRecord.Raw
$unassignedOtherRecord = Add-AllocationRecord $unassignedPlanId "QA-ALLOC-NONUNIT-DENY-$stamp" $otherUnitToken
Add-QaCase 'ALLOC-UNASSIGNED-NONUNIT-RECORD-DENIED' (Test-Rejected $unassignedOtherRecord) $unassignedOtherRecord.Raw

Write-Output '[6/7] Verify use responsible-user permissions and identity tampering'
$useRecord = Add-UseRecord $usePlanId "QA-USE-RECORD-$stamp" $memberAToken ([decimal]'10.00') '/profile/upload/qa/use-proof.txt'
Add-QaCase 'USE-RESPONSIBLE-RECORD-SUCCESS' (Test-QaSuccessResponse $useRecord) $useRecord.Raw
$useRecordId = [long](Get-DbScalar 'ry-fund' "select max(use_record_id) from fund_use_record where use_plan_id=$usePlanId and del_flag='0'")
$useIdentity = Get-DbScalar 'ry-fund' "select submit_user_id from fund_use_record where use_record_id=$useRecordId"
Add-QaCase 'USE-RECORD-IDENTITY-FROM-TOKEN' ($useIdentity -eq '9103') $useIdentity
$coreUseRecord = Add-UseRecord $usePlanId "QA-USE-CORE-DENY-$stamp" $coreAToken
Add-QaCase 'USE-NONRESPONSIBLE-CORE-RECORD-DENIED' (Test-Rejected $coreUseRecord) $coreUseRecord.Raw
$outsiderUseRecord = Add-UseRecord $usePlanId "QA-USE-OUTSIDER-DENY-$stamp" $outsiderToken
Add-QaCase 'USE-OUTSIDER-RECORD-DENIED' (Test-Rejected $outsiderUseRecord) $outsiderUseRecord.Raw

Write-Output '[7/7] Verify attachment download authorization boundary'
$allocationAttachmentId = [long](Get-DbScalar 'ry-fund' "select attachment_id from fund_attachment where business_type='ALLOCATION_RECORD' and business_id=$assignedRecordId and del_flag='0' limit 1")
$useAttachmentId = [long](Get-DbScalar 'ry-fund' "select attachment_id from fund_attachment where business_type='USE_RECORD' and business_id=$useRecordId and del_flag='0' limit 1")
$anonymousAllocationAttachment = Invoke-Fund Get "/fund/attachment/$allocationAttachmentId/download" $null
Add-QaCase 'ATTACHMENT-ALLOCATION-ANONYMOUS-DENIED' (Test-Rejected $anonymousAllocationAttachment) $anonymousAllocationAttachment.Raw
$outsiderUseAttachment = Invoke-Fund Get "/fund/attachment/$useAttachmentId/download" $outsiderToken
Add-QaCase 'ATTACHMENT-USE-OUTSIDER-DENIED' (Test-Rejected $outsiderUseAttachment) $outsiderUseAttachment.Raw
$bUseAttachment = Invoke-Fund Get "/fund/attachment/$useAttachmentId/download" $leaderBToken
Add-QaCase 'ATTACHMENT-USE-B-LEADER-DENIED' (Test-Rejected $bUseAttachment) $bUseAttachment.Raw

$script:Results | Format-Table -AutoSize
$failed = @($script:Results | Where-Object { $_.Status -eq 'FAIL' })
[pscustomobject]@{
    Status = $(if ($failed.Count -eq 0) { 'PASS' } else { 'FAIL' })
    Total = $script:Results.Count
    Passed = $script:Results.Count - $failed.Count
    Failed = $failed.Count
    FailedCases = @($failed | ForEach-Object { $_.Case })
} | ConvertTo-Json -Depth 5

if ($failed.Count -gt 0) { exit 1 }
