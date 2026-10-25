# Arquitectura propuesta

Estado: arquitectura aceptada; D03, D05, D06 y D07 implementados con TDD. Fecha: 2026-10-08. Ver [perfil de parámetros](parameter-presence-profile.md), [objetos](object-schemas-profile.md), [revisión](planning-review.md) y [desarrollo local](development.md).

La implementación mantiene un módulo y los cuatro paquetes. OperationContract incorpora una instantánea mínima de parámetros efectivos por OperationId. EffectiveParametersReader conserva procedencia del árbol original después de validar con Swagger; RequiredParametersRule permanece Java puro. El único puerto ContractAnalyzer expone también el perfil para que errores de lectura e informes de comparación mantengan la misma semántica. No se añaden módulos, plugins ni infraestructura.

D06 incorpora SchemaUseId/SchemaUse para identificar el uso efectivo, SchemaValueContract para las restricciones soportadas y SchemaInclusion como prueba pura de inclusión direccional. SchemaValuesReader conserva restricciones desconocidas como cobertura pendiente. No copia todo el modelo Swagger ni introduce interfaces o módulos nuevos. [Perfil y límites](schema-values-profile.md).

D07 extiende únicamente esa instantánea con ObjectShape, propiedades proyectadas, obligatoriedad, apertura y modos de acceso. Reutiliza la inclusión pura y el adaptador; no añade otro motor, registro de plugins, interfaces, módulos ni dependencias. Las ubicaciones de pruebas anidadas conservan su definición original. [Semántica y límites](object-schemas-profile.md).

D08 añade ArrayShape con items y límites conjuntos, reutilizando esa misma inclusión. El perfil versionado arrays conserva los anteriores; la normalización y las cachés recorren ahora también elementos, sin resolución externa. No hay nuevas dependencias ni infraestructura. [Perfil](array-schemas-profile.md).

D09 incorpora ResponseContract para códigos/formatos y su procedencia, incluido el caso sin cuerpo. ResponseContractsReader es el adaptador de esa instantánea y ResponsePolicyRule mantiene la política pura de disponibilidad separada de la inclusión de datos. Se reutilizan puerto, módulo y perfiles versionados; no se introduce otro servicio ni dependencias. [Política](response-policy-profile.md).

## Integración Git/GitHub

