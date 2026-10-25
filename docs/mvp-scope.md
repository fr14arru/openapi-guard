# Alcance del MVP

Estado: alcance de referencia aprobado. Se han implementado operaciones, parámetros, cuerpos, valores, objetos, arrays y política de respuestas (D03/D05–D09). `--profile=full` reúne la cobertura implementada; la validación comercial sigue pendiente. Las decisiones iniciales descritas abajo se concretan en los perfiles versionados publicados. Ver [política y límites actuales](response-policy-profile.md), [arrays](array-schemas-profile.md) y [desarrollo local](development.md).

## Objetivo y garantía

Primer entregable vendible: CLI local Spring Boot en modo no web para comparar dos contratos y fallar un job CI con una explicación útil. Motor Java independiente de Spring, resultados JSON/texto y cero infraestructura propia. Ofrecer un piloto de pago con onboarding manual, soporte y un perfil de reglas publicado; no esperar a tener SaaS o facturación automática para validar una venta.

Perspectiva: clientes del contrato base frente al contrato candidato. Una petición antigua debe seguir aceptándose y una respuesta nueva debe seguir siendo válida para el cliente antiguo. «Compatible» significa compatible en las facetas de operaciones, parámetros y datos del perfil publicado, con cobertura completa de sus usos relevantes. El informe enumera áreas excluidas; nunca garantiza seguridad, disponibilidad real, comportamiento ni funcionamiento de todos los clientes generados.

## Imprescindible para la primera entrega

- Dos documentos UTF-8 locales en JSON o YAML; comparación base → candidata explícita.
- OpenAPI 3.0.x para la primera versión de pago. Soporte 3.1.x posterior, adelantable si los primeros clientes lo necesitan. Otras familias o comparaciones entre familias producen diagnóstico explícito; no realizar migraciones automáticas.
- Referencias internas acíclicas, componentes reutilizados, arrays y objetos anidados dentro del perfil. Ciclos producen revisión sin bloquear ni expandir indefinidamente. Referencias externas producen error de entrada no soportada, sin intentar descargarlas.
- Identificación de operaciones por ruta y método; parámetros efectivos por nombre y ubicación, con overrides de operación y nombres de headers sin distinguir mayúsculas.
- Reglas de disponibilidad de operación, parámetros, obligatoriedad del cuerpo, tipos, enums, nullabilidad, propiedades y arrays. D06/D08 han adelantado límites numéricos, de longitud de strings y de arrays conjuntamente. D09 añade la política explícita de disponibilidad de códigos/formatos de respuesta.
- Proyección de propiedades `readOnly` y `writeOnly` según dirección y obligatoriedad aplicable. No interpretar estas anotaciones como prohibición general de enviar la propiedad.
- Informe estable con identificadores, severidad, ubicación, descripción, evidencia mínima, diagnósticos y cobertura.
- CLI con códigos de salida documentados y diferenciación de error, incompatibilidad e incertidumbre.
- Arranque y configuración con Spring Boot, aprobado por el usuario por su experiencia previa. Beans en el borde, motor comprobable sin contexto Spring y ninguna dependencia web/persistencia innecesaria.
- Corpus de regresión, instrucciones Maven/mvnd, receta CI neutral y documentación del perfil y sus exclusiones. Validar contratos de 1–3 equipos durante el desarrollo, no al final.

## Perfil de esquemas v1 propuesto

Soportar tipos explícitos `string`, `integer`, `number`, `boolean`, `object`, `array`; nullabilidad 3.0; `enum`; `properties`, `required`, `additionalProperties` booleano y `items` homogéneo. `additionalProperties` con esquema queda para la siguiente ampliación. `integer` es un subconjunto de `number`, no un mero cambio textual. Los defaults de objetos abiertos se interpretan según el dialecto. No implementar una réplica de JSON Schema para llegar al piloto.

Los resultados de inclusión se limitan a combinaciones que el motor pueda probar. Tipos ausentes, esquemas booleanos 3.1 y combinaciones de constraints fuera del subconjunto no se convierten en tipos inventados: revisión necesaria.

