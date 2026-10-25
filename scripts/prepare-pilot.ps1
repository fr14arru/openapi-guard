#requires -Version 7.0
<# .SYNOPSIS Genera un kit de repositorio privado para probar la Action copiada; no hace commits ni accede a GitHub. #>
param([Parameter(Mandatory)][string]$Package,[Parameter(Mandatory)][string]$Destination)
$ErrorActionPreference='Stop'
$packagePath=[IO.Path]::GetFullPath($Package)
$destinationPath=[IO.Path]::GetFullPath($Destination)
if (Test-Path -LiteralPath $destinationPath) { throw 'Destination already exists; use a new pilot directory.' }
& node (Join-Path $PSScriptRoot 'verify-package.mjs') $packagePath
if ($LASTEXITCODE -ne 0) { throw 'Package integrity failed; refusing pilot preparation.' }
[IO.Directory]::CreateDirectory($destinationPath) | Out-Null
Copy-Item -LiteralPath $packagePath -Destination (Join-Path $destinationPath '.guard-action') -Recurse
foreach ($directory in @('.github/workflows','contracts','pilot-cases')) { [IO.Directory]::CreateDirectory((Join-Path $destinationPath $directory)) | Out-Null }
Copy-Item -LiteralPath (Join-Path $packagePath 'examples/base.yaml') -Destination (Join-Path $destinationPath 'contracts/api.yaml')
foreach ($example in @('compatible','breaking','review')) { Copy-Item -LiteralPath (Join-Path $packagePath "examples/$example.yaml") -Destination (Join-Path $destinationPath 'pilot-cases') }
'not an OpenAPI contract' | Set-Content -LiteralPath (Join-Path $destinationPath 'pilot-cases/invalid.yaml') -Encoding utf8NoBOM
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../examples/pilot/openapi-guard.yml') -Destination (Join-Path $destinationPath '.github/workflows')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot '../docs/pilot-kit-readme.txt') -Destination (Join-Path $destinationPath 'README.md')
"/target/`n" | Set-Content -LiteralPath (Join-Path $destinationPath '.gitignore') -Encoding utf8NoBOM
"# Preserve the verified package bytes on every checkout.`n.guard-action/** -text`n" |
    Set-Content -LiteralPath (Join-Path $destinationPath '.gitattributes') -Encoding utf8NoBOM
Write-Output "Private pilot kit: $destinationPath"
Write-Output 'No repository was created or published. Review README and use a private test repository.'
