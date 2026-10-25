#requires -Version 7.0
param(
    [string]$ArchivePath,
    [string]$ExpectedSha512,
    [switch]$ValidateArchiveOnly
)
$ErrorActionPreference = 'Stop'
$version = '3.10.0'
# CI bootstrap only. Local developers keep their existing Maven/mvnd installation.
if (!$ArchivePath) {
    if (!$env:RUNNER_TEMP -or !$env:GITHUB_PATH) {
        throw 'Downloads are restricted to a GitHub runner; use installed Maven locally.'
    }
    $installation = Join-Path $env:RUNNER_TEMP "apache-maven-$version"
    $ArchivePath = Join-Path $env:RUNNER_TEMP "apache-maven-$version-bin.zip"
    $source = "https://archive.apache.org/dist/maven/maven-3/$version/binaries/apache-maven-$version-bin.zip"
    Invoke-WebRequest -Uri $source -OutFile $ArchivePath -TimeoutSec 60
    $checksum = (Invoke-WebRequest -Uri "$source.sha512" -TimeoutSec 60).Content
    $ExpectedSha512 = [regex]::Match([string]$checksum, '(?i)\b[0-9a-f]{128}\b').Value
}
if ($ExpectedSha512 -notmatch '^[0-9a-fA-F]{128}$') { throw 'Expected a SHA512 checksum.' }
$actual = (Get-FileHash -LiteralPath $ArchivePath -Algorithm SHA512).Hash
if ($actual -ine $ExpectedSha512) { throw 'Maven archive SHA512 mismatch; refusing extraction.' }
if ($ValidateArchiveOnly) { Write-Output 'SHA512 verified.'; return }
if (!$env:RUNNER_TEMP -or !$env:GITHUB_PATH) { throw 'Installation is restricted to a GitHub runner.' }
$installation = Join-Path $env:RUNNER_TEMP "apache-maven-$version"
Expand-Archive -LiteralPath $ArchivePath -DestinationPath $env:RUNNER_TEMP -Force
$bin = Join-Path $installation 'bin'
if (!(Test-Path -LiteralPath (Join-Path $bin 'mvn.cmd') -PathType Leaf)) { throw 'Maven archive lacks mvn.cmd.' }
Add-Content -LiteralPath $env:GITHUB_PATH -Value $bin -Encoding utf8
Write-Output "Apache Maven $version verified and prepared."
