package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Presencia y cobertura de formatos de cuerpo, incluyendo semántica incierta de métodos. */
class RequestBodyComparisonTest {
    @ParameterizedTest
    @CsvSource({"false,true,BREAKING", "true,false,COMPATIBLE", "false,false,COMPATIBLE", "true,true,COMPATIBLE"})
    void comparesRequiredBody(boolean old, boolean updated, String verdict) {
        var report = analyzer().analyze(contract(body(old, "application/json")), contract(body(updated, "application/json")));
        assertEquals(verdict, report.verdict().name());
        if (verdict.equals("BREAKING")) assertEquals("REQUEST_BODY_BECAME_REQUIRED", report.changes().getFirst().ruleId());
    }

    @ParameterizedTest
    @CsvSource({"application/json,text/plain,BREAKING", "application/json,application/*,COMPATIBLE", "application/*,application/json,BREAKING", "application/json,*/*,COMPATIBLE", "Application/JSON,application/json,COMPATIBLE"})
    void comparesMediaTypeCoverage(String old, String updated, String verdict) {
        var report = analyzer().analyze(contract(body(false, old)), contract(body(false, updated)));
        assertEquals(verdict, report.verdict().name());
        if (verdict.equals("BREAKING")) assertEquals("REQUEST_MEDIA_TYPE_REMOVED", report.changes().getFirst().ruleId());
    }

    @Test void additionAndRemovalUseDifferentPolicies() {
        var added = analyzer().analyze(contract(""), contract(body(true, "application/json")));
        assertEquals("REQUEST_BODY_REQUIRED_ADDED", added.changes().getFirst().ruleId());
        assertNull(added.changes().getFirst().location().baselinePointer());
        assertEquals("/paths/~1pets/post/requestBody", added.changes().getFirst().location().candidatePointer());
        assertEquals("COMPATIBLE", analyzer().analyze(contract(""), contract(body(false, "application/json"))).verdict().name());
        var removed = analyzer().analyze(contract(body(false, "application/json")), contract(""));
        assertEquals("PARTIAL", removed.status().name());
        assertEquals("REQUEST_BODY_REMOVAL_REQUIRES_REVIEW", removed.diagnostics().getFirst().code());
    }

    private SwaggerContractAnalyzer analyzer() {
        return new SwaggerContractAnalyzer(ComparisonProfile.valueOf("REQUEST_BODY"));
    }

    @ParameterizedTest
    @CsvSource({"GET,3.0.3", "GET,3.1.1", "HEAD,3.0.3", "DELETE,3.1.1", "OPTIONS,3.0.3", "TRACE,3.1.1"})
    void ambiguousMethodBodySemanticsRequireReview(String method, String version) {
        String old = contract(body(false, "application/json")).replace("post:", method.toLowerCase(java.util.Locale.ROOT) + ":").replace("3.0.3", version);
        String updated = old.replace("required: false", "required: true");
        var report = analyzer().analyze(old, updated);
        assertEquals("PARTIAL", report.status().name());
        assertTrue(report.changes().isEmpty());
        assertEquals("REQUEST_BODY_METHOD_REQUIRES_REVIEW", report.diagnostics().getFirst().code());
    }

    @Test void internalReferencesRetainDefinitionAndUsageWithoutExpansion() {
        String inline = contract(body(true, "application/json"));
        String referenced = contract("requestBody: {$ref: '#/components/requestBodies/Pet'}")
                + "components:\n  requestBodies:\n    Pet: {required: true, content: {application/json: {schema: {type: string}}}}\n";
        assertEquals("COMPATIBLE", analyzer().analyze(inline, referenced).verdict().name());
        var change = analyzer().analyze(contract(""), referenced).changes().getFirst();
        assertEquals("/components/requestBodies/Pet", change.location().candidatePointer());
        assertEquals("/paths/~1pets/post/requestBody", change.location().candidateUsagePointer());
        assertEquals(analyzer().analyze(contract(""), inline).changes().getFirst().id(), change.id());
    }

    @Test void cyclicBodyReferenceFailsWithoutSpeculativeFindings() {
        String referenced = contract("requestBody: {$ref: '#/components/requestBodies/Pet'}")
                + "components:\n  requestBodies:\n    Pet: {$ref: '#/components/requestBodies/Pet'}\n";
        var report = analyzer().analyze(contract(""), referenced);
        assertEquals("FAILED", report.status().name());
        assertTrue(report.changes().isEmpty());
    }

    @Test void unsupportedMediaParametersCannotGiveGreen() {
        var report = analyzer().analyze(contract(body(false, "application/json; charset=utf-8")),
                contract(body(false, "application/json")));
        assertEquals("PARTIAL", report.status().name());
        assertEquals("REQUEST_MEDIA_TYPE_REQUIRES_REVIEW", report.diagnostics().getFirst().code());
    }

    @Test void onlySharedUnambiguousOperationsAreCompared() {
        assertTrue(analyzer().analyze(contract(body(true, "application/json")),
                contract(body(true, "text/plain")).replace("post:", "put:")).changes().stream()
                .allMatch(change -> change.ruleId().equals("HTTP_METHOD_REMOVED")));
        assertEquals("COMPATIBLE", analyzer().analyze(contract(body(false, "application/json")),
                contract(body(false, "application/json")).replace("3.0.3", "3.0.4")).verdict().name());
        assertEquals("COMPATIBLE", analyzer().analyze(contract(body(false, "application/json")).replace("3.0.3", "3.1.1"),
                contract(body(false, "application/json")).replace("3.0.3", "3.1.1")).verdict().name());
    }

    private String body(boolean required, String media) {
        return "requestBody: {required: " + required + ", content: {'" + media + "': {schema: {type: string}}}}";
    }

    private String contract(String body) {
        return """
                openapi: 3.0.3
                info: {title: Pets, version: '1'}
                paths:
                  /pets:
                    post:
                      %s
                      responses: {'200': {description: OK}}
                """.formatted(body);
    }
}
