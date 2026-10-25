package com.openapiguard.adapter.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openapiguard.domain.ComparisonReport;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Presentación exterior del mismo informe; ningún formato cambia cobertura o veredicto.
 */
final class ReportRenderer {
    private final ObjectMapper json = new ObjectMapper();

    /**
     * Produce un informe completo con salto final, listo para stdout o archivo UTF-8.
     * @param report informe inmutable del motor
     * @param format json, text, markdown o ci, previamente validado
     * @return representación que conserva diagnósticos y exclusiones
     * @throws IOException si la serialización JSON falla
     */
    String render(ComparisonReport report, String format) throws IOException {
        return render(report, format, null);
    }

    /** Añade navegación de presentación sin modificar el informe JSON ni la semántica del motor. */
    String render(ComparisonReport report, String format, CandidateSource source) throws IOException {
        if (format.equals("json")) return json.writerWithDefaultPrettyPrinter().writeValueAsString(report) + System.lineSeparator();
        if (format.equals("ci")) return json.writerWithDefaultPrettyPrinter().writeValueAsString(ci(report, source)) + System.lineSeparator();
        if (format.equals("markdown")) return markdown(report);
    var bytes = new ByteArrayOutputStream();
        try (var stream = new PrintStream(bytes, true, StandardCharsets.UTF_8)) { printText(report, stream, source); }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    /** Envoltura versionada para integraciones: conserva el JSON semántico y añade posiciones exteriores. */
    private static Map<String, Object> ci(ComparisonReport report, CandidateSource source) {
        var findings = new ArrayList<Map<String, Object>>();
        for (var change : report.changes()) findings.add(Map.of(
                "id", change.id(), "ruleId", change.ruleId(), "severity", change.severity().name(),
                "source", "candidate", "description", change.description(),
                "location", change.location(),
                "positions", source == null ? List.of() : source.coordinates(change)));
        int number = 1;
        for (var diagnostic : report.diagnostics()) findings.add(Map.of(
                "id", "diagnostic-" + number++, "ruleId", diagnostic.code(),
                "severity", report.status() == ComparisonReport.Status.FAILED ? "ERROR" : "WARNING",
                "source", diagnostic.source(), "description", diagnostic.description(),
                "positions", source != null && diagnostic.source().equals("candidate")
                        ? source.coordinates(diagnostic.location(), "diagnostic") : List.of()));
        return Map.of("formatVersion", 1, "positionLimit", 8, "report", report, "findings", findings);
    }

    /**
     * Escapa datos del contrato: no permitir HTML, tablas o enlaces introducidos por rutas/diagnósticos.
     */
    private static String escape(Object value) {
        if (value == null) return "";
    var escaped = new StringBuilder();
        for (char c : value.toString().toCharArray()) {
            if (c == '\n' || c == '\r') escaped.append(' ');
            else if ("&<>|`[]*_\\".indexOf(c) >= 0) escaped.append("&#").append((int)c).append(';');
            else escaped.append(c);
        }
        return escaped.toString();
    }

    /**
     * Incluye identidad y ambas procedencias para que el Markdown sea útil en revisión de PR.
     */
    private static String markdown(ComparisonReport report) {
    var text = new StringBuilder("# OpenAPI Breaking Change Guard\n\n");
        text.append("Result: ").append(report.verdict()).append(" | Status: ").append(report.status())
                .append("\n\nProfile: ").append(escape(report.profileId())).append("\n\n## Changes\n\n");
        if (report.changes().isEmpty()) text.append("No breaking changes were demonstrated within the profile.\n\n");
        for (var change : report.changes()) {
            text.append("### ").append(escape(change.ruleId())).append("\n\n")
                .append("ID: ").append(escape(change.id())).append(" | Severity: ").append(change.severity()).append("\n\n")
                .append(escape(change.description())).append("\n\n");
            var location = change.location();
            text.append("Operation: ").append(escape(location.method())).append(" ")
                .append(escape(location.path())).append("\n\n");
            if (location.direction() != null) text.append("Direction: ").append(escape(location.direction())).append("\n\n");
            if (location.parameter() != null) text.append("Parameter: ").append(escape(location.parameter().in()))
                .append(" / ").append(escape(location.parameter().name())).append("\n\n");
            if (location.responseCode() != null) text.append("Response: ").append(escape(location.responseCode())).append("\n\n");
            if (location.mediaType() != null) text.append("Media type: ").append(escape(location.mediaType())).append("\n\n");
            text.append("Baseline definition: ").append(escape(location.baselinePointer())).append("\n\n")
                .append("Candidate definition: ").append(escape(location.candidatePointer())).append("\n\n")
                .append("Baseline usage: ").append(escape(location.baselineUsagePointer())).append("\n\n")
                .append("Candidate usage: ").append(escape(location.candidateUsagePointer())).append("\n\n");
        }
        text.append("## Diagnostics\n\n");
        for (var diagnostic : report.diagnostics()) text.append("- ").append(escape(diagnostic.code())).append(": ")
                .append(escape(diagnostic.description())).append(" | ").append(escape(diagnostic.source()))
                .append(" ").append(escape(diagnostic.location())).append('\n');
        text.append("\n## Excluded facets\n\n").append(escape(String.join(", ", report.excludedFacets())))
                .append("\n\nThe result is limited to the profile; it does not certify overall API compatibility.\n");
        return text.toString();
    }
    /**
     * Presenta los mismos datos del JSON para lectura humana; no es un protocolo de automatización.
     */
    private static void printText(ComparisonReport report, PrintStream output, CandidateSource source) {
        output.println("Result: " + report.verdict() + " | Status: " + report.status());
        output.println("Profile: " + report.profileId());
        output.println("Breaking changes detected: " + report.changes().size());
        int number = 1;
        for (var change : report.changes()) {
    var location = change.location();
            output.println(CandidateSource.single("Error " + number++ + " - "
                    + (source == null ? "Lines unavailable" : source.lineSummary(change))
                    + " | [" + change.severity() + "] " + change.ruleId() + " | "
                    + (location.method() == null ? "" : location.method() + " ") + location.path()
                    + " | " + change.description()
                    + (location.direction() == null ? "" : " | Direction: " + location.direction())
                    + (location.responseCode() == null ? "" : " | Response: " + location.responseCode())
                    + (location.mediaType() == null ? "" : " | Media type: " + location.mediaType())
                    + " | " + (source == null ? "Candidate: " + location.candidatePointer() : source.describe(change))
                    + (location.candidateUsagePointer() == null || location.candidateUsagePointer().equals(location.candidatePointer())
                    || source == null ? "" : " | Candidate usage: " + source.describe(location.candidateUsagePointer()))));
        }
        for (var diagnostic : report.diagnostics()) {
            output.println(CandidateSource.single("Error " + number++ + " - "
                    + (source != null && diagnostic.source().equals("candidate")
                    ? source.lineSummary(diagnostic.location()) : "Lines unavailable")
                    + " | Review: " + diagnostic.code() + " | " + diagnostic.source()
                    + " " + (source != null && diagnostic.source().equals("candidate")
                    ? source.describe(diagnostic.location()) : diagnostic.location()) + " | " + diagnostic.description()));
        }
        output.println("Excluded facets: " + String.join(", ", report.excludedFacets()));
        output.println("The result is limited to the profile; it does not certify overall API compatibility.");
    }

}
