package com.openapiguard.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Presencia obligatoria y relajación sin afirmar rechazo a partir de retiradas documentales. */
class RequiredParametersRuleTest {
    private final RequiredParametersRule rule = new RequiredParametersRule();
    private final OperationId operation = new OperationId("/pets", HttpMethod.GET);

    @ParameterizedTest
    @ValueSource(strings = {"query", "header", "cookie"})
    void newlyRequiredParametersBreakRequests(String in) {
        var after = parameter("limit", in, true, "/paths/~1pets/get/parameters/0");
        var report = rule.compare(contract(Map.of()), contract(Map.of(after.id(), after)));
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        var change = report.changes().getFirst();
        assertEquals("REQUIRED_PARAMETER_ADDED", change.ruleId());
        assertEquals(after.id(), change.location().parameter());
        assertNull(change.location().baselinePointer());
        assertEquals(after.definitionPointer(), change.location().candidatePointer());
        assertEquals(HttpMethod.GET, change.location().method());
    }

    @Test
    void optionalToRequiredBreaksAndTheReverseDoesNot() {
        var before = parameter("limit", "query", false, "/old/0");
        var after = parameter("limit", "query", true, "/new/1");
        var report = rule.compare(contract(Map.of(before.id(), before)), contract(Map.of(after.id(), after)));
        assertEquals("PARAMETER_BECAME_REQUIRED", report.changes().getFirst().ruleId());
        assertEquals("/old/0", report.changes().getFirst().location().baselinePointer());
        assertEquals("/new/1", report.changes().getFirst().location().candidatePointer());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE,
                rule.compare(contract(Map.of(after.id(), after)), contract(Map.of(before.id(), before))).verdict());
    }

    @Test
    void optionalAdditionsAndUnchangedContractsRemainCompatible() {
        var optional = parameter("limit", "query", false, "/new/0");
        var candidate = contract(Map.of(optional.id(), optional));
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, rule.compare(contract(Map.of()), candidate).verdict());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, rule.compare(candidate, candidate).verdict());
    }

    @Test
    void removalRequiresReviewRatherThanInventingARequestRejection() {
        var parameter = parameter("limit", "query", false, "/old/0");
        var report = rule.compare(contract(Map.of(parameter.id(), parameter)), contract(Map.of()));
        assertEquals(ComparisonReport.Status.PARTIAL, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertEquals("PARAMETER_REMOVAL_REQUIRES_REVIEW", report.diagnostics().getFirst().code());
    }

    @Test
    void serializationChangeIsPartialAndDoesNotHideIndependentBreaks() {
        var before = parameter("a", "query", false, "/old/0");
        var modified = new EffectiveParameter(before.id(), false, "/new/0", "/new/0", "different", true);
        var added = parameter("b", "cookie", true, "/new/1");
        var report = rule.compare(contract(Map.of(before.id(), before)),
                contract(Map.of(modified.id(), modified, added.id(), added)));
        assertEquals(ComparisonReport.Status.PARTIAL, report.status());
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(1, report.changes().size());
        assertEquals("PARAMETER_SERIALIZATION_REQUIRES_REVIEW", report.diagnostics().getFirst().code());
    }

    @Test
    void unsupportedPresenceNeverProducesAGreenOrASpeculativeBreak() {
        var complex = new EffectiveParameter(new ParameterId("filter", "query"), true,
                "/new/0", "/new/0", "form:true", false);
        var report = rule.compare(contract(Map.of()), contract(Map.of(complex.id(), complex)));
        assertEquals(ComparisonReport.Status.PARTIAL, report.status());
        assertEquals(ComparisonReport.Verdict.INDETERMINATE, report.verdict());
        assertTrue(report.changes().isEmpty());
        assertEquals("PARAMETER_PRESENCE_UNSUPPORTED", report.diagnostics().getFirst().code());
    }

    @Test
    void unsupportedBaselineKeepsItsOriginalSourceAndLocation() {
        var id = new ParameterId("filter", "query");
        var before = new EffectiveParameter(id, false, "/components/parameters/Filter", "/old/0", "form:true", false);
        var after = parameter("filter", "query", true, "/new/1");
        var report = rule.compare(contract(Map.of(id, before)), contract(Map.of(id, after)));
        assertEquals("baseline", report.diagnostics().getFirst().source());
        assertEquals("/old/0", report.diagnostics().getFirst().location());
        assertTrue(report.changes().isEmpty());
    }

    @Test
    void removedAndNewOperationsDoNotGenerateParameterDuplicates() {
        var required = parameter("limit", "query", true, "/new/0");
        var with = contract(Map.of(required.id(), required));
        var withoutOperation = new OperationContract("3.0.3", Map.of(), Map.of());
        assertTrue(rule.compare(with, withoutOperation).changes().isEmpty());
        assertTrue(rule.compare(with, withoutOperation).diagnostics().isEmpty());
        assertTrue(rule.compare(withoutOperation, with).changes().isEmpty());
    }

    @Test
    void idsIgnorePhysicalIndicesAndFindingsUseSemanticOrder() {
        var a = parameter("a", "query", true, "/new/5");
        var b = parameter("b", "query", true, "/new/0");
        var report = rule.compare(contract(Map.of()), contract(Map.of(b.id(), b, a.id(), a)));
        assertEquals(java.util.List.of("a", "b"), report.changes().stream()
                .map(change -> change.location().parameter().name()).toList());
        var moved = parameter("a", "query", true, "/new/42");
        assertEquals(report.changes().getFirst().id(),
                rule.compare(contract(Map.of()), contract(Map.of(moved.id(), moved))).changes().getFirst().id());
    }

    private OperationContract contract(Map<ParameterId, EffectiveParameter> parameters) {
        return new OperationContract("3.0.3", Map.of("/pets", Set.of(HttpMethod.GET)), Map.of(operation, parameters));
    }

    private static EffectiveParameter parameter(String name, String in, boolean required, String pointer) {
        return new EffectiveParameter(new ParameterId(name, in), required, pointer, pointer, "simple:false", true);
    }
}
