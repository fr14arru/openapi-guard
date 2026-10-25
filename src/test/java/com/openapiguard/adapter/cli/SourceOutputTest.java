package com.openapiguard.adapter.cli;

import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.application.CompareContracts;
import com.openapiguard.domain.ComparisonProfile;
import com.openapiguard.domain.ComparisonReport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Regresiones de navegación hacia el documento candidato, sin arrancar Spring. */
class SourceOutputTest {
    @TempDir Path directory;
    private static final String CONTRACT = """
            openapi: 3.0.3
            info: {title: Test, version: '1'}
            paths:
              /pets:
                get:
                  parameters:
                    - name: limit
                      in: query
                      required: false
                      schema: {type: integer}
                  responses:
                    '200': {description: OK}
            """;
    private String run(String old, String next) throws IOException {
        Path baseline = directory.resolve("old.yaml"), candidate = directory.resolve("new contract.yaml");
        Files.writeString(baseline, old); Files.writeString(candidate, next);
        var out = new ByteArrayOutputStream();
        new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer())).execute(
                new String[]{"--format=text", "--profile=full", baseline.toString(), candidate.toString()},
                new PrintStream(out, true, StandardCharsets.UTF_8), System.err);
        return out.toString(StandardCharsets.UTF_8);
    }
    @Test void requiredParameterHasOneLineWithCandidateLocationAndSnippet() throws Exception {
        String output = run(CONTRACT, CONTRACT.replace("required: false", "required: true"));
        var findings = output.lines().filter(line -> line.contains("PARAMETER_BECAME_REQUIRED")).toList();
        assertEquals(1, findings.size());
        assertTrue(findings.getFirst().startsWith("Error 1 - Lines 9 |"), output);
        assertTrue(findings.getFirst().contains(directory.resolve("new contract.yaml") + ":9:"), output);
        assertTrue(findings.getFirst().contains("required: true"), output);
        assertTrue(findings.getFirst().contains("becomes required"), output);
        assertFalse(output.contains("Anterior:"));
    }
    @Test void removedMethodPointsToExistingCandidateContainerAndLabelsApproximation() throws Exception {
        String output = run(CONTRACT, CONTRACT.replace("get:", "post:"));
        assertTrue(output.contains(directory.resolve("new contract.yaml") + ":4:"), output);
        assertTrue(output.contains("container"), output);
        assertTrue(output.contains("/pets:"), output);
    }
    @Test void missingInputReportsActualPathAndActionOnOneLine() throws Exception {
        var out = new ByteArrayOutputStream();
        Path missing = directory.resolve("missing.yaml");
        int code = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer())).execute(
                new String[]{"--format=text", missing.toString(), "new.yaml"}, new PrintStream(out), System.err);
        assertEquals(2, code);
        String line = out.toString().lines().filter(value -> value.contains("INPUT_READ_FAILED")).findFirst().orElseThrow();
        assertTrue(line.contains(missing.toString()));
        assertTrue(line.contains("Check"));
        assertTrue(line.startsWith("Error 1 - Lines unavailable |"), line);
    }
    @Test void jsonPointersHandleEscapedKeysArraysAndCrLf() {
        String json = "{\r\n  \"paths\": {\r\n    \"/a~b\": {\r\n      \"parameters\": [\r\n        {\"name\": \"id\"}\r\n      ]\r\n    }\r\n  }\r\n}";
        var source = new CandidateSource(directory.resolve("new.json"), json);
        assertTrue(source.describe("/paths/~1a~0b/parameters/0/name").contains(":5:"));
        assertTrue(source.describe("/paths/~1a~0b/parameters/0/name").contains("name"));
    }
    @Test void referencesNavigateToCandidateDefinitionAndUsage() throws Exception {
        String referenced = """
                openapi: 3.0.3
                info: {title: Test, version: '1'}
                paths:
                  /pets:
                    post:
                      requestBody:
                        content:
                          application/json:
                            schema: {$ref: '#/components/schemas/Limit'}
                      responses: {'200': {description: OK}}
                components:
                  schemas:
                    Limit:
                      type: integer
                """;
        String output = run(referenced, referenced.replace("type: integer", "type: string"));
        assertTrue(output.contains("REQUEST_SCHEMA_VALUES_RESTRICTED"), output);
        String finding = output.lines().filter(line -> line.contains("REQUEST_SCHEMA_VALUES_RESTRICTED")).findFirst().orElseThrow();
        assertTrue(finding.contains(":13:"), finding);
        assertTrue(finding.contains("L14: type: string"), finding);
        assertTrue(finding.contains("Candidate usage:"), finding);
        assertTrue(finding.startsWith("Error 1 - Lines 9, 13, 14 |"), finding);
        assertFalse(finding.contains("old.yaml"), finding);
    }
    @Test void removedEndpointUsesPathsContainer() throws Exception {
        String output = run(CONTRACT, "openapi: 3.0.3\ninfo: {title: Test, version: '1'}\npaths: {}\n");
        assertTrue(output.contains("ENDPOINT_REMOVED"));
        assertTrue(output.contains(directory.resolve("new contract.yaml") + ":3:"), output);
        assertTrue(output.contains("container"));
    }
    @Test void invalidOrAliasDocumentsNeverPublishPartialCoordinates() {
        for (String invalid : new String[]{"paths: [", "a: &a {name: x}\nb: *a\n", "a: 1\na: 2\n", "a: 1\n---\na: 2"}) {
            var source = new CandidateSource(directory.resolve("invalid.yaml"), invalid);
            assertTrue(source.describe("/a").contains("line unavailable"), invalid);
        }
    }
    @Test void compatibleContractsHaveNoFindingsAndControlsCannotSplitLines() throws Exception {
        String output = run(CONTRACT, CONTRACT);
        assertFalse(output.lines().anyMatch(line -> line.startsWith("Error ")));
        assertEquals("a b c d", CandidateSource.single("a\nb\rc\u001bd"));
    }
    @Test void missingCandidateIdentifiesCandidateInsteadOfBaseline() throws Exception {
        Path old = directory.resolve("old.yaml"), missing = directory.resolve("missing new.yaml");
        Files.writeString(old, CONTRACT);
        var out = new ByteArrayOutputStream();
        int code = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer())).execute(
                new String[]{"--format=text", old.toString(), missing.toString()},
                new PrintStream(out, true, StandardCharsets.UTF_8), System.err);
        assertEquals(2, code);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains(missing.toString()));
    }
    @Test void eachOfSeveralChangesOccupiesExactlyOneLine() throws Exception {
        String output = run(CONTRACT + "  /other:\n    get:\n      responses: {'200': {description: OK}}\n",
                CONTRACT.replace("required: false", "required: true"));
        var errors = output.lines().filter(line -> line.startsWith("Error ")).toList();
        assertEquals(2, errors.size(), output);
        assertTrue(errors.get(0).startsWith("Error 1 - Lines "), output);
        assertTrue(errors.get(1).startsWith("Error 2 - Lines "), output);
        assertFalse(output.lines().anyMatch(line -> line.startsWith("  ")), output);
    }

    @Test void baselineDiagnosticsNeverClaimCandidateLinesAndShareNumbering() throws Exception {
        var report = ComparisonReport.of(ComparisonProfile.RESPONSE_POLICY, List.of(), List.of(
                new ComparisonReport.Diagnostic("FIRST", "baseline", "/paths", "Old input"),
                new ComparisonReport.Diagnostic("SECOND", "candidate", "/paths", "New input")), true);
        String output = new ReportRenderer().render(report, "text", new CandidateSource(directory.resolve("new.yaml"), CONTRACT));
        assertTrue(output.contains("Error 1 - Lines unavailable | Review: FIRST"), output);
        assertTrue(output.contains("Error 2 - Lines 3"), output);
    }

    @Test void ciFormatPreservesReportAndProvidesStructuredCandidateCoordinates() throws Exception {
        var report = new CompareContracts(new SwaggerContractAnalyzer(ComparisonProfile.RESPONSE_POLICY))
                .compare(CONTRACT, CONTRACT.replace("required: false", "required: true"));
        var renderer = new ReportRenderer();
        var source = new CandidateSource(directory.resolve("new.yaml"), CONTRACT.replace("required: false", "required: true"));
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var ci = json.readTree(renderer.render(report, "ci", source));
        assertEquals(json.readTree(renderer.render(report, "json")), ci.path("report"));
        assertEquals(1, ci.path("formatVersion").asInt());
        var finding = ci.path("findings").get(0);
        assertEquals("PARAMETER_BECAME_REQUIRED", finding.path("ruleId").asText());
        assertEquals("candidate", finding.path("source").asText());
        assertEquals(ci.path("report").path("changes").get(0).path("location"), finding.path("location"));
        assertEquals(9, finding.path("positions").get(0).path("line").asInt());
        assertFalse(finding.path("positions").get(0).path("approximate").asBoolean());
        assertEquals("ci", CliOptions.parse(new String[]{"--format=ci", "old.yaml", "new.yaml"}).format());
    }

    @Test void ciRemovedElementOnlyPointsToExistingContainer() {
        var source = new CandidateSource(directory.resolve("new.yaml"), CONTRACT.replace("get:", "post:"));
        var points = source.coordinates("/paths/~1pets/get", "definition");
        assertEquals(1, points.size());
        assertEquals(4, points.getFirst().get("line"));
        assertEquals(true, points.getFirst().get("approximate"));
    }

    @Test void ciBaselineAndUnreadableInputsHaveNoCandidateCoordinates() throws Exception {
        var report = ComparisonReport.of(ComparisonProfile.RESPONSE_POLICY, List.of(), List.of(
                new ComparisonReport.Diagnostic("BAD_INPUT", "baseline", "/paths", "Old input")), true);
        var renderer = new ReportRenderer();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var envelope = json.readTree(renderer.render(report, "ci", new CandidateSource(directory.resolve("new.yaml"), CONTRACT)));
        assertTrue(envelope.path("findings").get(0).path("positions").isEmpty());
        assertTrue(new CandidateSource(directory.resolve("missing.yaml"), null).coordinates("/paths", "diagnostic").isEmpty());
    }
}
