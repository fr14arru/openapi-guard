> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Roadmap incremental revisado

**Decisiones confirmadas del 20 de octubre:** proveedor en España y en nombre propio; uso profesional o personal; total anual de 4,99 EUR por repositorio con impuestos incluidos. [Borrador y protección de consumidores](product-license-draft.md). El estado pendiente de impuestos del registro anterior es histórico; faltan aviso legal, aprobación del texto y activación/publicación.

**Actualización del 19 de octubre:** atribuciones recopiladas con catálogo y hashes para 35 bibliotecas más loader, textos completos y fuentes EPL en paquetes nuevos. Ver [cierre técnico](third-party-review.md). El [borrador de licencia propia](product-license-draft.md) requiere identidad del proveedor y aprobación; no se ha publicado una release ni activado ventas.

**Prioridad del 16 de octubre:** [cierre técnico y lanzamiento público en autoservicio](commercial-launch.md). Fran sustituye el piloto privado/manual por evaluación pública y ventas automáticas; no se exige conseguir tres equipos. Código estable integrado en main, candidato rc.2 registrado y revisión de terceros con huecos concretos. Confirma 4,99 EUR al año por repositorio y preparar Lemon Squeezy; impuestos, licencia y activación de ventas/publicación siguen pendientes.

La ejecución histórica del 13 de octubre está en [roadmap del candidato](candidate-roadmap.md): integridad, transporte, kit y versión/procedencia. Se conserva su procedimiento técnico; la estrategia comercial vigente está en el plan de lanzamiento.

Estado consolidado actualizado: [roadmap del producto](portal/roadmap.md). La continuación del 11 de octubre prepara un [paquete evaluable y Action compuesta](evaluable-distribution.md), sin publicar. La distribución legal/versionada y el piloto remoto preceden a más reglas o infraestructura. Las tareas siguientes conservan el historial y dependencias originales; no interpretar tareas ya implementadas como trabajo nuevo pendiente.

Continuación autorizada: D08 incorpora arrays/items/minItems/maxItems; D09 añade política de respuestas y `--profile=full`. Hay receta CI neutral con scripts probados contra el JAR. Los ocho commits de la entrega hasta ccd9e1c se han integrado mediante fast-forward en main y publicado en GitHub tras autorización explícita del usuario. Ver [arrays](array-schemas-profile.md), [respuestas](response-policy-profile.md) y [CI](ci.md). D10 tiene empaquetado/guía/receta; no se da por cerrada la revisión de atribuciones de distribución ni la validación comercial.

Estado: D01a, D02 y D03a/D03b/D03c desarrolladas. D05, D06 y D07, autorizadas con TDD, están implementadas junto con salida legible. D05 cubre cuerpos y formatos; D06 valores simples por dirección; D07 propiedades, obligatoriedad, objetos abiertos/cerrados y proyección de acceso. Swagger Parser seleccionado. D04 tiene controles iniciales de entrada/CLI incorporados, sin dar por cerrada toda su validación. Ver [perfil ampliado](object-schemas-profile.md) y [verificación](development.md).

Antes de ampliar reglas se reforzó D02 con 62 pruebas. D03 añade extracción de parámetros efectivos, dos reglas de obligatoriedad, pruebas del JAR y [146 pares sintéticos](contract-corpus.md). El usuario no dispone de contratos reales; el corpus sustituye la demostración técnica, mientras que la validación comercial con clientes sigue pendiente. Ver [estado de los tres pasos](next-steps.md) y [batería](test-strategy.md).

El plan anterior exigía completar un grafo de esquemas y casi todas las reglas antes de tener CLI o feedback comercial. Se sustituye por entregas de extremo a extremo: cada incremento se puede ejecutar, explica sus límites y sirve para revisar contratos del piloto. No hay fechas cerradas antes de medir la prueba técnica.

## Preparación antes de desarrollar

