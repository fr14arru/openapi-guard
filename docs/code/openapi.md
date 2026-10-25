# Adaptador OpenAPI y normalización

Este paquete implementa el puerto ContractAnalyzer. Swagger Parser valida y proporciona operaciones; Jackson conserva el árbol original para restricciones y procedencia. No se copia todo el modelo OpenAPI: se extraen solo las facetas comprometidas por el perfil.

## SwaggerContractAnalyzer

El constructor sin argumentos selecciona PARAMETER_PRESENCE. El constructor con perfil exige un valor no nulo y configura YAMLFactory/ObjectMapper: claves duplicadas estrictas, rechazo de tokens/documentos posteriores, decimales BigDecimal y profundidad 128. MAX_DOCUMENT_BYTES es 5 MiB, contado en UTF-8.

`analyze` lee ambos contratos con `read`. Si cualquiera falla, devuelve FAILED sin cambios especulativos. Si las familias 3.0/3.1 difieren, devuelve UNSUPPORTED_VERSION_PAIR. Ejecuta RemovedOperationsRule y, según capacidades del perfil, parámetros, cuerpos, schemas y política de respuestas. Ordena cambios por ruta, método, regla, identidad de parámetro, código y formato. Los diagnósticos se agregan en orden de las reglas; no hay una ordenación global equivalente a la de cambios.

`read` convierte excepciones de lectura/normalización en INVALID_OR_UNSUPPORTED_INPUT con source baseline/candidate. `readContract` tiene una variante accesible al paquete para tests y una interna que controla la extracción de parámetros. El flujo interno es:

1. Rechazar contenido nulo, vacío o demasiado grande.
2. Recorrer eventos YAML para rechazar aliases antes de materializar el árbol. LoaderOptions por sí solo no basta porque Jackson consume eventos directamente.
3. Leer un solo árbol; exigir versión 3.0.x/3.1.x y objeto paths explícito.
4. `checkReferences` recorre **todo** el árbol, también componentes no usados. Rechaza referencias externas, no textuales o inexistentes; solo admite `#/...`. No abre ningún recurso referenciado.
5. Comprobar Path Items: ruta que empieza por `/`, objeto y sin `$ref`. Las entradas x- de paths se ignoran.
6. Entregar el JSON del árbol a OpenAPIV3Parser con resolve/resolveFully desactivados. Un modelo ausente o cualquier mensaje del parser invalida la entrada. Esto no implica validar exhaustivamente cada keyword de OpenAPI/JSON Schema.
7. Extraer rutas/métodos y llamar a lectores específicos para las facetas activas. Construir OperationContract.

Swagger es una dependencia exterior. Las reglas reciben records del dominio, nunca su AST. El adaptador recibe strings, no URLs ni paths; la lectura de los dos archivos pertenece a la CLI.

## EffectiveParametersReader

`read` calcula la lista heredada del Path Item y la fusiona con la de la operación mediante ParameterId. La declaración local reemplaza la heredada del mismo identificador; una lista local vacía no borra la herencia.

`list` valida cada declaración y sigue referencias internas con límite 64 y conjunto de visitadas. Conserva el índice donde se usa y el pointer final donde se define. Rechaza duplicados en una misma lista, required no booleano, path opcional o sin correspondencia con la plantilla, y declaraciones que no tienen exactamente schema o content. Header Accept, Content-Type y Authorization se omiten según la semántica especial de OpenAPI.

La firma de serialización concatena style, explode, allowReserved, allowEmptyValue y uso de content, con defaults según ubicación. La presencia demostrable se limita a tipos escalares directos string/integer/number/boolean, sin content, allowEmptyValue, `$ref` o composición. Esta marca conservadora no se vuelve verdadera solo porque otro lector consiga normalizar el schema.

`invalid` añade el pointer al error de entrada. `escape` aplica JSON Pointer. Estos helpers no realizan E/S.

## RequestBodiesReader

`read` busca requestBody en cada operación, omite el ausente, resuelve referencias internas con el mismo límite 64 y conserva definición/uso. Exige objeto, content no vacío y required booleano cuando existe. Captura nombres originales de formatos en un conjunto; la cobertura sin distinguir mayúsculas la aplica RequestBodiesRule. No interpreta schema ni decide semántica de métodos: esas responsabilidades son distintas.

## ResponseContractsReader

