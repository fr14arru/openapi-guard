#requires -Version 7.0
param([string]$Jar = (Join-Path $PSScriptRoot '../target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'))
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path $PSScriptRoot -Parent
$testDirectory = Join-Path $repoRoot 'target/ci script tests'
[IO.Directory]::CreateDirectory($testDirectory) | Out-Null
$old = Join-Path $testDirectory 'contrato anterior.yaml'
$updated = Join-Path $testDirectory 'contrato nuevo.yaml'
Copy-Item -LiteralPath (Join-Path $repoRoot 'src/test/resources/openapi/responses-old.yaml') -Destination $old
Copy-Item -LiteralPath (Join-Path $repoRoot 'src/test/resources/openapi/responses-new.yaml') -Destination $updated
$scriptPath = Join-Path $PSScriptRoot 'compare-contracts.ps1'

function Invoke-Check([string]$Baseline, [string]$Candidate, [int]$Expected, [string]$Name,
                      [string]$SelectedJar = $Jar, [string]$Profile = 'full', [string]$Report = '') {
    if (!$Report) { $Report = Join-Path $testDirectory "$Name.json" }
    $start = [Diagnostics.ProcessStartInfo]::new('pwsh')
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    foreach ($argument in @('-NoProfile', '-File', $scriptPath, '-Baseline', $Baseline, '-Candidate', $Candidate,
            '-Jar', $SelectedJar, '-Report', $Report, '-Profile', $Profile)) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::Start($start)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $text = $stdout.GetAwaiter().GetResult()
    $errors = $stderr.GetAwaiter().GetResult()
    if ($process.ExitCode -ne $Expected) { throw "$Name expected $Expected, got $($process.ExitCode): $errors $text" }
    if ($Expected -ne 2) {
        if ($text -notmatch 'Result: .* \| Status: .* \| Profile: ') { throw "Expected English summary in $Name`: $text" }
        $jsonText = [IO.File]::ReadAllText($Report, [Text.UTF8Encoding]::new($false, $true))
        $document = $jsonText | ConvertFrom-Json
        if ($document.profileId -ne 'operations-data-response-policy-v1') { throw "Wrong profile in $Name" }
        if ($Expected -eq 0 -and $document.verdict -ne 'COMPATIBLE') { throw "Wrong compatible result in $Name" }
        if ($Expected -eq 1 -and $document.verdict -ne 'BREAKING') { throw "Wrong breaking result in $Name" }
        if ($Expected -eq 3 -and $document.status -ne 'PARTIAL') { throw "Wrong partial result in $Name" }
    }
    $process.Dispose()
    Write-Host "PASS $Name (exit $Expected)"
}

Invoke-Check $old $old 0 'compatible'
Invoke-Check $old $updated 1 'breaking'
Invoke-Check $updated $old 3 'review'
Invoke-Check $old (Join-Path $testDirectory 'missing.yaml') 2 'invalid-input'
Invoke-Check $old $old 2 'invalid-profile' -Profile 'unknown'
Invoke-Check $old $old 2 'missing-jar' -SelectedJar (Join-Path $testDirectory 'missing.jar')
$before = [IO.File]::ReadAllBytes($old)
Invoke-Check $old $updated 2 'protect-input' -Report $old
if (![Linq.Enumerable]::SequenceEqual[byte]($before, [IO.File]::ReadAllBytes($old))) { throw 'Input overwritten' }
$catalog = Join-Path $repoRoot 'examples/catalog'
Invoke-Check (Join-Path $catalog 'base.yaml') (Join-Path $catalog 'compatible.yaml') 0 'catalog-compatible'
Invoke-Check (Join-Path $catalog 'base.yaml') (Join-Path $catalog 'breaking.yaml') 1 'catalog-breaking'
Invoke-Check (Join-Path $catalog 'base.yaml') (Join-Path $catalog 'review.yaml') 3 'catalog-review'
Write-Host 'All ten CI checks passed against the real JAR.'
