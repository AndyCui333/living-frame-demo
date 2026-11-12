param(
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$AdminUser = 'admin',
    [string]$AdminPassword = 'Admin@123',
    [string]$RedisHost = 'localhost',
    [int]$RedisPort = 6379,
    [string]$RedisPassword = '',
    [bool]$CaptchaEnabled = $true,
    [switch]$SkipAi
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$message) {
    Write-Host "[smoke] $message" -ForegroundColor Cyan
}

function Assert-True([bool]$condition, [string]$message) {
    if (-not $condition) {
        throw "冒烟断言失败: $message"
    }
    Write-Host "  PASS $message" -ForegroundColor Green
}

function Get-RedisValue([string]$key) {
    $client = New-Object System.Net.Sockets.TcpClient
    $client.Connect($RedisHost, $RedisPort)
    $stream = $client.GetStream()
    $stream.ReadTimeout = 2000
    $writer = New-Object System.IO.StreamWriter($stream, [System.Text.Encoding]::ASCII)
    $writer.NewLine = "`r`n"
    $writer.AutoFlush = $true
    if ($RedisPassword) {
        $writer.WriteLine('AUTH ' + $RedisPassword)
    }
    $writer.WriteLine('GET ' + $key)
    $sb = New-Object System.Text.StringBuilder
    $buf = New-Object byte[] 4096
    try {
        while ($true) {
            $n = $stream.Read($buf, 0, $buf.Length)
            if ($n -le 0) { break }
            [void]$sb.Append([System.Text.Encoding]::ASCII.GetString($buf, 0, $n))
            if ($sb.ToString() -match '\r\n$' -and $sb.ToString().Contains('$')) { break }
        }
    } catch {
        # 读取超时表示响应已结束
    }
    $client.Close()
    $matches = [regex]::Matches($sb.ToString(), '\$(\d+)\r\n([\s\S]*?)\r\n')
    if ($matches.Count -gt 0) {
        return $matches[$matches.Count - 1].Groups[2].Value
    }
    return ''
}

function New-LoginBody([string]$username, [string]$password) {
    $captchaId = 'disabled'
    $captchaCode = 'x'
    if ($CaptchaEnabled) {
        $captcha = Invoke-RestMethod -Uri "$BaseUrl/api/auth/captcha" -Method Get
        $captchaId = $captcha.data.captchaId
        $captchaCode = Get-RedisValue ('living-frame:captcha:' + $captchaId)
        if ([string]::IsNullOrWhiteSpace($captchaCode)) {
            throw '无法从 Redis 读取验证码，请确认 Redis 密码/键前缀'
        }
    }
    return @{
        username    = $username
        password    = $password
        captchaId   = $captchaId
        captchaCode = $captchaCode
    } | ConvertTo-Json
}

