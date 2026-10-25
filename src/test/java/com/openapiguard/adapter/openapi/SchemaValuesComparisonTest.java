package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Valores escalares por dirección y restricciones conjuntas con incertidumbre explícita. */
class SchemaValuesComparisonTest {
    @Test void explanationsShowBaselineToCandidateConstraintsInBothDirectionsAndNestedProperties() {
        var request = new SwaggerContractAnalyzer(ComparisonProfile.RESPONSE_POLICY).analyze(
                contract("REQUEST", "{type: object, properties: {quantity: {type: integer, maximum: 100}}}"),
                contract("REQUEST", "{type: object, properties: {quantity: {type: integer, maximum: 10}}}"));
        assertTrue(request.changes().getFirst().description().contains("maximum: 100 -> 10"), request.toString());
        var response = compare("RESPONSE", "{type: string, enum: [PENDING, PAID]}",
                "{type: string, enum: [PENDING, PAID, CANCELLED]}");
        assertTrue(response.changes().getFirst().description().contains("enum: [PAID, PENDING] -> [CANCELLED, PAID, PENDING]"), response.toString());
    }

    @Test void arrayExplanationKeepsChronologicalBoundsWhenResponseInclusionIsReversed() {
        var analyzer = new SwaggerContractAnalyzer(ComparisonProfile.RESPONSE_POLICY);
        var report = analyzer.analyze(contract("RESPONSE", "{type: array, maxItems: 100, items: {type: string}}"),
                contract("RESPONSE", "{type: array, maxItems: 200, items: {type: string}}"));
        assertTrue(report.changes().getFirst().description().contains("maxItems: 100 -> 200"), report.toString());
    }

