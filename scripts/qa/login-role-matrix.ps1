param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$Password = 'admin123'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'qa-http-common.ps1')

$accounts = @(
    @{ User = 'admin'; Role = 'admin'; Must = @('*:*:*'); MustNot = @() },
    @{ User = 'a_leader'; Role = 'qa_research_leader'; Must = @('task:framework:add', 'task:submission:audit', 'fund:use:add'); MustNot = @('fund:budget:add') },
    @{ User = 'a_core'; Role = 'qa_research_member'; Must = @('task:submission:add', 'task:submission:withdraw', 'fund:use:list'); MustNot = @('task:framework:add', 'task:submission:audit') },
    @{ User = 'a_member'; Role = 'qa_research_member'; Must = @('task:submission:add', 'task:submission:withdraw', 'fund:use:list'); MustNot = @('task:framework:add', 'task:submission:audit') },
    @{ User = 'a_expert'; Role = 'qa_research_member'; Must = @('task:submission:add', 'task:submission:withdraw', 'fund:use:list'); MustNot = @('task:framework:add', 'task:submission:audit') },
    @{ User = 'b_leader'; Role = 'qa_research_leader'; Must = @('task:framework:add', 'task:submission:audit'); MustNot = @('fund:budget:add') },
    @{ User = 'b_core'; Role = 'qa_research_member'; Must = @('task:submission:add', 'task:submission:withdraw'); MustNot = @('task:framework:add') },
    @{ User = 'outsider'; Role = 'qa_outsider'; Must = @('fund:allocation:list'); MustNot = @('fund:use:list', 'task:info:list') },
    @{ User = 'alloc_manager'; Role = 'qa_allocation_manager'; Must = @('fund:allocation:assign', 'fund:allocation:record'); MustNot = @('fund:allocation:add') },
    @{ User = 'alloc_user'; Role = 'qa_allocation_operator'; Must = @('fund:allocation:record'); MustNot = @('fund:allocation:assign') },
    @{ User = 'other_unit_user'; Role = 'qa_allocation_operator'; Must = @('fund:allocation:record'); MustNot = @('fund:allocation:assign') }
)

$results = @()
foreach ($account in $accounts) {
    $token = Get-QaToken -BaseUrl $BaseUrl -Username $account.User -Password $Password
    $infoResponse = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/system/user/getInfo' -Token $token
    $info = Assert-QaSuccess -Response $infoResponse -Label "$($account.User) getInfo"
    $roles = @($info.roles)
    $permissions = @($info.permissions)

    if ($roles -notcontains $account.Role) {
        throw "$($account.User) expected role $($account.Role), actual: $($roles -join ',')"
    }
    foreach ($permission in $account.Must) {
        if ($permissions -notcontains $permission) {
            throw "$($account.User) missing expected permission $permission"
        }
    }
    foreach ($permission in $account.MustNot) {
        if ($permissions -contains $permission) {
            throw "$($account.User) unexpectedly has permission $permission"
        }
    }

    $routersResponse = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/system/menu/getRouters' -Token $token
    $routers = Assert-QaSuccess -Response $routersResponse -Label "$($account.User) routers"
    $results += [pscustomobject]@{
        User = $account.User
        Role = $account.Role
        PermissionCount = $permissions.Count
        RouterCount = @($routers.data).Count
        Status = 'PASS'
    }
}

$results | Format-Table -AutoSize
[pscustomobject]@{
    Status = 'PASS'
    Accounts = $results.Count
    Gateway = $BaseUrl
} | ConvertTo-Json
