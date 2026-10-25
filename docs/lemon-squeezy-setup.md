> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Preparación de Lemon Squeezy: licencia anual

## Paquete y modo de prueba

Se prepara un [paquete específico para test](test-store-delivery.md) con texto de licencia aprobado y sin los documentos personales de preparación legal. Fran indica expresamente mantener la tienda en modo prueba y no subir todavía nada. La creación y verificación del ZIP es trabajo local; no representa compra, entrega por tienda o publicación verificadas. Su ruta se registra en [entregas](pilot-delivery.md).

**Actualización posterior del 22 de octubre:** Fran ha leído y aprobado la redacción completa; ver [aprobación](license-approval.md). Las menciones inferiores a aprobación pendiente describen el estado anterior. Quedan aviso legal, privacidad, ensamblado y publicación de la licencia efectiva y ZIP final. [Siguiente paso](legal-setup.md).

## Cierre comercial confirmado el 22 de octubre de 2026

Fran confirma evaluación de 30 días desde el primer uso sin tarjeta; todos los colaboradores del repositorio y ejecuciones sin límite cobrado por Guard; traslado de licencia cesando el uso en el repositorio anterior; cancelación sin renovación con uso hasta terminar el periodo pagado y posterior renovación o cese; versiones publicadas durante la suscripción y soporte por correo sin SLA ni promesas de funcionalidades. No se añadirá servidor de activación ni verificación técnica de licencias.

La [redacción para revisión final](product-license-draft.md) recoge estas decisiones. La confirmación de negocio no hace efectiva la licencia ni autoriza nuevas cláusulas legales, ventas, publicación o modificación de candidatos congelados.

