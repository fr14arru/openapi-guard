> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Aprobación del texto y cierre de publicación

El 22 de octubre de 2026, tras leer el documento, Fran indica: «He leído el documento y me parece bien, vayamos con lo siguiente».

Esto aprueba la redacción de [la licencia revisada](product-license-draft.md), además de las decisiones comerciales previamente confirmadas. No se debe volver a solicitar aprobación sobre el mismo texto sin cambios sustantivos. La aprobación incluye el texto propuesto sobre terceros; no equivale a una auditoría jurídica independiente.

## Versión aprobada

- Commit que contiene el documento leído: `06a9a3bdcae373c99a5f0b793e190cb62cfb8794`.
- Archivo: `docs/product-license-draft.md`.
- Identificador Git del contenido: se conserva el documento sin modificar en este cambio; el commit anterior permite recuperar los bytes versionados independientemente de los finales de línea del checkout.
- Versión de revisión: `2026-10-22`.

El encabezado histórico «no vigente» identifica la copia de revisión. La redacción está aprobada; la publicación de una licencia efectiva sigue pendiente de completar identificación, privacidad, fecha efectiva, ensamblado del documento de comprador y entrega. La ficha interna y las notas de aprobación no deben adjuntarse como condiciones de venta.

## Qué sigue

Completar [la ficha de preparación legal](legal-setup.md). Los valores privados se guardan únicamente en `dist/legal-setup/datos-del-titular.md`, que Git ignora. La protección frente a commits no sustituye una política de privacidad ni convierte esos datos en secretos frente a otros usuarios del equipo.

Al publicar el aviso legal habrá datos identificativos accesibles al público por el canal elegido. Guardarlos temporalmente fuera de Git evita una publicación accidental; no elimina los requisitos del aviso legal.

Los cambios de redacción posteriores deben distinguir simples completados de identidad/enlaces de modificaciones de derechos, precio, responsabilidad o tratamiento de datos. Estas últimas deben revisarse expresamente.

La aprobación no abre ventas, cambia visibilidad del motor, hace merge ni publica releases. No se alteran los ZIP congelados. El instalador para repositorios públicos sigue pendiente según la cláusula de redistribución aprobada.