1. Aprobar el alcance revisado y obtener 2–3 pares representativos de contratos autorizados. Registrar dolor concreto, dialecto, reglas necesarias y propuesta de piloto de pago. Si dependen de 3.1 o composición, reordenar el alcance antes de construir.
2. Presentar versiones concretas de Spring Boot, dependencias/plugins y finalidad; aprobar descargas. La prueba inicial puede evaluar openapi-diff y Swagger Parser; no instalar todas las alternativas por defecto. Verificar compatibilidad Java 25 y alineación de dependencias entre Boot y parser. Revisar licencias/NOTICE de artefactos y transitivas antes de distribuir.

No contactar equipos, enviar mensajes ni realizar cobros sin instrucciones del usuario. La preparación comercial es una actividad del responsable, con ayuda documental si la solicita.

## Tres primeras tareas de desarrollo

| ID | Tarea | Depende de | Resultado verificable |
| --- | --- | --- | --- |
| D01a | Estructura de un módulo Java 25 con Spring Boot no web y JUnit 5, completada | Implementación y descargas aprobadas | POM, cuatro paquetes, prueba de arranque/cierre y builds Maven/mvnd correctos |
| D01b | Selección acotada de Swagger Parser para operaciones; evaluación de motores de esquemas pendiente | D01a y aprobación de nuevas descargas | Corpus de identidades 3.0/3.1; no se evalúan todavía reglas de esquemas de terceros |
| D02 | Primera comparación ejecutable desde CLI Boot: operación eliminada | D01b | Dos archivos locales → JSON/texto, regla estable y códigos 0/1/2/3; banner/logs no contaminan JSON y el contexto se cierra; facetas no implementadas figuran como pendientes; no green global |
| D03 | Parámetros efectivos y obligatoriedad en esa CLI | D02 | Herencia/overrides, alta obligatoria y opcional→obligatorio probados; eliminación/serialización inciertas visibles; demostración con contrato del piloto |

D01 es una prueba acotada, no un framework. Separar en commits el build y el corpus/decisión. Criterios para reutilizar: cumplir la matriz semántica, exponer suficiente cobertura y procedencia, poder impedir E/S externa y no exigir un fork costoso. Un motor que solo devuelve booleano incompatible/compatible no basta para garantizar el perfil. Si falla, documentar la discrepancia y usar Swagger Parser con reglas propias mínimas. Aprobar nuevas dependencias solo si cambia la lista autorizada.

## Incrementos posteriores por dependencia

| ID | Tarea pequeña | Depende de | Evidencia de finalización |
| --- | --- | --- | --- |
| D04 | Lectura y CLI robustas | D02 | Tamaño/alias/profundidad limitados, entradas inválidas y referencias externas rechazadas; rutas Windows con espacios, stdout/stderr y JSON válidos |
| D05 | Cuerpo requerido y media types de petición | D03, D04 | Required false→true; retirada de cuerpo como revisión y media types bajo la política documentada |
| D06 | Tipos, enums y nullabilidad de valores simples | D05 | Requests/responses con dirección inversa, integer/number, intersecciones de constraints soportadas y hallazgos inciertos |
| D07 | Objetos: properties, required, additionalProperties booleano | D06 | Propiedades opcionales/obligatorias y objetos abiertos/cerrados sin falsos positivos; proyección readOnly/writeOnly |
| D08 | Arrays y referencias internas acíclicas | D07 | Items y objetos anidados, inline/ref equivalentes, componentes compartidos por dirección; ciclos terminan con revisión |
| D09 | Respuestas documentadas y cierre del perfil mínimo | D08 | Comparación de cuerpos por código/media type; default/rangos/negociación no resueltos dan revisión; exclusiones diferenciadas de cobertura faltante |
| D10 | Paquete distribuible y receta CI neutral | D09 | JAR Java 25, guía sin IDE, notices requeridos, informe versionado y tests de regresión; medición básica y build Maven/mvnd |
| D11 | Ajuste de bloqueadores de venta | Feedback obtenido desde D02, D10 | Resolver una familia que bloquee contratos reales o acotar claramente el segmento; no ampliar todo OpenAPI por calendario |

