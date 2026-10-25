package com.openapiguard.adapter.openapi;

import com.openapiguard.domain.ComparisonProfile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

/** Arrays por dirección: items, longitudes, anidamiento y existencia de contraejemplos. */
class ArraySchemaComparisonTest {
    @Test void opaqueItemsCannotProveThatALongerArrayExists() {
        var report = compare("REQUEST", "{type: array, items: {type: string, pattern: '(?!)'}, maxItems: 4}",
                "{type: array, items: {type: string}, maxItems: 2}");
        assertEquals("PARTIAL", report.status().name());
        assertEquals("INDETERMINATE", report.verdict().name());
    }

    @Test void opaqueRequiredItemsCannotProveAnArrayTypeCounterexample() {
        var report = compare("REQUEST", "{type: array, minItems: 1, items: {type: string, pattern: '(?!)'}}",
                "{type: string}");
        assertEquals("INDETERMINATE", report.verdict().name());
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,integer,number,COMPATIBLE", "REQUEST,number,integer,BREAKING",
            "RESPONSE,integer,number,BREAKING", "RESPONSE,number,integer,COMPATIBLE"})
    void itemTypesDependOnDirection(String direction, String old, String updated, String verdict) {
        var report = compare(direction, array(old, 0, 4), array(updated, 0, 4));
        assertEquals(verdict, report.verdict().name());
        assertEquals("COMPLETED", report.status().name());
        if (verdict.equals("BREAKING")) {
            assertTrue(report.changes().getFirst().location().baselinePointer().endsWith("/items"));
            assertTrue(report.changes().getFirst().location().candidatePointer().endsWith("/items"));
        }
    }

    @ParameterizedTest
    @CsvSource({"REQUEST,0,4,1,4,BREAKING", "REQUEST,1,4,0,5,COMPATIBLE",
            "REQUEST,0,4,0,3,BREAKING", "RESPONSE,0,4,1,3,COMPATIBLE",
            "RESPONSE,1,4,0,4,BREAKING", "RESPONSE,0,3,0,4,BREAKING"})
    void lengthBoundsDependOnDirection(String direction, int oldMin, int oldMax, int newMin, int newMax, String verdict) {
        var report = compare(direction, array("boolean", oldMin, oldMax), array("boolean", newMin, newMax));
        assertEquals(verdict, report.verdict().name());
        assertEquals("COMPLETED", report.status().name());
    }

    @Test void emptyOnlyArraysDoNotCompareUnreachableItems() {
        assertEquals("COMPATIBLE", compare("REQUEST", array("string", 0, 0), array("integer", 0, 0)).verdict().name());
    }

    @Test void nestedArraysAndObjectsPreserveLocation() {
        String old = "{type: array, items: {type: object, properties: {tags: {type: array, items: {type: string}}}}}";
        String updated = old.replace("type: string", "type: string, enum: [one]");
        var report = compare("REQUEST", old, updated);
        assertEquals("BREAKING", report.verdict().name());
        assertTrue(report.changes().getFirst().location().candidatePointer().endsWith("/items/properties/tags/items"));
    }

    @Test void internalReferencesAreEquivalentAndCyclesRequireReview() {
        String inline = contract("REQUEST", "{type: array, items: {type: string}}");
        String referenced = contract("REQUEST", "{type: array, items: {$ref: '#/components/schemas/Item'}}")
                + "components:\n  schemas:\n    Item: {type: string}\n";
        var analyzer = analyzer();
        assertEquals("COMPATIBLE", analyzer.analyze(inline, referenced).verdict().name());
        String cycle = referenced.replace("Item: {type: string}", "Item: {type: array, items: {$ref: '#/components/schemas/Item'}}");
        assertEquals("PARTIAL", analyzer.analyze(cycle, cycle).status().name());
    }

    @Test void unsupportedAssertionsAndContradictionsCannotGiveGreen() {
        for (String schema : new String[]{"{type: array, items: {type: string}, uniqueItems: true}",
                "{type: array, items: {type: string}, minItems: 3, maxItems: 2}",
                "{type: array, items: {type: string, pattern: '^a'}}"}) {
            assertEquals("PARTIAL", compare("REQUEST", schema, schema).status().name());
        }
    }

    @Test void dialect31AllowsOmittedItemsAndBooleanItems() {
        String emptyOnly = contract("REQUEST", "{type: array, items: false}").replace("3.0.3", "3.1.1");
        String unrestricted = contract("REQUEST", "{type: array}").replace("3.0.3", "3.1.1");
        assertEquals("COMPATIBLE", analyzer().analyze(emptyOnly, unrestricted).verdict().name());
        assertEquals("BREAKING", analyzer().analyze(unrestricted, emptyOnly).verdict().name());
        String prefix = unrestricted.replace("type: array", "type: array, prefixItems: [{type: string}]");
        assertEquals("PARTIAL", analyzer().analyze(prefix, prefix).status().name());
    }

    @Test void priorProfileRetainsPartialArrayCoverage() {
        String document = contract("REQUEST", array("string", 0, 4));
        assertEquals("PARTIAL", new SwaggerContractAnalyzer(ComparisonProfile.OBJECT_SCHEMAS).analyze(document, document).status().name());
    }

    private com.openapiguard.domain.ComparisonReport compare(String direction, String old, String updated) {
        return analyzer().analyze(contract(direction, old), contract(direction, updated));
    }
    private SwaggerContractAnalyzer analyzer() { return new SwaggerContractAnalyzer(ComparisonProfile.valueOf("ARRAY_SCHEMAS")); }
    private String array(String type, int min, int max) {
        return "{type: array, items: {type: " + type + "}, minItems: " + min + ", maxItems: " + max + "}";
    }
    private String contract(String direction, String schema) { return ObjectSchemaComparisonTest.contract(direction, schema); }
}
