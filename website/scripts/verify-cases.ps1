#requires -Version 7.0
# Verifies documentation expectations against the existing Java engine, without a new build or downloads.
$ErrorActionPreference = 'Stop'
$websiteRoot = Split-Path $PSScriptRoot -Parent
$repositoryRoot = Split-Path $websiteRoot -Parent
$reportPath = Join-Path $repositoryRoot 'target/surefire-reports/TEST-com.openapiguard.adapter.cli.ComparisonCommandTest.xml'
$catalogPath = Join-Path $websiteRoot '.generated/catalog.json'
if (!(Test-Path -LiteralPath $reportPath)) { throw 'Ejecuta mvnd -o verify desde la raíz antes de verificar casos.' }
if (!(Test-Path -LiteralPath $catalogPath)) { throw 'Ejecuta pnpm prepare:content primero.' }
[xml]$testReport = Get-Content -LiteralPath $reportPath -Raw
$engineClasspath = ($testReport.testsuite.properties.property | Where-Object { $_.name -eq 'java.class.path' }).value
if (!$engineClasspath) { throw 'El informe de tests no contiene classpath.' }
$verifierPath = Join-Path $repositoryRoot 'target/VerifyDocumentationCases.java'
$source = @'
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.domain.ComparisonProfile;
class VerifyDocumentationCases {
    public static void main(String[] args) throws Exception {
        JsonNode cases = new ObjectMapper().readTree(Files.readString(Path.of(args[0]))).path("scenarios");
        int failures = 0;
        for (JsonNode example : cases) {
            ComparisonProfile profile = switch (example.path("profile").asText()) {
                case "parameters" -> ComparisonProfile.PARAMETER_PRESENCE;
                case "full" -> ComparisonProfile.RESPONSE_POLICY;
                default -> throw new IllegalArgumentException("Unknown documentation profile");
            };
            var report = new SwaggerContractAnalyzer(profile).analyze(example.path("baseline").asText(), example.path("candidate").asText());
            var expected = new ArrayList<String>();
            example.path("rules").forEach(rule -> expected.add(rule.asText()));
            expected.sort(String::compareTo);
            var rules = report.changes().stream().map(change -> change.ruleId()).sorted().toList();
            boolean matches = report.status().name().equals(example.path("status").asText())
                    && report.verdict().name().equals(example.path("verdict").asText()) && rules.equals(expected)
                    && (!example.has("diagnostic") || report.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(example.path("diagnostic").asText())));
            if (!matches) {
                failures++;
                System.out.println(example.path("id").asText() + " expected " + example.path("status") + "/" + example.path("verdict") + "/" + expected + " got " + report);
            }
        }
        System.out.println("Documentation cases: " + cases.size() + ", failures: " + failures);
        if (failures > 0) System.exit(1);
    }
}
'@
[IO.File]::WriteAllText($verifierPath, $source, [Text.UTF8Encoding]::new($false))
& java --class-path $engineClasspath $verifierPath $catalogPath
exit $LASTEXITCODE
