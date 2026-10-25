package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Runs the distributed artifact, not the test classpath or a mocked parser. */
/** Pruebas del JAR real en JVM separada; se ejecutan en verify tras empaquetar. */
class PackagedCliIT {
    @Test void packagedCiReportPreservesSemanticReportAndOriginalCandidatePositions() throws Exception {
        Path old = write("old.yaml", fixture("parameters-baseline.yaml"));
        Path candidate = write("new.yaml", fixture("parameters-required.yaml"));
        var result = run("--profile=full", "--format=ci", old.toString(), candidate.toString());
        assertEquals(1, result.code());
        var envelope = json.readTree(result.stdout());
        assertEquals(1, envelope.path("formatVersion").asInt());
        assertEquals(3, envelope.path("findings").size());
        assertEquals(json.readTree(run("--profile=full", old.toString(), candidate.toString()).stdout()), envelope.path("report"));
        assertEquals(6, envelope.path("findings").get(0).path("positions").get(0).path("line").asInt());
        assertFalse(result.stdout().contains(candidate.toString()));
    }
    @ParameterizedTest @ValueSource(strings = {"3.0.3", "3.1.1"})
    void packagedJarAppliesResponsePolicyAndPreservesReview(String version) throws Exception {
        Path old = write("old.yaml", fixture("responses-old.yaml").replace("3.0.3", version));
        Path updated = write("new.yaml", fixture("responses-new.yaml").replace("3.0.3", version));
        var result = run("--profile=full", old.toString(), updated.toString());
        assertEquals(1, result.code());
        var report = json.readTree(result.stdout());
        assertEquals("operations-data-response-policy-v1", report.path("profileId").asText());
        assertEquals("COMPLETED", report.path("status").asText());
        assertEquals(2, report.path("changes").size());
        assertEquals(result.stdout(), run("--profile=full", old.toString(), updated.toString()).stdout());
        assertEquals(0, run("--profile=full", old.toString(), old.toString()).code());
        assertEquals(3, run("--profile=full", updated.toString(), old.toString()).code());
        var text = run("--profile=full", "--format=text", old.toString(), updated.toString());
        assertTrue(text.stdout().contains("RESPONSE_STATUS_REMOVED"));
        assertTrue(text.stdout().contains("profile availability policy"));
        assertEquals(2, text.stdout().lines().filter(line -> line.matches("Error [12] - Lines .*\\[ERROR].*")).count());
        assertTrue(text.stdout().contains(updated.toAbsolutePath().normalize() + ":"));
        assertFalse(text.stdout().contains("old.yaml:"));
        assertEquals(1, text.code());
    }

    @ParameterizedTest @ValueSource(strings = {"3.0.3", "3.1.1"})
    void packagedJarComparesArraysByDirection(String version) throws Exception {
        Path old = write("old.yaml", fixture("arrays-old.yaml").replace("3.0.3", version));
        Path updated = write("new.yaml", fixture("arrays-new.yaml").replace("3.0.3", version));
        var result = run("--profile=arrays", old.toString(), updated.toString());
        assertEquals(1, result.code());
        var report = json.readTree(result.stdout());
        assertEquals("operations-parameters-array-schemas-v1", report.path("profileId").asText());
        assertEquals("COMPLETED", report.path("status").asText());
        assertEquals(2, report.path("changes").size());
        assertTrue(report.path("changes").get(0).path("location").path("candidatePointer").asText().endsWith("/items"));
        assertEquals(result.stdout(), run("--profile=arrays", old.toString(), updated.toString()).stdout());
        assertEquals(0, run("--profile=arrays", updated.toString(), old.toString()).code());
        var text = run("--profile=arrays", "--format=text", old.toString(), updated.toString());
        assertTrue(text.stdout().contains("Array items"));
        assertEquals(1, text.code());
    }