`format` se trata como anotación en este perfil de compatibilidad de datos; sus cambios generan advertencia para revisar clientes o validadores. `description`, `summary`, `example`, `examples`, tags y orden de campos no influyen en el veredicto. Cambios en `default`, `deprecated`, `operationId` y extensiones de proveedor pueden informarse como advertencias sobre comportamiento o herramientas, sin afirmar ruptura de datos.

`additionalProperties` con esquema, `allOf`, `oneOf`, `anyOf`, `not`, discriminadores, `pattern`, enums/const compuestos, tuples/`prefixItems`, `patternProperties`, `unevaluatedProperties`, dependencias, condicionales, `uniqueItems`, vocabularios personalizados y referencias dinámicas se detectan pero permanecen sin cubrir. Los límites numéricos/de longitud y const escalar 3.1 ya están implementados en D06; minItems/maxItems en D08. Un uso alcanzable que requiera construcciones opacas produce análisis parcial, incluso si no han cambiado. No basta comparar el texto para garantizar que no interacciona con otros cambios. Una ruptura demostrada puede coexistir con PARTIAL/BREAKING. Si esa restricción inutiliza los contratos del piloto, ampliar esa familia antes de vender.

El perfil de cuerpos v1 se centra en JSON (`application/json` y tipos concretos `application/*+json`, conservando cada identidad de media type). No asume equivalencia de distintos media types. Otros cuerpos, encoding multipart/XML, parámetros mediante `content` y serializaciones de parámetros que no se puedan comparar de forma fiable producen revisión en el uso correspondiente. Los parámetros simples se evalúan con sus defaults de `style`/`explode`; cambiar `style`, `explode`, `allowReserved` o ubicación obliga a revisión del protocolo aunque el tipo siga igual.

Las rutas se comparan literalmente. Una coincidencia única por plantilla con solo renombre de variable (`/pets/{id}` → `/pets/{petId}`) se marca para revisión y no se presenta automáticamente como eliminación de endpoint; si es ambigua, se registra cobertura incompleta en esas operaciones. El MVP no realiza matching heurístico de rutas movidas.

## Catálogo inicial y dirección

El catálogo siguiente describe la dirección semántica; los identificadores realmente emitidos se publican en cada perfil. D06/D08 ya comparan límites soportados. Revisión dentro del perfil impide un resultado verde. Las reglas se evalúan en operaciones base que siguen existiendo; esquemas de una operación nueva independiente no necesitan probar compatibilidad con un cliente anterior.

