> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Lanzamiento público y ventas con poca operación

**Actualización posterior del 22 de octubre:** Fran ha leído y aprobado la redacción completa; ver [aprobación](license-approval.md). Las menciones inferiores a aprobación pendiente describen el estado anterior. Quedan aviso legal, privacidad, ensamblado y publicación de la licencia efectiva y ZIP final. [Siguiente paso](legal-setup.md).

## Cierre comercial confirmado el 22 de octubre de 2026

Fran confirma evaluación de 30 días desde el primer uso sin tarjeta; todos los colaboradores del repositorio y ejecuciones sin límite cobrado por Guard; traslado de licencia cesando el uso en el repositorio anterior; cancelación sin renovación con uso hasta terminar el periodo pagado y posterior renovación o cese; versiones publicadas durante la suscripción y soporte por correo sin SLA ni promesas de funcionalidades. No se añadirá servidor de activación ni verificación técnica de licencias.

La [redacción para revisión final](product-license-draft.md) recoge estas decisiones. La confirmación de negocio no hace efectiva la licencia ni autoriza nuevas cláusulas legales, ventas, publicación o modificación de candidatos congelados.

Según comunica Fran el 21 de octubre, la cuenta Lemon Squeezy está activada y el producto existe en modo de prueba. No se han inspeccionado la cuenta, el checkout, la entrega ni una compra real. La documentación pública se mantiene separada del motor en [el repositorio de guías](https://github.com/fr14arru/OpenAPI-Breaking-Change-Guard-Docs).

Actualizado el 20 de octubre de 2026. Fran ha cambiado la estrategia: evaluación pública y autoservicio, sin exigir tres equipos piloto ni hacer altas y ventas manuales. Confirma operación desde España en nombre propio, uso profesional o personal y **4,99 EUR al año por repositorio, impuestos incluidos**, con **Lemon Squeezy** para preparar ventas. Aviso legal, condiciones definitivas y apertura de ventas siguen pendientes; Fran comunica cuenta activada y producto de prueba. Estas decisiones no activan términos, cuentas, pagos ni publicaciones.

## Punto de partida y orden de cierre

El código estable hasta b6e32d4 está integrado y subido a main. La entrega técnica verificable se registra en [registro del candidato](pilot-delivery.md). Incluye CLI, reglas full, Action, anotaciones, informes y comentario actualizable de PR. El núcleo no necesita Spring ni acceso a GitHub; el arranque exterior utiliza Spring Boot no web.

La PR sintética del consumidor ha mostrado rupturas, check satisfactorio por política y comentario visible en Conversation. Eso no acredita toda la matriz remota ni contratos de clientes. Quedan ejecuciones remotas compatibles, inválidas e inciertas y navegación de informes, aunque existan pruebas locales de esos comportamientos.

| Paso | Resultado concreto | Estado / criterio de cierre |
| --- | --- | --- |
| C01 | Congelar código estable y candidato con ZIP/hashes | Integración realizada; evidencia exacta en el registro de entrega. |
| C02 | Licencia del producto, oferta y terceros | Recopilación técnica de terceros implementada con textos/fuentes y catálogo; [licencia propia](product-license-draft.md) y obligaciones comerciales pendientes de aprobación del titular. |
| C03 | Evaluación pública y Action instalable por referencia | Diseñada; implementar con TDD tras C02. Un consumidor no copia JAR ni .guard-action. |
| C04 | Cobro, entrega y cancelación automáticos | Lemon Squeezy elegido para preparar ventas; configurar en modo prueba y probar el ciclo completo antes de cobrar. |
| C05 | Lanzamiento público y feedback voluntario | Publicar solo la versión aprobada y verificada, con instrucciones, oferta y canal de incidencias. |

No ampliar reglas para cerrar estos pasos. Los contratos aportados voluntariamente por usuarios orientarán las siguientes mejoras; no se exige disponer de tres equipos ni subir contratos para usar la herramienta.

## Precio confirmado y condiciones para revisar

Precio elegido por Fran: **total de 4,99 EUR al año por repositorio, impuestos incluidos**, con un único cobro anual. La propuesta mensual anterior queda descartada. Se admite uso profesional y personal. El alcance confirmado incluye ejecuciones sin tarificación propia por PR y acceso a versiones publicadas durante la suscripción. GitHub mantiene sus costes/límites de runner. No ofrecer soporte individual ilimitado, SLA, onboarding por llamada ni desarrollos a medida a ese precio, preservando los derechos imperativos de consumidores. El checkout no debe sumar impuestos a esos 4,99 EUR.

Cobros totales de referencia, con impuestos incluidos: 100 repositorios suponen 499 EUR/año; 1.000, 4.990 EUR/año. No son ingresos netos: restar impuestos incluidos, comisiones y otros costes. El equivalente de 0,416 EUR/mes por repositorio es solo una división orientativa, no una modalidad de facturación. Este volumen obliga a evitar soporte individual y costes fijos de infraestructura; no demuestra por sí solo rentabilidad.

Evaluación confirmada: 30 días desde el primer uso, sin tarjeta ni contacto obligatorio, con el mismo perfil e informes. Su redacción completa necesita aprobación antes de conceder derechos; hoy no hay control de caducidad. No anunciar una prueba de 30 días como función implementada. Ver [oferta](pilot-offer.md) y el borrador del repositorio docs/pilot-terms-draft.txt.

La cuota debe comprar algo concreto: autorización de uso continuado, actualizaciones y la integración documentada. El motor actual carece de activación, medición por repositorio o bloqueo por suscripción. Publicar un binario descargable no crea estos controles. Para no construir un servidor de licencias, se confirma una licencia comercial contractual sin claves ni servidor de activación; la validación técnica es un incremento separado si Fran la considera necesaria. Una clave descargada por sí sola no protege ni limita el ejecutable.

## Descubrimiento y cobro son canales distintos

[GitHub permite listar Actions de repositorios públicos](https://docs.github.com/en/actions/how-tos/create-and-publish-actions/publish-in-github-marketplace). El listado facilita descubrir e instalar la integración; no incorpora nuestros cobros. Los [planes de pago documentados por GitHub son para Apps](https://docs.github.com/en/apps/github-marketplace/selling-your-app-on-github-marketplace/pricing-plans-for-github-marketplace-apps). Desarrollar una App con servicio permanente añadiría operación antes de validar las ventas.

Recomendamos una Action pública pequeña, versionada e independiente del repositorio de desarrollo, más una tienda alojada para la oferta comercial. No cambiar la visibilidad del repositorio del motor como efecto secundario. Propuesta de derechos para revisar: envoltura propia bajo MIT y motor bajo licencia comercial con permiso temporal de evaluación. No se aplica ninguna de esas licencias sin aprobación del titular ni se cambian derechos de terceros.

Fran ha elegido Lemon Squeezy y comunica que ha completado la activación de la cuenta; no ha autorizado al agente a aceptar contratos ni abrir ventas. Actúa como [merchant of record](https://docs.lemonsqueezy.com/help/payments/merchant-of-record), gestionando el cobro y las obligaciones de la transacción que describe. No elimina las obligaciones propias del proveedor del software. Su [portal de clientes](https://docs.lemonsqueezy.com/help/online-store/customer-portal) evita desarrollar una página propia para facturación y suscripciones. Las [claves de suscripción](https://docs.lemonsqueezy.com/help/licensing/license-keys-subscriptions) siguen el estado del periodo contratado. Ver [preparación de la tienda anual](lemon-squeezy-setup.md).

Revisar las [comisiones](https://docs.lemonsqueezy.com/help/getting-started/fees) antes de activar el precio anual: base publicada de 5 % + 0,50 USD por operación, con suplementos posibles, incluido 0,5 % para suscripciones. Se calculan sobre el total del pedido; impuestos, divisa, método y liquidación impiden asumir un neto fijo en euros. El fijo se aplica al cobro anual, no doce veces al año. No confundir la tienda directa con su [Marketplace](https://docs.lemonsqueezy.com/help/marketplace): este cobra 30 % y exige ventas previas y aprobación. Una tienda pública tampoco garantiza tráfico.

## Instalación mínima que debemos construir

Estado actual: el consumidor copia un paquete local en .guard-action, añade workflow y configura los contratos. Esto sirve para evaluar, pero aún no cumple la instalación pública deseada.

Destino propuesto:

1. El usuario añade una única plantilla de workflow desde la guía o interfaz de GitHub.
2. La plantilla referencia nuestra Action pública mediante un commit publicado; usa contracts/ y full por defecto. Ofrece una elección explícita entre bloquear rupturas o solo informarlas.
3. La Action obtiene el ZIP completo de la versión exacta desde una entrega aprobada, verifica su checksum fijado en la versión de la Action e integridad, conserva licencias/fuentes, prepara Java y guarda informes. No debe descargar solo el JAR y omitir las atribuciones que lo acompañan. Nada de copiar JAR, compilar ni editar hashes por parte del usuario.
4. El comentario visible se habilita en la plantilla con pull-requests: write. La Action conserva su opción segura actual; no eleva permisos del repositorio por sí misma. Forks no reciben secretos ni publicación de comentarios privilegiados.
5. Si se aprueba activación comercial, se añade como máximo un secreto de licencia. Las opciones avanzadas se documentan aparte.

Una Action siempre necesita un workflow y permisos del consumidor. Prometer instalación sin ningún archivo requeriría otra experiencia, normalmente una GitHub App. No se justifica ahora por coste y mantenimiento.

Pruebas TDD del próximo incremento: versión/checksum fijados, archivo alterado o ausente, descarga fallida, rutas con espacios, instalación limpia, códigos 0/1/2/3, política de rupturas y comentario/artefactos. Después, PR reales desde otro repositorio en cada runner anunciado. Hasta entonces Windows es el entorno verificado; no ofrecer Linux/macOS como soporte demostrado.

## Automatización comercial y aceptación

Primero usar el entorno de prueba del proveedor. Fran debe crear/verificar su cuenta, aportar identidad y datos de liquidación y aprobar condiciones; no se hace en su nombre.

| Flujo | Resultado verificable antes de activar ventas |
| --- | --- |
| Compra inicial | Precio/moneda/impuestos visibles, consentimiento, recibo, acceso a descarga y condiciones sin intervención manual. |
| Renovación | Un único cobro por periodo; recibo y vigencia coherentes. |
| Cancelación | Autoservicio, sin cobro posterior y acceso conforme al periodo contratado. |
| Pago fallido | Reintentos y aviso del proveedor; estado documentado y sin alta manual. |
| Devolución | Procedimiento y estado de licencia coherentes con los términos y derechos aplicables. |
| Descarga/versión nueva | Integridad verificada y versión anterior identificable, sin enviar archivos a mano. |
| Incidencia del proveedor | Mensaje claro; nunca transformar un fallo de licencia/red en un informe de compatibilidad válido. |

El motor conserva su ejecución local. No introducir llamadas de licencia en dominio o reglas. Si se implementa validación externa, debe estar en el adaptador de distribución y probar timeout, secreto oculto, producto correcto y estado vencido; los códigos semánticos del motor no cambian. Definir previamente la política de disponibilidad y exposición de datos. No añadir un backend propio solo para guardar compradores que la plataforma ya gestiona.

## Operación mínima realista

Objetivo: ninguna alta, envío de binarios o factura manual en el flujo habitual. Documentación, demo y FAQ resuelven el primer uso; incidencias reproducibles se agrupan por prioridad. Los usuarios comparten ejemplos sanitizados voluntariamente, sin telemetría ni carga de contratos obligatoria.

Sigue siendo necesario revisar vulnerabilidades y cambios de runners, corregir fallos críticos, atender devoluciones/excepciones y mantener una versión soportada. Si ese esfuerzo deja de ser asumible, detener nuevas ventas y seguir el procedimiento de cierre contratado. No vender una promesa de mantenimiento cero ni actualizaciones perpetuas por una cuota pequeña.

## Decisiones que requieren a Fran

- Revisar condiciones de renovación y derechos de consumidores para el precio confirmado de 4,99 EUR/año/repositorio con impuestos incluidos. Operación en España y en nombre propio, uso profesional/personal, ya confirmados.
- Aprobar licencia comercial del motor y permiso de evaluación; decidir licencia de la Action pública.
- Completar identidad/contacto del proveedor y revisar cancelación, privacidad y obligaciones de terceros.
- Completar la cuenta de Lemon Squeezy y aprobar contratos, activación comercial y publicación final. La preparación está autorizada.

El 19 de octubre se completó la recopilación técnica de atribuciones con catálogo, textos y fuentes. Falta la aprobación de licencia/condiciones por el titular. El siguiente incremento de distribución prepara la Action pública sin copia de paquetes; el cobro se prueba antes del lanzamiento.

