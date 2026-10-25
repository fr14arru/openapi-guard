# Primera funcionalidad: operaciones eliminadas

Documento del perfil anterior, mantenido para consumidores Java que seleccionan ComparisonProfile.OPERATIONS_ONLY. La CLI actual utiliza [operations-parameter-presence-v1](parameter-presence-profile.md), que incorpora estas reglas y añade obligatoriedad de parámetros.

Perfil `operations-only-v1`. Swagger Parser v3 2.1.48 lee los contratos; el adaptador convierte únicamente rutas y métodos al modelo propio. `RemovedOperationsRule` y `CompareContracts` no importan Spring, Swagger, Jackson ni archivos. La CLI compone estos objetos mediante Boot.

## Uso

```powershell
mvnd clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/baseline.yaml src/test/resources/openapi/removed.yaml
```

Salida JSON UTF-8 en stdout, logs de aplicación UTF-8 en stderr. Los avisos propios de la JVM pueden usar su codificación nativa. Códigos: 0 sin eliminaciones dentro del perfil; 1 eliminaciones detectadas; 2 entrada inválida/no soportada o error de uso; 3 plantilla renombrada/ambigua sin ruptura probada. El contexto Boot se cierra antes de salir.

Cada hallazgo contiene `id` determinista SHA-256, `ruleId`, `severity=ERROR`, `location` y `description`. Reglas:

- `ENDPOINT_REMOVED`: desaparece una ruta que tenía al menos una operación; un hallazgo de ruta, sin duplicar todos sus métodos.
- `HTTP_METHOD_REMOVED`: la ruta permanece y desaparece uno de sus métodos. GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH y TRACE.

La ubicación contiene ruta, método cuando aplica, JSON Pointer original en baseline y puntero candidato nulo para elementos ausentes. El orden es estable por ruta/método y no depende del orden YAML/JSON ni de metadata.

`ComparisonReport` declara perfil, status, verdict, facetas excluidas, hallazgos y diagnósticos. COMPATIBLE significa solo ausencia de eliminaciones probadas en este perfil; no certifica esquemas, parámetros, respuestas, seguridad o comportamiento. Entradas inválidas dan FAILED/INDETERMINATE sin hallazgos especulativos. Una plantilla renombrada (`{id}` → `{petId}`) o ambigua da PARTIAL; no se genera una falsa eliminación de ruta.

## Límites explícitos

- Se admiten JSON/YAML UTF-8 de familias 3.0.x y 3.1.x, cada una comparada consigo misma. Esto adelanta únicamente la identidad de operaciones en 3.1, no su semántica de esquemas. 3.2/Swagger 2 y comparación entre familias no soportados.
- Se exige `paths` explícito, aunque esté vacío. Path Items deben estar inline; un `$ref` en Path Item se rechaza para no tratar operaciones referenciadas como eliminadas. Referencias internas en otras áreas se comprueban por existencia, sin analizar su semántica.
- Referencias externas se rechazan antes de llamar al parser, que usa `readContents` con resolve/resolveFully desactivados. No se descargan URLs ni se leen archivos referenciados. No se implementa resolución externa.
- La revisión de `$ref` es conservadora en todo el árbol, incluidos ejemplos/extensiones. Fragmentos URI codificados o referencias distintas de `#/...` quedan fuera; no se promete equivalencia de esas representaciones todavía.
- Máximo 5 MiB, profundidad 128, claves duplicadas y documentos múltiples rechazados. Alias YAML de escalares y colecciones rechazados mediante los tokens del parser Jackson antes de construir el árbol; el límite de alias de LoaderOptions por sí solo no protege este recorrido. No se ha medido un SLA ni aislado el parser en procesos.
- Diagnósticos del parser provocan fallo conservador, incluso advertencias; no confundir parsing satisfactorio con una validación semántica OpenAPI completa.
- Cambios en templates ambiguos no se resuelven heurísticamente. Rutas movidas de otra forma se consideran eliminadas bajo identidad literal.
- Fuera de alcance todas las otras reglas. No hay persistencia, HTTP, Docker ni servicios externos.

## Pruebas

Unitarias del dominio para ruta/métodos, orden, IDs, equivalencia, altas y templates. Fixtures YAML/JSON para comparación a través del caso de uso sin Spring, familias 3.0/3.1 e inputs inválidos/no soportados. Pruebas CLI para JSON/códigos y proceso Boot que termina, forzando streams nativos Windows-1252 y verificando JSON UTF-8 con acentos. Regresiones para alias de secuencia, mapa y escalar. Las 36 pruebas pasan; ver [verificación](development.md).
