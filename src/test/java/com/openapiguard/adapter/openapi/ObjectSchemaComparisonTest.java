package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Objetos por dirección, proyección de acceso, referencias y presupuestos de normalización. */
class ObjectSchemaComparisonTest {
    @ParameterizedTest
    @CsvSource({"REQUEST,false,true,BREAKING", "REQUEST,true,false,COMPATIBLE",
            "RESPONSE,false,true,COMPATIBLE", "RESPONSE,true,false,BREAKING"})
    void requiredPropertiesDependOnDirection(String direction, boolean old, boolean updated, String verdict) {
        assertEquals(verdict, compare(direction, object(false, old, true), object(false, updated, true)).verdict().name());
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,false,true,COMPATIBLE", "REQUEST,true,false,BREAKING",
            "RESPONSE,false,true,BREAKING", "RESPONSE,true,false,COMPATIBLE"})
    void additionalPropertiesDependOnDirection(String direction, boolean old, boolean updated, String verdict) {
        assertEquals(verdict, compare(direction, object(old, false, true), object(updated, false, true)).verdict().name());
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,true,COMPATIBLE", "REQUEST,false,BREAKING",
            "RESPONSE,true,BREAKING", "RESPONSE,false,COMPATIBLE"})
    void optionalPropertyRemovalDependsOnOpenOrClosedObjects(String direction, boolean open, String verdict) {
        assertEquals(verdict, compare(direction, object(open, false, true), object(open, false, false)).verdict().name());
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,true,BREAKING", "REQUEST,false,COMPATIBLE",
            "RESPONSE,true,COMPATIBLE", "RESPONSE,false,BREAKING"})
    void optionalPropertyAdditionCanConstrainPreviouslyAdditionalValues(String direction, boolean open, String verdict) {
        assertEquals(verdict, compare(direction, object(open, false, false), object(open, false, true)).verdict().name());
    }

    @Test void readOnlyAndWriteOnlyProjectRequiredByDirection() {
        String old = "{type: object, additionalProperties: false, properties: {name: {type: string, readOnly: true}}}";
        String required = old.replace("type: object,", "type: object, required: [name],");
        assertEquals("COMPATIBLE", compare("REQUEST", old, required).verdict().name());
        assertEquals("COMPATIBLE", compare("RESPONSE", old, required).verdict().name());
        assertEquals("BREAKING", compare("RESPONSE", required, old).verdict().name());
        old = old.replace("readOnly", "writeOnly");
        required = required.replace("readOnly", "writeOnly");
        assertEquals("BREAKING", compare("REQUEST", old, required).verdict().name());
        assertEquals("COMPATIBLE", compare("RESPONSE", old, required).verdict().name());
    }

    @Test void nestedPropertyChangesPreserveOriginalPointers() {
        String old = "{type: object, properties: {nested: {type: object, properties: {name: {type: string}}}}}";
        var report = compare("REQUEST", old, old.replace("type: string", "type: integer"));
        assertEquals("BREAKING", report.verdict().name());
        assertTrue(report.changes().getFirst().description().contains("name"));
        assertTrue(report.changes().getFirst().location().candidatePointer().endsWith("/properties/nested/properties/name"));
    }

    @Test void opaquePropertiesAndSchemaAdditionalPropertiesArePartial() {
        String opaque = "{type: object, properties: {name: {type: string, pattern: '^a'}}}";
        assertEquals("PARTIAL", compare("REQUEST", opaque, opaque).status().name());
        opaque = "{type: object, additionalProperties: {type: string}}";
        assertEquals("PARTIAL", compare("REQUEST", opaque, opaque).status().name());
        assertEquals("PARTIAL", compare("REQUEST", "{type: object, required: [name], additionalProperties: false}",
                "{type: object}").status().name());
    }

    @Test void defaultsAreOpenAndPropertiesDoNotImplyObjectType() {
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: object}", "{type: object, additionalProperties: true}").verdict().name());
        assertEquals("BREAKING", compare("REQUEST", "{properties: {name: {type: string}}}",
                "{type: object, properties: {name: {type: string}}}").verdict().name());
        assertEquals("COMPATIBLE", compare("RESPONSE", "{properties: {name: {type: string}}}",
                "{type: object, properties: {name: {type: string}}}").verdict().name());
    }

    @Test void unsupportedArraysAndCompositionRemainPartial() {
        assertEquals("PARTIAL", compare("REQUEST", "{type: object, properties: {tags: {type: array, items: {type: string}}}}",
                "{type: object, properties: {tags: {type: array, items: {type: string}}}}").status().name());
        assertEquals("PARTIAL", compare("RESPONSE", "{allOf: [{type: object}]}", "{type: object}").status().name());
    }

    private String object(boolean open, boolean required, boolean property) {
        return "{type: object, additionalProperties: " + open + (required ? ", required: [name]" : "")
                + (property ? ", properties: {name: {type: string}}" : "") + "}";
    }

    @Test
    @org.junit.jupiter.api.Timeout(5)
    void sharedAcyclicReferenceGraphStaysBoundedAndDetectsLeafChanges() {
        var document = new StringBuilder(contract("REQUEST", "{$ref: '#/components/schemas/N32'}"));
        document.append("components:\n  schemas:\n    N0: {type: boolean}\n");
        for (int level = 1; level <= 32; level++) document.append("    N").append(level)
                .append(": {type: object, properties: {left: {$ref: '#/components/schemas/N").append(level - 1)
                .append("'}, right: {$ref: '#/components/schemas/N").append(level - 1).append("'}}}\n");
        var analyzer = new SwaggerContractAnalyzer(ComparisonProfile.OBJECT_SCHEMAS);
        String old = document.toString();
        assertEquals("COMPATIBLE", analyzer.analyze(old, old).verdict().name());
        var breaking = analyzer.analyze(old, old.replace("N0: {type: boolean}", "N0: {type: boolean, enum: [true]}"));
        assertEquals("COMPLETED", breaking.status().name());
        assertEquals("BREAKING", breaking.verdict().name());
        assertEquals("/components/schemas/N0", breaking.changes().getFirst().location().candidatePointer());
    }

    @Test void normalizationBudgetIsVisibleInsteadOfSilentlyCertifyingIncompleteData() {
        var properties = new StringBuilder();
        for (int index = 0; index < 10_001; index++) {
            if (index > 0) properties.append(',');
            properties.append("field").append(index).append(": {type: boolean}");
        }
        String document = contract("REQUEST", "{type: object, properties: {" + properties + "}}");
        var report = new SwaggerContractAnalyzer(ComparisonProfile.OBJECT_SCHEMAS).analyze(document, document);
        assertEquals("PARTIAL", report.status().name());
        assertEquals("INDETERMINATE", report.verdict().name());
        assertTrue(report.diagnostics().stream().anyMatch(d -> d.description().contains("budget")));
    }

    @Test void sharedComponentIsComparedSeparatelyForRequestsAndResponses() {
        var analyzer = new SwaggerContractAnalyzer(ComparisonProfile.OBJECT_SCHEMAS);
        String old = contract("REQUEST", "{$ref: '#/components/schemas/Value'}")
                .replace("'200': {description: OK}", "'200': {description: OK, content: {application/json: {schema: {$ref: '#/components/schemas/Value'}}}}")
                + "components:\n  schemas:\n    Value: {type: object, properties: {name: {type: string}}}\n";
        String updated = old.replace("Value: {type: object,", "Value: {type: object, required: [name],");
        var requestBreak = analyzer.analyze(old, updated);
        assertEquals("COMPLETED", requestBreak.status().name());
        assertEquals(1, requestBreak.changes().size());
        assertEquals("REQUEST", requestBreak.changes().getFirst().location().direction());
        assertEquals("/components/schemas/Value/required", requestBreak.changes().getFirst().location().candidatePointer());
        var responseBreak = analyzer.analyze(updated, old);
        assertEquals(1, responseBreak.changes().size());
        assertEquals("RESPONSE", responseBreak.changes().getFirst().location().direction());
        assertEquals("200", responseBreak.changes().getFirst().location().responseCode());
    }

    @Test void mathematicallyIntegralLengthBoundsAcceptDecimalNotation() {
        assertEquals("COMPATIBLE", compare("REQUEST", "{type: string, maxLength: 1.0}", "{type: string, maxLength: 1}").verdict().name());
    }

    @Test void internalObjectReferencesAndCyclesPreserveCoverage() {
        var analyzer = new SwaggerContractAnalyzer(ComparisonProfile.valueOf("OBJECT_SCHEMAS"));
        String inline = contract("REQUEST", "{type: object, properties: {name: {type: string}}}");
        String referenced = contract("REQUEST", "{$ref: '#/components/schemas/Value'}")
                + "components:\n  schemas:\n    Value: {type: object, properties: {name: {type: string}}}\n";
        assertEquals("COMPATIBLE", analyzer.analyze(inline, referenced).verdict().name());
        String cyclic = referenced.replace("name: {type: string}", "name: {$ref: '#/components/schemas/Value'}");
        assertEquals("PARTIAL", analyzer.analyze(cyclic, cyclic).status().name());
        assertEquals("COMPATIBLE", analyzer.analyze(inline.replace("3.0.3", "3.1.1"), referenced.replace("3.0.3", "3.1.1")).verdict().name());
    }

    @Test void requiredPropertyDeletionAndInvalidAccessModesAreExplicit() {
        assertEquals("BREAKING", compare("RESPONSE", object(false, true, true), object(false, false, false)).verdict().name());
        assertEquals("FAILED", compare("REQUEST", "{type: object}",
                "{type: object, properties: {name: {type: string, readOnly: true, writeOnly: true}}}").status().name());
    }

    @Test void accessModeTransitionsRequireReviewRatherThanAssumingServerRejection() {
        String old = "{type: object, additionalProperties: false, properties: {name: {type: string}}}";
        String updated = old.replace("type: string", "type: string, readOnly: true");
        var report = compare("REQUEST", old, updated);
        assertEquals("PARTIAL", report.status().name());
        assertTrue(report.changes().isEmpty());
        assertTrue(report.diagnostics().getFirst().description().contains("readOnly"));
    }

    @Test void provenBreakAndOpaqueOptionalPropertyRetainPartialCoverage() {
        String old = "{type: object, properties: {name: {type: string, pattern: '^a'}, enabled: {type: boolean}}}";
        var report = compare("REQUEST", old, old.replace("type: object,", "type: object, required: [enabled],"));
        assertEquals("BREAKING", report.verdict().name());
        assertEquals("PARTIAL", report.status().name());
        assertEquals(1, report.changes().size());
    }

    private com.openapiguard.domain.ComparisonReport compare(String direction, String old, String updated) {
        var analyzer = new SwaggerContractAnalyzer(ComparisonProfile.valueOf("OBJECT_SCHEMAS"));
        return analyzer.analyze(contract(direction, old), contract(direction, updated));
    }

    static String contract(String direction, String schema) {
        String body = direction.equals("REQUEST") ? "requestBody: {content: {application/json: {schema: " + schema + "}}}" : "";
        String response = direction.equals("RESPONSE") ? "'200': {description: OK, content: {application/json: {schema: " + schema + "}}}" : "'200': {description: OK}";
        return """
                openapi: 3.0.3
                info: {title: Objects, version: '1'}
                paths:
                  /objects:
                    post:
                      %s
                      responses: {%s}
                """.formatted(body, response);
    }
}