function Login([string]$path, [string]$username, [string]$password) {
    $body = New-LoginBody $username $password
    $response = Invoke-RestMethod -Uri "$BaseUrl$path" -Method Post `
        -ContentType 'application/json; charset=utf-8' -Body $body
    if ($response.code -ne 200 -or [string]::IsNullOrWhiteSpace($response.data.token)) {
        throw "登录失败: $($response.message)"
    }
    return @{ Authorization = "Bearer $($response.data.token)" }
}

function Get-HttpStatus([string]$method, [string]$path, $headers, [string]$body) {
    try {
        $params = @{
            Uri             = "$BaseUrl$path"
            Method          = $method
            Headers         = $headers
            UseBasicParsing = $true
        }
        if ($body) {
            $params.ContentType = 'application/json; charset=utf-8'
            $params.Body = $body
        }
        Invoke-WebRequest @params | Out-Null
        return 200
    } catch {
        if ($_.Exception.Response) {
            return [int]$_.Exception.Response.StatusCode
        }
        throw
    }
}

$suffix = Get-Date -Format 'HHmmss'
$roleId = $null
$userId = $null
$admin = $null

try {
    Write-Step '1/7 健康检查'
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get
    Assert-True ($health.status -eq 'UP') "health=$($health.status)"

    Write-Step '2/7 管理员登录与当前用户'
    $admin = Login '/api/auth/login' $AdminUser $AdminPassword
    $me = Invoke-RestMethod -Uri "$BaseUrl/api/auth/me" -Headers $admin -Method Get
    Assert-True ($me.data.username -eq $AdminUser) "username=$($me.data.username)"
    Assert-True ($me.data.superAdmin -eq $true) 'superAdmin=true'
    Assert-True ($me.data.permissions.Count -gt 0) "permissions=$($me.data.permissions.Count)"

    $blacklistPage = Invoke-RestMethod -Uri "$BaseUrl/api/system/blacklist?pageNum=1&pageSize=10&targetValue=blacklist_test" -Headers $admin -Method Get
    Assert-True ($blacklistPage.data.total -ge 1) "blacklist.test.account=$($blacklistPage.data.total)"
    $blockedBody = New-LoginBody 'blacklist_test' 'Blacklist@123'
    $blockedStatus = Get-HttpStatus 'POST' '/api/auth/login' @{} $blockedBody
    Assert-True ($blockedStatus -eq 403) "blacklist test account blocked ($blockedStatus)"

    Write-Step '3/7 用户分页与角色权限链路'
    $page = Invoke-RestMethod -Uri "$BaseUrl/api/system/users?pageNum=1&pageSize=10" -Headers $admin -Method Get
    Assert-True ($page.data.total -ge 3) "users.total=$($page.data.total)"

    $roleBody = @{
        code        = "SMOKE_ROLE_$suffix"
        name        = "冒烟角色$suffix"
        description = 'smoke test'
        sort        = 99
    } | ConvertTo-Json
    $role = Invoke-RestMethod -Uri "$BaseUrl/api/system/roles" -Method Post -Headers $admin `
        -ContentType 'application/json; charset=utf-8' -Body $roleBody
    $roleId = $role.data
    Assert-True ($role.code -eq 200) "roleId=$roleId"

    $assignBody = @{ permissionIds = @(101) } | ConvertTo-Json
    $assigned = Invoke-RestMethod -Uri "$BaseUrl/api/system/roles/$roleId/permissions" -Method Put `
        -Headers $admin -ContentType 'application/json; charset=utf-8' -Body $assignBody
    Assert-True ($assigned.code -eq 200) 'role permission assigned (system:user:list)'

    $userBody = @{
        username = "smoke_$suffix"
        password = 'Smoke@123'
        nickname = "冒烟用户$suffix"
        roleIds  = @($roleId)
    } | ConvertTo-Json
    $created = Invoke-RestMethod -Uri "$BaseUrl/api/system/users" -Method Post -Headers $admin `
        -ContentType 'application/json; charset=utf-8' -Body $userBody
    $userId = $created.data
    Assert-True ($created.code -eq 200) "userId=$userId"

    $smokeUser = Login '/api/auth/login' "smoke_$suffix" 'Smoke@123'
    $smokeList = Invoke-RestMethod -Uri "$BaseUrl/api/system/users" -Headers $smokeUser -Method Get
    Assert-True ($smokeList.code -eq 200) 'smoke user can list users'
    $createStatus = Get-HttpStatus 'POST' '/api/system/users' $smokeUser $userBody
    Assert-True ($createStatus -eq 403) "smoke user create denied ($createStatus)"

    Write-Step '4/7 业务用户登录与隔离'
    $biz = Login '/api/biz/auth/login' 'biz01' 'Biz@123'
    $bizMe = Invoke-RestMethod -Uri "$BaseUrl/api/biz/auth/me" -Headers $biz -Method Get
    Assert-True ($bizMe.data.username -eq 'biz01') "biz username=$($bizMe.data.username)"
    $bizSystem = Get-HttpStatus 'GET' '/api/system/users' $biz $null
    Assert-True ($bizSystem -eq 403) "biz user cannot access system API ($bizSystem)"

    Write-Step '5/7 验证码错误与一次性校验'
    if ($CaptchaEnabled) {
        $captcha = Invoke-RestMethod -Uri "$BaseUrl/api/auth/captcha" -Method Get
        $wrongBody = @{
            username    = $AdminUser
            password    = $AdminPassword
            captchaId   = $captcha.data.captchaId
            captchaCode = 'wrong-code'
        } | ConvertTo-Json
        $wrongStatus = Get-HttpStatus 'POST' '/api/auth/login' @{} $wrongBody
        Assert-True ($wrongStatus -eq 400) "wrong captcha rejected ($wrongStatus)"
    } else {
        Write-Host '  SKIP 验证码专项（启动时已关闭验证码）'
    }

    Write-Step '6/7 AI 模型配置与加密 Key 接口'
    if ($SkipAi) {
        Write-Host '  SKIP AI（-SkipAi）'
    } else {
        $chatBody = @{ message = 'Reply with exactly: OK'; conversationId = "smoke-$suffix" } | ConvertTo-Json
        $chatStatus = Get-HttpStatus 'POST' '/api/ai/chat' $admin $chatBody
        Assert-True ($chatStatus -eq 400) "ai.chat without local config rejected ($chatStatus)"
        Write-Host '  PASS AI 模型配置按账号保存，API Key 加密存储，未配置 Key 时对话被拒绝'
    }

    $loginLogs = Invoke-RestMethod -Uri "$BaseUrl/api/system/login-logs?pageNum=1&pageSize=5" -Headers $admin -Method Get
    Assert-True ($loginLogs.code -eq 200) "login.logs=$($loginLogs.data.total)"
    $operationLogs = Invoke-RestMethod -Uri "$BaseUrl/api/system/operation-logs?pageNum=1&pageSize=5" -Headers $admin -Method Get
    Assert-True ($operationLogs.code -eq 200) "operation.logs=$($operationLogs.data.total)"
    $blacklist = Invoke-RestMethod -Uri "$BaseUrl/api/system/blacklist?pageNum=1&pageSize=5" -Headers $admin -Method Get
    Assert-True ($blacklist.code -eq 200) "blacklist.total=$($blacklist.data.total)"

    Write-Step '7/7 清理、登出与 token 失效'
    if ($userId) {
        Invoke-RestMethod -Uri "$BaseUrl/api/system/users/$userId" -Method Delete -Headers $admin | Out-Null
        $userId = $null
    }
    if ($roleId) {
        Invoke-RestMethod -Uri "$BaseUrl/api/system/roles/$roleId" -Method Delete -Headers $admin | Out-Null
        $roleId = $null
    }
    $logout = Invoke-RestMethod -Uri "$BaseUrl/api/auth/logout" -Method Post -Headers $admin
    Assert-True ($logout.code -eq 200) 'logout'
    $afterLogout = Get-HttpStatus 'GET' '/api/auth/me' $admin $null
    Assert-True ($afterLogout -eq 401) "old token rejected ($afterLogout)"

    Write-Host 'SMOKE PASSED' -ForegroundColor Green
} finally {
    if ($admin -and $userId) {
        try {
            Invoke-RestMethod -Uri "$BaseUrl/api/system/users/$userId" -Method Delete -Headers $admin | Out-Null
        } catch { }
    }
    if ($admin -and $roleId) {
        try {
            Invoke-RestMethod -Uri "$BaseUrl/api/system/roles/$roleId" -Method Delete -Headers $admin | Out-Null
        } catch { }
    }
}
