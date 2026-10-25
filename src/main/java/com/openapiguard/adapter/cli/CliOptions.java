package com.openapiguard.adapter.cli;

import com.openapiguard.domain.ComparisonProfile;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Opciones de una comparación, sin lectura de archivos ni ensamblado de Spring.
 * Mantiene los dos argumentos posicionales históricos y rechaza mezclarlos con nombres.
 * @param baseline contrato anterior
 * @param candidate contrato propuesto
 * @param format representación del informe
 * @param profile perfil explícito, o null para conservar el caso de uso inyectado
 * @param output destino opcional, o null para usar solamente stdout
 */
record CliOptions(Path baseline, Path candidate, String format, ComparisonProfile profile, Path output) {
    /**
     * Alias CLI estables; el orden también define la presentación de --list-profiles.
     */
    static final Map<String, ComparisonProfile> PROFILES = profiles();

    private static Map<String, ComparisonProfile> profiles() {
    var profiles = new LinkedHashMap<String, ComparisonProfile>();
        profiles.put("full", ComparisonProfile.RESPONSE_POLICY);
        profiles.put("arrays", ComparisonProfile.ARRAY_SCHEMAS);
        profiles.put("schemas", ComparisonProfile.OBJECT_SCHEMAS);
        profiles.put("schema-values", ComparisonProfile.SCHEMA_VALUES);
        profiles.put("request-body", ComparisonProfile.REQUEST_BODY);
        profiles.put("parameters", ComparisonProfile.PARAMETER_PRESENCE);
        profiles.put("operations", ComparisonProfile.OPERATIONS_ONLY);
        return java.util.Collections.unmodifiableMap(profiles);
    }

    /**
     * Analiza opciones anteriores a los contratos; las opciones repetidas no tienen precedencia implícita.
     * @param args argumentos originales de CLI
     * @return opciones válidas sin efectuar E/S
     * @throws IllegalArgumentException si falta un contrato, una opción es desconocida o se mezclan estilos
     */
    static CliOptions parse(String[] args) {
    var options = new LinkedHashMap<String, String>();
    int index = 0;
        while (index < args.length && args[index].startsWith("--")) {
    var option = args[index++]; int equals = option.indexOf('=');
            if (equals < 0) throw new IllegalArgumentException("Options require --name=value.");
    var key = option.substring(2, equals); var value = option.substring(equals + 1);
            if (!java.util.Set.of("baseline", "candidate", "profile", "format", "output").contains(key)
                    || value.isEmpty() || options.putIfAbsent(key, value) != null)
                throw new IllegalArgumentException("Unknown, empty or repeated option: " + key);
        }
    var format = options.getOrDefault("format", "json");
        if (!java.util.Set.of("json", "text", "markdown", "ci").contains(format)) throw new IllegalArgumentException("Unknown format.");
    ComparisonProfile profile = null;
        if (options.containsKey("profile")) {
            profile = PROFILES.get(options.get("profile"));
            if (profile == null) throw new IllegalArgumentException("Unknown profile.");
        }
    boolean named = options.containsKey("baseline") || options.containsKey("candidate");
        if (named && (index != args.length || !options.containsKey("baseline") || !options.containsKey("candidate"))
                || !named && args.length - index != 2) throw new IllegalArgumentException("Specify exactly two contracts, either positional or named.");
        return new CliOptions(Path.of(named ? options.get("baseline") : args[index]),
                Path.of(named ? options.get("candidate") : args[index + 1]), format, profile,
                options.containsKey("output") ? Path.of(options.get("output")) : null);
    }
}
