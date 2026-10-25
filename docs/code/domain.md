# Modelo y reglas del dominio

Todos los archivos de `domain` usan el JDK. Son snapshots y pruebas de compatibilidad, sin anotaciones Spring, nodos Jackson, modelos Swagger ni E/S. DDD se aplica mediante conceptos e invariantes del problema; no hacen falta agregados persistidos ni servicios de repositorio para comparar dos documentos.

## Valores e identidades

| Tipo | Responsabilidad e invariantes |
| --- | --- |
| HttpMethod | Los ocho métodos GET, PUT, POST, DELETE, OPTIONS, HEAD, PATCH y TRACE. |
| OperationId | Ruta literal y método no nulos; la ruta debe empezar por `/`. No intenta emparejar rutas renombradas. |
| ParameterId | Nombre no vacío y ubicación path/query/header/cookie. Headers se normalizan a minúsculas con Locale.ROOT; los demás nombres conservan mayúsculas. Orden por ubicación y después nombre. |
| EffectiveParameter | Identidad, obligatoriedad, definición, uso, firma de serialización y soporte de presencia. Exige identidad/pointers/firma no nulos y path obligatorio. `presenceSupported` no implica soporte de todas las facetas del parámetro. |
| RequestBodyContract | Presencia obligatoria, conjunto de formatos y pointers de definición/uso. Copia el conjunto y exige pointers no nulos. No contiene el schema. |
| ResponseContract | Formato normalizado → pointer original, definición y uso; copia el mapa. Un mapa vacío representa una respuesta sin cuerpo. |
| ScalarValue | Identidad JSON escalar: string, number, boolean o null. Canoniza números con BigDecimal, de modo que 1 y 1.0 son iguales. `integer` comprueba si el valor numérico es matemáticamente entero. No valida todas las representaciones textuales de boolean/null. |
| SchemaUseId | Operación, dirección, parámetro opcional, código y formato opcionales: identidad semántica del uso. No depende del nombre de components ni del índice físico del parámetro. El record no valida combinaciones arbitrarias de campos. |
| SchemaUse | Schema normalizado, pointer de uso y problema de representación. La cadena vacía indica que no se ha señalado un problema de representación; no sustituye la comprobación de restricciones opacas. |
| ObjectShape | Propiedades normalizadas, nombres requeridos, apertura a nombres adicionales y modos de acceso originales. Copia mapas/conjunto. El adaptador ya ha proyectado readOnly/writeOnly por dirección. |
| ArrayShape | Schema de items, mínimo y máximo de elementos. `items == null` significa sin restricción de elementos; máximo Integer.MAX_VALUE es el límite representado, no un objeto de infinito. |

`OperationContract` agrupa versión, rutas/métodos, parámetros efectivos, cuerpos, usos de schemas y respuestas. Copia las colecciones y los mapas anidados para no depender de estructuras mutables del parser. Sus sobrecargas rellenan facetas posteriores con mapas vacíos; existen para conservar usos anteriores y simplificar tests. Un snapshot creado a mano no es automáticamente un OpenAPI válido: la validación de entrada corresponde al adaptador.

`SchemaValueContract` representa conjuntamente tipos, enum, límites numéricos, longitudes, restricciones desconocidas, procedencia, objeto, flags de acceso y array. `Bound` contiene BigDecimal y exclusividad. Enum nulo significa sin restricción enum; lista vacía significa dominio contradictorio tras normalización, por ejemplo al intersectar const y enum. Las restricciones desconocidas se deduplican y ordenan. Las sobrecargas mantienen las facetas posteriores ausentes.

`acceptsConstraints` comprueba tipo y límites escalares, contando puntos de código Unicode en strings. `accepts` añade el enum. Estos métodos operan sobre `ScalarValue`: no son validadores de documentos JSON completos ni verifican objetos/arrays. La inclusión estructural está en `SchemaInclusion`.

## Resultado público

`ComparisonReport` contiene `profileId`, `status`, `verdict`, `excludedFacets`, `changes` y `diagnostics`. Copia las listas. `Diagnostic` tiene `code`, `source`, `location` y `description`; source distingue baseline, candidate, comparison o cli. Las dos fábricas `of` aplican la tabla de [estados](runtime.md); la sobrecarga antigua conserva OPERATIONS_ONLY.

`DetectedChange` contiene `id`, `ruleId`, `severity`, `location` y `description`. La severidad implementada es ERROR. `Location` tiene ruta, método, pointers anteriores/nuevos de definición, parámetro, pointers de uso, dirección, código y formato. Los campos no aplicables son nulos; un pointer vacío puede representar la raíz. Las sobrecargas de Location conservan llamadas históricas.