| Cambio | Petición | Respuesta / operación |
| --- | --- | --- |
| Eliminar ruta o método | No aplica | `BREAKING`: desaparece una operación utilizable |
| Añadir ruta o método independiente | No aplica | `NON_BREAKING`, salvo conflicto de matching que requiera revisión |
| Añadir parámetro obligatorio | `BREAKING`; añadir opcional no rompe | No aplica |
| Parámetro opcional → obligatorio | `BREAKING` | No aplica |
| Parámetro obligatorio → opcional | `NON_BREAKING` | No aplica |
| Eliminar parámetro | `REVIEW_REQUIRED`: ya no se garantiza su soporte; OpenAPI no define qué hace el servidor con parámetros desconocidos | No aplica |
| Cuerpo ausente/opcional → obligatorio | `BREAKING` | No aplica |
| Eliminar cuerpo documentado | `REVIEW_REQUIRED`: desaparece su garantía; la ausencia de requestBody no demuestra rechazo por el servidor | No aplica |
| Enum se reduce | `BREAKING` si excluye un valor antes válido | `NON_BREAKING`: menos salidas posibles |
| Enum se amplía o elimina la restricción | `NON_BREAKING` | `BREAKING` si permite valores fuera del contrato base |
| Tipo number → integer | `BREAKING` si excluye valores posibles | `NON_BREAKING` |
| Tipo integer → number | `NON_BREAKING` | `BREAKING` si permite valores antes inválidos |
| Tipos disjuntos, p. ej. string → object | `BREAKING` si el esquema es satisfacible | `BREAKING` si el esquema es satisfacible |
| Permitir null | `NON_BREAKING` | `BREAKING` si antes estaba excluido |
| Dejar de permitir null | `BREAKING` | `NON_BREAKING` |
| Propiedad opcional → obligatoria o nueva obligatoria | `BREAKING` si no era ya exigida/implicada | `NON_BREAKING` por obligatoriedad; comprobar también su tipo y objetos cerrados |
| Propiedad obligatoria → opcional | `NON_BREAKING` | `BREAKING`: el cliente ya no puede contar con su presencia |
| Eliminar propiedad obligatoria | Depende de `additionalProperties` y constraints; no asumir ruptura | `BREAKING` cuando permite respuestas sin esa propiedad |
| Eliminar propiedad opcional | Depende de si el objeto queda abierto/cerrado y de su esquema adicional | Con objeto cerrado nuevo suele restringir salidas; con objeto abierto puede ampliar valores y romper |
| Añadir propiedad opcional | Si antes se aceptaba como adicional, su nueva restricción puede romper | Rompe si el cliente base cerraba el objeto o restringía ese nombre como adicional |
| `additionalProperties` true → false | Restricción potencialmente `BREAKING` | Restricción potencialmente `NON_BREAKING` |
| Límites más restrictivos, ampliación posterior | `BREAKING` si excluyen datos posibles | `NON_BREAKING` si no hay otros cambios |
| Límites menos restrictivos, ampliación posterior | `NON_BREAKING` si no hay otros cambios | `BREAKING` si amplían datos posibles |
| Cambiar esquema de `items` | Misma inclusión que la petición, recursivamente | Misma inclusión que la respuesta, recursivamente |
| Eliminar código de respuesta | No aplica | `REVIEW_REQUIRED` por defecto; advertencia no bloqueante solo si se prueba reducción de salidas sin fallback que amplíe datos |
| Añadir código de respuesta | No aplica | `REVIEW_REQUIRED` hasta comprobar cobertura previa por `default` o rangos y su schema |
| Eliminar media type de petición | `BREAKING` si ya no hay una cobertura equivalente demostrable | No aplica |
| Añadir media type de respuesta | No aplica | `REVIEW_REQUIRED` por negociación; ruptura solo si se prueba que un cliente antiguo puede recibir una representación que no admite |
| Eliminar media type de respuesta | No aplica | Advertencia: menor oferta; su negociación puede requerir revisión |

La matriz describe cambios aislados con el resto del contrato equivalente y esquemas satisfacibles. La inclusión de tipos, enums y, cuando se implementen, límites se analiza conjuntamente. No reportar una ruptura solo por un diff de keyword: un enum podría impedir todos los valores que aparentemente añade un límite más amplio. Si no puede probarse el efecto, emitir revisión. Esquemas contradictorios o de satisfacibilidad no comprobable requieren revisión o diagnóstico, no conclusiones vacías por conjuntos imposibles.

Para objetos: comparar propiedades explícitas y su espacio residual de nombres adicionales. En peticiones, eliminar una propiedad de un objeto que sigue abierto puede ampliar entradas; cerrarlo sí puede rechazarlas. En respuestas, una propiedad opcional retirada en un objeto abierto puede quedar sin constraint, permitiendo valores antes inválidos. Este detalle debe tener tests separados.

Las respuestas con `default`, rangos `2XX`, comodines de contenido o negociación que el perfil no resuelva impiden certificar los usos afectados. Por ejemplo, quitar un `200` específico puede dejar ese código cubierto por un `default` más permisivo: no es necesariamente una reducción de salidas. Además, OpenAPI permite documentar solo respuestas conocidas; ausencia de un código no demuestra imposibilidad en ejecución. Las conclusiones sobre códigos se limitan al perfil de respuestas documentadas y cualquier incertidumbre es visible. Si el piloto necesita una política de «respuesta documentada prometida», se añadirá como perfil explícito.

Seguridad, servidores/base URL, callbacks, webhooks, links y headers de respuesta son facetas excluidas del producto v1 y siempre aparecen como tales en el informe. Su presencia no convierte automáticamente en parcial el análisis de datos que sí está comprometido. Un cambio detectado en ellas se informa como advertencia fuera del perfil, sin conclusión semántica. No ofrecer compatibilidad global: un API con autenticación cambiada puede conservar el mismo contrato de datos. A diferencia de esas facetas independientes, un `allOf` o `pattern` que afecta al cuerpo analizado sí impide certificar ese uso. Cambiar extensiones que alteren los datos también obliga a revisión; no asumir que todas las extensiones son metadata.

