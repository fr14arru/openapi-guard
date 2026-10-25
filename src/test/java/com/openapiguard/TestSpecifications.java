package com.openapiguard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Carga fixtures pequeñas del classpath, sin rutas dependientes del equipo. */
public final class TestSpecifications {
    private TestSpecifications() {}

    /** Lee openapi/name como UTF-8; la ausencia del recurso falla explícitamente. */
    public static String fixture(String name) throws IOException {
        try (var stream = TestSpecifications.class.getResourceAsStream("/openapi/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture " + name);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
