#requires -Version 7.0
<#
.SYNOPSIS
Genera y valida Javadoc público con el JDK existente, sin plugins ni descargas.
.DESCRIPTION
Por defecto verifica Maven offline para obtener classpath. -SkipBuild reutiliza
el informe de tests existente. Trata todos los avisos de doclint como errores.
.EXAMPLE
./scripts/javadoc.ps1
#>
param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$repository = Split-Path $PSScriptRoot -Parent
Push-Location $repository
try {
    if (!$SkipBuild) {
        & mvn -o verify
        if ($LASTEXITCODE -ne 0) { throw 'El build offline falló; no se genera documentación sobre un build fallido.' }
    }
    $report = 'target/surefire-reports/TEST-com.openapiguard.adapter.cli.ComparisonCommandTest.xml'
    if (!(Test-Path -LiteralPath $report)) { throw 'Falta el informe de tests; ejecuta mvnd -o verify.' }
    [xml]$tests = Get-Content -LiteralPath $report -Raw
    $classpath = ($tests.testsuite.properties.property | Where-Object { $_.name -eq 'java.class.path' }).value
    if (!$classpath) { throw 'No se pudo obtener classpath del build existente.' }
    & javadoc -quiet -Werror '-Xdoclint:all' --no-platform-links -notimestamp -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 `
        -d target/javadoc-api -classpath $classpath -sourcepath src/main/java -subpackages com.openapiguard
    if ($LASTEXITCODE -ne 0) { throw 'Javadoc contiene errores o avisos; revisar la salida anterior.' }
    # Tras validar el API estricto, incluir helpers privados para recorrer todo el código.
    # Solo se omite la obligación de etiquetas en helpers: sus algoritmos también están en Markdown.
    & javadoc -quiet -Werror '-Xdoclint:all,-missing' -private --no-platform-links -notimestamp `
        -encoding UTF-8 -docencoding UTF-8 -charset UTF-8 -d target/javadoc `
        -classpath $classpath -sourcepath src/main/java -subpackages com.openapiguard
    if ($LASTEXITCODE -ne 0) { throw 'Javadoc interno contiene errores de sintaxis o referencias.' }
    Write-Output (Join-Path $repository 'target/javadoc/index.html')
} finally { Pop-Location }
