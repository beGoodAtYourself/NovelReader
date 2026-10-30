param(
    [ValidateSet('Release', 'Debug')]
    [string]$Configuration = 'Release',
    [switch]$Install,
    [string]$DeviceSerial
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$localBuildRoot = Join-Path $projectRoot '.local-build'
$jdkDirectory = Get-ChildItem (Join-Path $localBuildRoot 'jdk') -Directory |
    Where-Object { Test-Path (Join-Path $_.FullName 'bin/java.exe') } |
    Select-Object -First 1
$sdkDirectory = Join-Path $localBuildRoot 'android-sdk'
$gradleExecutable = Join-Path $localBuildRoot 'gradle/gradle-9.8.0/bin/gradle.bat'

if (!$jdkDirectory -or !(Test-Path -LiteralPath $gradleExecutable)) {
    throw '缺少项目本地 JDK 21 或 Gradle 9.8.0，请先准备 .local-build 中的构建工具。'
}
if ($Configuration -eq 'Release' -and !(Test-Path (Join-Path $localBuildRoot 'gradle-cache/gradle.properties'))) {
    throw '缺少 .local-build/gradle-cache/gradle.properties 中的本地签名配置。'
}
if ($Install -and [string]::IsNullOrWhiteSpace($DeviceSerial)) {
    throw '安装时必须通过 -DeviceSerial 指定设备，避免安装到错误的手机。'
}

$environmentNames = @('JAVA_HOME', 'ANDROID_HOME', 'ANDROID_SDK_ROOT', 'GRADLE_USER_HOME', 'PATH')
$savedEnvironment = @{}
foreach ($name in $environmentNames) {
    $savedEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
}

Push-Location $projectRoot
try {
    # 环境变量仅在本次构建进程中生效，不修改系统配置。
    $env:JAVA_HOME = $jdkDirectory.FullName
    $env:ANDROID_HOME = $sdkDirectory
    $env:ANDROID_SDK_ROOT = $sdkDirectory
    $env:GRADLE_USER_HOME = Join-Path $localBuildRoot 'gradle-cache'
    $env:PATH = (Join-Path $jdkDirectory.FullName 'bin') + ';' + $env:PATH

    $logDirectory = Join-Path $localBuildRoot 'logs'
    New-Item -ItemType Directory -Path $logDirectory -Force | Out-Null
    $logPath = Join-Path $logDirectory ('build-{0}-{1}.log' -f $Configuration.ToLowerInvariant(), (Get-Date -Format 'yyyyMMdd-HHmmss'))
    & $gradleExecutable ":app:assembleApp$Configuration" --no-configuration-cache --console=plain 2>&1 |
        Tee-Object -FilePath $logPath
    if ($LASTEXITCODE -ne 0) { throw "构建失败，日志：$logPath" }

    $outputDirectory = Join-Path $projectRoot ('app/build/outputs/apk/app/' + $Configuration.ToLowerInvariant())
    $metadata = Get-Content (Join-Path $outputDirectory 'output-metadata.json') -Raw | ConvertFrom-Json
    $arm64Output = $metadata.elements | Where-Object {
        @($_.filters | Where-Object { $_.filterType -eq 'ABI' -and $_.value -eq 'arm64-v8a' }).Count -gt 0
    } | Select-Object -First 1
    if (!$arm64Output) {
        $arm64Output = $metadata.elements | Where-Object { @($_.filters).Count -eq 0 } | Select-Object -First 1
    }
    if (!$arm64Output) { throw '没有找到适用于小米 14 的 arm64 或通用 APK。' }
    $apkPath = Join-Path $outputDirectory $arm64Output.outputFile
    Write-Host "APK：$apkPath"

    if ($Install) {
        $adbCommand = Get-Command adb.exe -ErrorAction SilentlyContinue
        $adbExecutable = if ($adbCommand) { $adbCommand.Source } else { Join-Path $sdkDirectory 'platform-tools/adb.exe' }
        & $adbExecutable -s $DeviceSerial install -r $apkPath
        if ($LASTEXITCODE -ne 0) { throw '安装失败，请检查手机上的 USB 安装确认提示。' }
        Write-Host "已安装到设备 $DeviceSerial"
    }
} finally {
    Pop-Location
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $savedEnvironment[$name], 'Process')
    }
}
