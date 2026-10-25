package com.openapiguard.adapter.openapi;

import com.openapiguard.application.CompareContracts;
import com.openapiguard.domain.ComparisonReport;
import com.openapiguard.domain.HttpMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static com.openapiguard.TestSpecifications.fixture;
import static org.junit.jupiter.api.Assertions.*;

/** Fronteras de tamaño/profundidad y rechazo de referencias sin E/S externa. */
class ContractBoundaryIntegrationTest {
    private final CompareContracts comparison = new CompareContracts(new SwaggerContractAnalyzer());

    @ParameterizedTest
    @EnumSource(HttpMethod.class)
    void everyHttpMethodSurvivesParsingAndIsDetectedWhenRemoved(HttpMethod method) {
        String baseline = """
                openapi: 3.0.3
                info: {title: Methods, version: '1'}
                paths:
                  /pets:
                    %s:
                      responses:
                        '200': {description: OK}
                """.formatted(method.name().toLowerCase(Locale.ROOT));
        String candidate = """
                openapi: 3.0.3
                info: {title: Methods, version: '1'}
                paths:
                  /pets: {}
                """;
        var report = comparison.compare(baseline, candidate);
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(1, report.changes().size());
        assertEquals("HTTP_METHOD_REMOVED", report.changes().getFirst().ruleId());
        assertEquals(method, report.changes().getFirst().location().method());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, comparison.compare(baseline, baseline).verdict());
    }

    @Test
    void emptyPathsAndMetadataOnlyPathsHaveNoOperationsToRemove() {
        String empty = "openapi: 3.0.3\ninfo: {title: Empty, version: '1'}\npaths: {}\n";
        String metadata = empty.replace("paths: {}", "paths:\n  /metadata:\n    summary: Documentation only");
        var report = comparison.compare(metadata, empty);
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, report.verdict());
        assertTrue(report.changes().isEmpty());
    }

    @Test
    void acceptsTheDocumentByteLimitAndRejectsOneByteMore() throws Exception {
        String base = fixture("removed.yaml");
        int remaining = SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES
                - base.getBytes(StandardCharsets.UTF_8).length - 2;
        String atLimit = base + "\n#" + "a".repeat(remaining);
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, comparison.compare(base, atLimit).verdict());
        assertFailed(comparison.compare(base, atLimit + "a"));
    }

    @Test
    void sizeLimitCountsUtf8BytesRatherThanCharacters() throws Exception {
        String base = fixture("removed.yaml");
        String candidate = base + "\nx-note: '" + "é".repeat(SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES / 2) + "'\n";
        assertTrue(candidate.length() < SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES);
        assertFailed(comparison.compare(base, candidate));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yaml", "json"})
    void excessiveNestingIsRejectedBeforeComparison(String format) throws Exception {
        String nested = "[".repeat(129) + "0" + "]".repeat(129);
        String candidate = format.equals("yaml") ? fixture("removed.yaml") + "\nx-deep: " + nested
                : "{\"openapi\":\"3.0.3\",\"info\":{\"title\":\"Deep\",\"version\":\"1\"},\"paths\":{},\"x-deep\":" + nested + "}";
        assertFailed(comparison.compare(fixture("baseline.yaml"), candidate));
    }

    @Test
    void externalHttpReferenceIsRejectedWithoutOpeningAConnection() throws Exception {
        // A listening local socket makes a forbidden fetch observable, without an external service.
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(250);
            String base = fixture("baseline.yaml");
            String candidate = base + "\ncomponents:\n  schemas:\n    Pet:\n      $ref: 'http://127.0.0.1:"
                    + listener.getLocalPort() + "/schema.yaml'\n";
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
                var report = comparison.compare(base, candidate);
                assertFailed(report);
                assertTrue(report.diagnostics().getFirst().description().contains("External or unsupported reference"));
            });
            assertThrows(SocketTimeoutException.class, () -> {
                try (var connection = listener.accept()) {
                    fail("The parser attempted to access an external reference");
                }
            });
        }
    }

    @Test
    void recursiveInternalSchemasRemainExcludedWithoutExpandingThem() throws Exception {
        String base = fixture("removed.yaml");
        String recursive = base + """

                components:
                  schemas:
                    Node:
                      type: object
                      properties:
                        child:
                          $ref: '#/components/schemas/Node'
                """;
        var report = comparison.compare(base, recursive);
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, report.verdict());
        assertTrue(report.excludedFacets().contains("responseSchemas"));
    }

    private static void assertFailed(ComparisonReport report) {
        assertEquals(ComparisonReport.Status.FAILED, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertFalse(report.diagnostics().isEmpty());
    }

    @Test
    void expandedProfileRejectsActiveExternalReferencesWithoutNetworkAccess() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            listener.setSoTimeout(250);
            String document = ObjectSchemaComparisonTest.contract("REQUEST", "{$ref: 'http://127.0.0.1:" + listener.getLocalPort() + "/schema'}");
            var analyzer = new SwaggerContractAnalyzer(com.openapiguard.domain.ComparisonProfile.OBJECT_SCHEMAS);
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> assertFailed(analyzer.analyze(document, document)));
            assertThrows(SocketTimeoutException.class, () -> { try (var connection = listener.accept()) { fail("Forbidden network request"); } });
        }
    }
}