    @Test void abbreviatedEnumDetailsPreserveWholeUnicodeCharacters() {
        String value = "a".repeat(159) + "🐈tail";
        var report = compare("RESPONSE", "{type: string, enum: [old]}", "{type: string, enum: ['" + value + "']}");
        assertTrue(report.changes().getFirst().description().contains("🐈..."), report.toString());
    }
    @ParameterizedTest
    @CsvSource({"REQUEST,integer,number,COMPATIBLE", "REQUEST,number,integer,BREAKING",
            "RESPONSE,integer,number,BREAKING", "RESPONSE,number,integer,COMPATIBLE",
            "REQUEST,string,boolean,BREAKING", "RESPONSE,string,boolean,BREAKING"})
    void typeInclusionDependsOnDirection(String direction, String old, String updated, String verdict) {
        var report = compare(direction, "{type: " + old + "}", "{type: " + updated + "}");
        assertEquals(verdict, report.verdict().name());
        assertEquals("COMPLETED", report.status().name());
        if (verdict.equals("BREAKING")) {
            assertTrue(report.changes().getFirst().ruleId().startsWith(direction + "_SCHEMA_"));
            assertTrue(report.changes().getFirst().location().candidatePointer().contains("schema"));
        }
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,false,true,COMPATIBLE", "REQUEST,true,false,BREAKING",
            "RESPONSE,false,true,BREAKING", "RESPONSE,true,false,COMPATIBLE"})
    void nullableChangesAreDirectional(String direction, boolean old, boolean updated, String verdict) {
        assertEquals(verdict, compare(direction, "{type: string, nullable: " + old + "}",
                "{type: string, nullable: " + updated + "}").verdict().name());
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,COMPATIBLE", "RESPONSE,BREAKING"})
    void enumExpansionHasOppositeEffects(String direction, String verdict) {
        assertEquals(verdict, compare(direction, "{type: string, enum: [a]}",
                "{type: string, enum: [a, b]}").verdict().name());
        String reverse = verdict.equals("BREAKING") ? "COMPATIBLE" : "BREAKING";
        assertEquals(reverse, compare(direction, "{type: string, enum: [a, b]}",
                "{type: string, enum: [a]}").verdict().name());
    }

    @Test void enumTypeAndBoundsAreEvaluatedTogether() {
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: number, enum: [1, 2]}", "{type: integer}").verdict().name());
        assertEquals("COMPATIBLE", compare("RESPONSE", "{type: integer}", "{type: number, enum: [1.0, 2]}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: integer, enum: [1, 2, 10], maximum: 2}",
                "{type: integer, enum: [1, 2]}").verdict().name());
        assertEquals("BREAKING", compare("REQUEST", "{type: integer, enum: [1, 2]}",
                "{type: integer, minimum: 2}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: number, minimum: 1, maximum: 1}", "{type: integer}").verdict().name());
    }

    @Test void numericAndStringLimitsRespectDirection() {
        assertEquals("BREAKING", compare("REQUEST", "{type: integer, minimum: 0}", "{type: integer, minimum: 1}").verdict().name());
        assertEquals("COMPATIBLE", compare("RESPONSE", "{type: integer, minimum: 0}", "{type: integer, minimum: 1}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: integer, minimum: 0.5}", "{type: integer, minimum: 1}").verdict().name());
        assertEquals("BREAKING", compare("RESPONSE", "{type: string, maxLength: 2}", "{type: string, maxLength: 3}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: string, enum: [ab]}", "{type: string, minLength: 2}").verdict().name());
    }

    @Test void opaqueAndContradictorySchemasCannotGiveGreen() {
        assertEquals("PARTIAL", compare("REQUEST", "{type: string, pattern: '^a'}", "{type: string}").status().name());
        assertEquals("PARTIAL", compare("REQUEST", "{type: object}", "{type: object}").status().name());
        assertEquals("PARTIAL", compare("REQUEST", "{type: integer, minimum: 2, maximum: 1}", "{type: integer}").status().name());
        assertEquals("PARTIAL", compare("REQUEST", "{type: string, enum: [1]}", "{type: string}").status().name());
    }

    @Test void supports31UnionNullWithoutConvertingDialects() {
        var report = analyzer().analyze(contract("REQUEST", "{type: [string, 'null']}").replace("3.0.3", "3.1.1"),
                contract("REQUEST", "{type: string}").replace("3.0.3", "3.1.1"));
        assertEquals("BREAKING", report.verdict().name());
    }

    @Test void sharedComponentsAreAnalyzedByUseAndCyclesArePartial() {
        String old = contract("REQUEST", "{$ref: '#/components/schemas/Value'}")
                + "components:\n  schemas:\n    Value: {type: number}\n";
        String updated = old.replace("type: number", "type: integer");
        assertEquals("BREAKING", analyzer().analyze(old, updated).verdict().name());
        assertEquals("/components/schemas/Value", analyzer().analyze(old, updated).changes().getFirst().location().candidatePointer());
        String cycle = old.replace("{type: number}", "{$ref: '#/components/schemas/Value'}");
        assertEquals("PARTIAL", analyzer().analyze(cycle, cycle).status().name());
    }

    private com.openapiguard.domain.ComparisonReport compare(String direction, String old, String updated) {
        return analyzer().analyze(contract(direction, old), contract(direction, updated));
    }

    @Test void nullableDoesNotOverrideEnumAndMissingTypeIsNotInvented() {
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: string, nullable: true, enum: [a]}",
                "{type: string, enum: [a]}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{enum: [a]}", "{type: string}").verdict().name());
        assertEquals("BREAKING", compare("REQUEST", "{}", "{type: string}").verdict().name());
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: integer}", "{}").verdict().name());
    }

    @Test void exclusiveBoundsAreDialectSpecificAndJoint() {
        assertEquals("BREAKING", compare("REQUEST", "{type: number, minimum: 1}", "{type: number, minimum: 1, exclusiveMinimum: true}").verdict().name());
        var report = analyzer().analyze(contract("REQUEST", "{type: number, minimum: 2}").replace("3.0.3", "3.1.1"),
                contract("REQUEST", "{type: number, minimum: 2, exclusiveMinimum: 1}").replace("3.0.3", "3.1.1"));
        assertEquals("COMPATIBLE", report.verdict().name());
    }

    @Test void parameterValuesAreComparedByEffectiveIdentity() {
        String old = contract("REQUEST", "{type: string}").replace("post:", "post:\n      parameters: [{name: limit, in: query, schema: {type: number}}]");
        String updated = old.replace("type: number", "type: integer");
        var report = analyzer().analyze(old, updated);
        assertEquals("BREAKING", report.verdict().name());
        assertEquals("limit", report.changes().getFirst().location().parameter().name());
    }

    private SwaggerContractAnalyzer analyzer() {
        return new SwaggerContractAnalyzer(ComparisonProfile.valueOf("SCHEMA_VALUES"));
    }

    @Test void missingSchemaPointersReferToExistingMediaObject() {
        String old = contract("RESPONSE", "{type: string}");
        String updated = old.replace("schema: {type: string}", "example: value");
        var report = analyzer().analyze(old, updated);
        assertEquals("BREAKING", report.verdict().name());
        assertEquals("/paths/~1values/post/responses/200/content/application~1json", report.changes().getFirst().location().candidatePointer());
    }

    @Test void unknownSchemaDialectCannotGiveGreen() {
        String document = contract("REQUEST", "{type: string, $schema: 'https://example.invalid/custom-dialect'}").replace("3.0.3", "3.1.1");
        assertEquals("PARTIAL", analyzer().analyze(document, document).status().name());
    }

    private String contract(String direction, String schema) {
        String body = direction.equals("REQUEST") ? "requestBody: {content: {application/json: {schema: " + schema + "}}}" : "";
        String response = direction.equals("RESPONSE") ? "'200': {description: OK, content: {application/json: {schema: " + schema + "}}}" : "'200': {description: OK}";
        return """
                openapi: 3.0.3
                info: {title: Values, version: '1'}
                paths:
                  /values:
                    post:
                      %s
                      responses: {%s}
                """.formatted(body, response);
    }
}
