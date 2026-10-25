package com.openapiguard.adapter.cli;

import com.openapiguard.application.CompareContracts;
import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Presentación humana, ayuda, opciones y códigos equivalentes a salida JSON. */
class HumanOutputTest {
    @Test void selectsFullProfileAndExplainsRemovedResponseCodes() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        String document = "openapi: 3.0.3\ninfo: {title: Responses, version: '1'}\npaths:\n  /values:\n    get:\n      responses: {'200': {description: OK}, '204': {description: Empty}}\n";
        Files.writeString(old, document);
        Files.writeString(updated, document.replace(", '204': {description: Empty}", ""));
        var output = new ByteArrayOutputStream();
        assertEquals(1, command().execute(new String[]{"--profile=full", "--format=text", old.toString(), updated.toString()},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("RESPONSE_STATUS_REMOVED"));
        assertTrue(output.toString().contains("Response: 204"));
        assertTrue(output.toString().contains("profile availability policy"));
    }

    @Test void selectsArrayProfileAndExplainsItemChanges() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        String document = """
                openapi: 3.0.3
                info: {title: Arrays, version: '1'}
                paths:
                  /values:
                    post:
                      requestBody: {content: {application/json: {schema: {type: array, items: {type: number}}}}}
                      responses: {'200': {description: OK}}
                """;
        Files.writeString(old, document);
        Files.writeString(updated, document.replace("type: number", "type: integer"));
        var output = new ByteArrayOutputStream();
        assertEquals(1, command().execute(new String[]{"--profile=arrays", "--format=text", old.toString(), updated.toString()},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("Array items"));
        assertTrue(output.toString().contains("operations-parameters-array-schemas-v1"));
    }

    @TempDir Path directory;

    @Test void explainsBreakingChangesAndCoverage() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        Files.writeString(old, fixture("baseline.yaml"));
        Files.writeString(updated, fixture("removed.yaml"));
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        int code = command().execute(new String[]{"--format=text", old.toString(), updated.toString()},
                new PrintStream(output, true, StandardCharsets.UTF_8), new PrintStream(errors));
        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(1, code);
        assertTrue(text.contains("BREAKING"));
        assertTrue(text.contains("HTTP_METHOD_REMOVED"));
        assertTrue(text.contains("POST /pets"));
        assertTrue(text.contains(updated.toAbsolutePath().normalize() + ":"));
        assertTrue(text.contains("container; element absent"));
        assertTrue(text.contains("responseSchemas"));
        assertTrue(text.contains("operations-parameter-presence-v1"));
        assertEquals("", errors.toString());
    }

    @Test void helpSucceedsWithoutReadingFiles() {
        var output = new ByteArrayOutputStream();
        assertEquals(0, command().execute(new String[]{"--help"}, new PrintStream(output), System.err));
        assertTrue(output.toString().contains("--format=text"));
        assertTrue(output.toString().contains("3"));
    }

    @Test void invalidInputHasHumanDiagnosticsAndFailureCode() {
        var output = new ByteArrayOutputStream();
        assertEquals(2, command().execute(new String[]{"--format=text", "missing-old", "missing-new"},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("FAILED"));
        assertTrue(output.toString().contains("INPUT_READ_FAILED"));
    }

    @Test void selectsRequestBodyProfileExplicitly() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        String contract = """
                openapi: 3.0.3
                info: {title: Pets, version: '1'}
                paths:
                  /pets:
                    post:
                      requestBody: {required: %s, content: {application/json: {schema: {type: string}}}}
                      responses: {'200': {description: OK}}
                """;
        Files.writeString(old, contract.formatted("false"));
        Files.writeString(updated, contract.formatted("true"));
        var output = new ByteArrayOutputStream();
        assertEquals(1, command().execute(new String[]{"--profile=request-body", "--format=text", old.toString(), updated.toString()},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("REQUEST_BODY_BECAME_REQUIRED"));
        assertTrue(output.toString().contains("operations-parameters-request-body-v1"));
    }

    private ComparisonCommand command() {
        return new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer()));
    }

    @Test void rejectsUnknownOrDuplicateOptionsWithoutProducingAnAnalysis() {
        for (String[] args : java.util.List.of(new String[]{"--profile=unknown", "old", "new"},
                new String[]{"--format=text", "--format=json", "old", "new"},
                new String[]{"--profile=schemas", "--profile=operations", "old", "new"})) {
            var output = new ByteArrayOutputStream();
            var errors = new ByteArrayOutputStream();
            assertEquals(2, command().execute(args, new PrintStream(output), new PrintStream(errors)));
            assertEquals("", output.toString());
            assertTrue(errors.toString().contains("--help"));
        }
    }

    @Test void selectsTheCompleteSchemaProfileForHumansAndAutomation() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        Files.writeString(old, fixture("objects-old.yaml"));
        Files.writeString(updated, fixture("objects-new.yaml"));
        var output = new ByteArrayOutputStream();
        assertEquals(1, command().execute(new String[]{"--profile=schemas", "--format=text", old.toString(), updated.toString()},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("REQUEST_SCHEMA_VALUES_RESTRICTED"));
        assertTrue(output.toString().contains("RESPONSE_SCHEMA_VALUES_EXPANDED"));
        assertTrue(output.toString().contains("operations-parameters-object-schemas-v1"));
    }

    @Test void schemaValuesProfileWorksFromCliWithResponseDirection() throws Exception {
        Path old = directory.resolve("old.yaml");
        Path updated = directory.resolve("new.yaml");
        String document = """
                openapi: 3.0.3
                info: {title: Values, version: '1'}
                paths:
                  /values:
                    get:
                      responses:
                        '200': {description: OK, content: {application/json: {schema: {type: %s}}}}
                """;
        Files.writeString(old, document.formatted("integer"));
        Files.writeString(updated, document.formatted("number"));
        var output = new ByteArrayOutputStream();
        assertEquals(1, command().execute(new String[]{"--profile=schema-values", "--format=text", old.toString(), updated.toString()},
                new PrintStream(output), System.err));
        assertTrue(output.toString().contains("RESPONSE_SCHEMA_VALUES_EXPANDED"));
        assertTrue(output.toString().contains("200"));
        assertTrue(output.toString().contains("application/json"));
    }
}