## Reglas difíciles y cómo probarlas

| Dificultad | Fixtures y criterio |
| --- | --- |
| Dirección | Mismo enum, tipo y límite usado en request y response; veredictos opuestos donde proceda |
| Objetos abiertos/cerrados | Eliminar/añadir propiedad opcional y obligatoria con `additionalProperties` ausente, true, false y schema; comprobar un valor concreto excluido/admitido |
| Constraints combinadas | Enum intersectado con tipo/límites; ampliación textual sin ampliación real; ningún error especulativo |
| `readOnly` / `writeOnly` | Propiedad requerida reutilizada en ambas direcciones; cambiar anotaciones; defaults y nullabilidad separados |
| Parámetros efectivos | Herencia en Path Item, override local, mismo nombre en query/header, headers con distinto casing y reordenación |
| Dialectos | Probar `nullable` 3.0 y rechazar otros dialectos v1; preparar los casos null/exclusive bounds/siblings cuando se amplíe a 3.1 |
| Referencias y ciclos | Inline/ref acíclicas equivalentes, referencia rota, ciclo propio/mutuo y usos en ambas direcciones; los ciclos dan revisión y terminan |
| Códigos y media types | Eliminar/añadir 200/201, `default`, `2XX`, JSON/sufijos JSON y comodines; revisión visible donde falte prueba |
| Composición | `allOf` y uniones presentes; exigir informe parcial, no un verde por igualdad de texto |
| Seguridad de entrada | YAML con alias expansivos, tamaño/límite excedido y `$ref` remoto o relativo; verificar que no se abre red ni archivos externos |
| Determinismo | JSON/YAML equivalentes, orden distinto, dos ejecuciones y Windows; IDs semánticos y orden estables |

## Funcionalidades posteriores

1. Ampliar cobertura según contratos del piloto: límites, additionalProperties con esquema, 3.1, ciclos y composición primero si bloquean ventas; seguridad, headers, rangos y serialización según demanda. No prometer una solución general a JSON Schema.
2. Uso CI mediante la CLI, después GitHub Action fina y finalmente GitHub App con permisos mínimos y comparación en pull requests.
3. Baselines versionadas, políticas configurables, excepciones auditables y caducidad; ninguna supresión silenciosa.
4. Adaptador HTTP sobre Spring Boot y consola web si los usuarios los necesitan. Boot ya forma parte de la CLI; exponer una API y desplegar un servicio son ampliaciones posteriores. Un único proceso inicialmente; persistencia solo cuando haya historial, cuentas o suscripciones que lo exijan.
5. Multiempresa, autenticación, facturación recurrente y retención de informes. Son requisitos comerciales futuros, no trabajo implícito del motor.

## Criterios de salida y piloto comercial

- Todos los casos comprometidos tienen pruebas de ruptura, compatibilidad y exclusiones aplicables; cada caso incierto queda visible.
- Build y tests equivalentes con Maven estándar y mvnd en Windows, sin IDE ni funcionalidades preview.
- JSON consumible automáticamente, orden/IDs estables y códigos de salida probados.
- Ningún acceso externo implícito; límites de entrada y recorrido demostrados con fixtures.
- Medición básica de duración y memoria con corpus/equipo identificados y límites documentados. No hacer de un objetivo arbitrario de latencia una puerta de venta antes de conocer el uso real.
- Piloto con 1–3 equipos y contratos reales autorizados desde la primera CLI útil: medir tiempo ahorrado, falsos positivos confirmados, casos sin cobertura y disposición a pagar. No almacenar contratos sin consentimiento.

Hipótesis de ingresos iniciales: piloto de pago y licencia/soporte comercial con alta manual. Hipótesis de recurrencia: suscripción por actualizaciones de reglas y soporte si existe demanda, y después integración gestionada, políticas de equipo e historial. No implementar un sistema DRM, suscripciones ni cobros automáticos antes de validar la compra; tampoco asumir que una CLI por sí sola genera recurrencia. Acordar precio y oferta con los primeros equipos antes de ampliar infraestructura.

Ver [arquitectura](architecture.md) y [roadmap](roadmap.md).
