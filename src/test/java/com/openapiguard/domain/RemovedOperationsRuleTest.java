package com.openapiguard.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Disponibilidad pura de endpoints/métodos, plantillas ambiguas y orden de hallazgos. */
class RemovedOperationsRuleTest {
    private final RemovedOperationsRule rule = new RemovedOperationsRule();

    @Test
    void reportsOneEndpointRemovalWithoutDuplicatingMethods() {
        var baseline = contract(Map.of("/pets/~special", Set.of(HttpMethod.GET, HttpMethod.POST)));
        var report = rule.compare(baseline, contract(Map.of()));
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(1, report.changes().size());
        var change = report.changes().getFirst();
        assertEquals("ENDPOINT_REMOVED", change.ruleId());
        assertEquals(DetectedChange.Severity.ERROR, change.severity());
        assertEquals("/paths/~1pets~1~0special", change.location().baselinePointer());
        assertNull(change.location().candidatePointer());
        assertFalse(change.description().isBlank());
        assertEquals(change.id(), rule.compare(baseline, contract(Map.of())).changes().getFirst().id());
    }

    @ParameterizedTest
    @EnumSource(HttpMethod.class)
    void detectsEveryRemovedHttpMethod(HttpMethod method) {
        var report = rule.compare(contract(Map.of("/pets", Set.of(method))),
                contract(Map.of("/pets", Set.of())));
        assertEquals("HTTP_METHOD_REMOVED", report.changes().getFirst().ruleId());
        assertEquals(method, report.changes().getFirst().location().method());
    }

    @Test
    void additionsAndEquivalentContractsHaveNoRemovals() {
        var baseline = contract(Map.of("/pets", Set.of(HttpMethod.GET)));
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, rule.compare(baseline, baseline).verdict());
        var candidate = contract(Map.of("/pets", Set.of(HttpMethod.GET, HttpMethod.POST),
                "/health", Set.of(HttpMethod.GET)));
        assertTrue(rule.compare(baseline, candidate).changes().isEmpty());
        assertFalse(rule.compare(baseline, candidate).excludedFacets().isEmpty());
    }

    @Test
    void templateRenameIsIndeterminateInsteadOfAnEndpointRemoval() {
        var report = rule.compare(contract(Map.of("/pets/{id}", Set.of(HttpMethod.GET))),
                contract(Map.of("/pets/{petId}", Set.of(HttpMethod.GET))));
        assertEquals(ComparisonReport.Status.PARTIAL, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertEquals("PATH_MATCH_REQUIRES_REVIEW", report.diagnostics().getFirst().code());
    }

    @Test
    void findingsAreSortedAndMetadataOnlyPathsAreNotEndpoints() {
        var report = rule.compare(contract(Map.of("/z", Set.of(HttpMethod.GET),
                "/a", Set.of(HttpMethod.POST), "/empty", Set.of())), contract(Map.of()));
        assertEquals(java.util.List.of("/a", "/z"), report.changes().stream()
                .map(change -> change.location().path()).toList());
    }

    private static OperationContract contract(Map<String, Set<HttpMethod>> paths) {
        return new OperationContract("3.0.3", paths);
    }
}