Según comunica Fran el 21 de octubre, la cuenta Lemon Squeezy está activada y el producto existe en modo de prueba. No se han inspeccionado la cuenta, el checkout, la entrega ni una compra real. La documentación pública se mantiene separada del motor en [el repositorio de guías](https://github.com/fr14arru/OpenAPI-Breaking-Change-Guard-Docs).

Decisión de Fran del 16 de octubre de 2026: **4,99 EUR al año por repositorio** y Lemon Squeezy como plataforma de venta. La configuración inicial de esta guía ya fue realizada por Fran según la actualización anterior; el resto describe comprobaciones pendientes, no acciones ejecutadas por el agente. No se aceptan contratos ni activan cobros en esta entrega.

## Ficha del producto para configurar

| Campo | Valor previsto |
| --- | --- |
| Nombre | OpenAPI Breaking Change Guard — Annual repository license |
| Titular y contacto de soporte | Francisco Arce Ruiz — rayopuyi5@gmail.com, confirmados por el titular. |
| Operación y compradores | España, en nombre propio; usuarios profesionales y particulares. |
| Modalidad | Suscripción recurrente anual; un cobro cada año. |
| Moneda y precio | EUR, 4,99 por un repositorio y año. |
| Cantidad inicial | Una licencia para un repositorio; sin medición por ejecución. |
| Impuestos | Incluidos en los 4,99 EUR anuales. Activar tax-inclusive pricing y comprobar que el checkout no los suma al total. |
| Cancelación | Autoservicio; sin siguiente renovación, con vigencia hasta fin del periodo pagado conforme a términos aprobados. |
| Entrega | Versión comercial aprobada y sus atribuciones; rc.3 sigue siendo un candidato técnico congelado con borradores; no debe adjuntarse a ventas como paquete definitivo. |
| Evaluación pública | Propuesta separada de 30 días sin tarjeta; no activar un trial de suscripción por confundirlo con descarga evaluable. |
| Soporte | Documentación y canal asíncrono de incidencias, sin SLA ni atención individual ilimitada. |

Lemon Squeezy admite [intervalos anuales de suscripción](https://docs.lemonsqueezy.com/help/products/subscriptions). Precio anual, impuestos incluidos y tipo de compradores están confirmados; aviso legal, licencia y términos siguen pendientes de cierre. Su opción [tax-inclusive pricing](https://docs.lemonsqueezy.com/help/payments/sales-tax-vat) es una configuración de tienda: revisar su efecto en otros productos, si existen.

## Secuencia de preparación

1. Fran crea o utiliza su cuenta y completa personalmente identidad y aceptación de plataforma. No compartir contraseña, claves maestras ni datos bancarios en Git.
2. Mantener el [modo de prueba](https://docs.lemonsqueezy.com/help/getting-started/test-mode), crear el producto con la ficha anterior y conservar el enlace de checkout de prueba fuera de las instrucciones públicas.
3. Revisar nombre, precio anual, moneda, impuestos y renovación visibles. Añadir términos/contacto aprobados antes de preparar modo real.
4. Preparar recibo, enlace de portal y acceso a la versión aprobada. Las claves de licencia son opcionales hasta definir activación: hoy el motor no las valida ni limita repositorios.
5. Ejecutar y registrar los casos de la matriz. No habilitar API/webhooks o un backend propio si el checkout y portal alojados bastan para la oferta contractual.
6. Tras resolver atribuciones, licencia y pruebas, solicitar aprobación para activar/publicar la configuración concreta. No copiar automáticamente productos a modo real ni realizar compras de dinero real.

El checkout cobra por la licencia; no puede demostrar por sí solo que se usa en un único repositorio. Inicialmente ese alcance es contractual. Si se requiere validación técnica, diseñarla como un incremento exterior al núcleo y no prometerla antes de implementarla.

## Matriz y evidencia necesaria

| Caso | Comprobación | Evidencia pendiente |
| --- | --- | --- |
| Compra inicial | Total EUR 4,99, ciclo anual e impuestos desglosados dentro del total; probar comprador particular y profesional. | Checkout y pedido de prueba, sin datos sensibles. |
| Renovación | Siguiente fecha anual y cargo único por periodo; no mensual. | Estado/fecha de suscripción y ejercicio soportado por el entorno de prueba. |
| Cancelación | Se detiene el cobro futuro y se conserva la vigencia del periodo pagado. | Portal/estado y fecha final. |
| Pago fallido | Estado y reintentos coherentes, sin alta manual ni duplicar licencia. | Resultado de prueba de fallo. |
| Devolución | Procedimiento y efecto sobre acceso conforme a los términos. | Operación simulada y comprobación de estado. |
| Consumidores | Información de desistimiento y conformidad, confirmación de condiciones y canal de reclamación. No presumir renuncia por descarga. | Flujo y documentos reales pendientes de verificar. |
| Clave, si se habilita | Pertenece al producto y sigue el periodo; no confundir emisión con validación por Guard. | Estado de clave y pruebas del adaptador si llega a existir. |
| Entrega de archivos | ZIP aprobado, checksum verificable y acceso desde el canal comercial. | Pendiente de validación final específica. |

**Límite del modo prueba:** la documentación indica que las descargas están deshabilitadas para compras de prueba. Pasar checkout/suscripciones no valida la descarga real. Validar integridad local por separado y dejar la entrega por tienda como puerta pendiente hasta un procedimiento autorizado que permita comprobarla. No utilizar datos de tarjeta reales en modo prueba. [Fuente: modo de prueba](https://docs.lemonsqueezy.com/help/getting-started/test-mode).

La política contractual debe encajar con el vendedor de la transacción. Lemon Squeezy se reserva decisiones de reembolso para evitar disputas; no prometer que las devoluciones dependen exclusivamente de Fran. Revisar su [política de reembolsos](https://docs.lemonsqueezy.com/help/payments/refunds-chargebacks) al configurar la cuenta.

## Margen y mantenimiento

100 repositorios suponen cobros totales de 499 EUR/año; 1.000, de 4.990 EUR/año, impuestos incluidos. No son beneficios ni ingresos netos. Restar impuestos incluidos, comisiones, devoluciones y costes propios antes de evaluar viabilidad.

Las [comisiones publicadas](https://docs.lemonsqueezy.com/help/getting-started/fees) incluyen una parte fija en USD y porcentajes/suplementos. Comprobar el plan de la cuenta y el coste efectivo en EUR; no trasladar 0,50 USD como si fueran 0,50 EUR ni prometer ingreso neto. Al facturar anualmente hay una operación ordinaria por renovación anual, no doce cargos mensuales.

Mantener autoservicio para compra/cancelación y una única oferta. No incluir consultoría ni funciones a medida en 4,99 al año. La elección de precio no elimina mantenimiento crítico ni obligaciones del vendedor.

Ver [oferta](pilot-offer.md), [lanzamiento](commercial-launch.md) y [atribuciones pendientes](third-party-review.md). Los hashes y borradores dentro del candidato rc.2 no se modifican por actualizar esta guía.
