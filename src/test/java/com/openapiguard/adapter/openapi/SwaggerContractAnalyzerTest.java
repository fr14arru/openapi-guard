package com.openapiguard.adapter.openapi;

import com.openapiguard.application.CompareContracts;
import com.openapiguard.domain.ComparisonReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static com.openapiguard.TestSpecifications.fixture;

import static org.junit.jupiter.api.Assertions.*;

/** Contrato del adaptador y caso de uso sin Spring: formatos, versiones y entradas fallidas. */
class SwaggerContractAnalyzerTest {
    private final CompareContracts comparison = new CompareContracts(new SwaggerContractAnalyzer());

    @Test
    void comparesSmallFilesThroughTheUseCaseWithoutSpring() throws IOException {
        var report = comparison.compare(fixture("baseline.yaml"), fixture("removed.yaml"));
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(java.util.List.of("ENDPOINT_REMOVED", "HTTP_METHOD_REMOVED"),
                report.changes().stream().map(change -> change.ruleId()).toList());
    }

    @Test
    void reorderedJsonAndYamlAreEquivalentAndAdditionsDoNotBreak() throws IOException {
        var baseline = fixture("baseline.yaml");
        assertEquals(ComparisonReport.Verdict.COMPATIBLE,
                comparison.compare(baseline, fixture("equivalent.json")).verdict());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE,
                comparison.compare(fixture("removed.yaml"), baseline).verdict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.0", "3.1.1"})
    void operationRemovalWorksInSupportedFamilies(String version) throws IOException {
        var report = comparison.compare(fixture("baseline.yaml").replace("3.0.3", version),
                fixture("removed.yaml").replace("3.0.3", version));
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(2, report.changes().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not: [valid", "{}", "openapi: 2.0", "openapi: 3.2.0"})
    void invalidCandidateDoesNotProduceFalseRemovals(String invalid) throws IOException {
        var report = comparison.compare(fixture("baseline.yaml"), invalid);
        assertEquals(ComparisonReport.Status.FAILED, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertEquals("candidate", report.diagnostics().getFirst().source());
    }

    @Test
    void rejectsCrossFamilyComparison() throws IOException {
        var base = fixture("baseline.yaml");
        assertEquals(ComparisonReport.Status.FAILED,
                comparison.compare(base, base.replace("3.0.3", "3.1.0")).status());
    }

    @Test
    void internalResponseReferencesDoNotChangeOperationIdentity() throws IOException {
        var base = fixture("removed.yaml");
        var withRef = base.replace("description: OK", "$ref: '#/components/responses/Ok'")
                + "\ncomponents:\n  responses:\n    Ok:\n      description: OK\n";
        var report = comparison.compare(base, withRef);
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, report.verdict());
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n"})
    void parserValidationAndDanglingReferencesFailClosed(String lineEnding) throws IOException {
        var base = fixture("baseline.yaml").replace("\r\n", "\n").replace("\n", lineEnding);
        // Match the fixture's actual line ending so this mutation cannot silently
        // become a no-op after a Git checkout or source archive on Windows.
        var missingTitle = base.replace("  title: Pets" + lineEnding, "");
        assertNotEquals(base, missingTitle, "The candidate must actually lose its required title");
        assertEquals(ComparisonReport.Status.FAILED,
                comparison.compare(base, missingTitle).status());
        var brokenRef = base + "\ncomponents:\n  schemas:\n    Pet:\n      $ref: '#/missing'\n";
        assertEquals(ComparisonReport.Status.FAILED, comparison.compare(base, brokenRef).status());
    }

    @Test
    void rejectsMultipleDocuments() throws IOException {
        var base = fixture("baseline.yaml");
        assertEquals(ComparisonReport.Status.FAILED,
                comparison.compare(base, base + "\n---\n" + base).status());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[1, 2]", "{key: value}", "value"})
    void rejectsYamlAliasesWithoutReportingRemovals(String value) throws IOException {
        var report = comparison.compare(fixture("baseline.yaml"),
                fixture("removed.yaml") + "\nx-first: &alias " + value + "\nx-second: *alias\n");
        assertEquals(ComparisonReport.Status.FAILED, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertEquals("candidate", report.diagnostics().getFirst().source());
        assertEquals("YAML aliases are unsupported.", report.diagnostics().getFirst().description());
    }

    @Test
    void rejectsDuplicateKeysAndOversizedDocuments() throws IOException {
        var base = fixture("baseline.yaml");
        assertEquals(ComparisonReport.Status.FAILED,
                comparison.compare(base, base + "\npaths: {}\n").status());
        assertEquals(ComparisonReport.Status.FAILED, comparison.compare(base,
                " ".repeat(SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES + 1)).status());
    }

    @Test
    void rejectsExternalRefsWithoutTouchingTheReferencedFile() throws IOException {
        var base = fixture("baseline.yaml");
        var malicious = base + "\ncomponents:\n  schemas:\n    Pet:\n      $ref: 'file:///not-read.yaml'\n";
        var report = comparison.compare(base, malicious);
        assertEquals(ComparisonReport.Status.FAILED, report.status());
        assertTrue(report.diagnostics().getFirst().description().contains("External or unsupported reference"));
    }

    @Test
    void rejectsLocalPathItemRefsInsteadOfSilentlyTreatingTheirMethodsAsAbsent() throws IOException {
        var base = fixture("baseline.yaml");
        var candidate = """
                openapi: 3.1.0
                info: {title: Pets, version: '1'}
                paths:
                  /pets:
                    $ref: '#/components/pathItems/Pet'
                components:
                  pathItems:
                    Pet:
                      get:
                        responses:
                          '200': {description: OK}
                """;
        assertEquals(ComparisonReport.Status.FAILED,
                comparison.compare(base.replace("3.0.3", "3.1.0"), candidate).status());
    }

}
