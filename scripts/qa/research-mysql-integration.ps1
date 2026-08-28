param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$MySqlExe = 'D:\ruoyi\dev-tools\mysql-5.7.44-winx64\bin\mysql.exe',
    [string]$MySqlPassword = 'password',
    [switch]$SkipFixtureReset
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

function Get-DbScalar([string]$Sql) {
    $previousPassword = $env:MYSQL_PWD
    try {
        $env:MYSQL_PWD = $MySqlPassword
        $output = & $MySqlExe --host=127.0.0.1 --port=3307 --user=root `
            '--database=ry-research' --batch --skip-column-names "--execute=$Sql"
        if ($LASTEXITCODE -ne 0) {
            throw "MySQL query failed: $Sql"
        }
        return ($output | Select-Object -Last 1)
    }
    finally {
        $env:MYSQL_PWD = $previousPassword
    }
}

function Invoke-Api([string]$Method, [string]$Path, [string]$Token, [object]$Body = $null) {
    return Invoke-QaRawRequest -BaseUrl $BaseUrl -Method $Method `
        -Path ('/ruoyi-research' + $Path) -Token $Token -Body $Body
}

function Add-Framework([string]$Name, [long]$GroupId, [long]$LeadDeptId,
        [string]$Token, [object[]]$Units = @()) {
    return Invoke-Api Post '/framework' $Token @{
        groupId = $GroupId
        frameworkName = $Name
        year = 2026
        leadDeptId = $LeadDeptId
        overallGoal = 'QA real MySQL integration'
        status = '0'
        sort = 1
        units = $Units
    }
}

function Get-FrameworkByName([string]$Name, [string]$Token) {
    $response = Invoke-Api Get ('/framework/list?frameworkName=' + [uri]::EscapeDataString($Name) +
        '&pageNum=1&pageSize=100') $Token
    $body = Assert-QaSuccess -Response $response -Label "framework list $Name"
    return @($body.rows) | Where-Object { $_.frameworkName -eq $Name } | Select-Object -First 1
}

function Add-Task([string]$Name, [long]$FrameworkId, [long]$GroupId,
        [long]$ParentId, [string]$Token, [object]$Level = $null) {
    $body = @{
        frameworkId = $FrameworkId
        groupId = $GroupId
        parentId = $ParentId
        taskName = $Name
        taskType = 'QA'
        description = 'QA real MySQL integration'
        startDate = '2026-01-01'
        deadline = '2026-12-31'
        sort = 1
    }
    if ($null -ne $Level) { $body.level = $Level }
    return Invoke-Api Post '/task' $Token $body
}

function Get-TaskByName([string]$Name, [long]$FrameworkId, [string]$Token) {
    $response = Invoke-Api Get ('/task/list?frameworkId=' + $FrameworkId +
        '&taskName=' + [uri]::EscapeDataString($Name)) $Token
    $body = Assert-QaSuccess -Response $response -Label "task list $Name"
    return @($body.data) | Where-Object { $_.taskName -eq $Name } | Select-Object -First 1
}

function Update-Task([object]$Task, [long]$ParentId, [string]$Token, [object]$Level = $null) {
    $body = @{
        taskId = $Task.taskId
        frameworkId = $Task.frameworkId
        groupId = $Task.groupId
        parentId = $ParentId
        taskName = $Task.taskName
        taskType = $Task.taskType
        description = $Task.description
        startDate = '2026-01-01'
        deadline = '2026-12-31'
        sort = $Task.sort
    }
    if ($null -ne $Level) { $body.level = $Level }
    return Invoke-Api Put '/task' $Token $body
}

function Add-Submission([string]$Name, [long]$DeliverableId, [string]$Token) {
    return Invoke-Api Post '/submission' $Token @{
        deliverableId = $DeliverableId
        submissionName = $Name
        submissionDesc = 'QA draft'
        groupId = $groupB
        frameworkId = 999999999
        taskId = 999999999
        submitUserId = 1
        submitDeptId = 105
        status = '3'
        archiveUserId = 1
        version = 99
    }
}

if (-not $SkipFixtureReset) {
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
}

Write-Output '[1/9] Login fixed role accounts through Gateway'
$adminToken = Get-QaToken -BaseUrl $BaseUrl -Username 'admin'
$leaderAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_leader'
$coreAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_core'
$memberAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_member'
$expertAToken = Get-QaToken -BaseUrl $BaseUrl -Username 'a_expert'
$leaderBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_leader'
$coreBToken = Get-QaToken -BaseUrl $BaseUrl -Username 'b_core'
$outsiderToken = Get-QaToken -BaseUrl $BaseUrl -Username 'outsider'
$proofBytes = [Text.Encoding]::UTF8.GetBytes("research attachment proof $stamp")
$uploadResponse = Invoke-QaMultipartUpload -BaseUrl $BaseUrl -Token $memberAToken `
    -FileName "成果附件-$stamp.txt" -Content $proofBytes -ContentType 'text/plain'
$uploadBody = Assert-QaSuccess -Response $uploadResponse -Label 'research proof upload'
$proofUrl = $uploadBody.data.url
Add-QaCase 'FILE-SERVICE-RESEARCH-PROOF-UPLOAD' (-not [string]::IsNullOrWhiteSpace($proofUrl)) $uploadResponse.Raw

Write-Output '[2/9] Create A/B annual frameworks and verify maintenance permissions'
$frameworkAName = "QA-A-$stamp"
$frameworkBName = "QA-B-$stamp"
$frameworkA2Name = "QA-A2-$stamp"
$createA = Add-Framework $frameworkAName $groupA 103 $leaderAToken @(
    @{ deptId = 104; sort = 1 }
)
Add-QaCase 'FRAMEWORK-A-LEADER-CREATE' (Test-QaSuccessResponse $createA) $createA.Raw
$frameworkA = Get-FrameworkByName $frameworkAName $leaderAToken

$createB = Add-Framework $frameworkBName $groupB 105 $leaderBToken
Add-QaCase 'FRAMEWORK-B-LEADER-CREATE' (Test-QaSuccessResponse $createB) $createB.Raw
$frameworkB = Get-FrameworkByName $frameworkBName $leaderBToken

$createA2 = Add-Framework $frameworkA2Name $groupA 103 $leaderAToken
Add-QaCase 'FRAMEWORK-A2-LEADER-CREATE' (Test-QaSuccessResponse $createA2) $createA2.Raw
$frameworkA2 = Get-FrameworkByName $frameworkA2Name $leaderAToken

$coreCreate = Add-Framework "QA-CORE-DENY-$stamp" $groupA 103 $coreAToken
Add-QaCase 'FRAMEWORK-CORE-CREATE-DENIED' (Test-Rejected $coreCreate) $coreCreate.Raw
$crossCreate = Add-Framework "QA-CROSS-DENY-$stamp" $groupB 105 $leaderAToken
Add-QaCase 'FRAMEWORK-A-LEADER-CANNOT-MAINTAIN-B' (Test-Rejected $crossCreate) $crossCreate.Raw

Write-Output '[3/9] Build and validate a three-level task tree'
$rootName = "QA-ROOT-$stamp"
$parentName = "QA-PARENT-$stamp"
$leafName = "QA-LEAF-$stamp"

$rootResponse = Add-Task $rootName $frameworkA.frameworkId $groupA 0 $leaderAToken 1
Add-QaCase 'TREE-LEVEL-1-CREATE' (Test-QaSuccessResponse $rootResponse) $rootResponse.Raw
$root = Get-TaskByName $rootName $frameworkA.frameworkId $leaderAToken

$parentResponse = Add-Task $parentName $frameworkA.frameworkId $groupA $root.taskId $leaderAToken
Add-QaCase 'TREE-LEVEL-2-CREATE' (Test-QaSuccessResponse $parentResponse) $parentResponse.Raw
$parent = Get-TaskByName $parentName $frameworkA.frameworkId $leaderAToken

$leafResponse = Add-Task $leafName $frameworkA.frameworkId $groupA $parent.taskId $leaderAToken
Add-QaCase 'TREE-LEVEL-3-CREATE' (Test-QaSuccessResponse $leafResponse) $leafResponse.Raw
$leaf = Get-TaskByName $leafName $frameworkA.frameworkId $leaderAToken

$validateMissing = Invoke-Api Get "/task/framework/$($frameworkA.frameworkId)/validate" $leaderAToken
Add-QaCase 'TREE-LEAF-WITHOUT-DELIVERABLE-DENIED' (Test-Rejected $validateMissing) $validateMissing.Raw

$level4 = Add-Task "QA-LEVEL4-$stamp" $frameworkA.frameworkId $groupA $leaf.taskId $leaderAToken
Add-QaCase 'TREE-LEVEL-4-DENIED' (Test-Rejected $level4) $level4.Raw
$crossFramework = Add-Task "QA-CROSS-FRAMEWORK-$stamp" $frameworkA2.frameworkId $groupA $root.taskId $leaderAToken
Add-QaCase 'TREE-CROSS-FRAMEWORK-PARENT-DENIED' (Test-Rejected $crossFramework) $crossFramework.Raw
$crossGroup = Add-Task "QA-CROSS-GROUP-$stamp" $frameworkB.frameworkId $groupB $root.taskId $leaderBToken
Add-QaCase 'TREE-CROSS-GROUP-PARENT-DENIED' (Test-Rejected $crossGroup) $crossGroup.Raw
$selfMove = Update-Task $root $root.taskId $leaderAToken 1
Add-QaCase 'TREE-SELF-PARENT-DENIED' (Test-Rejected $selfMove) $selfMove.Raw
$descendantMove = Update-Task $root $leaf.taskId $leaderAToken 1
Add-QaCase 'TREE-DESCENDANT-PARENT-DENIED' (Test-Rejected $descendantMove) $descendantMove.Raw

Write-Output '[4/9] Create deliverable and verify assignee permission matrix'
$deliverableResponse = Invoke-Api Post '/deliverable' $leaderAToken @{
    groupId = $groupA
    taskId = $leaf.taskId
    deliverableName = "QA-DELIVERABLE-$stamp"
    requirement = 'Two archived submissions required'
    requiredNum = 2
    deadline = '2026-12-31'
    isRequired = '1'
    sort = 1
}
$deliverableBody = Assert-QaSuccess -Response $deliverableResponse -Label 'create deliverable'
$deliverable = $deliverableBody.data
Add-QaCase 'DELIVERABLE-CREATE' ($null -ne $deliverable.deliverableId) $deliverableResponse.Raw

$validateComplete = Invoke-Api Get "/task/framework/$($frameworkA.frameworkId)/validate" $leaderAToken
Add-QaCase 'TREE-VALID-WITH-LEAF-DELIVERABLE' (Test-QaSuccessResponse $validateComplete) $validateComplete.Raw

$unassignedMatrix = @(
    @{ Name = 'LEADER'; Token = $leaderAToken; Expected = $true },
    @{ Name = 'CORE'; Token = $coreAToken; Expected = $true },
    @{ Name = 'MEMBER'; Token = $memberAToken; Expected = $false },
    @{ Name = 'EXPERT'; Token = $expertAToken; Expected = $false }
)
foreach ($entry in $unassignedMatrix) {
    $response = Invoke-Api Get "/deliverable/$($deliverable.deliverableId)/can-submit" $entry.Token
    $body = Assert-QaSuccess -Response $response -Label "can-submit $($entry.Name)"
    Add-QaCase "SUBMIT-UNASSIGNED-$($entry.Name)" ([bool]$body.data -eq $entry.Expected) $response.Raw
}

$coreAssign = Invoke-Api Put "/deliverable/$($deliverable.deliverableId)/assignees" $coreAToken @{ userIds = @(9103) }
Add-QaCase 'ASSIGN-CORE-DENIED' (Test-Rejected $coreAssign) $coreAssign.Raw
$outsiderAssign = Invoke-Api Put "/deliverable/$($deliverable.deliverableId)/assignees" $leaderAToken @{ userIds = @(9107) }
Add-QaCase 'ASSIGN-OUTSIDER-DENIED' (Test-Rejected $outsiderAssign) $outsiderAssign.Raw
$memberAssign = Invoke-Api Put "/deliverable/$($deliverable.deliverableId)/assignees" $leaderAToken @{ userIds = @(9103) }
Add-QaCase 'ASSIGN-MEMBER-SUCCESS' (Test-QaSuccessResponse $memberAssign) $memberAssign.Raw

$assignedMatrix = @(
    @{ Name = 'LEADER'; Token = $leaderAToken; Expected = $false },
    @{ Name = 'CORE'; Token = $coreAToken; Expected = $false },
    @{ Name = 'MEMBER'; Token = $memberAToken; Expected = $true },
    @{ Name = 'EXPERT'; Token = $expertAToken; Expected = $false }
)
foreach ($entry in $assignedMatrix) {
    $response = Invoke-Api Get "/deliverable/$($deliverable.deliverableId)/can-submit" $entry.Token
    $body = Assert-QaSuccess -Response $response -Label "assigned can-submit $($entry.Name)"
    Add-QaCase "SUBMIT-ASSIGNED-$($entry.Name)" ([bool]$body.data -eq $entry.Expected) $response.Raw
}

Write-Output '[5/9] Verify cross-topic IDOR and request identity tampering'
$bReadATask = Invoke-Api Get "/task/$($leaf.taskId)" $leaderBToken
Add-QaCase 'IDOR-B-LEADER-READ-A-TASK-DENIED' (Test-Rejected $bReadATask) $bReadATask.Raw
$outsiderReadATask = Invoke-Api Get "/task/$($leaf.taskId)" $outsiderToken
Add-QaCase 'IDOR-OUTSIDER-READ-A-TASK-DENIED' (Test-Rejected $outsiderReadATask) $outsiderReadATask.Raw

$submission1Response = Add-Submission "QA-SUBMISSION-1-$stamp" $deliverable.deliverableId $memberAToken
$submission1Body = Assert-QaSuccess -Response $submission1Response -Label 'create submission 1'
$submission1 = $submission1Body.data
Add-QaCase 'SUBMISSION-1-CREATE' ($null -ne $submission1.submissionId) $submission1Response.Raw

$identity = Get-DbScalar "select concat(group_id,',',framework_id,',',task_id,',',submit_user_id,',',submit_dept_id,',',status,',',version) from task_submission where submission_id=$($submission1.submissionId)"
$expectedIdentity = "$groupA,$($frameworkA.frameworkId),$($leaf.taskId),9103,104,0,0"
Add-QaCase 'SUBMISSION-IDENTITY-FROM-TOKEN-AND-DB' ($identity -eq $expectedIdentity) $identity

$bReadSubmission = Invoke-Api Get "/submission/$($submission1.submissionId)" $leaderBToken
Add-QaCase 'IDOR-B-LEADER-READ-A-SUBMISSION-DENIED' (Test-Rejected $bReadSubmission) $bReadSubmission.Raw
$outsiderReadSubmission = Invoke-Api Get "/submission/$($submission1.submissionId)" $outsiderToken
Add-QaCase 'IDOR-OUTSIDER-READ-A-SUBMISSION-DENIED' (Test-Rejected $outsiderReadSubmission) $outsiderReadSubmission.Raw

$tamperUpdate = Invoke-Api Put '/submission' $memberAToken @{
    submissionId = $submission1.submissionId
    deliverableId = $deliverable.deliverableId
    submissionName = "QA-SUBMISSION-1-EDITED-$stamp"
    submissionDesc = 'identity fields must be ignored'
    submitUserId = 1
    submitDeptId = 105
    status = '3'
    archiveUserId = 1
    version = 0
}
Add-QaCase 'SUBMISSION-DRAFT-EDIT' (Test-QaSuccessResponse $tamperUpdate) $tamperUpdate.Raw
$tamperedIdentity = Get-DbScalar "select concat(submit_user_id,',',submit_dept_id,',',status,',',ifnull(archive_user_id,'NULL')) from task_submission where submission_id=$($submission1.submissionId)"
Add-QaCase 'SUBMISSION-TAMPERED-IDENTITY-IGNORED' ($tamperedIdentity -eq '9103,104,0,NULL') $tamperedIdentity

Write-Output '[6/9] Verify attachment metadata permissions before workflow transitions'
$attachmentResponse = Invoke-Api Post "/submission/$($submission1.submissionId)/attachments" $memberAToken @{
    fileName = 'qa-proof.txt'
    originalName = 'QA proof.txt'
    fileUrl = $proofUrl
    fileSize = $proofBytes.Length
    fileType = 'text/plain'
    groupId = $groupB
    uploadUserId = 1
}
Add-QaCase 'ATTACHMENT-OWNER-ADD' (Test-QaSuccessResponse $attachmentResponse) $attachmentResponse.Raw
$attachmentId = [long](Get-DbScalar "select max(attachment_id) from task_attachment where submission_id=$($submission1.submissionId) and del_flag='0'")
$attachmentIdentity = Get-DbScalar "select concat(group_id,',',upload_user_id) from task_attachment where attachment_id=$attachmentId"
Add-QaCase 'ATTACHMENT-IDENTITY-FROM-SERVER' ($attachmentIdentity -eq "$groupA,9103") $attachmentIdentity
$bAttachmentList = Invoke-Api Get "/submission/$($submission1.submissionId)/attachments" $leaderBToken
Add-QaCase 'ATTACHMENT-B-LEADER-LIST-DENIED' (Test-Rejected $bAttachmentList) $bAttachmentList.Raw
$outsiderDownload = Invoke-Api Get "/submission/attachment/$attachmentId/download" $outsiderToken
Add-QaCase 'ATTACHMENT-OUTSIDER-DOWNLOAD-DENIED' (Test-Rejected $outsiderDownload) $outsiderDownload.Raw

Write-Output '[7/9] Execute reject, resubmit, approve and archived-lock workflow'
$submit1 = Invoke-Api Put "/submission/$($submission1.submissionId)/submit" $memberAToken
Add-QaCase 'WORKFLOW-DRAFT-TO-PENDING' (Test-QaSuccessResponse $submit1) $submit1.Raw
$pendingStatus = Get-DbScalar "select status from task_submission where submission_id=$($submission1.submissionId)"
Add-QaCase 'WORKFLOW-PENDING-DB' ($pendingStatus -eq '1') $pendingStatus

$pendingEdit = Invoke-Api Put '/submission' $memberAToken @{
    submissionId = $submission1.submissionId
    deliverableId = $deliverable.deliverableId
    submissionName = 'must reject pending edit'
}
Add-QaCase 'WORKFLOW-PENDING-EDIT-DENIED' (Test-Rejected $pendingEdit) $pendingEdit.Raw
$pendingDelete = Invoke-Api Delete "/submission/$($submission1.submissionId)" $memberAToken
Add-QaCase 'WORKFLOW-PENDING-DELETE-DENIED' (Test-Rejected $pendingDelete) $pendingDelete.Raw

$rejectNoOpinion = Invoke-Api Put "/submission/$($submission1.submissionId)/reject" $leaderAToken @{}
Add-QaCase 'WORKFLOW-REJECT-OPINION-REQUIRED' (Test-Rejected $rejectNoOpinion) $rejectNoOpinion.Raw
$reject1 = Invoke-Api Put "/submission/$($submission1.submissionId)/reject" $leaderAToken @{ opinion = 'Please revise' }
Add-QaCase 'WORKFLOW-PENDING-TO-REJECTED' (Test-QaSuccessResponse $reject1) $reject1.Raw
$rejectedEdit = Invoke-Api Put '/submission' $memberAToken @{
    submissionId = $submission1.submissionId
    deliverableId = $deliverable.deliverableId
    submissionName = "QA-SUBMISSION-1-REVISED-$stamp"
    submissionDesc = 'revised after rejection'
}
Add-QaCase 'WORKFLOW-REJECTED-EDIT-SUCCESS' (Test-QaSuccessResponse $rejectedEdit) $rejectedEdit.Raw
$resubmit1 = Invoke-Api Put "/submission/$($submission1.submissionId)/resubmit" $memberAToken @{ opinion = 'Revised' }
Add-QaCase 'WORKFLOW-REJECTED-TO-PENDING' (Test-QaSuccessResponse $resubmit1) $resubmit1.Raw
$coreApprove = Invoke-Api Put "/submission/$($submission1.submissionId)/approve" $coreAToken @{ opinion = 'forged audit' }
Add-QaCase 'WORKFLOW-CORE-APPROVE-DENIED' (Test-Rejected $coreApprove) $coreApprove.Raw
$bApprove = Invoke-Api Put "/submission/$($submission1.submissionId)/approve" $leaderBToken @{ opinion = 'cross group' }
Add-QaCase 'WORKFLOW-B-LEADER-APPROVE-A-DENIED' (Test-Rejected $bApprove) $bApprove.Raw
$approve1 = Invoke-Api Put "/submission/$($submission1.submissionId)/approve" $leaderAToken @{ opinion = 'Approved 1' }
Add-QaCase 'WORKFLOW-PENDING-TO-ARCHIVED' (Test-QaSuccessResponse $approve1) $approve1.Raw
$archivedDownload = Invoke-QaBinaryRequest -BaseUrl $BaseUrl `
    -Path "/ruoyi-research/submission/attachment/$attachmentId/download" -Token $coreAToken
$archivedBytesMatch = $archivedDownload.HttpStatus -eq 200 -and
    [Convert]::ToBase64String($archivedDownload.Bytes) -eq [Convert]::ToBase64String($proofBytes)
Add-QaCase 'ATTACHMENT-ARCHIVED-GROUP-MEMBER-DOWNLOAD-CONTENT' $archivedBytesMatch `
    "HTTP=$($archivedDownload.HttpStatus),bytes=$($archivedDownload.Bytes.Length)"

$archivedEdit = Invoke-Api Put '/submission' $memberAToken @{
    submissionId = $submission1.submissionId
    deliverableId = $deliverable.deliverableId
    submissionName = 'must reject archived edit'
}
Add-QaCase 'WORKFLOW-ARCHIVED-EDIT-DENIED' (Test-Rejected $archivedEdit) $archivedEdit.Raw
$archivedDelete = Invoke-Api Delete "/submission/$($submission1.submissionId)" $memberAToken
Add-QaCase 'WORKFLOW-ARCHIVED-DELETE-DENIED' (Test-Rejected $archivedDelete) $archivedDelete.Raw
$archivedAttachmentAdd = Invoke-Api Post "/submission/$($submission1.submissionId)/attachments" $memberAToken @{
    fileName = 'locked.txt'; originalName = 'locked.txt'; fileUrl = '/profile/upload/qa/locked.txt'
}
Add-QaCase 'WORKFLOW-ARCHIVED-ATTACHMENT-ADD-DENIED' (Test-Rejected $archivedAttachmentAdd) $archivedAttachmentAdd.Raw

Write-Output '[8/9] Verify automatic completion and recursive rollback'
$submission2Response = Add-Submission "QA-SUBMISSION-2-$stamp" $deliverable.deliverableId $memberAToken
$submission2Body = Assert-QaSuccess -Response $submission2Response -Label 'create submission 2'
$submission2 = $submission2Body.data
Add-QaCase 'SUBMISSION-2-CREATE' ($null -ne $submission2.submissionId) $submission2Response.Raw
$submit2 = Invoke-Api Put "/submission/$($submission2.submissionId)/submit" $memberAToken
Add-QaCase 'SUBMISSION-2-SUBMIT' (Test-QaSuccessResponse $submit2) $submit2.Raw
$approve2 = Invoke-Api Put "/submission/$($submission2.submissionId)/approve" $leaderAToken @{ opinion = 'Approved 2' }
Add-QaCase 'SUBMISSION-2-APPROVE' (Test-QaSuccessResponse $approve2) $approve2.Raw

$finishedState = Get-DbScalar "select concat(d.archived_num,',',d.status,',',l.status,',',p.status,',',r.status) from task_deliverable d join task_info l on l.task_id=d.task_id join task_info p on p.task_id=l.parent_id join task_info r on r.task_id=p.parent_id where d.deliverable_id=$($deliverable.deliverableId)"
Add-QaCase 'COMPLETION-2-ARCHIVED-FINISHES-TREE' ($finishedState -eq '2,2,2,2,2') $finishedState

$moveAfterSubmission = Update-Task $leaf $root.taskId $leaderAToken 2
Add-QaCase 'TREE-MOVE-WITH-SUBMISSIONS-DENIED' (Test-Rejected $moveAfterSubmission) $moveAfterSubmission.Raw

$cancel1 = Invoke-Api Put "/submission/$($submission1.submissionId)/cancel-approve" $leaderAToken @{ opinion = 'Rollback completion' }
Add-QaCase 'WORKFLOW-CANCEL-APPROVE' (Test-QaSuccessResponse $cancel1) $cancel1.Raw
$rollbackState = Get-DbScalar "select concat(d.archived_num,',',d.status,',',l.status,',',p.status,',',r.status,',',s.status) from task_deliverable d join task_info l on l.task_id=d.task_id join task_info p on p.task_id=l.parent_id join task_info r on r.task_id=p.parent_id join task_submission s on s.submission_id=$($submission1.submissionId) where d.deliverable_id=$($deliverable.deliverableId)"
Add-QaCase 'COMPLETION-CANCEL-ROLLS-BACK-TREE' ($rollbackState -eq '1,1,1,1,1,1') $rollbackState

Write-Output '[9/9] Verify immutable audit history'
$auditActions = Get-DbScalar "select group_concat(action order by audit_id separator ',') from task_submission_audit where submission_id=$($submission1.submissionId)"
Add-QaCase 'AUDIT-FULL-STATE-MACHINE-HISTORY' ($auditActions -eq 'SUBMIT,REJECT,RESUBMIT,APPROVE,CANCEL_APPROVE') $auditActions
$auditActors = Get-DbScalar "select group_concat(audit_user_id order by audit_id separator ',') from task_submission_audit where submission_id=$($submission1.submissionId)"
Add-QaCase 'AUDIT-ACTORS-FROM-TOKEN' ($auditActors -eq '9103,9101,9103,9101,9101') $auditActors
$submission2Actions = Get-DbScalar "select group_concat(action order by audit_id separator ',') from task_submission_audit where submission_id=$($submission2.submissionId)"
Add-QaCase 'AUDIT-SECOND-SUBMISSION-HISTORY' ($submission2Actions -eq 'SUBMIT,APPROVE') $submission2Actions

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
