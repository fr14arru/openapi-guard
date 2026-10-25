# Estado y roadmap del producto

Actualizado el 25 de octubre de 2026. Código propio gratuito bajo MIT, con aportaciones voluntarias pendientes de enlace. El modelo de pago anterior queda sustituido. Ver [transición y estado de apertura](../open-source.md).

## Qué existe

Motor Java puro con CLI Spring Boot no web; contratos OpenAPI YAML/JSON, perfiles versionados e informes de ruptura, incertidumbre y cobertura. El perfil full incorpora operaciones, parámetros, cuerpos, valores, objetos, arrays y política de respuestas. Ver [alcance](../mvp-scope.md).

La integración GitHub genera anotaciones, informes y comentario actualizable en PR. Permite rupturas intencionadas por política sin ocultar errores o revisiones pendientes. Existe evidencia remota parcial con un consumidor; no certifica todos los escenarios de forks o plataformas. El paquete local incluye JAR, Action, integridad, avisos y fuentes de terceros.

## Próximas entregas

1. **Apertura del código completada:** repositorio público independiente bajo MIT, historial nuevo y [reglas activas de main](../github-governance.md), conservando el original privado. Sigue pendiente una release binaria MIT.
2. **Simplificar la instalación:** las guías de usuario pasan al modelo gratuito y explican compilación y paquete local. Después, construir un candidato MIT trazable, validar ZIP y ejecución, y preparar una Action pública por commit/versión que evite copiar el paquete. Publicar solo tras revisar la entrega y sus permisos.
3. **Cerrar la experiencia del usuario:** probar instalación desde un repositorio limpio, PR compatible, incompatible, corregida y con entradas inválidas. Documentar actualización, incertidumbre y fallos de permisos. Recibir feedback por issues y contribuciones por fork; añadir PayPal al final.

Después, priorizar reglas nuevas por contratos que realmente lo necesiten: composición, serialización o negociación. Mantener TDD, dirección request/response y resultados parciales explícitos. No añadir cuentas, licencias de pago, infraestructura de cobro, base de datos ni servicios para este modelo.

El [roadmap histórico](../roadmap.md) conserva decisiones y evidencias anteriores; los compromisos comerciales que contiene ya no son el plan vigente.
