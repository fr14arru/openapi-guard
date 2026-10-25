package com.openapiguard.adapter.cli;

import com.openapiguard.adapter.openapi.SwaggerContractAnalyzer;
import com.openapiguard.application.CompareContracts;
import com.openapiguard.domain.ComparisonReport;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Coordina opciones, lectura acotada, caso de uso y salida; formato y persistencia son piezas exteriores.
 */
public final class ComparisonCommand {
    private final CompareContracts comparison;
    private final ReportRenderer renderer = new ReportRenderer();

    /**
     * Conserva el caso de uso inyectado salvo selección explícita de perfil.
     * @param comparison comparación predeterminada, independiente de Spring
     */
    public ComparisonCommand(CompareContracts comparison) { this.comparison = comparison; }

    /**
     * Ejecuta comparación o consulta informativa sin cerrar los streams del llamante.
     * Fallos al guardar bloquean aunque la comparación sea compatible. Una ruptura probada
     * conserva código 1 aunque también haya diagnóstico de cobertura parcial.
     * @param args opciones anteriores a dos contratos, o nombres explícitos
     * @param output informe completo sin logs ni confirmaciones adicionales
     * @param errors explicación de errores de argumentos/escritura
     * @return 0 compatible/información, 1 ruptura, 2 fallo, 3 incertidumbre
     */
    public int execute(String[] args, PrintStream output, PrintStream errors) {
        if (args.length == 1 && args[0].equals("--help")) {
            output.println("Usage: java -jar guard.jar [--format=json|--format=text|--format=markdown|--format=ci] [--profile=full|arrays|schemas|schema-values|request-body|parameters|operations] [--output=report.json] <baseline.yaml|json> <candidate.yaml|json>");
            output.println("Alternative: --baseline=old.yaml --candidate=new.yaml; do not mix with positional arguments.");
            output.println("Baseline contract first, candidate contract second. JSON is the default.");
            output.println("Default profile: parameters. All implemented coverage: --profile=full. See --list-profiles.");
            output.println("Exit codes: 0 compatible within the profile; 1 breaking change; 2 invalid input/output; 3 review required.");
            return 0;
        }
        if (args.length == 1 && args[0].equals("--list-profiles")) {
            CliOptions.PROFILES.forEach((alias, profile) -> output.println(alias + " | " + profile.id()
                    + " | Excluded: " + String.join(", ", profile.excludedFacets())));
            return 0;
        }
    CliOptions options;
        try { options = CliOptions.parse(args); }
        catch (IllegalArgumentException exception) { errors.println("Usage: " + exception.getMessage() + " See --help."); return 2; }
        if (options.output() != null) {
            try { ReportFiles.validate(options.output(), options.baseline(), options.candidate()); }
            catch (IOException exception) { errors.println(exception.getMessage()); return 2; }
        }
    var selected = options.profile() == null ? comparison : new CompareContracts(new SwaggerContractAnalyzer(options.profile()));
    ComparisonReport report;
        String candidateContent = null;
        Path reading = options.baseline();
        try {
            String baselineContent = read(reading);
            reading = options.candidate();
            candidateContent = read(reading);
            report = selected.compare(baselineContent, candidateContent);
        }
        catch (IOException | IllegalArgumentException exception) {
            report = ComparisonReport.of(selected.profile(), List.of(), List.of(new ComparisonReport.Diagnostic(
                    "INPUT_READ_FAILED", "cli", reading.toAbsolutePath().normalize().toString(),
                    "Could not read the contract. Check that the file exists, is accessible and contains UTF-8 (maximum 5 MiB). "
                            + exception.getMessage())), true);
        }
        try {
    var content = renderer.render(report, options.format(),
            java.util.Set.of("text", "ci").contains(options.format()) ? new CandidateSource(options.candidate(), candidateContent) : null);
            if (options.output() != null) ReportFiles.write(options.output(), content, options.baseline(), options.candidate());
            output.print(content);
        } catch (IOException exception) { errors.println("Could not write the report: " + exception.getMessage()); return 2; }
        if (report.status() == ComparisonReport.Status.FAILED) return 2;
        return switch (report.verdict()) { case BREAKING -> 1; case COMPATIBLE -> 0; case INDETERMINATE -> 3; };
    }

    /**
     * El byte adicional demuestra exceso sin cargar el archivo completo.
     * @param path contrato local
     * @return contenido UTF-8 estricto sin conversiones con pérdida
     * @throws IOException si la entrada es ilegible, no es UTF-8 o supera 5 MiB
     */
    private static String read(Path path) throws IOException {
        try (var stream = Files.newInputStream(path)) {
    var content = stream.readNBytes(SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES + 1);
            if (content.length > SwaggerContractAnalyzer.MAX_DOCUMENT_BYTES) throw new IOException("Document exceeds 5 MiB.");
            return StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(content)).toString();
        }
    }
}
