#requires -Version 7.0
<#
.SYNOPSIS
Construye y verifica un candidato privado desde un checkout limpio; no publica.
.DESCRIPTION
Usa herramientas existentes y Maven offline. Vincula versión/commit al manifiesto
del JAR y registra evidencias de builds y tests. Rechaza destinos reutilizados.
#>
param(
    [string]$Version='0.1.0-rc.1',
    [string]$Repository=(Split-Path $PSScriptRoot -Parent),
    [string]$OutputRoot,
    [ValidateSet('mvn','mvnd')][string]$BuildTool='mvnd'
)
$ErrorActionPreference='Stop'
$previousJar=$env:GUARD_TEST_JAR
$pushed=$false
try {
    if ($Version -notmatch '^\d+\.\d+\.\d+(?:-rc\.[1-9]\d*)?$') { throw 'Candidate version must be an explicit semantic version, optionally -rc.N; no SNAPSHOT.' }
    $repositoryPath=[IO.Path]::GetFullPath($Repository)
    if (& git -C $repositoryPath status --porcelain) { throw 'A clean source checkout is required before building a candidate.' }
    if ($LASTEXITCODE -ne 0) { throw 'Source repository is unavailable.' }
    $sourceCommit=(& git -C $repositoryPath rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve source commit.' }
    if (!$OutputRoot) { $OutputRoot=Join-Path $repositoryPath 'dist' }
    $destination=Join-Path ([IO.Path]::GetFullPath($OutputRoot)) "$Version-$($sourceCommit.Substring(0,8))"
    if (Test-Path -LiteralPath $destination) { throw 'Candidate destination already exists; use a new output root.' }
    $java=(& java --version 2>&1 | Out-String)
    if ($LASTEXITCODE -ne 0 -or $java -notmatch '(?m)^(?:openjdk|java) 25\.' -or $java -notmatch 'Corretto') { throw 'Amazon Corretto Java 25 is required; no tools are installed automatically.' }
    [IO.Directory]::CreateDirectory($destination) | Out-Null
    Push-Location $repositoryPath
    $pushed=$true
    & $BuildTool -o "-Drevision=$Version" "-Dguard.sourceCommit=$sourceCommit" clean verify *> (Join-Path $destination 'mvnd-verify.log')
    if ($LASTEXITCODE -ne 0) { throw 'Candidate build failed; inspect the build log.' }
    & mvn -o "-Drevision=$Version" "-Dguard.sourceCommit=$sourceCommit" verify *> (Join-Path $destination 'maven-verify.log')
    if ($LASTEXITCODE -ne 0) { throw 'Standard Maven verification failed; inspect the build log.' }
    $jar=Join-Path $repositoryPath "target/openapi-breaking-change-guard-$Version.jar"
    if (!(Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'Build did not produce the requested version.' }
    $env:GUARD_TEST_JAR=$jar
    $tests=@(Get-ChildItem -LiteralPath (Join-Path $repositoryPath 'scripts') -Filter '*.test.mjs' | Sort-Object Name | ForEach-Object FullName)
    & node --test @tests *> (Join-Path $destination 'node-tests.log')
    if ($LASTEXITCODE -ne 0) { throw 'Candidate adapter tests failed; inspect node-tests.log.' }
    & pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repositoryPath 'scripts/test-ci.ps1') -Jar $jar *> (Join-Path $destination 'ci-tests.log')
    if ($LASTEXITCODE -ne 0) { throw 'Candidate CI tests failed.' }
    if ((& git -C $repositoryPath rev-parse HEAD).Trim() -ne $sourceCommit -or (& git -C $repositoryPath status --porcelain)) { throw 'Source changed during verification; candidate proof is refused.' }
    $proof=@{formatVersion=1;verified=$true;version=$Version;sourceCommit=$sourceCommit;
        jarSha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant();
        verifiedAtUtc=[DateTime]::UtcNow.ToString('o');java=$java.Trim();checks=@('clean-verify','standard-maven-verify','node-tests','ci-tests');
        remotePrValidated=$false;publicationApproved=$false}
    $proofPath=Join-Path $destination 'build-proof.json'
    $proof | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $proofPath -Encoding utf8NoBOM
    $package=Join-Path $destination 'package'
    & pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repositoryPath 'scripts/prepare-evaluation.ps1') -Jar $jar -Destination $package -BuildProof $proofPath
    if ($LASTEXITCODE -ne 0) { throw 'Candidate package failed.' }
    $archive=Join-Path $destination "openapi-guard-$Version.zip"
    & pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repositoryPath 'scripts/archive-evaluation.ps1') -Package $package -Archive $archive
    if ($LASTEXITCODE -ne 0) { throw 'Candidate archival failed.' }
    $checksum=(Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash
    $installed=Join-Path $destination 'installed'
    & pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repositoryPath 'scripts/install-evaluation.ps1') -Archive $archive -ExpectedSha256 $checksum -Destination $installed
    if ($LASTEXITCODE -ne 0) { throw 'Candidate transport verification failed.' }
    & pwsh -NoProfile -ExecutionPolicy Bypass -File (Join-Path $repositoryPath 'scripts/prepare-pilot.ps1') -Package $installed -Destination (Join-Path $destination 'pilot-kit')
    if ($LASTEXITCODE -ne 0) { throw 'Candidate pilot kit preparation failed.' }
    Write-Output "Private candidate ready: $destination"
    Write-Output 'Public release, customer terms and remote PR validation remain pending approval/evidence.'
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 2
} finally {
    $env:GUARD_TEST_JAR=$previousJar
    if ($pushed) { Pop-Location }
}