    @TempDir Path directory;
    private final ObjectMapper json = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.1"})
    void packagedJarComparesObjectsAndPreservesDirectionAndCoverage(String version) throws Exception {
        Path old = write("old.yaml", fixture("objects-old.yaml").replace("3.0.3", version));
        Path updated = write("new.yaml", fixture("objects-new.yaml").replace("3.0.3", version));
        var result = run("--profile=schemas", old.toString(), updated.toString());
        assertEquals(1, result.code());
        var report = json.readTree(result.stdout());
        assertEquals("operations-parameters-object-schemas-v1", report.path("profileId").asText());
        assertEquals("COMPLETED", report.path("status").asText());
        assertEquals(2, report.path("changes").size());
        assertTrue(report.path("changes").get(0).path("location").path("candidatePointer").asText().endsWith("/required"));
        assertTrue(report.path("changes").get(1).path("location").path("baselinePointer").asText().endsWith("/required"));
        assertEquals(result.stdout(), run("--profile=schemas", old.toString(), updated.toString()).stdout());
        assertEquals(0, run("--profile=schemas", updated.toString(), old.toString()).code());
        var text = run("--profile=schemas", "--format=text", old.toString(), updated.toString());
        assertEquals(1, text.code());
        assertTrue(text.stdout().contains("name"));
        assertTrue(text.stdout().contains("REQUEST"));
        assertTrue(text.stdout().contains("RESPONSE"));
        Files.writeString(updated, fixture("objects-new.yaml").replace("3.0.3", version).replace("type: string", "type: string, pattern: '^a'"));
        var partial = json.readTree(run("--profile=schemas", old.toString(), updated.toString()).stdout());
        assertEquals("PARTIAL", partial.path("status").asText());
        assertEquals("BREAKING", partial.path("verdict").asText());
    }

    @Test
    void packagedJarAnalyzesRequestAndResponseScalarValues() throws Exception {
        Path old = write("old.yaml", fixture("schema-values-old.yaml"));
        Path updated = write("new.yaml", fixture("schema-values-new.yaml"));
        var result = run("--profile=schema-values", old.toString(), updated.toString());
        assertEquals(1, result.code());
        var report = json.readTree(result.stdout());
        assertEquals("COMPLETED", report.path("status").asText());
        assertEquals(2, report.path("changes").size());
        assertEquals("REQUEST", report.path("changes").get(0).path("location").path("direction").asText());
        assertEquals("RESPONSE", report.path("changes").get(1).path("location").path("direction").asText());
        assertEquals("200", report.path("changes").get(1).path("location").path("responseCode").asText());
        assertEquals("application/json", report.path("changes").get(1).path("location").path("mediaType").asText());
        assertEquals(0, run("--profile=schema-values", updated.toString(), old.toString()).code());
        var text = run("--profile=schema-values", "--format=text", old.toString(), updated.toString());
        assertEquals(1, text.code());
        assertTrue(text.stdout().contains("Response: 200"));
    }

    @Test
    void packagedJarComparesRequiredBodiesAndRemovedMediaTypes() throws Exception {
        Path old = write("anterior.yaml", fixture("body-optional.yaml"));
        Path updated = write("nuevo.yaml", fixture("body-required.yaml"));
        var result = run("--profile=request-body", old.toString(), updated.toString());
        assertEquals(1, result.code());
        var report = json.readTree(result.stdout());
        assertEquals("operations-parameters-request-body-v1", report.path("profileId").asText());
        assertEquals("COMPLETED", report.path("status").asText());
        assertEquals(2, report.path("changes").size());
        assertEquals("REQUEST_BODY_BECAME_REQUIRED", report.path("changes").get(0).path("ruleId").asText());
        assertEquals("REQUEST_MEDIA_TYPE_REMOVED", report.path("changes").get(1).path("ruleId").asText());
        assertEquals(result.stdout(), run("--profile=request-body", old.toString(), updated.toString()).stdout());
        var text = run("--format=text", "--profile=request-body", old.toString(), updated.toString());
        assertEquals(1, text.code());
        assertTrue(text.stdout().contains("text/plain"));
        assertTrue(text.stdout().contains("body"));
        assertEquals(0, run("--profile=request-body", updated.toString(), old.toString()).code());
    }

