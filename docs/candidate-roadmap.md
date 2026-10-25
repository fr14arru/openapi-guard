> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Candidato de evaluación: procedimiento y matriz

Actualización del 16 de octubre: la estrategia vigente es [evaluación pública y ventas automáticas](commercial-launch.md), por decisión de Fran. El piloto privado/manual fue la estrategia inicial del 12 de octubre; se conserva aquí el procedimiento técnico y la matriz, no como requisito comercial vigente. El [registro actual de rc.2](pilot-delivery.md) identifica binarios, hashes y evidencia remota parcial.

## Roadmap ejecutado

| Orden | Entrega | Evidencia / cierre |
| --- | --- | --- |
| P01 | Integridad completa y transporte | Manifiesto v2 con todos los archivos, ZIP y SHA256 externo, rechazo de cambios/ausencias/extras y rutas ZIP peligrosas. |
| P02 | Piloto sin pasos de instalación pendientes | Kit con .guard-action, workflow real, contrato base y variantes; ocho escenarios de consumo independiente. |
| P03 | Candidato versionado y trazable | Versión Maven configurable, commit/version en el JAR, build limpio offline, pruebas, proof, paquete, extracción verificada y kit. |
| P04 | Preparación comercial | Términos privados en borrador, oferta y límites explícitos; checklist de aprobación antes de vender. |
| P05 | Verificación y entrega | Maven/mvnd, adaptadores, CI, Javadoc y portal; commits en rama de trabajo, sin publicación externa. |

No se amplía la semántica OpenAPI. El núcleo sigue independiente de Spring y GitHub. No se instala ninguna dependencia nueva, se añade almacenamiento ni se introduce un servicio remoto.

