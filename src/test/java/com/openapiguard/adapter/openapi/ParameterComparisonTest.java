package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** Comparación vertical de presencia y conservación de perfiles/identidades anteriores. */
class ParameterComparisonTest {
    private final SwaggerContractAnalyzer analyzer = new SwaggerContractAnalyzer();

    @ParameterizedTest
    @ValueSource(strings = {"3.0.3", "3.1.1"})
    void comparesEffectiveRequiredParametersAcrossSupportedFamilies(String version) {
        String before = document("", "- {name: limit, in: query, schema: {type: integer}}\n").replace("3.0.3", version);
        String after = document("", """
                - {name: limit, in: query, required: true, schema: {type: integer}}
                - {name: trace, in: header, required: true, schema: {type: string}}
                """).replace("3.0.3", version);
        var report = analyzer.analyze(before, after);
        assertEquals("operations-parameter-presence-v1", report.profileId());
        assertEquals(ComparisonReport.Status.COMPLETED, report.status());
        assertEquals(ComparisonReport.Verdict.BREAKING, report.verdict());
        assertEquals(2, report.changes().size());
        assertEquals(java.util.Set.of("PARAMETER_BECAME_REQUIRED", "REQUIRED_PARAMETER_ADDED"),
                report.changes().stream().map(change -> change.ruleId()).collect(java.util.stream.Collectors.toSet()));
        assertTrue(report.excludedFacets().contains("parameterSchemas"));
        assertFalse(report.excludedFacets().contains("parameters"));
        assertEquals(ComparisonReport.Verdict.COMPATIBLE,
                analyzer.analyze(after, after.replace("required: true", "required: false")).verdict());
    }

    @Test
    void effectiveOverrideWinsOverARequiredInheritedDeclaration() {
        String before = document("", "- {name: limit, in: query, schema: {type: integer}}\n");
        String after = document("- {name: limit, in: query, required: true, schema: {type: integer}}\n",
                "- {name: limit, in: query, schema: {type: integer}}\n");
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, analyzer.analyze(before, after).verdict());
        var inheritedOnly = document("- {name: limit, in: query, required: true, schema: {type: integer}}\n", "");
        assertEquals("PARAMETER_BECAME_REQUIRED", analyzer.analyze(before, inheritedOnly).changes().getFirst().ruleId());
    }

    @Test
    void refAndInlineAreEquivalentAndPointersPointToDefinitionsAndUsages() {
        String before = document("", "- {name: limit, in: query, schema: {type: integer}}\n");
        String after = document("", "- {$ref: '#/components/parameters/Limit'}\n") + """
                components:
                  parameters:
                    Limit: {name: limit, in: query, required: true, schema: {type: integer}}
                """;
        var change = analyzer.analyze(before, after).changes().getFirst();
        assertEquals("/components/parameters/Limit", change.location().candidatePointer());
        assertEquals("/paths/~1pets/get/parameters/0", change.location().candidateUsagePointer());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, analyzer.analyze(after,
                document("", "- {name: limit, in: query, required: true, schema: {type: integer}}\n")).verdict());
    }

    @Test
    void complexPresenceIsPartialAndResponseRequiredIsExcluded() {
        String empty = document("", "");
        String complex = document("", "- {name: filter, in: query, required: true, schema: {type: object}}\n");
        assertEquals(ComparisonReport.Status.PARTIAL, analyzer.analyze(empty, complex).status());
        String response = empty.replace("{description: OK}",
                "{description: OK, content: {application/json: {schema: {type: object, required: [id], properties: {id: {type: string}}}}}}");
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, analyzer.analyze(empty, response).verdict());
    }

    @Test
    void headerCaseAndExplicitSerializationDefaultsAreEquivalent() {
        String old = document("", "- {name: X-Trace, in: header, required: true, schema: {type: string}}\n");
        String candidate = document("", "- {name: x-trace, in: header, required: true, style: simple, explode: false, schema: {type: string}}\n");
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, analyzer.analyze(old, candidate).verdict());
    }

    @Test
    void legacyProfileRetainsItsScopeAndRemovalIds() {
        var legacy = new SwaggerContractAnalyzer(com.openapiguard.domain.ComparisonProfile.OPERATIONS_ONLY);
        String empty = document("", "");
        String complex = document("", "- {name: filter, in: query, required: true, schema: {type: object}}\n");
        var oldProfile = legacy.analyze(empty, complex);
        assertEquals("operations-only-v1", oldProfile.profileId());
        assertEquals(ComparisonReport.Verdict.COMPATIBLE, oldProfile.verdict());
        assertTrue(oldProfile.excludedFacets().contains("parameters"));
        String removed = "openapi: 3.0.3\ninfo: {title: Parameters, version: '1'}\npaths: {}\n";
        assertEquals(legacy.analyze(empty, removed).changes().getFirst().id(),
                analyzer.analyze(empty, removed).changes().getFirst().id());
    }

    private static String document(String inherited, String local) {
        return "openapi: 3.0.3\ninfo: {title: Parameters, version: '1'}\npaths:\n  /pets:\n"
                + (inherited.isEmpty() ? "" : "    parameters:\n" + inherited.indent(6))
                + "    get:\n" + (local.isEmpty() ? "" : "      parameters:\n" + local.indent(8))
                + "      responses: {'200': {description: OK}}\n";
    }
}
