package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.dataformat.yaml.*;
import com.openapiguard.domain.DetectedChange;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import org.yaml.snakeyaml.LoaderOptions;

/** Índice de presentación sobre el texto original; nunca resuelve referencias ni decide compatibilidad. */
final class CandidateSource {
    private final Path path;
    private final Map<String, JsonLocation> positions = new HashMap<>();
    private final String[] lines;

    /** Conserva claves y elementos de arrays antes de que el parser de dominio normalice el contrato. */
    CandidateSource(Path path, String content) {
        this.path = path.toAbsolutePath().normalize();
        lines = content == null ? new String[0] : content.split("\\R", -1);
        if (content == null) return;
        var options = new LoaderOptions();
        options.setMaxAliasesForCollections(0);
        options.setNestingDepthLimit(128);
        options.setCodePointLimit(5 * 1024 * 1024);
        var factory = YAMLFactory.builder().loaderOptions(options)
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(128).build())
                .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
        try (var parser = factory.createParser(content)) {
            if (parser.nextToken() != null) visit(parser, "");
            if (parser.nextToken() != null) positions.clear();
        } catch (IOException | RuntimeException exception) {
            // Nunca publicar posiciones parciales de un documento inválido o alias expandido.
            positions.clear();
        }
    }

    /** Recorrido acotado por Jackson; claves RFC 6901, índices físicos y marcas originales. */
    private void visit(YAMLParser parser, String pointer) throws IOException {
        if (parser.isCurrentAlias()) throw new IOException("Unsupported alias");
        if (positions.size() > 100_000) throw new IOException("Source position budget exceeded");
        positions.putIfAbsent(pointer, parser.currentTokenLocation());
        if (parser.currentToken() == JsonToken.START_OBJECT) {
            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) throw new IOException("Incomplete object");
                String child = pointer + "/" + token(parser.currentName());
                positions.put(child, parser.currentTokenLocation());
                if (parser.nextToken() == null) throw new IOException("Missing value");
                visit(parser, child);
            }
        } else if (parser.currentToken() == JsonToken.START_ARRAY) {
            int index = 0;
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                if (parser.currentToken() == null) throw new IOException("Incomplete array");
                visit(parser, pointer + "/" + index++);
            }
        }
    }

    /** Prioriza definición candidata; si desaparece, retrocede hasta un contenedor existente. */
    String describe(DetectedChange change) {
        return describe(pointer(change));
    }

    /** Selecciona el nodo que explica el cambio, exclusivamente en el candidato. */
    private static String pointer(DetectedChange change) {
        var location = change.location();
        String pointer = location.candidatePointer();
        if (pointer != null && (change.ruleId().equals("PARAMETER_BECAME_REQUIRED")
                || change.ruleId().equals("REQUIRED_PARAMETER_ADDED"))) pointer += "/required";
        if (pointer == null) pointer = "/paths/" + token(location.path())
                + (location.method() == null ? "" : "/" + location.method().name().toLowerCase(Locale.ROOT));
        return pointer;
    }

    /** Líneas de contexto ordenadas y únicas de definición y uso; no son un diff de keywords. */
    String lineSummary(DetectedChange change) {
        var relevant = relevantLines(pointer(change));
        String usage = change.location().candidateUsagePointer();
        if (usage != null && !usage.equals(change.location().candidatePointer())) relevant.addAll(relevantLines(usage));
        return summary(relevant);
    }

    /** No atribuye posiciones a documentos ilegibles ni a entradas anteriores. */
    String lineSummary(String pointer) { return summary(relevantLines(pointer)); }

    /** Coordenadas de presentación para CI; nunca incluyen rutas temporales ni posiciones del baseline. */
    List<Map<String, Object>> coordinates(DetectedChange change) {
        var result = new TreeMap<Integer, Map<String, Object>>();
        coordinates(pointer(change), "definition").forEach(position -> result.put((Integer) position.get("line"), position));
        String usage = change.location().candidateUsagePointer();
        if (usage != null && !usage.equals(change.location().candidatePointer()))
            coordinates(usage, "usage").forEach(position -> result.putIfAbsent((Integer) position.get("line"), position));
        return result.values().stream().limit(8).toList();
    }

    /** Para elementos ausentes solo anota el contenedor, sin señalar sus hijos como culpables. */
    List<Map<String, Object>> coordinates(String pointer, String role) {
        if (positions.isEmpty()) return List.of();
        String requested = pointer == null ? "" : pointer;
        String found = requested;
        while (!positions.containsKey(found) && !found.isEmpty()) found = found.substring(0, found.lastIndexOf('/'));
        var mark = positions.get(found);
        if (mark == null || mark.getLineNr() < 1) return List.of();
        boolean approximate = !found.equals(requested);
        var relevant = approximate ? new TreeSet<>(Set.of(mark.getLineNr())) : relevantLines(found);
        return relevant.stream().limit(8).map(line -> {
            Map<String, Object> position = new LinkedHashMap<>();
            position.put("line", line);
            position.put("column", line == mark.getLineNr() ? Math.max(1, mark.getColumnNr()) : 1);
            position.put("approximate", approximate);
            position.put("role", line == mark.getLineNr() ? role : "context");
            return position;
        }).toList();
    }

    private static String summary(SortedSet<Integer> relevant) {
        if (relevant.isEmpty()) return "Lines unavailable";
        return "Lines " + String.join(", ", relevant.stream().limit(8).map(String::valueOf).toList())
                + (relevant.size() > 8 ? " (context truncated)" : "");
    }

    /** El ancestro existente permite navegar cuando el elemento fue eliminado. */
    private SortedSet<Integer> relevantLines(String pointer) {
        var relevant = new TreeSet<Integer>();
        if (positions.isEmpty()) return relevant;
        String found = pointer == null ? "" : pointer;
        while (!positions.containsKey(found) && !found.isEmpty()) found = found.substring(0, found.lastIndexOf('/'));
        var mark = positions.get(found);
        if (mark == null || mark.getLineNr() < 1) return relevant;
        relevant.add(mark.getLineNr());
        String prefix = found + "/";
        positions.forEach((key, position) -> {
            if (key.startsWith(prefix) && position.getLineNr() > 0) relevant.add(position.getLineNr());
        });
        return relevant;
    }

    /** No inventa líneas: contenedor explícito si falta el nodo; archivo solo si no hay índice. */
    String describe(String pointer) {
        if (positions.isEmpty()) return single(path.toString()) + " [line unavailable]";
        String requested = pointer == null ? "" : pointer;
        String found = requested;
        while (!positions.containsKey(found) && !found.isEmpty()) {
            found = found.substring(0, found.lastIndexOf('/'));
        }
        var mark = positions.get(found);
        if (mark == null || mark.getLineNr() < 1) return single(path.toString()) + " [line unavailable]";
        int line = mark.getLineNr();
        return single(path.toString()) + ":" + line + ":" + Math.max(1, mark.getColumnNr())
                + (found.equals(requested) ? "" : " [container; element absent]")
                + " | " + excerpt(found, line);
    }

    /** Muestra el nodo comprometido, sin atribuir a una única keyword una prueba de constraints conjuntas. */
    private String excerpt(String pointer, int line) {
        var relevant = new TreeSet<Integer>();
        relevant.add(line);
        positions.forEach((key, mark) -> {
            if (key.startsWith(pointer + "/") && mark.getLineNr() > 0) relevant.add(mark.getLineNr());
        });
        var excerpt = new StringJoiner(" ; ");
        relevant.stream().limit(8).filter(number -> number <= lines.length)
                .forEach(number -> excerpt.add("L" + number + ": " + single(lines[number - 1].strip())));
        if (relevant.size() > 8) excerpt.add("... context truncated; open the contract");
        return excerpt.toString();
    }

    private static String token(String value) { return value.replace("~", "~0").replace("/", "~1"); }

    /** Mantiene un hallazgo por línea y evita secuencias de control del contrato en la terminal. */
    static String single(String value) {
        if (value == null) return "";
        var result = new StringBuilder();
        value.codePoints().forEach(c -> result.appendCodePoint(Character.isISOControl(c) || c == 0x2028 || c == 0x2029 ? ' ' : c));
        return result.toString();
    }
}
