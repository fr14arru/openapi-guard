#requires -Version 7.0
<#
.SYNOPSIS
Prepara un paquete local evaluable e inventaría el JAR real; no instala ni publica.
.DESCRIPTION
Incluye MIT para código propio y evidencias de terceros por dependencia.
No publica releases. Rechaza destinos existentes.
#>
param(
    [string]$Jar = $(if ($env:GUARD_TEST_JAR) { $env:GUARD_TEST_JAR } else { Join-Path $PSScriptRoot '../target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar' }),
    [string]$Destination = (Join-Path $PSScriptRoot '../target/evaluation'),
    [string]$BuildProof
)
$ErrorActionPreference='Stop'
$repository=Split-Path $PSScriptRoot -Parent
$jarPath=[IO.Path]::GetFullPath($Jar)
$destinationPath=[IO.Path]::GetFullPath($Destination)
if (![IO.File]::Exists($jarPath)) { throw 'Build and verify the JAR before preparing an evaluation package.' }
if (Test-Path -LiteralPath $destinationPath) { throw 'Destination already exists; choose a fresh directory.' }
$archive=[IO.Compression.ZipFile]::OpenRead($jarPath)
try {
    $libraries=@($archive.Entries | Where-Object FullName -Like 'BOOT-INF/lib/*.jar' | Sort-Object FullName)
    if (!$libraries.Count -or !$archive.GetEntry('META-INF/MANIFEST.MF')) { throw 'Expected an executable Spring Boot JAR.' }
    $reader=[IO.StreamReader]::new($archive.GetEntry('META-INF/MANIFEST.MF').Open())
    try { $jarManifest=$reader.ReadToEnd() } finally { $reader.Dispose() }
    if ($BuildProof) {
        $proof=Get-Content -LiteralPath $BuildProof -Raw | ConvertFrom-Json
        if ($proof.jarSha256 -ne (Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant() -or
            $proof.sourceCommit -notmatch '^[a-f0-9]{40}$' -or !$proof.verified -or
            $jarManifest -notmatch "(?m)^Guard-Source-Commit: $([regex]::Escape($proof.sourceCommit))\r?$" -or
            $jarManifest -notmatch "(?m)^Guard-Version: $([regex]::Escape($proof.version))\r?$") { throw 'Build proof does not match the verified JAR.' }
    }
    [IO.Directory]::CreateDirectory($destinationPath) | Out-Null
    [IO.Directory]::CreateDirectory((Join-Path $destinationPath 'scripts')) | Out-Null
    [IO.Directory]::CreateDirectory((Join-Path $destinationPath 'examples')) | Out-Null
    [IO.Directory]::CreateDirectory((Join-Path $destinationPath 'third-party')) | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $destinationPath 'guard.jar')
    Copy-Item -LiteralPath (Join-Path $repository 'action.yml') -Destination $destinationPath
    foreach ($script in @('action-runner.mjs','git-contracts.mjs','github-presentation.mjs','pr-comment.mjs','verify-package.mjs')) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $script) -Destination (Join-Path $destinationPath 'scripts')
    }
    foreach ($example in @('base','compatible','breaking','review')) {
        Copy-Item -LiteralPath (Join-Path $repository "examples/catalog/$example.yaml") -Destination (Join-Path $destinationPath 'examples')
    }
    Copy-Item -LiteralPath (Join-Path $repository 'docs/evaluation-package-readme.txt') -Destination (Join-Path $destinationPath 'README.txt')
    if ($BuildProof) { Copy-Item -LiteralPath $BuildProof -Destination (Join-Path $destinationPath 'build-proof.json') }
    $dependencies=@()
    foreach ($entry in $libraries) {
        $memory=[IO.MemoryStream]::new()
        $stream=$entry.Open()
        try { $stream.CopyTo($memory) } finally { $stream.Dispose() }
        $digest=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($memory.ToArray())).ToLowerInvariant()
        $memory.Position=0
        $nested=[IO.Compression.ZipArchive]::new($memory,[IO.Compression.ZipArchiveMode]::Read)
        try {
            $evidence=@()
            $index=0
            foreach ($document in $nested.Entries | Where-Object {
                $_.FullName -match '(?i)(/[^/]*(license|notice|copying)[^/]*$|META-INF/maven/.*/pom\.xml$)' -and
                $_.FullName -notmatch '(?i)\.(class|jar|zip)$'
            } | Sort-Object FullName) {
                if ($document.Length -gt 2MB) { throw 'Embedded attribution evidence exceeds 2 MiB.' }
                # Generated basenames prevent embedded ZIP paths from escaping the package.
                $name='third-party/{0:D2}-{1:D2}.txt' -f $dependencies.Count,$index++
                $inputStream=$document.Open()
                $outputStream=[IO.File]::Create((Join-Path $destinationPath $name))
                try { $inputStream.CopyTo($outputStream) } finally { $inputStream.Dispose(); $outputStream.Dispose() }
                $evidence+=@{originalPath=$document.FullName;file=$name}
            }
            $dependencies+=@{artifact=$entry.Name;sha256=$digest;embeddedEvidence=$evidence;reviewStatus='PENDING'}
        } finally { $nested.Dispose(); $memory.Dispose() }
    }
    $sourceCommit=(& git -C $repository rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0) { throw 'Cannot determine source commit.' }
    $dirty=[bool](& git -C $repository status --porcelain)
    if ($jarManifest -notmatch '(?m)^Spring-Boot-Version: ([0-9][^\r\n]*)\r?$') { throw 'Cannot identify the Spring Boot loader version.' }
    $bootVersion=$Matches[1]
    # Loader classes are outside BOOT-INF/lib: pin their actual bytes separately.
    [string[]]$loaderLines=@(foreach ($entry in $archive.Entries | Where-Object FullName -Match '^org/springframework/boot/loader/.+\.class$') {
        $stream=$entry.Open()
        try { $hash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData($stream)).ToLowerInvariant() } finally { $stream.Dispose() }
        "$($entry.FullName)|$hash"
    })
    if (!$loaderLines.Count) { throw 'Spring Boot loader classes are missing.' }
    [Array]::Sort($loaderLines,[StringComparer]::Ordinal)
    $loaderHash=[Convert]::ToHexString([Security.Cryptography.SHA256]::HashData([Text.Encoding]::UTF8.GetBytes(($loaderLines -join "`n")))).ToLowerInvariant()
    $manifest=@{formatVersion=2;distributionStatus='OPEN_SOURCE_LOCAL_PACKAGE';
        sourceCommit=$sourceCommit;workingTreeDirty=$dirty;javaVersion=25;profile='full';
        jarSha256=(Get-FileHash -LiteralPath $jarPath -Algorithm SHA256).Hash.ToLowerInvariant();dependencies=$dependencies;
        loader=@{artifact="spring-boot-loader-$bootVersion";sha256=$loaderHash}}
    if ($BuildProof) {
        $manifest.sourceCommit=$proof.sourceCommit
        $manifest.version=$proof.version
        $manifest.buildVerified=$true
        $manifest.distributionStatus='OPEN_SOURCE_CANDIDATE_PUBLICATION_PENDING'
    }
    $rows=$dependencies | ForEach-Object { "| $($_.artifact) | $($_.embeddedEvidence.Count) | PENDING |" }
    @("# Third-party review evidence",'',
        'Project code is MIT licensed. This inventory is technical evidence, not legal clearance for third-party components.',
        'See LICENSE.txt and THIRD-PARTY-NOTICES.md for project licensing, pinned library/loader evidence, full texts and corresponding source.',
        'Embedded files can contain upstream notices for bundled code; preserve and review them, not only Maven license names.',
        '', '| Runtime artifact | Embedded documents | Review |','| --- | --- | --- |') + $rows |
        Set-Content -LiteralPath (Join-Path $destinationPath 'THIRD-PARTY-REVIEW.md') -Encoding utf8NoBOM
    # Offline catalog validation must succeed before the outer manifest can approve integrity.
    $manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $destinationPath 'manifest.json') -Encoding utf8NoBOM
    & node (Join-Path $PSScriptRoot 'prepare-attributions.mjs') $destinationPath
    if ($LASTEXITCODE -ne 0) { throw 'Attribution component/evidence validation failed; package is incomplete.' }
    $manifest=Get-Content -LiteralPath (Join-Path $destinationPath 'manifest.json') -Raw | ConvertFrom-Json -AsHashtable
    $manifest.integrity=@(Get-ChildItem -LiteralPath $destinationPath -File -Recurse | Sort-Object FullName | ForEach-Object {
        if ($_.Name -eq 'manifest.json' -and $_.DirectoryName -eq $destinationPath) { return }
        @{path=[IO.Path]::GetRelativePath($destinationPath,$_.FullName).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
    })
    $manifest | ConvertTo-Json -Depth 12 | Set-Content -LiteralPath (Join-Path $destinationPath 'manifest.json') -Encoding utf8NoBOM
    Write-Output "Local evaluation package: $destinationPath"
    Write-Output "Runtime libraries: $($dependencies.Count). Publication approval: pending."
} finally { $archive.Dispose() }
