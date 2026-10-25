package com.openapiguard.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Prueba inclusión conjunta de valores, objetos y arrays en el fragmento soportado.
 * El llamante elige dirección; una ausencia de prueba devuelve UNKNOWN, nunca compatibilidad global.
 */
public final class SchemaInclusion {
    /** Sin estado compartido; cada compare crea sus propias caches. */
    public SchemaInclusion() {}
    /** Inclusión demostrada, refutada o no decidida con la cobertura disponible. */
    public enum Outcome {
        /** Inclusión demostrada. */ INCLUDED,
        /** Inclusión refutada por evidencia. */ EXCLUDED,
        /** Prueba insuficiente; no equivale a inclusión. */ UNKNOWN
    }
    /** Resultado y procedencia; incomplete puede ser true incluso con una ruptura demostrada.
     * @param outcome inclusión demostrada, refutada o desconocida
     * @param reason explicación de la conclusión para revisión humana
     * @param sourcePointer JSON Pointer de la definición en el origen; null si no aplica
     * @param targetPointer JSON Pointer de la definición en el destino; null si no aplica
     * @param incomplete true si persisten lagunas, incluso cuando ya se demostró ruptura
     */
    public record Result(Outcome outcome, String reason, String sourcePointer, String targetPointer, boolean incomplete) {
        /** Crea un resultado sin procedencia específica; UNKNOWN implica cobertura incompleta.
         * @param outcome inclusión demostrada, refutada o desconocida
         * @param reason explicación de la conclusión para revisión humana
         */
        public Result(Outcome outcome, String reason) { this(outcome, reason, null, null, outcome == Outcome.UNKNOWN); }
    }
    /** Límites normalizados usados por la prueba de inclusión.
     * @param lower extremo numérico exacto; null cuando no está acotado
     * @param upper extremo numérico exacto; null cuando no está acotado
     */
    private record Interval(SchemaValueContract.Bound lower, SchemaValueContract.Bound upper) {
        boolean empty() {
            return lower != null && upper != null && (lower.value().compareTo(upper.value()) > 0
                    || lower.value().compareTo(upper.value()) == 0 && (lower.exclusive() || upper.exclusive()));
        }
        boolean singleton() { return !empty() && lower != null && upper != null && lower.value().compareTo(upper.value()) == 0; }
    }

    /** Inicia una evaluación aislada: ninguna cache mutable se comparte entre llamadas públicas.
     * @param source conjunto de valores cuya inclusión se prueba
     * @param target conjunto de valores que debe contener al origen
     * @return prueba de inclusión con procedencia y lagunas
     */
    public Result compare(SchemaValueContract source, SchemaValueContract target) {
        return new Evaluation().compare(source, target);
    }

    /** Caches belong to one evaluation; public calls share no mutable state. */
    private static final class Evaluation {
        private final java.util.Map<Pair, Result> compared = new java.util.HashMap<>();
        private final java.util.Map<SchemaValueContract, Outcome> satisfiableNodes = new java.util.IdentityHashMap<>();
        private final java.util.Map<SchemaValueContract, Boolean> uncertainNodes = new java.util.IdentityHashMap<>();

        /** Clave por identidad de nodos: evita igualdad estructural recursiva de subgrafos compartidos. */
        private static final class Pair {
            private final SchemaValueContract source;
            private final SchemaValueContract target;
            private Pair(SchemaValueContract source, SchemaValueContract target) { this.source = source; this.target = target; }
            /** Compara identidades de los nodos; no expande igualdad estructural recursiva.
             * @param other posible par de nodos con el que comparar identidad
             * @return true cuando coinciden las identidades de los nodos, sin comparación estructural
             */
            @Override public boolean equals(Object other) { return other instanceof Pair pair && source == pair.source && target == pair.target; }
            /** Combina identidades sin recorrer el grafo de esquemas.
             * @return hash de identidades compatible con equals
             */
            @Override public int hashCode() { return 31 * System.identityHashCode(source) + System.identityHashCode(target); }
        }

