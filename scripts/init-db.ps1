param(
    [string]$HostName = '127.0.0.1',
    [int]$Port = 3306,
    [string]$User = 'root',
    [string]$Password = $env:MYSQL_PASSWORD,
    [string]$Database = 'living_frame',
    [string]$MysqlExe = 'mysql',
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path $PSScriptRoot -Parent
$dbDir = Join-Path $projectRoot 'living-frame-admin\src\main\resources\db'
$createScript = Join-Path $dbDir '00-create-database.sql'
$schemaScript = Join-Path $dbDir 'schema.sql'
$dataScript = Join-Path $dbDir 'data.sql'

foreach ($file in @($createScript, $schemaScript, $dataScript)) {
    if (-not (Test-Path -LiteralPath $file)) {
        throw "找不到数据库初始化文件: $file"
    }
}

if ($Database -notmatch '^[A-Za-z0-9_]+$') {
    throw "数据库名不合法: $Database"
}

$sql = @(
    (Get-Content -LiteralPath $createScript -Raw -Encoding UTF8),
    "`nUSE ``$Database``;`n",
    (Get-Content -LiteralPath $schemaScript -Raw -Encoding UTF8),
    "`n",
    (Get-Content -LiteralPath $dataScript -Raw -Encoding UTF8)
) -join "`n"

$tempFile = Join-Path ([System.IO.Path]::GetTempPath()) ("living-frame-init-{0}.sql" -f [guid]::NewGuid().ToString('N'))
[System.IO.File]::WriteAllText($tempFile, $sql, [System.Text.UTF8Encoding]::new($false))

if ($DryRun) {
    Write-Host "初始化 SQL 已生成: $tempFile" -ForegroundColor Cyan
    Write-Host "数据库: $Database" -ForegroundColor Cyan
    return
}

if (-not (Get-Command $MysqlExe -ErrorAction SilentlyContinue)) {
    throw "找不到 mysql 客户端: $MysqlExe。请安装 MySQL 客户端，或通过 -MysqlExe 指定完整路径。"
}

$oldMysqlPwd = $env:MYSQL_PWD
try {
    if ($Password) {
        $env:MYSQL_PWD = $Password
    } else {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    }

    $mysqlArgs = @(
        "--host=$HostName",
        "--port=$Port",
        "--user=$User",
        "--default-character-set=utf8mb4"
    )
    $command = '"' + $MysqlExe + '" ' + ($mysqlArgs -join ' ') + ' < "' + $tempFile + '"'
    & cmd.exe /d /c $command
    if ($LASTEXITCODE -ne 0) {
        throw "数据库初始化失败，mysql 退出码: $LASTEXITCODE"
    }
    Write-Host "数据库初始化完成: $Database" -ForegroundColor Green
} finally {
    if ($null -eq $oldMysqlPwd) {
        Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
    } else {
        $env:MYSQL_PWD = $oldMysqlPwd
    }
    Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
}
