package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.application.CompareContracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Salida del comando en memoria y contrato de errores de argumentos. */
class ComparisonCommandTest {
    @TempDir Path directory;

    @Test
    void returnsStructuredJsonAndExitCodesForRealFilesWithSpaces() throws Exception {
        Path baseline = directory.resolve("old contract.yaml");
        Path candidate = directory.resolve("new contract.yaml");
        Files.writeString(baseline, fixture("baseline.yaml"));
        Files.writeString(candidate, fixture("removed.yaml"));
        assertEquals(1, execute(baseline, candidate));
        Files.writeString(candidate, fixture("equivalent.json"));
        assertEquals(0, execute(baseline, candidate));
        Files.writeString(candidate, "invalid: [");
        assertEquals(2, execute(baseline, candidate));
        Files.writeString(baseline, template("id"));
        Files.writeString(candidate, template("petId"));
        assertEquals(3, execute(baseline, candidate));
    }

    @Test
    void missingFilesAndWrongArgumentsAreInputErrors() throws Exception {
        assertEquals(2, execute(directory.resolve("missing"), directory.resolve("missing")));
        var command = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer()));
        assertEquals(2, command.execute(new String[0], System.out, new PrintStream(new ByteArrayOutputStream())));
    }

    private int execute(Path baseline, Path candidate) throws Exception {
        var output = new ByteArrayOutputStream();
        var errors = new ByteArrayOutputStream();
        var command = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer()));
        int code = command.execute(new String[]{baseline.toString(), candidate.toString()},
                new PrintStream(output, true, StandardCharsets.UTF_8), new PrintStream(errors));
        var json = new ObjectMapper().readTree(output.toString(StandardCharsets.UTF_8));
        assertEquals("operations-parameter-presence-v1", json.path("profileId").asText());
        assertTrue(json.path("excludedFacets").isArray());
        if (code == 1) {
            var change = json.path("changes").get(0);
            assertEquals("ERROR", change.path("severity").asText());
            assertFalse(change.path("ruleId").asText().isEmpty());
            assertFalse(change.path("location").path("path").asText().isEmpty());
            assertFalse(change.path("description").asText().isEmpty());
        }
        return code;
    }

    private static String template(String parameter) {
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
                """.formatted(parameter, parameter);
    }
}