        /** Memoriza la prueba, completa pointers y conserva lagunas de ambos subárboles. */
        private Result compare(SchemaValueContract source, SchemaValueContract target) {
            var pair = new Pair(source, target);
            var cached = compared.get(pair);
            if (cached != null) return cached;
            var result = compute(source, target);
            var located = new Result(result.outcome(), result.reason(), result.sourcePointer() == null ? source.definitionPointer() : result.sourcePointer(),
                    result.targetPointer() == null ? target.definitionPointer() : result.targetPointer(),
                    result.incomplete() || uncertainty(source) || uncertainty(target));
            compared.put(pair, located);
            return located;
        }

        /** Prueba opacidad, existencia, dominios finitos, tipos y restricciones estructurales, en ese orden. */
        private Result compute(SchemaValueContract source, SchemaValueContract target) {
            if (!source.unsupported().isEmpty() || !target.unsupported().isEmpty()) return unknown("Unsupported constructs: " + source.unsupported() + " / " + target.unsupported());
            if (source.types().contains("object") && objectSatisfiable(source) != Outcome.INCLUDED
                    || target.types().contains("object") && objectSatisfiable(target) != Outcome.INCLUDED)
                return unknown("Satisfiability of required properties has not been established.");
            Set<String> sourceTypes = effectiveTypes(source);
            Set<String> targetTypes = effectiveTypes(target);
            var sourceValues = finiteValues(source, sourceTypes);
            var targetValues = finiteValues(target, targetTypes);
            if (sourceTypes.isEmpty() || targetTypes.isEmpty() || sourceValues != null && sourceValues.isEmpty()
                    || targetValues != null && targetValues.isEmpty()) return unknown("Contradictory schema or schema without valid values.");
            // Un dominio finito permite probar todos sus valores, usando tipo y límites a la vez.
            // Comparar solo los enums brutos ignoraría valores ya excluidos por otras restricciones.
            if (sourceValues != null) {
                return sourceValues.stream().allMatch(predicate(target)) ? included() : excluded("An allowed value no longer satisfies type, enum or bounds.");
            }
            for (String type : new java.util.TreeSet<>(sourceTypes)) {
                if (!targetTypes.contains(type) && !(type.equals("integer") && targetTypes.contains("number"))) {
                    var array = arrayShape(source);
                    if (type.equals("array") && array.minItems() > 0 && array.items() != null
                            && satisfiable(array.items()) != Outcome.INCLUDED)
                        return unknown("A valid array of the excluded type has not been established.");
                    return excluded("Type " + type + " is no longer allowed.");
                }
            }
            // Un origen infinito no puede declararse incluido por comparar listas de enums.
            // Primero buscar un testigo excluido; solo aceptar cuando se demuestra cobertura total.
            if (target.enumValues() != null) {
                if (enumWitness(source, sourceTypes, target)) return excluded("The enum excludes previously allowed values.");
                if (enumCovers(source, sourceTypes, target)) return included();
                return unknown("Complete enum coverage of the value domain has not been established.");
            }
            if (sourceTypes.contains("string") && (source.minLength() < target.minLength() || source.maxLength() > target.maxLength()))
                return excluded("Length bounds exclude allowed values.");
            for (String type : List.of("integer", "number")) {
                if (!sourceTypes.contains(type)) continue;
                var oldRange = interval(source, type.equals("integer"));
                var newRange = interval(target, type.equals("integer"));
                if (!contains(newRange, oldRange)) return excluded("Numeric bounds exclude allowed values.");
            }
            Result object = sourceTypes.contains("object") ? objectInclusion(source, target) : included();
            if (object.outcome() == Outcome.EXCLUDED) return object;
            Result array = sourceTypes.contains("array") ? arrayInclusion(source, target) : included();
            if (array.outcome() == Outcome.EXCLUDED) return array;
            return object.outcome() == Outcome.UNKNOWN ? object : array;
        }

        private ArrayShape arrayShape(SchemaValueContract schema) {
            return schema.arrayShape() == null ? new ArrayShape(null, 0, Integer.MAX_VALUE) : schema.arrayShape();
        }

        /** Si items es imposible, solo puede existir el array vacío; otros máximos se conservan. */
        private int arrayMaximum(SchemaValueContract schema) {
            var shape = arrayShape(schema);
            return shape.items() != null && satisfiable(shape.items()) == Outcome.EXCLUDED ? 0 : shape.maxItems();
        }

