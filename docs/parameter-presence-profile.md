# Perfil de operaciones y presencia de parámetros

Perfil predeterminado de la CLI: `operations-parameter-presence-v1`. Implementa D03a, D03b y la validación técnica D03c sobre el mismo núcleo Java y adaptadores. No compara el contenido de las respuestas ni añade infraestructura.

## Extracción efectiva

La ruta y el método identifican la operación. Los parámetros se heredan del Path Item y una definición en la operación sustituye la heredada con la misma identidad. Una lista local vacía conserva la herencia. Se conserva tanto el JSON Pointer de la definición como el del uso; para un parámetro referenciado pueden ser distintos.

Identidad: `(name, in)`, normalizando únicamente los nombres de headers a minúsculas con Locale.ROOT. Query/path/cookie conservan mayúsculas. Se ignoran los headers Accept, Content-Type y Authorization con cualquier casing. La política sigue [OpenAPI 3.0.4: parámetros](https://spec.openapis.org/oas/v3.0.4.html#parameter-object) y [3.1.1: parámetros](https://spec.openapis.org/oas/v3.1.1.html#parameter-object).

`required` ausente equivale a false para query/header/cookie; un parámetro path exige true y debe corresponder a la plantilla. El parser valida la estructura antes de extraer. El adaptador comprueba además duplicados efectivos, booleanos, identidad y exclusividad schema/content, evitando coerciones silenciosas del modelo de terceros.

Las referencias internas a parámetros se leen del árbol original sin dereferenciar todo el documento. Se conservan origen y uso, se detectan ciclos y se limita el recorrido a 64 referencias. Los siblings de Reference Object no sustituyen name/in/required de su destino. Continúan rechazadas las referencias externas, inexistentes y los Path Items con `$ref`.

La instantánea de dominio contiene solo identidad, obligatoriedad, procedencia y un resumen de serialización/presencia. No contiene JsonNode, Swagger ni anotaciones Spring. Los cambios de esquemas pertenecen a un incremento posterior.

## Catálogo de reglas v1

| Regla | Condición | Resultado |
| --- | --- | --- |
| ENDPOINT_REMOVED | Desaparece una ruta con operaciones | ERROR/BREAKING |
| HTTP_METHOD_REMOVED | Desaparece un método de una ruta existente | ERROR/BREAKING |
| REQUIRED_PARAMETER_ADDED | En una operación existente aparece un parámetro obligatorio con presencia soportada | ERROR/BREAKING |
| PARAMETER_BECAME_REQUIRED | El parámetro efectivo pasa de opcional a obligatorio y conserva la serialización soportada | ERROR/BREAKING |

Las reglas de parámetros son de petición. Añadir una operación no rompe por sus parámetros nuevos; eliminarla produce solo los hallazgos de operaciones. No se aplican reglas de parámetros en rutas con identidad ambigua. Las propiedades required de una respuesta quedan excluidas y no se confunden con obligatoriedad de petición.

Alta opcional, obligatoriedad relajada e identidad efectiva equivalente no rompen por esta faceta. Retirar/renombrar un parámetro emite `PARAMETER_REMOVAL_REQUIRES_REVIEW`: el documento no demuestra si el servidor rechaza enviarlo. Una ruptura independiente conserva BREAKING aunque existan diagnósticos de revisión.

## Cobertura y diagnóstico

Se soporta la presencia de parámetros escalares cuyo schema declara tipo string/integer/number/boolean. Arrays, objetos, content, tipos ausentes/uniones, esquemas referenciados o con allOf/anyOf/oneOf y allowEmptyValue requieren revisión (`PARAMETER_PRESENCE_UNSUPPORTED`). Incluso contratos idénticos con esos parámetros son PARTIAL: igualdad de texto no demuestra una capacidad no implementada. El origen del diagnóstico distingue baseline de candidate.

Se normalizan style/explode por ubicación y allowReserved/allowEmptyValue por sus valores por defecto. Cambios en ese resumen provocan `PARAMETER_SERIALIZATION_REQUIRES_REVIEW`, sin inventar una ruptura semántica. El perfil no evalúa toda la serialización ni sus esquemas. Los cambios de tipo/enum/límites quedan excluidos como `parameterSchemas`; por tanto COMPATIBLE certifica únicamente presencia/obligatoriedad soportadas y disponibilidad de operaciones.

Además quedan excluidos requestSchemas, responseSchemas, responseCodes, mediaTypes, security, servers, callbacks y webhooks. Siguen vigentes tamaño 5 MiB, profundidad 128, rechazo de alias/duplicados/documentos múltiples, comparación por familia 3.0/3.1 y fallo conservador ante mensajes del parser. No es un validador completo de OpenAPI ni una prueba del comportamiento del servidor.

## Informe y evolución pública

El formato mantiene profileId, status, verdict, excludedFacets, changes y diagnostics. Códigos CLI: 0 compatible dentro del perfil; 1 ruptura demostrada; 2 entrada inválida/error de uso; 3 revisión sin ruptura demostrada.

Location añade `parameter: {name, in}`, `baselineUsagePointer` y `candidateUsagePointer`. Los punteros baselinePointer/candidatePointer señalan definiciones originales; un parámetro nuevo tiene baselinePointer nulo. Para cambios de operaciones, los nuevos campos son nulos. Es una ampliación del JSON que los consumidores deben tolerar.

IDs de parámetros: SHA-256 del perfil, regla, dirección REQUEST e identidad de operación/parámetro, sin índices físicos. Cambiar el orden o usar ref/inline no cambia la identidad del hallazgo equivalente. Los IDs de eliminación conservan su algoritmo de `operations-only-v1` para mantener la identidad de las reglas existentes.

El constructor explícito `new SwaggerContractAnalyzer(ComparisonProfile.OPERATIONS_ONLY)` conserva el perfil anterior para consumidores Java. El constructor sin argumentos y la CLI usan el nuevo perfil. No hay selector de perfiles en los argumentos CLI todavía; el cambio predeterminado está documentado, nunca reutiliza el identificador del perfil antiguo para prometer más cobertura.

## Ejemplos ejecutables

```powershell
mvnd clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/parameters-baseline.yaml src/test/resources/openapi/parameters-required.yaml
$LASTEXITCODE
```

Resultado: tres hallazgos, código 1. Limit heredado pasa a obligatorio en GET y POST; GET añade X-Trace obligatorio mediante referencia interna.

```powershell
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/query-required.json src/test/resources/openapi/corpus/query-optional.json
$LASTEXITCODE
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/empty.json src/test/resources/openapi/corpus/invalid-required.json
$LASTEXITCODE
```

Resultados: relajación compatible con código 0; entrada inválida con código 2. Consultar el [corpus y su manifiesto](contract-corpus.md) para todos los pares sintéticos.
