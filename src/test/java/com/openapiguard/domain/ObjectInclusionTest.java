package com.openapiguard.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

/** Oráculo independiente para objetos abiertos/cerrados, presencia y valores de propiedades. */
class ObjectInclusionTest {
    record Shape(boolean open, boolean property, boolean required, Set<Boolean> values) {}

    @ParameterizedTest @MethodSource("pairs")
    void inclusionMatchesIndependentObjectOracle(Shape old, Shape updated) {
        boolean included = objects().stream().filter(value -> accepts(old, value)).allMatch(value -> accepts(updated, value));
        var result = new SchemaInclusion().compare(schema(old), schema(updated));
        assertEquals(included ? SchemaInclusion.Outcome.INCLUDED : SchemaInclusion.Outcome.EXCLUDED, result.outcome());
        assertFalse(result.incomplete());
    }

    static Stream<Arguments> pairs() {
        var shapes = new ArrayList<Shape>();
        for (boolean open : List.of(false, true)) {
            shapes.add(new Shape(open, false, false, Set.of()));
            for (boolean required : List.of(false, true)) {
                for (Set<Boolean> values : List.of(Set.of(true), Set.of(false), Set.of(true, false))) shapes.add(new Shape(open, true, required, values));
            }
        }
        return shapes.stream().flatMap(old -> shapes.stream().map(updated -> Arguments.of(old, updated)));
    }

    private List<Map<String, Object>> objects() {
        // A missing property is distinct from an explicit null value.
        var objects = new ArrayList<Map<String, Object>>();
        List<Object> values = Arrays.asList(true, false, 0, "text", null, List.of(), Map.of());
        objects.add(Map.of());
        for (Object x : values) {
            var named = new HashMap<String, Object>(); named.put("x", x); objects.add(named);
            var additional = new HashMap<String, Object>(); additional.put("other", x); objects.add(additional);
            for (Object other : values) { var both = new HashMap<String, Object>(named); both.put("other", other); objects.add(both); }
        }
        return objects;
    }

    private boolean accepts(Shape schema, Map<String, Object> value) {
        if (schema.required() && !value.containsKey("x")) return false;
        for (var entry : value.entrySet()) {
            if (entry.getKey().equals("x") && schema.property()) {
                if (!(entry.getValue() instanceof Boolean bool) || !schema.values().contains(bool)) return false;
            } else if (!schema.open()) return false;
        }
        return true;
    }

    private SchemaValueContract schema(Shape shape) {
        var properties = new HashMap<String, SchemaValueContract>();
        if (shape.property()) properties.put("x", new SchemaValueContract(Set.of("boolean"), shape.values().stream()
                .map(value -> new ScalarValue("boolean", value.toString())).toList(), null, null, 0, Integer.MAX_VALUE, List.of(), "/schema/properties/x"));
        return new SchemaValueContract(Set.of("object"), null, null, null, 0, Integer.MAX_VALUE, List.of(), "/schema",
                new ObjectShape(properties, shape.required() ? Set.of("x") : Set.of(), shape.open()), false, false);
    }
}
