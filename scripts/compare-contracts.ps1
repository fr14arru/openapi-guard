#requires -Version 7.0
param(
    [Parameter(Mandatory)][string]$Baseline,
    [Parameter(Mandatory)][string]$Candidate,
    [string]$Jar = (Join-Path $PSScriptRoot '../target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'),
    [string]$Report = (Join-Path $PSScriptRoot '../target/report.json'),
    [string]$Profile = 'full'
)
$ErrorActionPreference = 'Stop'
$process = $null
try {
    $baselinePath = [IO.Path]::GetFullPath($Baseline)
    $candidatePath = [IO.Path]::GetFullPath($Candidate)
    $jarPath = [IO.Path]::GetFullPath($Jar)
    $reportPath = [IO.Path]::GetFullPath($Report)
    foreach ($protectedPath in @($baselinePath, $candidatePath, $jarPath)) {
        if ([string]::Equals($reportPath, $protectedPath, [StringComparison]::OrdinalIgnoreCase)) {
            throw 'The report cannot overwrite a contract or the JAR.'
        }
    }
    if (![IO.File]::Exists($jarPath)) { throw "JAR does not exist: $jarPath. Run mvnd clean verify first." }
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($reportPath)) | Out-Null
    $utf8 = [Text.UTF8Encoding]::new($false, $true)
    [IO.File]::WriteAllText($reportPath, '', $utf8)
    $start = [Diagnostics.ProcessStartInfo]::new('java')
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.StandardOutputEncoding = $utf8
    $start.StandardErrorEncoding = [Text.UTF8Encoding]::new($false)
    foreach ($argument in @('-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8', '-jar', $jarPath,
            "--profile=$Profile", '--format=json', $baselinePath, $candidatePath)) { $start.ArgumentList.Add($argument) }
    $process = [Diagnostics.Process]::Start($start)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    if (!$process.WaitForExit(60000)) {
        $process.Kill($true)
        throw 'The comparison exceeded the 60-second limit.'
    }
    $json = $stdout.GetAwaiter().GetResult()
    $errors = $stderr.GetAwaiter().GetResult()
    if ($errors) { [Console]::Error.Write($errors) }
    $exitCode = $process.ExitCode
    if ($exitCode -notin @(0, 1, 2, 3)) { throw "The Java process terminated unexpectedly with exit code $exitCode." }
    if (!$json.Trim()) {
        if ($exitCode -eq 2) { exit 2 }
        throw 'The comparison did not produce a JSON report.'
    }
    $document = $json | ConvertFrom-Json -ErrorAction Stop
    if ($document.status -notin @('COMPLETED', 'PARTIAL', 'FAILED') -or $document.verdict -notin @('COMPATIBLE', 'BREAKING', 'INDETERMINATE')) {
        throw 'The report does not have the expected format.'
    }
    [IO.File]::WriteAllText($reportPath, $json, $utf8)
    Write-Host "Result: $($document.verdict) | Status: $($document.status) | Profile: $($document.profileId)"
    foreach ($change in $document.changes) { Write-Host "[$($change.ruleId)] $($change.description)" }
    foreach ($diagnostic in $document.diagnostics) { Write-Host "Review [$($diagnostic.code)]: $($diagnostic.description)" }
    Write-Host "UTF-8 report: $reportPath"
    exit $exitCode
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 2
} finally {
    if ($null -ne $process) { $process.Dispose() }
}
