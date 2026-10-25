#requires -Version 7.0
<#
.SYNOPSIS
Compara contratos con un solo comando, sin instalar dependencias.
.DESCRIPTION
Usa full y texto por defecto. Si falta el JAR predeterminado, construye offline
con mvnd o Maven. Los códigos 0/1/2/3 se conservan. Las rutas de entrada/salida
son relativas al directorio del usuario; los ejemplos Demo y el JAR al repositorio.
.EXAMPLE
./guard.ps1 -Demo
.EXAMPLE
./guard.ps1 -Baseline old.yaml -Candidate new.yaml -Format markdown -Output report.md
#>
param(
    [string]$Baseline,
    [string]$Candidate,
    [switch]$Demo,
    [ValidateSet('full','arrays','schemas','schema-values','request-body','parameters','operations')][string]$Profile = 'full',
    [ValidateSet('json','text','markdown')][string]$Format = 'text',
    [string]$Output,
    [string]$Jar
)
$ErrorActionPreference = 'Stop'
try {
    if ($Demo) {
        if ($Baseline -or $Candidate) { throw 'Demo does not accept additional contracts.' }
        $Baseline = Join-Path $PSScriptRoot 'examples/catalog/base.yaml'
        $Candidate = Join-Path $PSScriptRoot 'examples/catalog/compatible.yaml'
    }
    if (!$Baseline -or !$Candidate) { throw 'Specify -Baseline and -Candidate, or use -Demo.' }
    if (!$Jar) {
        $Jar = Join-Path $PSScriptRoot 'target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar'
        if (!(Test-Path -LiteralPath $Jar -PathType Leaf)) {
            $buildTool = Get-Command mvnd -ErrorAction SilentlyContinue
            if (!$buildTool) { $buildTool = Get-Command mvn -ErrorAction SilentlyContinue }
            if (!$buildTool) { throw 'Maven/mvnd is missing. Tools are not installed automatically.' }
            Push-Location $PSScriptRoot
            try {
                # La primera compilación no debe contaminar stdout cuando se pide JSON.
                & $buildTool.Source -o clean verify | ForEach-Object { [Console]::Error.WriteLine($_) }
                if ($LASTEXITCODE -ne 0) { throw 'The offline build failed. No dependencies were downloaded; check the preceding error.' }
            } finally { Pop-Location }
        }
    }
    if (!(Test-Path -LiteralPath $Jar -PathType Leaf)) { throw "JAR does not exist: $Jar" }
    $cliArguments = @("--profile=$Profile", "--format=$Format", "--baseline=$Baseline", "--candidate=$Candidate")
    if ($Output) { $cliArguments += "--output=$Output" }
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' -jar $Jar @cliArguments
    exit $LASTEXITCODE
} catch {
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 2
}