## Construir un candidato desde un checkout limpio

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/build-candidate.ps1 -Version 0.1.0-rc.1
```

El script exige Corretto 25 y herramientas existentes. Ejecuta mvnd clean verify y Maven verify offline con versión/commit explícitos, todas las pruebas Node y la receta CI. Después vincula hash/commit/version en build-proof.json y el JAR, prepara el paquete, crea ZIP/checksum, lo extrae y verifica, y genera pilot-kit. Un error detiene la entrega. No usa un modo skip-tests para aprobar candidatos.

Salida persistente bajo `dist/0.1.0-rc.1-COMMIT8/`, ignorada por Git y conservada al ejecutar Maven clean. Incluye logs, build-proof.json, package/, installed/, ZIP y checksum, y pilot-kit/. El directorio debe ser nuevo; para repetir el mismo commit/version usar otro OutputRoot. No hay limpieza automática de entregas previas.

Los builds habituales conservan 0.1.0-SNAPSHOT. `-Drevision=0.1.0-rc.1` afecta solo al build solicitado. Los tests del JAR utilizan el artefacto realmente empaquetado; los adaptadores aceptan GUARD_TEST_JAR para validar el candidato sin reconstruir el motor del consumidor. No se publica el proyecto como dependencia Maven: una distribución Maven para terceros requeriría revisar el POM publicado, aparte de esta entrega binaria.

El proof es evidencia del procedimiento local, no firma de un tercero ni attestation criptográfica. La publicación sigue marcada como pendiente; un proof válido no aprueba licencias o ventas. El manifiesto de evaluación sin proof conserva su carácter de preparación manual no vinculada a un build certificado por el procedimiento.

## Matriz de validación del consumidor

1. Abrir el README de pilot-kit/ y utilizar un repositorio de prueba autorizado; el consumidor de Fran ya existe y tiene PR. Para uno nuevo, inicializar el kit como main, versionar sus archivos y configurar su propio remoto. No cambiar el origin del proyecto del motor. El kit actual sigue marcado para evaluación privada; no publicarlo como distribución aprobada.
2. Desde una rama nueva del contrato base, copiar pilot-cases/breaking.yaml sobre contracts/api.yaml, commit/push y abrir PR a main. No hace falta compilar en el consumidor: .guard-action incluye el JAR.
3. Abrir Checks, anotaciones, resumen y artefactos. Confirmar los commits base/candidato del JSON y las posiciones del contrato nuevo. Guardar el enlace de ejecución.
4. Repetir cada caso desde la base: equivalente, compatible, ruptura, incierto, inválido, eliminación, renombrado y cambios ajenos al contrato. Los esperados son 0,0,1,3,2,1,0,0.
5. Evaluar facilidad de instalación, mensajes, tiempo y casos indeterminados con un contrato autorizado del otro proyecto. Los ejemplos sintéticos no demuestran ausencia de falsos positivos en todos los contratos.

El workflow se dispara en PR y merge queue; no en un push directo a main. Las PR se comparan contra el merge propuesto. Primero se valida y después el consumidor decide si exige el check para fusionar. Fork, merge queue y otros sistemas operativos necesitan ejecuciones reales adicionales cuando estén incluidos en la oferta.

## Pruebas y límites de instalación

Se verifica el ZIP con un SHA256 obtenido por un canal confiable, antes de extraer. Los scripts rechazan destinos existentes, traversal, rutas absolutas/Windows, duplicados, symlinks ZIP y presupuestos excedidos. Node valida un conjunto cerrado de archivos regulares: ausencias, extras, cambios o symlinks hacen fallar la entrega. La Action verifica el paquete en modo bundled antes de llamar al adaptador. SHA256 no reemplaza firma ni control del publicador.

Si una extracción falla después de crear el destino, tratarlo como parcial y no ejecutarlo. Elegir otro destino para reintentar; no se borra una ruta del usuario automáticamente. Para CLI basta Java 25; para instalación verificable y Action se requiere Node >=20, y para preparar/extractar kits estos scripts requieren PowerShell 7. El piloto GitHub usa Windows/Bash del runner y Corretto 25. Linux/macOS no se ofrecen como soporte validado todavía.

## Propuesta comercial histórica

La oferta inicial proponía evaluación privada y soporte/venta manual. Ha sido sustituida por la [oferta de evaluación pública y autoservicio](pilot-offer.md). No hay precio final, cobro activado ni renovación automática. El texto de términos sigue siendo un borrador; debe completarse antes de conceder derechos o vender.

No prometemos certificar toda la API, corregir rupturas automáticamente ni resolver todo JSON Schema. Los contratos se procesan localmente/en el runner, sin telemetría ni subida obligatoria. Soporte solo recibe ejemplos compartidos expresamente, preferentemente sintéticos o redactados.

La [documentación de cobros de GitHub](https://docs.github.com/en/apps/github-marketplace/selling-your-app-on-github-marketplace/pricing-plans-for-github-marketplace-apps) distingue Apps de publicación de Actions. La oferta privada no presupone que un listado de Action cobre suscripciones por nosotros. Publicar en [Marketplace](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/publish-in-github-marketplace) tiene requisitos distintos y no está autorizado por construir el kit.

## Puertas antes de comercializar a terceros

| Puerta | Estado al preparar el candidato | Responsable / evidencia pendiente |
| --- | --- | --- |
| Build, integridad y casos locales | Verificable mediante build-candidate y logs | Candidato generado sin fallos. |
| PR real en otro repositorio | Evidencia parcial obtenida | Ruptura permitida y comentario visibles; completar otros escenarios y comprobar el candidato exacto. |
| Términos y licencia del producto | Borrador, no efectivos | Fran: identidad, derechos concedidos y texto revisado/aprobado. |
| Atribuciones de 35 bibliotecas y loader | Evidencia embebida, clearance pendiente | Revisar textos, licencias heredadas y componentes incluidos. No tratar inventario automático como aprobación legal. |
| Precio, facturación y soporte | Confirmados 4,99 EUR/año/repositorio y preparación de Lemon Squeezy | Resolver impuestos, condiciones, atención limitada y pruebas antes de activar ventas. |
| Publicación/release/Marketplace | No autorizada ni realizada | Aprobación explícita de destino, archivos y modalidad, después de revisar el candidato. |

Resultado: candidato técnico listo para probar y procesar contratos. La estrategia vigente busca disponibilidad pública sin tres equipos obligatorios; no declarar venta lista hasta cerrar las puertas comerciales. Una protección de main, una PR real o aprobación legal no se simulan por pasar tests locales.
