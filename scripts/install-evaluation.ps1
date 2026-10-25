#requires -Version 7.0
<# .SYNOPSIS Verifica checksum externo, rutas y límites ZIP antes de extraer a un destino nuevo. #>
param([Parameter(Mandatory)][string]$Archive,[Parameter(Mandatory)][string]$ExpectedSha256,[Parameter(Mandatory)][string]$Destination)
$ErrorActionPreference='Stop'
$destinationPath=[IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $destinationPath) { throw 'Destination already exists.' }
if ($ExpectedSha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'Expected an external SHA256 checksum.' }
if ((Get-FileHash -LiteralPath $Archive -Algorithm SHA256).Hash -ine $ExpectedSha256) { throw 'Archive SHA256 mismatch; refusing extraction.' }
$zip=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Archive))
try {
    $names=[Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
    [long]$total=0
    if ($zip.Entries.Count -gt 5000) { throw 'Archive exceeds entry budget.' }
    foreach ($entry in $zip.Entries) {
        $name=$entry.FullName.TrimEnd('/')
        if (!$name -or $name.Contains('\') -or $name -match '[\x00-\x1f:]' -or @($name.Split('/') | Where-Object { !$_ -or $_ -eq '.' -or $_ -eq '..' }).Count) { throw 'Unsafe archive path.' }
        if (!$names.Add($name)) { throw 'Duplicate archive path.' }
        if ((($entry.ExternalAttributes -shr 16) -band 0xF000) -eq 0xA000) { throw 'Archive symbolic links are unsupported.' }
        $total+=$entry.Length
        if ($entry.Length -gt 100MB -or $total -gt 200MB) { throw 'Archive exceeds extraction budget.' }
    }
    [IO.Directory]::CreateDirectory($destinationPath) | Out-Null
    foreach ($entry in $zip.Entries) {
        $target=[IO.Path]::GetFullPath((Join-Path $destinationPath $entry.FullName))
        $prefix=$destinationPath.TrimEnd([IO.Path]::DirectorySeparatorChar)+[IO.Path]::DirectorySeparatorChar
        if (!$target.StartsWith($prefix,[StringComparison]::OrdinalIgnoreCase)) { throw 'Archive path escapes destination.' }
        if ($entry.FullName.EndsWith('/')) { [IO.Directory]::CreateDirectory($target) | Out-Null; continue }
        [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target)) | Out-Null
        [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$false)
    }
} finally { $zip.Dispose() }
& node (Join-Path $PSScriptRoot 'verify-package.mjs') $destinationPath
if ($LASTEXITCODE -ne 0) { throw 'Extracted package verification failed; do not use the partial destination.' }
Write-Output "Evaluation package installed: $destinationPath"
