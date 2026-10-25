# Distribución de código abierto

El 22 de octubre de 2026 Fran aprueba **MIT** para el código propio y sustituye el modelo de venta por repositorio por uso gratuito y aportaciones voluntarias. No se necesita suscripción, clave de licencia ni activación. MIT permite usar, modificar, redistribuir y vender copias, conservando su aviso. Ver [licencia íntegra](../LICENSE).

Las bibliotecas conservan sus derechos y licencias. El paquete mantiene los textos, avisos, inventario y fuentes correspondientes recopilados en `licensing/`; MIT no sustituye esas obligaciones. Ver [revisión de terceros](third-party-review.md).

## Paquetes nuevos e historial

`scripts/prepare-evaluation.ps1` conserva su nombre por compatibilidad, pero ahora copia MIT como `LICENSE.txt`, registra su SHA256 en las atribuciones e integridad y excluye los términos comerciales de piloto. Sin prueba de build indica `OPEN_SOURCE_LOCAL_PACKAGE`; con una prueba válida indica `OPEN_SOURCE_CANDIDATE_PUBLICATION_PENDING`. Preparar archivos no publica una release ni demuestra por sí solo un build verificado.

Los ZIP rc.2/rc.3 y el paquete de prueba de tienda permanecen inmutables, con sus hashes y estados históricos. No se deben presentar como nuevas entregas MIT. `prepare-test-delivery.mjs` y su documento comercial fijado por hash se conservan únicamente para reproducir esa evidencia antigua; no forman parte del flujo actual. La privacidad y el aviso legal privados siguen fuera de Git, del portal y del paquete.

## Orden para la apertura

1. Revisar esta adaptación y ejecutar los controles técnicos de código, paquetes y portal.
2. **Decisión confirmada el 25 de octubre:** publicar una copia limpia sin historial como `openapi-guard` y conservar el repositorio original privado. No cambiar su remoto ni reescribir sus commits. La búsqueda del correo privado de autor no lo encuentra en los archivos de HEAD; sí en los metadatos históricos, que quedan fuera de la copia.
3. Configurar y comprobar permisos y reglas de main; ver [gobierno del repositorio](github-governance.md). CODEOWNERS no basta.
4. Generar un candidato MIT desde un commit limpio, comprobar ZIP/hash/ejecución e integrar/publicar con la autorización correspondiente. La Action remota por versión todavía requiere una entrega publicada; mientras tanto funciona el paquete local.
5. Añadir el enlace público de PayPal cuando Fran lo facilite al cerrar los pasos anteriores. Sin cobro obligatorio ni funciones restringidas.

No se publican domicilio, NIF ni documentos locales de identificación. Esta transición no declara que MIT o las aportaciones voluntarias resuelvan por sí solos obligaciones fiscales o de privacidad.

## Copia limpia para el nuevo repositorio

Se exporta un commit concreto mediante `git archive` a un ZIP local bajo `dist/public-source/`, y se extrae a una carpeta nueva. El ZIP no contiene `.git` ni archivos ignorados: no transporta commits, autores, remotos o configuración local. Conserva fuentes, pruebas, documentación, licencias y archivos de fuentes de terceros. Los hashes escritos en documentos históricos son referencias textuales, no el historial Git.

El registro local `snapshot-verification.json`, junto al ZIP, identifica commit de origen, SHA256 del ZIP y verificaciones. Ese registro es procedencia técnica local; no se incorpora automáticamente a una release. La comprobación busca el correo privado histórico y marcadores comunes de credenciales, verifica ausencia de carpetas privadas y coteja los archivos de atribuciones con su catálogo. Es una comprobación acotada, no una garantía universal de ausencia de secretos.

Antes del primer commit público hay que usar una identidad de autor pública, revisar archivos y reglas de main, y confirmar el destino. Exportar la copia no crea el repositorio remoto ni publica una Action. La instalación por versión y el enlace de aportaciones siguen pendientes.

## Verificación de esta adaptación

La prueba de atribuciones falló primero al esperar MIT frente al estado anterior de aprobación pendiente. Después de implementar, pasan 100 pruebas Node (con el JAR rc.3 congelado para evitar competir con Maven), 963 pruebas Java con `mvn -o verify`, diez pruebas del portal, sus 218 ejemplos contrastados con el motor y Javadoc con doclint. El portal Docusaurus compila. No se han descargado dependencias ni modificado binarios históricos.

La comprobación de nombres versionados no encuentra archivos de preparación privada en `dist/` ni del IDE. No sustituye una auditoría completa del contenido de todo el historial. El repositorio documental público separado todavía anuncia la oferta anterior: deberá actualizarse antes de anunciar la apertura, cuidando también la identidad de autor de sus nuevos commits.

El directorio de trabajo público propuesto es `D:\Projects\openapi-guard`. Su AGENTS.md se sustituye por instrucciones de desarrollo públicas, sin el historial operativo privado; el portal usa el nombre `openapi-guard`. Estas adaptaciones se registran como cambios respecto de la exportación. El primer commit usa el contacto público solo para ese comando, sin modificar la identidad global. No se copian ramas ni objetos del repositorio original.

La compilación desde una exportación detectó una prueba que eliminaba `info.title` suponiendo LF. Con CRLF la sustitución no hacía nada y el analizador devolvía correctamente COMPLETED para una entrada válida. La regresión usa ambos finales de línea, verifica que la mutación se aplicó y mantiene el rechazo de contratos inválidos. El cambio es del test, no del motor.