El publicador opcional pr-comment.mjs es otro adaptador exterior de Node sobre el informe existente y la API REST de GitHub. No modifica Java ni añade librerías. Se carga después de verificar el paquete, separa el token del proceso comparador y limita la escritura al comentario del bot para el workflow/directorio. La política de merge sigue en la Action. [Permisos y concurrencia](github-annotations.md#comentario-en-la-conversación-de-la-pr).

La política `fail-on-breaking` pertenece al adaptador de Action. No modifica el veredicto ni los códigos del núcleo/CLI: decide el estado del check después de conservar el informe. Las rupturas permitidas siguen siendo `BREAKING`; errores e incertidumbre siguen fallando. La descripción del cambio puede añadir diferencias de restricciones del nodo ya identificado por la prueba, manteniendo siempre el orden baseline → candidate y la identidad semántica existente. [Presentación y política](github-annotations.md).

El cierre técnico de distribución añade verificación de conjunto completo de archivos, transporte ZIP y kit de consumidor. El build del candidato exige checkout limpio, versión explícita y commit en el JAR; el proof registra verificación local, no aprobación comercial ni firma externa. No cambian dominio/reglas. [Roadmap y límites de la entrega](candidate-roadmap.md).

La Action compuesta es un adaptador exterior sobre los scripts existentes: prepara Corretto 25 y verifica un JAR precompilado por SHA256. No añade Spring ni GitHub al dominio, dependencias npm/Maven ni compilación al consumidor. El paquete evaluable inventaría las bibliotecas del JAR real y no aprueba su publicación. [Distribución y límites](evaluable-distribution.md).

Las posiciones físicas pertenecen a adapter.cli. El formato ci añade una envoltura versionada alrededor del informe semántico sin modificar dominio, reglas ni JSON histórico. Los scripts exteriores traducen esas posiciones a rutas Git y anotaciones de Actions. Ver [presentación CI](github-annotations.md).

La integración Git/GitHub selecciona archivos y extrae blobs mediante scripts externos que invocan la CLI. No introduce proveedores en dominio/aplicación, reglas nuevas ni dependencias Maven. Node usa módulos estándar y queda fuera del build Java. Su envoltorio conserva informes full y añade política de eliminación de contratos. Ver [diseño y límites](git-integration.md).

## Punto de partida

Inspección inicial: repositorio `D:\Projects\openapi-breaking-change-guard` vacío salvo `.git`, en `master`, sin commits ni remotos. En la revisión hay documentación sin versionar y un directorio `.idea/` ajeno a esta tarea. El commit de revisión incluirá únicamente documentación y `AGENTS.md`; se preservará `.idea/` sin incorporarlo.

El entorno informa Amazon Corretto 25.0.4.1 y Maven 3.10.0 en Windows 11. El cliente mvnd 1.0.6 está disponible, pero su comprobación de versión no pudo acceder al registro del daemon en `.m2` bajo los permisos de esta sesión. Esto no demuestra un fallo del entorno normal del usuario. No se ejecutaron builds ni se instalaron dependencias.

## Decisiones propuestas

1. Monolito de biblioteca y CLI local, sin servicio desplegado. Java 25 estable, Maven estándar y mvnd como acelerador opcional.
2. Un único módulo Maven inicialmente. Los límites entre paquetes bastan hasta que exista un segundo consumidor o una necesidad real de publicación independiente.
3. Dominio propio pequeño e inmutable; Swagger queda encerrado en el adaptador de entrada. No copiar todo OpenAPI ni escribir un parser YAML/JSON propio.
4. Reglas explícitas, deterministas, con contexto de petición/respuesta y pruebas por pares de contratos.
5. Tres conclusiones semánticas: incompatible, compatible dentro del perfil y revisión necesaria. Una comparación parcial nunca certifica compatibilidad.
6. Seleccionar Swagger Parser v3 2.1.48 para leer contratos en esta funcionalidad. Comparar identidades de operaciones con una regla propia pequeña; evaluar reutilización de openapi-diff para futuros cambios de esquemas sin añadir ahora ese motor ni sus reglas.
7. Spring Boot desde el inicio para arranque, configuración y ensamblado de la CLI, por familiaridad del desarrollador. HTTP, GitHub y almacenamiento se incorporan cuando haya una necesidad comercial validada.

## Librerías Java evaluadas

La consulta documental no sustituye una prueba local. No se fija una versión sin revisar el artefacto, sus transitivas y las fixtures.

| Opción | Papel y capacidades | Coste o limitación | Decisión |
| --- | --- | --- | --- |
| Swagger Parser + swagger-models | Lee JSON/YAML y produce POJOs; documenta soporte 3.1 desde 2.1.0 y opciones de referencias | La resolución difiere entre 3.0 y 3.1; puede transformar el modelo y perder una relación directa con la ubicación original | Primera opción; controlar resolución y conservar procedencia |
| Apicurio Data Models, actualmente Apitomy Data Models | Lectura, modelos, validación y visitors; el repositorio actual declara 3.0.x, 3.1.x y otros formatos | Mayor superficie que necesitamos; verificar coordenadas vigentes y comportamiento de referencias en una prueba | Alternativa si Swagger falla el corpus |
| openapi4j | Parser y validadores de esquema/peticiones | Repositorio archivado; riesgo de mantenimiento | Descartar como dependencia principal |
| OpenAPITools openapi-diff | Comparador Java existente, con salidas de informe | No es solo un parser; adoptar su motor también adopta sus políticas y limitaciones | Evaluar como alternativa de construir/reutilizar y referencia diferencial; no usar como oráculo absoluto |
| Swagger Core / springdoc-openapi | Modelado y generación/documentación de APIs Java | Generar una especificación no resuelve la comparación de dos contratos | No añadirlos como herramientas de generación en esta fase |

Fuentes oficiales consultadas el 2026-10-08: [Swagger Parser](https://github.com/swagger-api/swagger-parser), [modelos y resolución 3.1](https://github.com/swagger-api/swagger-core/wiki/Swagger-2.X---OpenAPI-3.1), [Apitomy Data Models](https://github.com/Apitomy/apitomy-data-models), [openapi4j](https://github.com/openapi4j/openapi4j), [openapi-diff](https://github.com/OpenAPITools/openapi-diff), [Swagger Core](https://github.com/swagger-api/swagger-core).

La selección para operaciones se limita al parser candidato ya identificado, Swagger Parser. La regla de conjuntos de métodos/rutas no necesita un motor externo completo ni sus políticas. No se ha validado todavía si conviene reutilizar openapi-diff para esquemas: esa decisión permanece pendiente y no se declara que haya fallado una evaluación inexistente. Una conclusión positiva de terceros nunca se traducirá a `COMPATIBLE` sin comprobar el perfil comprometido.

### Mantenimiento y licencias

Comprobación documental el 2026-10-08; versiones observadas son evidencia, no selección de dependencias:

| Biblioteca | Evidencia de mantenimiento | Licencia declarada / uso comercial |
| --- | --- | --- |
| Swagger Parser y swagger-models/Core | Releases del parser hasta 2.1.48, con correcciones y actualizaciones de dependencias: señales de mantenimiento activo | Apache-2.0 en sus repositorios |
| OpenAPITools openapi-diff | Releases hasta 2.1.7, con correcciones y actualizaciones: señales de mantenimiento activo | Apache-2.0 |
| Apitomy Data Models | Repositorio disponible y no archivado, con soporte declarado amplio; no se pudo verificar fecha del último commit/release en esta consulta | Apache-2.0; mantener como alternativa, sin afirmar actividad reciente no comprobada |
| openapi4j | Archivado desde 2021-07-09; sin mantenimiento en ese repositorio | Apache-2.0; descartado por mantenimiento, no por licencia |

Evidencia: [releases Swagger Parser](https://github.com/swagger-api/swagger-parser/releases), [releases openapi-diff](https://github.com/OpenAPITools/openapi-diff/releases), [licencia del parser](https://github.com/swagger-api/swagger-parser/blob/master/LICENSE), [Swagger Core](https://github.com/swagger-api/swagger-core), [licencia openapi-diff](https://github.com/OpenAPITools/openapi-diff/blob/master/LICENSE), [Apitomy](https://github.com/Apitomy/apitomy-data-models), [archivo y licencia openapi4j](https://github.com/openapi4j/openapi4j).

Apache-2.0 permite uso, modificación y distribución comerciales, también con código propietario; requiere conservar licencia y avisos aplicables, identificar modificaciones en archivos modificados y trasladar los avisos NOTICE pertinentes. No concede derechos sobre marcas. El empaquetado de la CLI debe incluir las atribuciones requeridas. La compatibilidad final depende también de las transitivas y de los artefactos exactos que se distribuyan; revisar esa lista al seleccionar versiones. Fuente: [texto de Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Compatibilidad y lenguaje del dominio

Contexto delimitado: compatibilidad de contratos entre una versión base y una candidata, para clientes existentes. No se evalúa si el servidor real cumple el contrato ni la compatibilidad de código generado.

- Petición: todo dato permitido antes debe seguir siendo aceptable: `RequestOld ⊆ RequestNew`.
- Respuesta: todo dato que el nuevo servidor puede producir debe seguir cumpliendo lo que el cliente esperaba: `ResponseNew ⊆ ResponseOld`.

Estas inclusiones orientan las reglas para el perfil soportado; no se pretende resolver inclusión arbitraria de JSON Schema. La relación de conjuntos cubre cuerpos y valores. La desaparición de una operación es además una ruptura de disponibilidad contractual. Las políticas prácticas, como la desaparición de una respuesta documentada, se presentan separadamente de las rupturas demostradas.

Un componente reutilizado puede ser compatible en una petición e incompatible en una respuesta. Se analiza por uso efectivo, no una sola vez por nombre de componente. Los componentes no usados no afectan al veredicto; opcionalmente podrán informarse como inventario en una fase posterior.

Objetos valor previstos: identidad de operación/parámetro, ubicación, cambio y versión del perfil. `ComparisonReport` es el resultado público. Solo crear instantáneas propias de contrato/nodos cuando hagan falta reglas propias; si se adapta un motor maduro, basta traducir hallazgos y cobertura sin duplicar todo su modelo. No hay entidades persistentes, repositorios de base de datos ni agregados artificiales.

## Paquetes y límites

Raíz provisional: `com.openapiguard`, pendiente de confirmar titularidad/nombre comercial. Un módulo, cuatro paquetes principales; crear subpaquetes solo cuando el volumen lo justifique.

| Paquete | Responsabilidad | Dependencias permitidas |
| --- | --- | --- |
| `domain` | Objetos valor, perfil, resultados y reglas propias necesarias | JDK |
| `application` | Caso de uso concreto `CompareContracts` y puerto `ContractAnalyzer` para el motor externo o propio | Dominio/JDK |
| `adapter.openapi` | Parser/motor maduro, normalización estrictamente necesaria, cobertura y traducción a resultados propios | Puerto, dominio y bibliotecas externas |
| `adapter.cli` | Archivos, argumentos, JSON/texto, arranque Boot y configuración de beans | Aplicación, adaptador OpenAPI y Spring Boot |

Flujo previsto: `CLI → CompareContracts → ContractAnalyzer → informe propio → CLI`. El puerto expresa la capacidad de analizar dos contenidos bajo un perfil y devolver hallazgos/cobertura; el adaptador puede delegar en terceros o en reglas propias. Las reglas propias permanecen puras en el dominio. Solo añadir un puerto separado de lectura si dos implementaciones o el flujo lo necesitan.

Usar inyección por constructor y configuración explícita de beans en el borde de la aplicación. Una interfaz de regla y un registro explícito permiten añadir reglas sin modificar el parser o la CLI; no hace falta descubrimiento dinámico de plugins ni escanear reglas mediante reflexión propia. Una regla recibe un contexto tipado, devuelve hallazgos y no realiza E/S.

El núcleo no importa Spring, Jackson, Swagger ni clases del sistema de archivos. El puerto recibe contenido y un identificador de origen abstracto; la CLI obtiene los bytes. El adaptador convierte sin propagar POJOs mutables de terceros. No crear interfaz de entrada para un único caso de uso, DTO duplicado por capa, paquete bootstrap ni repositorio ficticio. El lenguaje y las invariantes hacen útil DDD; las anotaciones o el número de capas no lo garantizan.

El motor funciona como llamadas a objetos Java normales y se prueba sin iniciar Spring. La CLI se ejecuta como aplicación Spring Boot en modo no web; Boot crea los objetos a través de configuración exterior y ejecuta el comando. El dominio y el caso de uso no necesitan ApplicationContext ni anotaciones Spring. El futuro adaptador HTTP podrá llamar al mismo caso de uso.

Esta elección aprovecha la experiencia laboral del desarrollador y evita mantener un arranque/configuración diferente. Su coste es el contexto de arranque y un artefacto mayor; medir el tiempo de inicio real en CI antes de optimizar. Mantener la selección mínima de dependencias: Boot para la aplicación CLI, sin servidor embebido ni starters web/JPA. Elegir versión estable compatible con Java 25, revisar su gestión de dependencias frente al parser y solicitar aprobación de la lista antes de descargar. Configurar logs y banner para que no contaminen stdout JSON; verificar cierre del contexto y códigos de salida del proceso.

Módulos posteriores, solo si aportan valor: `guard-core` (dominio y aplicación), `guard-openapi` (adaptador) y `guard-cli` (distribución Boot). Un futuro `guard-service` con HTTP invocaría el mismo caso de uso. No se crean esos módulos todavía. La separación futura debe mantener `guard-core` libre de dependencias de adaptadores y Spring.

## Lectura, normalización y cobertura

1. Comprobar tamaño, versión y sintaxis. Primera versión vendible para OpenAPI 3.0.x; 3.1.x es la siguiente ampliación, adelantable si el piloto lo exige. Sin conversión implícita desde Swagger 2.0 ni entre familias.
2. Validar estructura; un POJO no nulo no garantiza validez. Clasificar diagnósticos del parser con una política documentada y validación propia de precondiciones del perfil.
3. Rechazar referencias externas antes de activar cualquier resolución. En el MVP solo se permiten fragmentos internos al documento; no hay acceso a red ni lectura de archivos referenciados. No asumir que una opción del parser por sí sola basta: probar ausencia de E/S.
4. Normalizar parámetros heredados del Path Item y sus overrides por operación, defaults semánticos y proyecciones por dirección. Conservar ausencia de tipo, `additionalProperties`, límites y diferencias de dialecto; no inventar `object` donde el tipo falta.
5. Conservar referencias y procedencia original; evitar dereferenciación total y flattening sin demostrar que preservan semántica. Si se reutiliza un motor, comprobar estas propiedades con tests de adaptación.
6. Para reglas propias, resolver referencias internas bajo demanda y detectar ciclos con pares de nodos y contexto. No construir un AST completo ni un algoritmo general de inclusión por adelantado. La comparación semántica de ciclos queda para después del piloto; inicialmente produce revisión.
7. Aplicar reglas solo a usos alcanzables de las facetas comprometidas. Registrar exclusiones e incertidumbre por ubicación. Ordenar resultados de forma estable. Presencia de seguridad o metadata fuera de esas facetas no invalida automáticamente el análisis de datos: el informe declara ese límite y no ofrece compatibilidad global del API.

Valores iniciales propuestos, pendientes de medir: 5 MiB por documento y profundidad 128, además de límites de nodos y alias proporcionados por el parser. Superar un límite produce diagnóstico explícito, no truncamiento silencioso. Medir duración y memoria en un corpus representativo; no diseñar todavía aislamiento por procesos ni timeout interruptible general. Antes de aceptar entradas de terceros en un servicio, será necesario un presupuesto de recursos con cancelación efectiva.

## Modelo del informe

| Campo de `ComparisonReport` | Significado |
| --- | --- |
| `reportSchemaVersion` | Versión del formato JSON, separada de la versión del motor |
| `engineVersion`, `profileId`, `profileVersion` | Reproducibilidad y semántica aplicada |
| `baseline`, `candidate` | ID de origen, versión OpenAPI y SHA-256 del contenido; no incluir documentos ni rutas sensibles por defecto |
| `status` | `COMPLETED`, `PARTIAL` o `FAILED` |
| `verdict` | `BREAKING`, `COMPATIBLE` o `INDETERMINATE` |
| `coverage` | Facetas comprometidas, exclusiones de producto y usos sin analizar dentro del perfil con razones; no vender un porcentaje como prueba de corrección |
| `changes` | Hallazgos ordenados y sin duplicados por el mismo uso |
| `diagnostics` | Errores de entrada, resolución, límites y análisis, separados de los cambios |

| Campo de `DetectedChange` | Significado |
| --- | --- |
| `id` | Huella determinista de `ruleId`, versión del perfil, dirección, identidad semántica de uso y valores; excluir índices físicos/punteros que cambien al reordenar; no UUID aleatorio |
| `ruleId` | Código estable, por ejemplo `REQUEST_PARAMETER_REQUIRED_ADDED`; versionar el catálogo |
| `severity` | `ERROR`, `WARNING` o `INFO`, para priorizar |
| `compatibility` | `BREAKING`, `NON_BREAKING` o `REVIEW_REQUIRED`; no deducirlo solo de la severidad |
| `direction` | `REQUEST`, `RESPONSE` o `OPERATION` |
| `location` | Ruta, método, parámetro o código de respuesta/media type y punteros JSON originales en ambas versiones |
| `description` | Explicación concreta orientada al consumidor |
| `before`, `after` | Resumen semántico mínimo, con ausencia explícita, no una copia del contrato |
| `evidence` | Restricción y justificación; generar ejemplos testigo automáticamente queda para después del piloto |

Los punteros JSON escapan `~` y `/`; ejemplo: `/paths/~1pets/post/parameters/0/required`. Guardar tanto la ubicación de uso como la definición referenciada. Un elemento añadido no tiene puntero anterior y uno eliminado no tiene puntero posterior. La identidad estable no depende del índice del array de parámetros ni del texto traducido de la descripción. Línea y columna son opcionales: el parser podría no preservarlas.

Ejemplo conceptual, no contrato JSON definitivo: `ruleId=REQUEST_PARAMETER_REQUIRED_ADDED`, `severity=ERROR`, `compatibility=BREAKING`, `direction=REQUEST`, operación `POST /pets`, parámetro `query:tenantId`, antes ausente, después obligatorio; descripción: «Las peticiones existentes sin tenantId dejan de cumplir el contrato».

Agregación: cualquier ruptura probada da `BREAKING`, incluso en análisis parcial. Sin rupturas, solo `COMPLETED` con toda la cobertura comprometida da `COMPATIBLE`; `PARTIAL` o `FAILED` dan `INDETERMINATE`. Una entrada inválida da `FAILED`, sin hallazgos semánticos especulativos. Un análisis con hallazgos de revisión dentro del perfil es `PARTIAL`. Un área excluida por producto no se cuenta como cubierta: siempre aparece en el informe y en el texto «compatible en las facetas del perfil», nunca «API totalmente compatible».

Códigos CLI propuestos: 0 compatible; 1 ruptura detectada; 2 entrada/opciones inválidas o fallo; 3 análisis indeterminado sin ruptura probada. Una ruptura parcial sale con 1 y conserva `status=PARTIAL`. JSON va a stdout y diagnósticos operativos a stderr. No depender del texto humano para automatizar.

## Extensión y verificación

Cada regla tiene identificador, precondiciones, dirección aplicable, resultado y ejemplos positivos, negativos y de incertidumbre. Se suprimen hallazgos hijos redundantes cuando desaparece toda la operación, conservando la explicación de mayor nivel. Los cambios en un esquema compartido se reportan por uso afectado.

Fixtures pequeñas y válidas para cada dialecto; assertions sobre veredicto, regla, ubicación y cobertura. Añadir pruebas de invariantes: identidad, representación JSON/YAML equivalente, reordenación, inline frente a referencia y repetibilidad. Invertir base/candidata no debe producir necesariamente el mismo veredicto.

Probar el dominio sin parser; después comprobar el adaptador con contratos reales. Las pruebas diferenciales con openapi-diff ayudan a encontrar discrepancias, pero una diferencia requiere revisar la especificación y la política. No asumir que la mayoría de herramientas tiene razón.

## Riesgos y decisiones pendientes

Incremento aprobado D05: añadir únicamente `RequestBodyContract` con presencia, formatos y procedencia a la instantánea del dominio, y `RequestBodiesRule` pura. El perfil adicional y su alcance se documentan en [request-body-profile.md](request-body-profile.md). No crear un modelo completo de OpenAPI ni introducir infraestructura para esta faceta. La CLI añade selección explícita y texto legible, conservando JSON y códigos de salida.

| Riesgo o decisión | Tratamiento / momento |
| --- | --- |
| Soporte de parser confundido con soporte de comparación 3.1 | Perfil publicado, cobertura visible y prueba de fidelidad antes del motor |
| Falsos positivos por dirección o objetos abiertos | Matriz de reglas y fixtures adversariales antes de cada regla |
| Composición y recursividad compleja | Revisión explícita; ampliar con corpus y prueba semántica |
| Clientes generados o deserializadores estrictos | Fuera de garantía; futuro perfil específico |
| Resolución externa, YAML hostil y consumo de CPU | Sin red/referencias externas; límites, tests de aislamiento y presupuesto |
| Dependencias transitivas y cambios del parser | Versiones fijadas, revisión de licencias/avisos de seguridad y regresión del corpus al actualizar |
| JDK 25 y herramientas | Verificar Maven y mvnd en Windows; no usar preview ni incubator |
| Valor frente a alternativas gratuitas | Piloto con contratos reales; validar ahorro de revisión antes de SaaS |
| Formato de informe, paquete, distribución y licencia del producto | Decidir antes de publicar la primera versión consumible |
| Política de códigos/media types y formats | Aprobar el perfil conservador descrito en el alcance; cualquier endurecimiento requiere perfil distinto |
| Esfuerzo y rendimiento | Estimar después de la prueba del parser; no prometer fechas sin evidencia |

Referencias normativas para el diseño: [OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html) y [OpenAPI 3.1.1](https://spec.openapis.org/oas/v3.1.1.html). Son referencias del análisis, no una promesa de soportar cada construcción. Las decisiones y reglas propuestas son políticas del producto y deben validarse con el corpus.