Los pointers siguen JSON Pointer: `~` se escapa como `~0` y `/` como `~1`. Definición y uso se conservan por separado para referencias internas. En respuesta la prueba invierte source/target, pero la salida vuelve a etiquetar baseline/candidate correctamente. Cuando el nodo desaparece se usa, donde corresponde, un contenedor existente del candidato; no se inventa un pointer al nodo eliminado.

Las fábricas internas de `DetectedChange` construyen ubicaciones, descripciones y SHA-256 UTF-8 de la identidad semántica. `removed`, `parameterRequired` y `requestBody` conservan su versión histórica; `schema` incorpora el perfil activo; `responsePolicy` usa su versión de política. `token` codifica longitud y valor, o nulo, para evitar ambigüedad al concatenar identidades. No se usa texto descriptivo, nombre físico del componente o índice de parámetro como identidad. Cambiar estos hashes es un cambio público que requiere revisar versiones y regresiones.

## Reglas y decisiones

Las reglas operan sobre operaciones compartidas no ambiguas, salvo la regla de retirada que precisamente detecta su ausencia. Una ruta con metadatos pero sin métodos no es un endpoint activo. Los resultados se agregan en el adaptador; no hay una interfaz por cada regla.

| Clase | Rupturas demostradas | Casos que requieren revisión |
| --- | --- | --- |
| RemovedOperationsRule | Endpoint ausente: un hallazgo sin duplicar métodos. Ruta existente: cada método eliminado. | Plantilla renombrada o ambigua, como `/pets/{id}` frente a `/pets/{petId}`. |
| RequiredParametersRule | Parámetro nuevo obligatorio o parámetro opcional que pasa a obligatorio. | Retirada, representación no soportada o cambio de firma de serialización. |
| RequestBodiesRule | Cuerpo nuevo obligatorio, opcional que pasa a obligatorio, media type anterior sin cobertura candidata. | Cuerpo retirado, semántica fuera de POST/PUT/PATCH, formato con parámetros/patrones no soportados. |
| SchemaValuesRule | Inclusión EXCLUDED: REQUEST_SCHEMA_VALUES_RESTRICTED o RESPONSE_SCHEMA_VALUES_EXPANDED. | Cobertura ausente aplicable, representación opaca o inclusión desconocida/incompleta. |
| ResponsePolicyRule | Código concreto eliminado sin fallback aplicable; representación concreta anterior retirada. | Adiciones de códigos/formatos, default/rangos, comodines y negociación incierta. |

`RemovedOperationsRule.templateShape` sustituye nombres entre llaves por `{}`; `shapes` cuenta rutas por plantilla, incluyendo las que tienen solo metadatos. Las reglas posteriores reutilizan ese criterio para evitar dar certezas cuando no se puede identificar la operación.

`RequiredParametersRule` une identidades de ambos lados y sigue un orden conservador: retirada → soporte anterior → soporte candidato → serialización → obligatoriedad. Por eso un parámetro que además cambia representación puede dar revisión en vez de afirmar ruptura de presencia. No supone que borrar documentación equivale a que el servidor rechace el parámetro.

`RequestBodiesRule.supported` reconoce formatos concretos, `type/*` y `*/*`; `covers` aplica cobertura sin distinguir mayúsculas. La obligatoriedad demostrada puede coexistir con incertidumbre de formatos. La política de presencia de cuerpos no hace la prueba de datos JSON.

`SchemaValuesRule` une usos en orden estable y llama al algoritmo con **baseline ⊆ candidate para petición**, **candidate ⊆ baseline para respuesta**. Un enum de petición más amplio normalmente mantiene clientes anteriores; un enum de respuesta más amplio puede romper consumidores. Si se encuentra una ruptura y además una laguna, devuelve tanto el hallazgo como un diagnóstico. En `full` las retiradas de metadatos de respuesta las trata ResponsePolicyRule; en perfiles anteriores se conserva la revisión de cobertura histórica. La ausencia de schema de petición no debe inventarse a partir de una respuesta retirada.

`ResponsePolicyRule` separa dos conceptos: inclusión de payloads y continuidad de respuestas documentadas. Retirar un código se clasifica por la política publicada, no por invertir mecánicamente la regla de petición. `compareMedia` compara identidades concretas; si hay patrones no demuestra retiradas. Un `default` o rango de la misma familia puede cubrir un código retirado, pero exige revisión; un rango de otra familia no oculta esa retirada.

Para keywords conjuntas y casos satisfacibles/contradictorios, continuar con [el algoritmo](inclusion.md). Los catálogos completos de diagnósticos y reglas públicas se mantienen en los documentos de perfiles existentes.