    @Test
    void packagedJarSupportsHumanOutputAndHelp() throws Exception {
        Path baseline = write("anterior.yaml", fixture("baseline.yaml"));
        Path candidate = write("nuevo.yaml", fixture("removed.yaml"));
        var result = run("--format=text", baseline.toString(), candidate.toString());
        assertEquals(1, result.code());
        assertTrue(result.stdout().contains("POST /pets"));
        assertTrue(result.stdout().contains("The HTTP method was removed:"));
        assertTrue(result.stdout().contains("Excluded facets:"));
        var help = run("--help");
        assertEquals(0, help.code());
        assertTrue(help.stdout().contains("--format=text"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.1"})
    void packagedJarDetectsInheritedAndReferencedParameterBreaks(String version) throws Exception {
        Path baseline = write("parámetros anteriores.yaml", fixture("parameters-baseline.yaml").replace("3.0.3", version));
        Path candidate = write("parámetros nuevos.yaml", fixture("parameters-required.yaml").replace("3.0.3", version));
        var first = run(baseline.toString(), candidate.toString());
        var breaking = report(first, 1, "COMPLETED", "BREAKING");
        assertEquals(3, breaking.path("changes").size());
        var inherited = breaking.path("changes").get(0);
        assertEquals("PARAMETER_BECAME_REQUIRED", inherited.path("ruleId").asText());
        assertEquals("/paths/~1pets/parameters/0", inherited.path("location").path("candidatePointer").asText());
        var referenced = breaking.path("changes").get(1);
        assertEquals("REQUIRED_PARAMETER_ADDED", referenced.path("ruleId").asText());
        assertTrue(referenced.path("location").path("baselinePointer").isNull());
        assertEquals("x-trace", referenced.path("location").path("parameter").path("name").asText());
        assertEquals("header", referenced.path("location").path("parameter").path("in").asText());
        assertEquals("/components/parameters/Trace", referenced.path("location").path("candidatePointer").asText());
        assertEquals("/paths/~1pets/get/parameters/0", referenced.path("location").path("candidateUsagePointer").asText());
        assertEquals("POST", breaking.path("changes").get(2).path("location").path("method").asText());
        assertEquals(first.stdout(), run(baseline.toString(), candidate.toString()).stdout());
    }

    @Test
    void packagedJarHandlesOptionalAdditionsAndRelaxationWithoutFalseBreaks() throws Exception {
        String old = fixture("parameters-baseline.yaml");
        String required = fixture("parameters-required.yaml");
        Path baseline = write("baseline.yaml", old);
        Path candidate = write("candidate.yaml", required.replace("required: true", "required: false"));
        assertTrue(report(run(baseline.toString(), candidate.toString()), 0, "COMPLETED", "COMPATIBLE")
                .path("changes").isEmpty());
        Files.writeString(baseline, required);
        assertTrue(report(run(baseline.toString(), candidate.toString()), 0, "COMPLETED", "COMPATIBLE")
                .path("changes").isEmpty());
    }

    @Test
    void packagedJarShowsRemovalAndUnsupportedPresenceAsReview() throws Exception {
        Path baseline = write("baseline.yaml", fixture("parameters-required.yaml"));
        Path candidate = write("candidate.yaml", fixture("parameters-baseline.yaml"));
        var removed = report(run(baseline.toString(), candidate.toString()), 3, "PARTIAL", "INDETERMINATE");
        assertEquals("PARAMETER_REMOVAL_REQUIRES_REVIEW", removed.path("diagnostics").get(0).path("code").asText());
        Files.writeString(baseline, fixture("parameters-baseline.yaml"));
        Files.writeString(candidate, fixture("parameters-baseline.yaml").replace("{type: integer}", "{type: object}"));
        var unsupported = report(run(baseline.toString(), candidate.toString()), 3, "PARTIAL", "INDETERMINATE");
        assertEquals("PARAMETER_PRESENCE_UNSUPPORTED", unsupported.path("diagnostics").get(0).path("code").asText());
    }

    @Test
    void invalidParameterReferenceFailsWithoutInventingRemovedOperations() throws Exception {
        Path baseline = write("baseline.yaml", fixture("parameters-baseline.yaml"));
        Path candidate = write("candidate.yaml", fixture("parameters-required.yaml").replace(
                "name: X-Trace, in: header, required: true, schema: {type: string}",
                "$ref: '#/components/parameters/Trace'"));
        var failed = report(run(baseline.toString(), candidate.toString()), 2, "FAILED", "INDETERMINATE");
        assertTrue(failed.path("changes").isEmpty());
        assertEquals("candidate", failed.path("diagnostics").get(0).path("source").asText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.1"})
    void detectsRemovalsWithStableIdentityAndOriginalPointers(String version) throws Exception {
        Path baseline = write("versión anterior.yaml", fixture("baseline.yaml").replace("3.0.3", version));
        Path candidate = write("versión nueva.yaml", fixture("removed.yaml").replace("3.0.3", version));
        var result = run(baseline.toString(), candidate.toString());
        var report = report(result, 1, "COMPLETED", "BREAKING");
        assertEquals(2, report.path("changes").size());
        var endpoint = report.path("changes").get(0);
        assertEquals("ENDPOINT_REMOVED", endpoint.path("ruleId").asText());
        assertEquals("/health", endpoint.path("location").path("path").asText());
        assertEquals("/paths/~1health", endpoint.path("location").path("baselinePointer").asText());
        assertTrue(endpoint.path("location").path("method").isNull());
        var method = report.path("changes").get(1);
        assertEquals("HTTP_METHOD_REMOVED", method.path("ruleId").asText());
        assertEquals("POST", method.path("location").path("method").asText());
        assertEquals("/paths/~1pets/post", method.path("location").path("baselinePointer").asText());
        assertTrue(method.path("description").asText().contains("method"));
        for (var change : report.path("changes")) {
            assertEquals("ERROR", change.path("severity").asText());
            assertTrue(change.path("id").asText().matches("[0-9a-f]{64}"));
            assertTrue(change.path("location").path("candidatePointer").isNull());
        }
        assertEquals(result.stdout(), run(baseline.toString(), candidate.toString()).stdout(),
                "Independent JVM runs must produce the same ordered report and IDs");
    }

    @Test
    void equivalentYamlAndJsonAndAdditionsAreCompatibleOnlyWithinTheProfile() throws Exception {
        Path baseline = write("baseline.yaml", fixture("baseline.yaml"));
        Path equivalent = write("equivalent.json", fixture("equivalent.json"));
        var same = report(run(baseline.toString(), equivalent.toString()), 0, "COMPLETED", "COMPATIBLE");
        assertTrue(same.path("changes").isEmpty());
        assertTrue(same.path("diagnostics").isEmpty());
        Path smaller = write("smaller.yaml", fixture("removed.yaml"));
        assertTrue(report(run(smaller.toString(), baseline.toString()), 0,
                "COMPLETED", "COMPATIBLE").path("changes").isEmpty());
    }

    @Test
    void renamedTemplateIsPartialAndAProvenRemovalStillBlocks() throws Exception {
        Path baseline = write("baseline.yaml", template("id"));
        Path candidate = write("candidate.yaml", template("petId"));
        var partial = report(run(baseline.toString(), candidate.toString()), 3, "PARTIAL", "INDETERMINATE");
        assertTrue(partial.path("changes").isEmpty());
        assertEquals("PATH_MATCH_REQUIRES_REVIEW", partial.path("diagnostics").get(0).path("code").asText());
        Files.writeString(baseline, template("id") + "  /health:\n    get:\n      responses:\n        '200': {description: OK}\n");
        var breaking = report(run(baseline.toString(), candidate.toString()), 1, "PARTIAL", "BREAKING");
        assertEquals(1, breaking.path("changes").size());
        assertEquals("/health", breaking.path("changes").get(0).path("location").path("path").asText());
        assertEquals(1, breaking.path("diagnostics").size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"baseline", "candidate"})
    void malformedContractCannotProduceSpeculativeRemovals(String source) throws Exception {
        Path baseline = write("baseline.yaml", fixture("baseline.yaml"));
        Path candidate = write("candidate.yaml", fixture("removed.yaml"));
        Files.writeString(source.equals("baseline") ? baseline : candidate, "openapi: [broken");
        var failed = report(run(baseline.toString(), candidate.toString()), 2, "FAILED", "INDETERMINATE");
        assertTrue(failed.path("changes").isEmpty());
        assertEquals(source, failed.path("diagnostics").get(0).path("source").asText());
    }

    @Test
    void missingFilesAndInvalidUtf8ProduceReadDiagnostics() throws Exception {
        Path baseline = write("baseline.yaml", fixture("baseline.yaml"));
        Path candidate = directory.resolve("missing.yaml");
        var missing = report(run(baseline.toString(), candidate.toString()), 2, "FAILED", "INDETERMINATE");
        assertEquals("INPUT_READ_FAILED", missing.path("diagnostics").get(0).path("code").asText());
        Files.write(candidate, new byte[]{(byte) 0xc3, 0x28});
        var invalid = report(run(baseline.toString(), candidate.toString()), 2, "FAILED", "INDETERMINATE");
        assertTrue(invalid.path("changes").isEmpty());
        assertEquals("INPUT_READ_FAILED", invalid.path("diagnostics").get(0).path("code").asText());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void wrongNumberOfArgumentsUsesStderrAndReturnsUsageError(int count) throws Exception {
        var result = run(java.util.Collections.nCopies(count, "unused.yaml").toArray(String[]::new));
        assertEquals(2, result.code(), result.stderr());
        assertTrue(result.stdout().isBlank(), "Usage errors must not masquerade as a JSON report");
        assertTrue(result.stderr().contains("Usage:"));
    }

    @Test
    void oversizedFileIsRejectedAtTheCliBoundary() throws Exception {
        Path baseline = write("baseline.yaml", fixture("baseline.yaml"));
        Path candidate = write("oversized.yaml", " ".repeat(5 * 1024 * 1024 + 1));
        var failed = report(run(baseline.toString(), candidate.toString()), 2, "FAILED", "INDETERMINATE");
        assertTrue(failed.path("changes").isEmpty());
        assertEquals("INPUT_READ_FAILED", failed.path("diagnostics").get(0).path("code").asText());
    }

    private JsonNode report(Result result, int code, String status, String verdict) throws Exception {
        assertEquals(code, result.code(), result.stderr());
        JsonNode report = json.readTree(result.stdout());
        assertEquals("operations-parameter-presence-v1", report.path("profileId").asText());
        assertEquals(status, report.path("status").asText());
        assertEquals(verdict, report.path("verdict").asText());
        assertTrue(report.path("excludedFacets").toString().contains("requestSchemas"));
        assertTrue(report.path("excludedFacets").toString().contains("parameterSchemas"));
        assertTrue(report.path("excludedFacets").toString().contains("parameterSerialization"));
        assertTrue(report.path("changes").isArray());
        assertTrue(report.path("diagnostics").isArray());
        return report;
    }

    private Result run(String... args) throws Exception {
        String configuredJar = System.getProperty("guard.executableJar");
        assertNotNull(configuredJar, "Run mvnd verify to build and test the executable JAR");
        Path jar = Path.of(configuredJar).toAbsolutePath();
        assertTrue(Files.isRegularFile(jar), "The packaged JAR must exist: " + jar);
        Path output = Files.createTempFile(directory, "stdout-", ".json");
        Path errors = Files.createTempFile(directory, "stderr-", ".log");
        Path executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        var command = new ArrayList<>(List.of(executable.toString(), "-Dstdout.encoding=windows-1252",
                "-Dstderr.encoding=windows-1252", "-jar", jar.toString()));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).directory(directory.toFile())
                .redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Packaged CLI must terminate within 20 seconds");
            return new Result(process.exitValue(), Files.readString(output),
                    Files.readString(errors, Charset.forName("windows-1252")));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    private Path write(String name, String content) throws Exception {
        Path path = directory.resolve(name);
        Files.writeString(path, content);
        return path;
    }

    private static String template(String name) {
        return """
                openapi: 3.0.3
                info: {title: Pets, version: '1'}
                paths:
                  /pets/{%s}:
                    parameters:
                      - {in: path, name: '%s', required: true, schema: {type: string}}
                    get:
                      responses:
                        '200': {description: OK}
                """.formatted(name, name);
    }

    private record Result(int code, String stdout, String stderr) {}
}
