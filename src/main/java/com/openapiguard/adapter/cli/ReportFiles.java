package com.openapiguard.adapter.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * Persistencia local de presentación; no forma parte del motor ni añade un puerto de dominio.
 */
final class ReportFiles {
    private ReportFiles() {}

    /**
     * Impide reemplazar entradas, incluyendo aliases por symlink o hardlink existentes.
     * @param destination archivo del informe
     * @param inputs contratos que deben conservarse
     * @throws IOException si el destino coincide con una entrada o es un directorio
     */
    static void validate(Path destination, Path... inputs) throws IOException {
    var normalized = destination.toAbsolutePath().normalize();
        if (Files.isDirectory(normalized)) throw new IOException("The report must be a file.");
        for (var input : inputs) {
            if (normalized.equals(input.toAbsolutePath().normalize())
                    || Files.exists(normalized) && Files.exists(input) && Files.isSameFile(normalized, input))
                throw new IOException("The report cannot overwrite an input contract.");
        }
    }

    /**
     * Escribe UTF-8 sin BOM y reemplaza el destino solo después de completar el temporal.
     * Se intenta movimiento atómico; sistemas que no lo soportan usan reemplazo normal.
     * No garantiza atomicidad en esos sistemas ni protección frente a modificaciones concurrentes hostiles.
     * @param destination archivo solicitado
     * @param content informe completo
     * @param inputs entradas protegidas
     * @throws IOException si no puede crear, escribir o mover el archivo
     */
    static void write(Path destination, String content, Path... inputs) throws IOException {
        validate(destination, inputs);
    var absolute = destination.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
    var temporary = Files.createTempFile(absolute.getParent(), ".guard-report-", ".tmp");
        try {
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            validate(destination, inputs);
            try { Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unavailable) { Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
