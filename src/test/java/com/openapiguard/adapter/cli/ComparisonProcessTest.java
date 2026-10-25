package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Arranque en proceso separado, cierre y código de ruptura con JSON limpio. */
class ComparisonProcessTest {
    @TempDir Path directory;

    @Test
    void bootEntryPointClosesAndExitsWithBreakingCodeAndCleanJson() throws Exception {
        Path baseline = directory.resolve("baseline.yaml");
        Path candidate = directory.resolve("candidate.yaml");
        Files.writeString(baseline, fixture("baseline.yaml"));
        Files.writeString(candidate, fixture("removed.yaml"));
        Path output = directory.resolve("stdout.json");
        Path errors = directory.resolve("stderr.log");
        String executable = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java").toString();
        Process process = new ProcessBuilder(executable,
                "-Dstdout.encoding=windows-1252", "-Dstderr.encoding=windows-1252",
                "-cp", System.getProperty("java.class.path"),
                OpenApiGuardApplication.class.getName(), baseline.toString(), candidate.toString())
                .redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
        try {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "CLI must terminate");
            // JVM warnings can precede main and use the native stderr encoding.
            assertEquals(1, process.exitValue(), Files.readString(errors, Charset.forName("windows-1252")));
            var json = new ObjectMapper().readTree(Files.readString(output));
            assertEquals("BREAKING", json.path("verdict").asText());
            assertEquals(2, json.path("changes").size());
            assertTrue(json.path("changes").get(1).path("description").asText().contains("method"));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}