        /** No inventa arrays no vacíos cuando no está probada la existencia de un item válido. */
        private Result arrayInclusion(SchemaValueContract source, SchemaValueContract target) {
            var before = arrayShape(source);
            var after = arrayShape(target);
            int oldMax = arrayMaximum(source);
            int newMax = arrayMaximum(target);
            var oldItems = before.items() == null ? unrestricted(source.definitionPointer()) : before.items();
            var newItems = after.items() == null ? unrestricted(target.definitionPointer()) : after.items();
            if (before.minItems() < after.minItems() || oldMax > newMax) {
                if (!(before.minItems() == 0 && after.minItems() > 0) && satisfiable(oldItems) != Outcome.INCLUDED)
                    return unknown("A valid array with the excluded length has not been established.");
                return excluded("Array bounds exclude allowed lengths (minItems/maxItems).");
            }
            if (oldMax == 0) return included();
            if (satisfiable(oldItems) != Outcome.INCLUDED) return unknown("A valid array element has not been established.");
            var result = compare(oldItems, newItems);
            return new Result(result.outcome(), "Array items: " + result.reason(),
                    result.sourcePointer(), result.targetPointer(), result.incomplete());
        }

        private ObjectShape shape(SchemaValueContract schema) {
            return schema.objectShape() == null ? new ObjectShape(java.util.Map.of(), Set.of(), true) : schema.objectShape();
        }

        /** Schema universal para restricciones ausentes; conserva un pointer al contenedor real. */
        private SchemaValueContract unrestricted(String pointer) {
            return new SchemaValueContract(Set.of("string", "boolean", "null", "integer", "number", "object", "array"), null,
                    null, null, 0, Integer.MAX_VALUE, List.of(), pointer);
        }

        /** Compara required, apertura y propiedades efectivas; la ausencia en un objeto abierto permite cualquier valor. */
        private Result objectInclusion(SchemaValueContract source, SchemaValueContract target) {
            var before = shape(source);
            var after = shape(target);
            for (var access : new java.util.TreeMap<>(before.accessModes()).entrySet()) {
                if (after.accessModes().containsKey(access.getKey()) && !access.getValue().equals(after.accessModes().get(access.getKey())))
                    return unknown("Property " + access.getKey() + " changes readOnly/writeOnly; review its effective behavior.");
            }
            var missing = new java.util.TreeSet<>(after.required());
            missing.removeAll(before.required());
            if (!missing.isEmpty()) return new Result(Outcome.EXCLUDED,
                    "The object may omit properties required by the other contract: " + missing + ".",
                    source.definitionPointer(), target.definitionPointer() + "/required", false);
            if (before.additionalProperties() && !after.additionalProperties()) return excluded("The object no longer allows additional property names (additionalProperties).");
            var names = new java.util.TreeSet<>(before.properties().keySet());
            names.addAll(after.properties().keySet());
            Result pending = null;
            for (String name : names) {
                var old = before.properties().get(name);
                var updated = after.properties().get(name);
                if (old == null && !before.additionalProperties()) continue;
                if (updated == null && !after.additionalProperties()) return new Result(Outcome.EXCLUDED, "Property " + name + " is no longer allowed.",
                        old == null ? source.definitionPointer() : old.definitionPointer(), target.definitionPointer(), false);
                var result = compare(old == null ? unrestricted(source.definitionPointer()) : old,
                        updated == null ? unrestricted(target.definitionPointer()) : updated);
                if (result.outcome() == Outcome.EXCLUDED) return new Result(result.outcome(), "Property " + name + ": " + result.reason(),
                        result.sourcePointer(), result.targetPointer(), result.incomplete());
                if (result.outcome() == Outcome.UNKNOWN) pending = result;
            }
            return pending == null ? included() : pending;
        }

        /** Exige evidencia de valores para propiedades requeridas antes de demostrar un contraejemplo de objeto. */
        private Outcome objectSatisfiable(SchemaValueContract schema) {
            var shape = shape(schema);
            for (String name : shape.required()) {
                var property = shape.properties().get(name);
                if (property == null && !shape.additionalProperties()) return Outcome.EXCLUDED;
                if (property != null) {
                    var outcome = satisfiable(property);
                    if (outcome != Outcome.INCLUDED) return outcome;
                }
            }
            return Outcome.INCLUDED;
        }

