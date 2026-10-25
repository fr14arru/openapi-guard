package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Política de respuestas a través del parser, separada de inclusión de sus payloads. */
class ResponsePolicyComparisonTest {
    @Test void invalidAndCyclicResponseReferencesFailWithoutSpeculativeChanges() {
        String dangling = contract("{'200': {$ref: '#/components/responses/Missing'}}");
        String cyclic = contract("{'200': {$ref: '#/components/responses/Loop'}}")
                + "components:\n  responses:\n    Loop: {$ref: '#/components/responses/Loop'}\n";
        for (String invalid : new String[]{dangling, cyclic}) {
            var report = analyzer().analyze(contract("{'200': {description: OK}}"), invalid);
            assertEquals("FAILED", report.status().name());
            assertTrue(report.changes().isEmpty());
        }
    }

    @Test void removedResponseSchemaDoesNotInventRequestCoverageDiagnostics() {
        String old = contract(response("application/json")).replace("get:", "post:\n      requestBody: {content: {application/json: {schema: {type: string}}}}");
        String updated = old.replace("'200': {description: OK, content: {'application/json': {schema: {type: string}}}}",
                "'204': {description: Empty}");
        var report = analyzer().analyze(old, updated);
        assertTrue(report.diagnostics().stream().noneMatch(d -> d.code().startsWith("REQUEST_SCHEMA")));
    }

    @Test void removedConcreteCodeIsAPublishedPolicyBreakEvenWithoutBody() {
        var report = compare("{'200': {description: OK}, '204': {description: Empty}}", "{'200': {description: OK}}");
        assertEquals("COMPLETED", report.status().name());
        assertEquals("BREAKING", report.verdict().name());
        assertEquals(1, report.changes().size());
        var change = report.changes().getFirst();
        assertEquals("RESPONSE_STATUS_REMOVED", change.ruleId());
        assertEquals("204", change.location().responseCode());
        assertEquals("RESPONSE", change.location().direction());
        assertTrue(change.location().baselinePointer().endsWith("/responses/204"));
        assertTrue(change.location().candidatePointer().endsWith("/responses"));
    }

    @Test void removedConcreteRepresentationHasOriginalLocation() {
        var report = compare(response("Application/JSON"), response("text/plain"));
        assertEquals("BREAKING", report.verdict().name());
        assertTrue(report.changes().stream().anyMatch(c -> c.ruleId().equals("RESPONSE_MEDIA_TYPE_REMOVED")
                && c.location().baselinePointer().endsWith("/content/Application~1JSON")
                && c.location().mediaType().equals("application/json")));
    }

    @Test void removingPayloadFormatsWithoutReplacementIsDetected() {
        var report = compare(response("application/json"), "{'200': {description: OK}}");
        assertEquals("COMPLETED", report.status().name());
        assertEquals("RESPONSE_MEDIA_TYPE_REMOVED", report.changes().getFirst().ruleId());
    }

    @Test void additionsRequireReviewOfConsumerAndNegotiationPolicy() {
        var status = compare("{'200': {description: OK}}", "{'200': {description: OK}, '201': {description: New}}");
        assertEquals("INDETERMINATE", status.verdict().name());
        assertTrue(status.diagnostics().stream().anyMatch(d -> d.code().equals("RESPONSE_STATUS_ADDED_REQUIRES_REVIEW")));
        var media = compare("{'200': {description: OK}}", response("application/json"));
        assertEquals("INDETERMINATE", media.verdict().name());
        assertTrue(media.diagnostics().stream().anyMatch(d -> d.code().equals("RESPONSE_MEDIA_TYPE_ADDED_REQUIRES_REVIEW")));
    }

    @ParameterizedTest
    @CsvSource({"default", "2XX"})
    void fallbackCanCoverRemovedExplicitCodeButCannotCertifyCompatibility(String fallback) {
        var report = compare("{'200': {description: OK}}", "{'" + fallback + "': {description: Fallback}}");
        assertTrue(report.changes().isEmpty());
        assertEquals("PARTIAL", report.status().name());
        assertEquals("INDETERMINATE", report.verdict().name());
    }

