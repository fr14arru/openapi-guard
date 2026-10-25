#requires -Version 7.0
<# .SYNOPSIS Crea un ZIP local verificado y su checksum; no publica ni sobrescribe. #>
param([Parameter(Mandatory)][string]$Package,[Parameter(Mandatory)][string]$Archive)
$ErrorActionPreference='Stop'
$source=[IO.Path]::GetFullPath($Package)
$destination=[IO.Path]::GetFullPath($Archive)
if (Test-Path -LiteralPath $destination) { throw 'Archive already exists.' }
if (Test-Path -LiteralPath "$destination.sha256") { throw 'Checksum already exists.' }
if ($destination.StartsWith($source.TrimEnd([IO.Path]::DirectorySeparatorChar)+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'Archive must be outside the package.' }
& node (Join-Path $PSScriptRoot 'verify-package.mjs') $source
if ($LASTEXITCODE -ne 0) { throw 'Package verification failed; refusing archival.' }
[IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($destination)) | Out-Null
[IO.Compression.ZipFile]::CreateFromDirectory($source,$destination,[IO.Compression.CompressionLevel]::Optimal,$false)
$hash=(Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
"$hash  $([IO.Path]::GetFileName($destination))" | Set-Content -LiteralPath "$destination.sha256" -Encoding utf8NoBOM
Write-Output "Local archive: $destination"
