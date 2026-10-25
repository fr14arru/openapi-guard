# Política de respuestas: D09

`--profile=full` selecciona `operations-data-response-policy-v1`, reúne D05–D08 y añade la disponibilidad de respuestas documentadas. El nombre full significa toda la cobertura implementada, no todo OpenAPI. Los perfiles anteriores conservan sus identificadores y semántica; el predeterminado sigue siendo parámetros.

## Política publicada

| Cambio | Conclusión |
| --- | --- |
| Retirada de código concreto sin fallback aplicable | RESPONSE_STATUS_REMOVED, ERROR |
| Retirada de formato concreto de un código compartido, sin comodines | RESPONSE_MEDIA_TYPE_REMOVED, ERROR |
| Nuevo código concreto | RESPONSE_STATUS_ADDED_REQUIRES_REVIEW |
| Nuevo formato o cuerpo antes no documentado | RESPONSE_MEDIA_TYPE_ADDED_REQUIRES_REVIEW |
| default/rango, incluso sin cuerpo y sin cambios | Análisis parcial: precedencia/cobertura efectiva pendiente |
| Formatos con comodines o parámetros | Revisión de cobertura y precedencia |
| Cuerpo JSON de código/formato compartido | Inclusión de respuesta new ⊆ old, con valores/objetos/arrays soportados |

Las dos reglas de retirada son una política explícita de disponibilidad documental: se conserva el conjunto de códigos y representaciones publicados. No son una demostración de expansión del conjunto de valores de respuesta ni prueban que un servidor real deje de producirlos. La explicación humana identifica esa política. Las adiciones pueden afectar consumidores o negociación, pero no se inventa una ruptura universal.

Una respuesta concreta retirada que puede cubrirse por default o su rango correspondiente solicita revisión. Un rango de otra clase no oculta una retirada demostrada. El perfil no interpreta schemas de fallback ni certifica sus equivalencias. En OpenAPI la definición concreta tiene precedencia sobre el rango y la representación más específica sobre un comodín; esas interacciones no se sustituyen por equivalencia textual. [Responses y Response Objects oficiales](https://spec.openapis.org/oas/v3.0.4.html#responses-object).

Se analizan códigos sin cuerpo. Los formatos se normalizan por mayúsculas conservando los punteros originales. Las referencias internas de Response Objects conservan definición y uso; las cíclicas/externas/ausentes fallan sin hallazgos especulativos. Retirar una operación no duplica sus retiradas de respuestas.

## Límites del informe

Se emite el perfil versionado, status COMPLETED/PARTIAL/FAILED y verdict COMPATIBLE/BREAKING/INDETERMINATE. Una ruptura demostrada puede coexistir con PARTIAL. Los códigos de proceso siguen siendo 0/1/2/3; código 3 debe impedir certificación automática hasta revisión. Headers/links de respuesta, negociación efectiva, serialización compleja, seguridad y demás facetas excluidas no se certifican. Construcciones opacas en datos comprometidos producen revisión.

## Ejemplo reproducible

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=text src/test/resources/openapi/responses-old.yaml src/test/resources/openapi/responses-new.yaml
$LASTEXITCODE
```

Dos rupturas: retirada de application/json para 200 y retirada de 204, código 1. Comparar responses-old consigo mismo da código 0. La comparación inversa añade código/formato y da revisión, código 3.

## TDD

Trece casos nuevos fallaron por ausencia del perfil antes de implementarlo. Otros ciclos rojos/verdes cubren la selección CLI y un diagnóstico de petición incorrecto al retirar un schema de respuesta en una operación con requestBody. Hay pruebas del dominio sin parser para IDs/inmutabilidad y conservación de una ruptura con cobertura parcial. Las pruebas del JAR real verifican ambos dialectos, códigos, JSON/texto, determinismo, equivalencia y revisión. Los contratos son sintéticos, no una validación comercial.