    @Test void unchangedFallbackWithoutBodyIsStillExplicitlyPartial() {
        String response = "{default: {description: Fallback}}";
        assertEquals("PARTIAL", compare(response, response).status().name());
    }

    @Test void unrelatedRangeDoesNotHideProvenCodeRemoval() {
        var report = compare("{'200': {description: OK}}", "{'4XX': {description: Errors}}");
        assertEquals("BREAKING", report.verdict().name());
        assertEquals("PARTIAL", report.status().name());
    }

    @Test void wildcardCannotProveAConcreteRepresentationWasRemoved() {
        var report = compare(response("application/json"), response("application/*"));
        assertEquals("PARTIAL", report.status().name());
        assertTrue(report.changes().isEmpty());
    }

    @Test void responseReferencesRetainDefinitionAndUse() {
        String old = contract("{'200': {$ref: '#/components/responses/Value'}}")
                + "components:\n  responses:\n    Value: {description: OK, content: {application/json: {schema: {type: string}}}}\n";
        String updated = old.replace("application/json", "application/xml");
        var report = analyzer().analyze(old, updated);
        var change = report.changes().stream().filter(c -> c.ruleId().equals("RESPONSE_MEDIA_TYPE_REMOVED")).findFirst().orElseThrow();
        assertTrue(change.location().baselinePointer().startsWith("/components/responses/Value"));
        assertTrue(change.location().baselineUsagePointer().startsWith("/paths/~1values/get/responses/200"));
    }

    @Test void equivalentDescriptionsAndMediaCaseAreCompatibleAndIdsIgnoreDefinitionNames() {
        assertEquals("COMPATIBLE", compare(response("Application/JSON"), response("application/json")).verdict().name());
        assertEquals("COMPATIBLE", compare("{'204': {description: Empty}}", "{'204': {description: No payload}}").verdict().name());
        String old = contract("{'200': {description: OK}, '204': {$ref: '#/components/responses/Empty'}}")
                + "components:\n  responses:\n    Empty: {description: Empty}\n";
        String updated = contract("{'200': {description: OK}}");
        var first = analyzer().analyze(old, updated).changes().getFirst();
        var renamed = analyzer().analyze(old.replace("Empty", "Renamed"), updated).changes().getFirst();
        assertEquals(first.id(), renamed.id());
    }

    @Test void payloadComparisonStillUsesResponseDirectionAndArrays() {
        String old = "{'200': {description: OK, content: {application/json: {schema: {type: array, items: {type: integer}}}}}}";
        var report = compare(old, old.replace("type: integer", "type: number"));
        assertEquals("BREAKING", report.verdict().name());
        assertEquals("RESPONSE_SCHEMA_VALUES_EXPANDED", report.changes().getFirst().ruleId());
        assertEquals("COMPLETED", report.status().name());
    }

    @Test void removedOperationDoesNotDuplicateResponsePolicyFindings() {
        String old = contract(response("application/json"));
        var report = analyzer().analyze(old, old.replace("get:", "post:").replace("application/json", "application/xml"));
        assertEquals(1, report.changes().size());
        assertEquals("HTTP_METHOD_REMOVED", report.changes().getFirst().ruleId());
    }

    @Test void priorArrayProfileStillExcludesResponseCodePolicy() {
        var report = new SwaggerContractAnalyzer(ComparisonProfile.ARRAY_SCHEMAS).analyze(
                contract("{'200': {description: OK}, '204': {description: Empty}}"), contract("{'200': {description: OK}}"));
        assertEquals("COMPATIBLE", report.verdict().name());
    }

    private SwaggerContractAnalyzer analyzer() { return new SwaggerContractAnalyzer(ComparisonProfile.valueOf("RESPONSE_POLICY")); }
    private com.openapiguard.domain.ComparisonReport compare(String old, String updated) { return analyzer().analyze(contract(old), contract(updated)); }
    private String response(String media) { return "{'200': {description: OK, content: {'" + media + "': {schema: {type: string}}}}}"; }
    private String contract(String responses) {
        return "openapi: 3.0.3\ninfo: {title: Response policy, version: '1'}\npaths:\n  /values:\n    get:\n      responses: " + responses + "\n";
    }
}