`read` extrae códigos concretos 100–599, rangos 1XX–5XX o default; ignora extensiones x-. Sigue referencias internas con detección de ciclo y límite 64. Valida respuesta/content como objetos, conservando también respuestas sin content.

Los formatos se normalizan a minúsculas para identidad y apuntan al nombre original para procedencia. Dos claves que colisionan tras normalización fallan. `escape` conserva pointers correctos. La cobertura de default/rangos y la política de retirada se decide en ResponsePolicyRule; los payloads se normalizan en SchemaValuesReader.

## SchemaValuesReader

Una instancia pertenece a un documento y recibe flags objects/arrays. Las sobrecargas preservan el fragmento escalar y el de objetos. `dialect31` distingue sintaxis, sin convertir automáticamente familias. `read` recorre operaciones/identidades en orden estable, creando usos de petición para cuerpos y parámetros, y de respuesta para cada código/formato documentado.

`content` acepta como representación demostrable application/json y sufijos +json concretos, sin encoding. Otros formatos, comodines o encoding generan representationIssue. Default/rangos de respuesta y cuerpos fuera de POST/PUT/PATCH también señalan revisión. Conserva un pointer al objeto media cuando no existe schema, en vez de apuntar a un nodo inexistente. Detecta colisiones de formatos normalizados.

`referenceObject` sigue referencias de respuesta para localizar content. `schema` normaliza recursivamente:

| Faceta | Comportamiento |
| --- | --- |
| Presupuesto | 10000 llamadas de normalización por instancia/documento; exceso crea nodo opaco. Profundidad de referencias/visitadas máxima 64. |
| Referencias | Solo internas previamente comprobadas. Ciclo/profundidad dan opacidad. En 3.1, restricciones hermanas de `$ref` requieren composición y revisión. |
| Cache | Clave dirección + referencia, para conservar proyección distinta de petición/respuesta. Solo cachea resultados sin lagunas de ciclo/profundidad/budget en sus descendientes. |
| Tipos | Ausente = todos los tipos. 3.1 permite array de tipos y null; 3.0 nullable solo amplía un tipo textual. nullable en 3.1 da laguna. |
| Enum/const | Solo escalares; enum vacío de entrada inválido. Const 3.1 se intersecta con enum. Valores compuestos o const en 3.0 son lagunas. |
| Límites | BigDecimal; precisión y valor absoluto de escala como máximo 1024. Longitudes enteras exactas no negativas dentro de int. |
| Dialecto | Reconoce dialectos base OpenAPI 3.1 y JSON Schema 2020-12; otro `$schema` o jsonSchemaDialect global impide certeza. |
| Objetos | properties recursivas; required sin duplicados; additionalProperties booleano, abierto por defecto. Schema en additionalProperties es opaco. |
| Proyección | Retira readOnly de petición y writeOnly de respuesta, también de required. Conserva accessModes originales; ambos flags true invalidan la entrada. |
| Arrays | Items homogéneos y límites. Tipo array explícito en 3.0 exige items; 3.1 permite items omitido y booleano. |
| Keywords | KNOWN contiene datos interpretados y anotaciones excluidas. Cualquier otra keyword comprometida queda en unsupported, incluidas extensiones dentro de schemas. |

El código acepta nodos booleanos en `schema`; el parser y las comprobaciones de dialecto/contexto siguen formando parte de la validación anterior. No utilizar este método como parser de cualquier dialecto por separado.

`opaque` crea un nodo con todos los tipos y un motivo de incertidumbre, **no** un nodo demostrado compatible. `scalar` transforma null/string/boolean/number y devuelve null para valores compuestos. `decimal`, `length` y `flag` validan formatos y presupuestos. `bound` trata exclusiveMinimum/Maximum como booleanos en 3.0 y valores numéricos en 3.1, seleccionando el límite conjunto más fuerte. `cacheable` recorre por identidad y excluye lagunas cuyo resultado depende del contexto de referencias o presupuesto. `escape` implementa procedencia.

## Límites que conviene revisar explícitamente

Las referencias externas se rechazan incluso si pertenecen a una faceta excluida. Los aliases YAML se rechazan, no se expanden. El perfil determina qué datos se normalizan, pero no relaja el límite global de entrada. Ciclos de parámetros/cuerpos/respuestas son errores de entrada; ciclos de schemas dentro de la cobertura son análisis parcial. Idéntica restricción desconocida en ambos documentos sigue siendo desconocida. Estas decisiones evitan convertir ausencia de implementación en una garantía de compatibilidad.