Cada tarea incorpora objetos de dominio y normalización que necesita; no hay tarea previa para modelar toda la especificación. El informe mínimo se diseña en D02 y se mantiene estable: IDs, ruleId, severidad, ubicación, descripción, perfil y cobertura. No posponer la CLI hasta completar el motor.

## Feedback y venta durante el desarrollo

- Desde D02: demostrar salida real y capturar las rupturas que más tiempo cuestan revisar. Es una versión de evaluación con cobertura todavía limitada.
- Desde D03: revisar pares reales y medir falsos positivos y usos indeterminados. Si todo queda indeterminado, reordenar el plan; no esperar a D10 para descubrirlo.
- Antes de D10: acordar oferta y precio del piloto con el responsable. La primera entrega de pago solo se ofrece si el perfil resuelve cambios relevantes de esos contratos, su garantía es entendible y hay interés explícito.
- Tras D10/D11: piloto pagado con onboarding y soporte manual; validar ahorro, uso repetido y renovación. El éxito comercial no se da por conseguido al empaquetar un JAR.

No convertir el feedback o la venta en una dependencia de todos los tests técnicos: el responsable puede avanzar esa validación mientras se desarrollan incrementos. Tampoco confundir una herramienta parcial de evaluación con un MVP terminado.

## Ampliaciones condicionadas

| Ampliación | Cuándo | Prueba necesaria |
| --- | --- | --- |
| Límites y additionalProperties con schema | Bloquean contratos del piloto | Dirección, constraints conjuntas y ejemplo manual que justifique ruptura |
| OpenAPI 3.1 | Demanda real, o requisito del piloto antes de v1 | Null/uniones, exclusive bounds y siblings de refs; diferencias de dialecto verificadas |
| Ciclos y composición | Uso frecuente que impida vender | Corpus recursivo/composición y cobertura demostrable, sin expansión ilimitada |
| Action específica GitHub | Fricción repetida al usar CLI en CI | Base/candidata correctas, permisos mínimos y errores accionables |
| Facetas seguridad/headers/servers | Clientes necesitan garantía más amplia | Perfil/versionado nuevo y fixtures de esas facetas |
| GitHub App o adaptador HTTP sobre Boot | Necesidad de gestión central validada | Requisitos HTTP/auth, límites de recursos y coste estimado antes de despliegue; Boot ya se usa en la CLI |
| Cuentas, persistencia y pagos automáticos | Suscripciones manuales suficientes para justificar operación | Retención/aislamiento y ciclo de cobro verificables |

## Criterio de entrega

El 2026-10-10 se autorizó documentación/organización y cuatro funcionalidades elegidas desde producto. Se entregan contratos por nombre, Markdown, guardado seguro y lanzador de un comando con demo, además de descubrimiento de perfiles y Javadoc estricto. No cambia la semántica del motor ni se incorpora infraestructura. Ver [objetivos, uso y pruebas](product-usability.md). Distribución versionada, piloto real y activación de protección GitHub continúan como seguimiento.

Tests por regla y dirección, incertidumbre visible, cero E/S externa implícita, build independiente del editor y facetas declaradas. No exigir benchmark ambicioso, motor completo de JSON Schema, AST propio general ni toda una plataforma SaaS para vender el primer piloto.

D01a/D02/D03 terminaron anteriormente. D05–D09 están implementados con reglas propias acotadas y perfiles explícitos, sin incorporar otro motor. D10 mantiene pendientes atribuciones y validación comercial. El usuario autorizó tres incrementos de integración: comparación Git y workflow están implementados y verificados localmente; ejecución remota y check obligatorio de main siguen pendientes de acceso y aprobación de descargas. D11 tiene esta entrega inicial, sin GitHub App ni SaaS. Ver [integración y tareas de activación](git-integration.md), [arquitectura](architecture.md) y [resultados del build](development.md).
