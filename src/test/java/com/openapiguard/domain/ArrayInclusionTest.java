package com.openapiguard.domain;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

/** Oráculo independiente de inclusión para 324 pares de arrays booleanos acotados. */
class ArrayInclusionTest {
    record Shape(int min, int max, Set<Boolean> values) {}

    @ParameterizedTest @MethodSource("pairs")
    void inclusionMatchesIndependentArrayOracle(Shape source, Shape target) {
        boolean included = samples().stream().filter(value -> accepts(source, value)).allMatch(value -> accepts(target, value));
        var result = new SchemaInclusion().compare(schema(source), schema(target));
        assertEquals(included ? SchemaInclusion.Outcome.INCLUDED : SchemaInclusion.Outcome.EXCLUDED, result.outcome());
        assertFalse(result.incomplete());
    }

    static Stream<Arguments> pairs() {
        var shapes = new ArrayList<Shape>();
        for (int min = 0; min <= 2; min++) for (int max = min; max <= 2; max++)
            for (Set<Boolean> values : List.of(Set.of(true), Set.of(false), Set.of(true, false)))
                shapes.add(new Shape(min, max, values));
        return shapes.stream().flatMap(source -> shapes.stream().map(target -> Arguments.of(source, target)));
    }

    private List<List<Object>> samples() {
        var arrays = new ArrayList<List<Object>>();
        arrays.add(List.of());
        List<Object> values = Arrays.asList(true, false, null, 0, "text", List.of(), Map.of());
        for (Object first : values) {
            arrays.add(Collections.singletonList(first));
            for (Object second : values) arrays.add(Arrays.asList(first, second));
        }
        return arrays;
    }

    private boolean accepts(Shape shape, List<Object> array) {
        return array.size() >= shape.min() && array.size() <= shape.max()
                && array.stream().allMatch(value -> value instanceof Boolean bool && shape.values().contains(bool));
    }

    private SchemaValueContract schema(Shape shape) {
        var items = new SchemaValueContract(Set.of("boolean"), shape.values().stream()
                .map(value -> new ScalarValue("boolean", value.toString())).toList(), null, null, 0, Integer.MAX_VALUE, List.of(), "/schema/items");
        return new SchemaValueContract(Set.of("array"), null, null, null, 0, Integer.MAX_VALUE, List.of(), "/schema",
                null, false, false, new ArrayShape(items, shape.min(), shape.max()));
    }
}
