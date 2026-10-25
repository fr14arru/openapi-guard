package com.openapiguard.adapter.cli;

import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.application.CompareContracts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica las mejoras de uso con contratos reales sin arrancar Spring. */
class UsabilityTest {
    @TempDir Path directory;
    private record Execution(int code, String output, String errors) {}
    private Execution execute(String... args) {
        var out = new ByteArrayOutputStream(); var err = new ByteArrayOutputStream();
        int code = new ComparisonCommand(new CompareContracts(new SwaggerContractAnalyzer())).execute(args,
                new PrintStream(out, true, StandardCharsets.UTF_8), new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Execution(code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
    }
    private Path contract(String name, String fixture) throws IOException {
        var path = directory.resolve(name); Files.writeString(path, fixture(fixture)); return path;
    }
    @Test void namedContractsWorkInEitherOptionOrder() throws Exception {
        var old = contract("old with spaces.yaml", "baseline.yaml"); var next = contract("new.yaml", "removed.yaml");
        var result = execute("--candidate=" + next, "--baseline=" + old);
        assertEquals(1, result.code()); assertTrue(result.output().contains("BREAKING"));
        assertEquals(2, execute("--baseline=" + old, next.toString()).code());
        assertEquals(2, execute("--baseline=" + old, "--baseline=" + old, "--candidate=" + next).code());
    }
    @Test void markdownPreservesRulesDiagnosticsAndCoverage() throws Exception {
        var old = contract("old.yaml", "baseline.yaml"); var next = contract("new.yaml", "removed.yaml");
        var result = execute("--format=markdown", old.toString(), next.toString());
        assertEquals(1, result.code()); assertTrue(result.output().startsWith("# OpenAPI"));
        assertTrue(result.output().contains("BREAKING")); assertTrue(result.output().contains("REMOVED"));
        assertTrue(result.output().contains("Excluded facets"));
        assertEquals(2, execute("--format=markdown", "missing", "missing").code());
        assertTrue(execute("--format=markdown", "missing", "missing").output().contains("INPUT&#95;READ&#95;FAILED"));
    }
    @Test void outputIsUtf8AndMatchesStdoutWithoutChangingExitCode() throws Exception {
        var old = contract("old.yaml", "baseline.yaml"); var next = contract("new.yaml", "removed.yaml");
        var report = directory.resolve("reports with spaces/report.md");
        var result = execute("--format=markdown", "--output=" + report, old.toString(), next.toString());
        assertEquals(1, result.code()); assertEquals(result.output(), Files.readString(report));
        assertFalse(Files.readString(report).startsWith("\ufeff"));
        Files.writeString(next, fixture("equivalent.json"));
        assertEquals(0, execute("--output=" + report, old.toString(), next.toString()).code());
        assertTrue(Files.readString(report).startsWith("{"));
    }
    @Test void reportNeverOverwritesInputsAndWriteErrorsBlock() throws Exception {
        var old = contract("old.yaml", "baseline.yaml"); var next = contract("new.yaml", "removed.yaml");
        var original = Files.readString(old);
        assertEquals(2, execute("--output=" + old, old.toString(), next.toString()).code());
        assertEquals(original, Files.readString(old));
        var folder = Files.createDirectory(directory.resolve("folder"));
        assertEquals(2, execute("--output=" + folder, old.toString(), next.toString()).code());
    }
    @Test void helpExplainsNewFeaturesAndProfilesCanBeDiscovered() {
        var help = execute("--help"); assertEquals(0, help.code());
        assertTrue(help.output().contains("--baseline=")); assertTrue(help.output().contains("--output="));
        var profiles = execute("--list-profiles"); assertEquals(0, profiles.code());
        assertTrue(profiles.output().contains("operations-data-response-policy-v1"));
        assertTrue(profiles.output().contains("parameters"));
    }
    @Test void markdownEscapesContractDataRatherThanEmbeddingHtmlOrLinks() throws Exception {
        var report = com.openapiguard.domain.ComparisonReport.of(java.util.List.of(), java.util.List.of(
                new com.openapiguard.domain.ComparisonReport.Diagnostic("TEST", "source", "pointer",
                        "<script>alert(1)</script> [link](https://example.invalid) | row")), false);
        var markdown = new ReportRenderer().render(report, "markdown");
        assertFalse(markdown.contains("<script>")); assertFalse(markdown.contains("[link]"));
        assertTrue(markdown.contains("&#60;script&#62;")); assertTrue(markdown.contains("&#124;"));
    }
    @Test void reportCannotOverwriteHardlinkAliasOfContract() throws Exception {
        var old = contract("old.yaml", "baseline.yaml"); var next = contract("new.yaml", "removed.yaml");
        var alias = Files.createLink(directory.resolve("alias.yaml"), old);
        var original = Files.readString(old);
        assertEquals(2, execute("--output=" + alias, old.toString(), next.toString()).code());
        assertEquals(original, Files.readString(old));
    }
}