        /** Memoriza existencia: aquí INCLUDED significa satisfacible, no inclusión entre dos schemas. */
        private Outcome satisfiable(SchemaValueContract schema) {
            var cached = satisfiableNodes.get(schema);
            if (cached != null) return cached;
            var result = calculateSatisfiable(schema);
            satisfiableNodes.put(schema, result);
            return result;
        }

        /** Busca una rama válida del fragmento normalizado sin decidir restricciones desconocidas. */
        private Outcome calculateSatisfiable(SchemaValueContract schema) {
            if (!schema.unsupported().isEmpty()) return Outcome.UNKNOWN;
            var types = effectiveTypes(schema);
            var values = finiteValues(schema, types);
            if (types.isEmpty() || values != null && values.isEmpty()) return Outcome.EXCLUDED;
            if (types.stream().anyMatch(type -> !type.equals("object") && !type.equals("array"))) return Outcome.INCLUDED;
            if (types.contains("array")) {
                var shape = arrayShape(schema);
                if (shape.minItems() == 0 || shape.items() == null) return Outcome.INCLUDED;
                var itemResult = satisfiable(shape.items());
                if (itemResult == Outcome.INCLUDED) return Outcome.INCLUDED;
                if (!types.contains("object")) return itemResult;
            }
            return types.contains("object") ? objectSatisfiable(schema) : Outcome.UNKNOWN;
        }

        /** Propaga lagunas de descendientes alcanzables, con cache por identidad. */
        private boolean uncertainty(SchemaValueContract schema) {
            var cached = uncertainNodes.get(schema);
            if (cached != null) return cached;
            boolean result = !schema.unsupported().isEmpty() || satisfiable(schema) != Outcome.INCLUDED
                    || schema.objectShape() != null && schema.objectShape().properties().values().stream().anyMatch(this::uncertainty)
                    || schema.arrayShape() != null && arrayMaximum(schema) > 0 && schema.arrayShape().items() != null && uncertainty(schema.arrayShape().items());
            uncertainNodes.put(schema, result);
            return result;
        }

        /** Elimina ramas imposibles por límites y canoniza un number singleton entero. */
        private Set<String> effectiveTypes(SchemaValueContract schema) {
            var types = new java.util.HashSet<>(schema.types());
            if (interval(schema, true).empty()) types.remove("integer");
            if (interval(schema, false).empty()) types.remove("number");
            if (schema.minLength() > schema.maxLength()) types.remove("string");
            if (arrayShape(schema).minItems() > arrayMaximum(schema)) types.remove("array");
            if (types.contains("number") && interval(schema, false).singleton()
                    && interval(schema, false).lower().value().stripTrailingZeros().scale() <= 0) {
                types.remove("number");
                types.add("integer");
            }
            return types;
        }

        /** Enumera dominios escalares acotados; null indica que no se obtuvo un dominio finito completo. */
        private List<ScalarValue> finiteValues(SchemaValueContract schema, Set<String> types) {
            if (schema.enumValues() != null) return schema.enumValues().stream().filter(schema::acceptsConstraints).distinct().toList();
            List<ScalarValue> values = new ArrayList<>();
            for (String type : types) {
                switch (type) {
                    case "null" -> values.add(new ScalarValue("null", "null"));
                    case "boolean" -> { values.add(new ScalarValue("boolean", "true")); values.add(new ScalarValue("boolean", "false")); }
                    case "string" -> { if (schema.maxLength() != 0) return null; values.add(new ScalarValue("string", "")); }
                    case "integer", "number" -> {
                        var range = interval(schema, type.equals("integer"));
                        if (range.singleton()) values.add(new ScalarValue("number", range.lower().value().toPlainString()));
                        else if (type.equals("integer") && range.lower() != null && range.upper() != null
                                && range.upper().value().subtract(range.lower().value()).compareTo(BigDecimal.valueOf(1024)) < 0) {
                            for (BigDecimal value = range.lower().value(); value.compareTo(range.upper().value()) <= 0; value = value.add(BigDecimal.ONE))
                                values.add(new ScalarValue("number", value.toPlainString()));
                        } else return null;
                    }
                    default -> { return null; }
                }
            }
            return values.stream().filter(schema::accepts).distinct().toList();
        }

