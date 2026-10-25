# Perfil de presencia y representaciones de petición

El incremento D05 añade el perfil `operations-parameters-request-body-v1`, seleccionable con `--profile=request-body`. Conserva el perfil predeterminado anterior; para la cobertura conjunta D05/D06/D07 usar `--profile=schemas`. Incluye sus reglas de operaciones y parámetros, y estas reglas nuevas:

| Regla | Condición |
| --- | --- |
| REQUEST_BODY_REQUIRED_ADDED | Una operación existente incorpora un cuerpo obligatorio |
| REQUEST_BODY_BECAME_REQUIRED | El cuerpo pasa de opcional a obligatorio |
| REQUEST_MEDIA_TYPE_REMOVED | Un formato/rango anterior deja de estar cubierto por los rangos candidatos |

La ausencia de `required` equivale a false. La retirada completa del cuerpo produce `REQUEST_BODY_REMOVAL_REQUIRES_REVIEW`: su ausencia no demuestra rechazo en ejecución. No comparar cuerpos de operaciones nuevas/eliminadas ni plantillas ambiguas. El núcleo mantiene reglas puras y snapshots inmutables; el adaptador extrae únicamente presencia, formatos y procedencia.

El perfil prueba cuerpos en POST/PUT/PATCH. En GET/HEAD/DELETE/OPTIONS/TRACE la semántica de cuerpo se marca como revisión (`REQUEST_BODY_METHOD_REQUIRES_REVIEW`), sin afirmar ruptura de obligatoriedad. Esta política conservadora se mantiene en 3.0 y 3.1: el permiso documental no demuestra comportamiento HTTP. Ver [Operation Object 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#operation-object) y [3.1.1](https://spec.openapis.org/oas/v3.1.1.html#operation-object). Se añadió la regresión al auditar D07, reproduciendo seis falsos positivos antes de corregirlos.

La cobertura de formatos admite tipos concretos, `type/*` y `*/*`, sin distinguir mayúsculas. Los parámetros de media type y otros patrones producen revisión explícita. Las referencias internas a cuerpos conservan punteros de definición y uso; ciclos o cadenas de más de 64 referencias fallan sin resultados especulativos. No se resuelven archivos ni URLs.

Los esquemas de los cuerpos siguen excluidos de este perfil: un cambio de esquema bajo una representación cubierta no se certifica. El perfil ampliado `--profile=schemas` compara esquemas en usos JSON soportados; los solapamientos de rangos que no puede resolver producen revisión. No basta comparar claves de content para certificar sus datos. Tampoco se analizan todavía encoding ni negociación de respuestas.

Ejemplo reproducible:

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=request-body --format=text src/test/resources/openapi/body-optional.yaml src/test/resources/openapi/body-required.yaml
```

Resultado: dos rupturas, cuerpo obligatorio y retirada de text/plain. La comparación inversa es compatible en este perfil. Cada hallazgo contiene ID determinista, severidad ERROR, operación, punteros y descripción. Para formatos retirados, el puntero candidato señala content, sin inventar una entrada inexistente.

Referencia normativa: [Request Body Object de OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#request-body-object), que define obligatoriedad por defecto y rangos con precedencia por especificidad.

TDD: los primeros diez casos fallaron al no existir el perfil; la selección desde CLI tuvo después su propia fase roja. Hay matrices de obligatoriedad/cobertura, referencias, ciclos, dialectos, eliminación de operaciones y representación incierta, más ejecución del JAR con JSON y texto.
