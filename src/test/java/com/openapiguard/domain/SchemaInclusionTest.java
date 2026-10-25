package com.openapiguard.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Intervalos enteros exhaustivos y regresiones de restricciones conjuntas, Unicode y determinismo. */
class SchemaInclusionTest {
    @Test void incompatibleUnionReportsFirstTypeInCanonicalOrder() {
        var source = new SchemaValueContract(Set.of("string", "null"), null, null, null,
                0, Integer.MAX_VALUE, List.of(), "/source");
        var target = new SchemaValueContract(Set.of("integer"), null, null, null,
                0, Integer.MAX_VALUE, List.of(), "/target");
        var result = new SchemaInclusion().compare(source, target);
        assertEquals(SchemaInclusion.Outcome.EXCLUDED, result.outcome());
        assertEquals("Type null is no longer allowed.", result.reason());
    }

    @ParameterizedTest @MethodSource("integerIntervals")
    void boundedIntegerInclusionMatchesExhaustiveMathematicalOracle(int oldMin, int oldMax, int newMin, int newMax) {
        boolean included = IntStream.rangeClosed(oldMin, oldMax).allMatch(value -> value >= newMin && value <= newMax);
        var result = new SchemaInclusion().compare(integer(oldMin, oldMax), integer(newMin, newMax));
        assertEquals(included ? SchemaInclusion.Outcome.INCLUDED : SchemaInclusion.Outcome.EXCLUDED, result.outcome());
    }

    static Stream<Arguments> integerIntervals() {
        var intervals = List.of(new int[]{-2, -1}, new int[]{-2, 2}, new int[]{0, 0}, new int[]{0, 2}, new int[]{1, 2}, new int[]{2, 3});
        return intervals.stream().flatMap(old -> intervals.stream().map(updated -> Arguments.of(old[0], old[1], updated[0], updated[1])));
    }

    @Test void unknownAssertionsRemainUnknownEvenWhenIdentical() {
        var schema = new SchemaValueContract(Set.of("string"), null, null, null, 0, Integer.MAX_VALUE, List.of("pattern"), "/schema");
        assertEquals(SchemaInclusion.Outcome.UNKNOWN, new SchemaInclusion().compare(schema, schema).outcome());
    }

    @Test void lengthCountsUnicodeCodePointsRatherThanUtf16Units() {
        var source = new SchemaValueContract(Set.of("string"), List.of(new ScalarValue("string", "🐈")), null, null, 0, Integer.MAX_VALUE, List.of(), "/schema");
        var target = new SchemaValueContract(Set.of("string"), null, null, null, 1, 1, List.of(), "/schema");
        assertEquals(SchemaInclusion.Outcome.INCLUDED, new SchemaInclusion().compare(source, target).outcome());
    }

    private SchemaValueContract integer(int minimum, int maximum) {
        return new SchemaValueContract(Set.of("integer"), null, new SchemaValueContract.Bound(BigDecimal.valueOf(minimum), false),
                new SchemaValueContract.Bound(BigDecimal.valueOf(maximum), false), 0, Integer.MAX_VALUE, List.of(), "/schema");
    }

    @Test void largeEnumsProveIntegerIntervalCoverageAndApplyTheirBounds() {
        var values = IntStream.rangeClosed(-1024, 1024).mapToObj(value -> new ScalarValue("number", Integer.toString(value))).toList();
        var complete = new SchemaValueContract(Set.of("integer"), values, null, null, 0, Integer.MAX_VALUE, List.of(), "/schema");
        assertEquals(SchemaInclusion.Outcome.INCLUDED, new SchemaInclusion().compare(integer(-1024, 1024), complete).outcome());
    }

    @Test void largeEnumBoundsCanExcludeValuesDespiteMatchingEnumEntries() {
        var values = IntStream.rangeClosed(-1024, 1024).mapToObj(value -> new ScalarValue("number", Integer.toString(value))).toList();
        var narrowed = new SchemaValueContract(Set.of("integer"), values, new SchemaValueContract.Bound(BigDecimal.ZERO, false), null, 0, Integer.MAX_VALUE, List.of(), "/schema");
        assertEquals(SchemaInclusion.Outcome.EXCLUDED, new SchemaInclusion().compare(integer(-1024, 1024), narrowed).outcome());
    }
}
