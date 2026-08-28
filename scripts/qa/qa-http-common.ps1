Set-StrictMode -Version 2.0

function Get-QaCaptchaAnswer {
    param(
        [Parameter(Mandatory = $true)][string]$Uuid,
        [string]$RedisServer = '127.0.0.1',
        [int]$RedisPort = 6379
    )

    $key = 'captcha_codes:' + $Uuid
    $client = New-Object System.Net.Sockets.TcpClient($RedisServer, $RedisPort)
    try {
        $stream = $client.GetStream()
        $command = "*2`r`n`$3`r`nGET`r`n`$$($key.Length)`r`n$key`r`n"
        $bytes = [Text.Encoding]::UTF8.GetBytes($command)
        $stream.Write($bytes, 0, $bytes.Length)
        $buffer = New-Object byte[] 2048
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

function Invoke-QaRawRequest {
    param(
        [Parameter(Mandatory = $true)][string]$BaseUrl,
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [string]$Token,
        [object]$Body = $null
    )

    $params = @{
        Method = $Method
        Uri = $BaseUrl.TrimEnd('/') + $Path
        ContentType = 'application/json'
        UseBasicParsing = $true
    }
    if (-not [string]::IsNullOrWhiteSpace($Token)) {
        $params.Headers = @{ Authorization = 'Bearer ' + $Token }
    }
    if ($null -ne $Body) {
        $params.Body = $Body | ConvertTo-Json -Depth 20 -Compress
    }

    try {
        $response = Invoke-WebRequest @params
        $parsed = $null
        if (-not [string]::IsNullOrWhiteSpace($response.Content)) {
            $parsed = $response.Content | ConvertFrom-Json
        }
        return [pscustomobject]@{
            HttpStatus = [int]$response.StatusCode
            Body = $parsed
            Raw = $response.Content
        }
    }
    catch {
        $status = 0
        $raw = $_.Exception.Message
        if ($null -ne $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            try {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object IO.StreamReader($stream)
                $raw = $reader.ReadToEnd()
                $reader.Close()
            }
            catch {
                $raw = $_.Exception.Message
            }
        }
        $parsed = $null
        try { $parsed = $raw | ConvertFrom-Json } catch { }
        return [pscustomobject]@{
            HttpStatus = $status
            Body = $parsed
            Raw = $raw
        }
    }
}

function Invoke-QaMultipartUpload {
    param(
        [Parameter(Mandatory = $true)][string]$BaseUrl,
        [Parameter(Mandatory = $true)][string]$Token,
        [Parameter(Mandatory = $true)][string]$FileName,
        [Parameter(Mandatory = $true)][byte[]]$Content,
        [string]$ContentType = 'application/octet-stream'
    )

    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    $multipart = New-Object System.Net.Http.MultipartFormDataContent
    $fileContent = New-Object System.Net.Http.ByteArrayContent -ArgumentList @(,$Content)
    try {
        $client.DefaultRequestHeaders.Authorization =
            New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
        $fileContent.Headers.ContentType =
            New-Object System.Net.Http.Headers.MediaTypeHeaderValue($ContentType)
        $multipart.Add($fileContent, 'file', $FileName)
        $response = $client.PostAsync($BaseUrl.TrimEnd('/') + '/file/upload', $multipart).Result
        $raw = $response.Content.ReadAsStringAsync().Result
        $parsed = $null
        try { $parsed = $raw | ConvertFrom-Json } catch { }
        return [pscustomobject]@{
            HttpStatus = [int]$response.StatusCode
            Body = $parsed
            Raw = $raw
        }
    }
    finally {
        $fileContent.Dispose()
        $multipart.Dispose()
        $client.Dispose()
    }
}

function Invoke-QaBinaryRequest {
    param(
        [Parameter(Mandatory = $true)][string]$BaseUrl,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Token
    )

    Add-Type -AssemblyName System.Net.Http
    $client = New-Object System.Net.Http.HttpClient
    try {
        $client.DefaultRequestHeaders.Authorization =
            New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer', $Token)
        $response = $client.GetAsync($BaseUrl.TrimEnd('/') + $Path).Result
        return [pscustomobject]@{
            HttpStatus = [int]$response.StatusCode
            ContentType = $(if ($null -eq $response.Content.Headers.ContentType) {
                $null
            } else {
                $response.Content.Headers.ContentType.MediaType
            })
            Bytes = $response.Content.ReadAsByteArrayAsync().Result
        }
    }
    finally {
        $client.Dispose()
    }
}

function Test-QaSuccessResponse {
    param([Parameter(Mandatory = $true)]$Response)

    if ($Response.HttpStatus -lt 200 -or $Response.HttpStatus -ge 300) {
        return $false
    }
    if ($null -ne $Response.Body -and
        $Response.Body.PSObject.Properties.Name -contains 'code') {
        return [int]$Response.Body.code -eq 200
    }
    return $true
}

function Assert-QaSuccess {
    param(
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if (-not (Test-QaSuccessResponse $Response)) {
        throw "$Label expected success, got HTTP $($Response.HttpStatus): $($Response.Raw)"
    }
    return $Response.Body
}

function Assert-QaFailure {
    param(
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][string]$Label
    )

    if (Test-QaSuccessResponse $Response) {
        throw "$Label expected rejection, but succeeded: $($Response.Raw)"
    }
    return $Response.Body
}

function Get-QaToken {
    param(
        [Parameter(Mandatory = $true)][string]$BaseUrl,
        [Parameter(Mandatory = $true)][string]$Username,
        [string]$Password = 'admin123',
        [string]$RedisServer = '127.0.0.1',
        [int]$RedisPort = 6379
    )

    $captcha = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Get -Path '/code'
    $captchaBody = Assert-QaSuccess -Response $captcha -Label "$Username captcha"
    $answer = Get-QaCaptchaAnswer -Uuid $captchaBody.uuid -RedisServer $RedisServer -RedisPort $RedisPort
    $login = Invoke-QaRawRequest -BaseUrl $BaseUrl -Method Post -Path '/auth/login' -Body @{
        username = $Username
        password = $Password
        code = $answer
        uuid = $captchaBody.uuid
    }
    $loginBody = Assert-QaSuccess -Response $login -Label "$Username login"
    $token = $loginBody.data.access_token
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "$Username login did not return an access token"
    }
    return $token
}