        /** Demuestra exclusión por cardinalidad o testigos acotados; false no demuestra inclusión. */
        private boolean enumWitness(SchemaValueContract source, Set<String> types, SchemaValueContract target) {
            if (types.contains("object") || types.contains("array") || types.contains("number")) return true;
            if (types.contains("integer")) {
                var range = interval(source, true);
                if (range.lower() == null || range.upper() == null) return true;
                long count = target.enumValues().stream().distinct().filter(v -> v.integer() && source.acceptsConstraints(v) && target.acceptsConstraints(v)).count();
                if (range.upper().value().subtract(range.lower().value()).add(BigDecimal.ONE).compareTo(BigDecimal.valueOf(count)) > 0) return true;
            }
            if (types.contains("string") && source.minLength() <= 1024) {
                var acceptsTarget = predicate(target);
                int length = Math.max(1, source.minLength());
                if (length <= source.maxLength()) {
                    for (int codePoint = 32; codePoint < 1056; codePoint++) {
                        var value = new ScalarValue("string", new String(Character.toChars(codePoint)).repeat(length));
                        if (source.acceptsConstraints(value) && !acceptsTarget.test(value)) return true;
                    }
                }
            }
            return false;
        }

        /** Solo afirma cobertura cuando todos los dominios presentes pueden justificarse completamente. */
        private boolean enumCovers(SchemaValueContract source, Set<String> types, SchemaValueContract target) {
            for (String type : types) {
                switch (type) {
                    case "null" -> { if (!target.accepts(new ScalarValue("null", "null"))) return false; }
                    case "boolean" -> { if (!target.accepts(new ScalarValue("boolean", "true")) || !target.accepts(new ScalarValue("boolean", "false"))) return false; }
                    case "integer" -> {
                        var range = interval(source, true);
                        if (range.lower() == null || range.upper() == null) return false;
                        long count = target.enumValues().stream().distinct().filter(v -> v.integer() && source.acceptsConstraints(v) && target.acceptsConstraints(v)).count();
                        if (range.upper().value().subtract(range.lower().value()).add(BigDecimal.ONE).compareTo(BigDecimal.valueOf(count)) != 0) return false;
                    }
                    case "string" -> { if (source.maxLength() != 0 || !target.accepts(new ScalarValue("string", ""))) return false; }
                    default -> { return false; }
                }
            }
            return true;
        }

        /** Convierte extremos de integer a enteros inclusivos; conserva exclusividad para number. */
        private Interval interval(SchemaValueContract schema, boolean integer) {
            var lower = schema.minimum();
            var upper = schema.maximum();
            if (integer) {
                if (lower != null) {
                    BigDecimal value = lower.value().setScale(0, RoundingMode.CEILING);
                    if (lower.exclusive() && value.compareTo(lower.value()) == 0) value = value.add(BigDecimal.ONE);
                    lower = new SchemaValueContract.Bound(value, false);
                }
                if (upper != null) {
                    BigDecimal value = upper.value().setScale(0, RoundingMode.FLOOR);
                    if (upper.exclusive() && value.compareTo(upper.value()) == 0) value = value.subtract(BigDecimal.ONE);
                    upper = new SchemaValueContract.Bound(value, false);
                }
            }
            return new Interval(lower, upper);
        }

        /** Prueba contención de extremos considerando ausencia de límite y exclusividad. */
        private boolean contains(Interval target, Interval source) {
            if (target.lower() != null && (source.lower() == null || source.lower().value().compareTo(target.lower().value()) < 0
                    || source.lower().value().compareTo(target.lower().value()) == 0 && target.lower().exclusive() && !source.lower().exclusive())) return false;
            return target.upper() == null || source.upper() != null && (source.upper().value().compareTo(target.upper().value()) < 0
                    || source.upper().value().compareTo(target.upper().value()) == 0 && (!target.upper().exclusive() || source.upper().exclusive()));
        }

        private Result included() { return new Result(Outcome.INCLUDED, "Values are included."); }
        private Result excluded(String reason) { return new Result(Outcome.EXCLUDED, reason); }
        private Result unknown(String reason) { return new Result(Outcome.UNKNOWN, reason); }

        private java.util.function.Predicate<ScalarValue> predicate(SchemaValueContract schema) {
            Set<ScalarValue> allowed = schema.enumValues() == null ? null : Set.copyOf(schema.enumValues());
            return value -> (allowed == null || allowed.contains(value)) && schema.acceptsConstraints(value);
        }
    }
}
