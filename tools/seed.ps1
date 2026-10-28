# Load test automations into a debug build on the phone.
# Usage: tools\seed.ps1 -Json '[{...}]' [-Replace]
param(
    [Parameter(Mandatory = $true)][string]$Json,
    [switch]$Replace,
    [string]$Serial = $env:ADB_SERIAL,
    [string]$Adb = $(if ($env:ADB) { $env:ADB } else { 'adb' })
)
$b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($Json))
$args = @('-s', $Serial, 'shell', 'am', 'broadcast', '-n', 'com.tuvin.pixelautomations/.DebugImportReceiver', '--es', 'b64', $b64)
if ($Replace) { $args += @('--ez', 'replace', 'true') }
& $Adb @args
