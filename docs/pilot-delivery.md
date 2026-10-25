> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Registro de entrega de evaluación

## Entrega de prueba de tienda: 0.1.0-rc.3-test.1

Preparada el 22 de octubre de 2026. **Solo para pruebas; no subida a Lemon Squeezy ni publicada.** No se ha consultado ni modificado la cuenta remota. Fran indica mantener modo prueba y no subir nada todavía.

Ruta exacta del ZIP:

```text
D:\Projects\openapi-breaking-change-guard\dist\lemon-squeezy-test\0.1.0-rc.3-test.1-2e8f0f2\openapi-guard-0.1.0-rc.3-test.1.zip
```

ZIP SHA256:

```text
081a0b271ba417a172748d0e6d441ee78ac9e7826cb78dd04784b789a325cc7a
```

Tamaño: 15.889.412 bytes. El archivo .sha256 adyacente contiene el checksum. delivery-verification.json, checks/ y extracted/ conservan la evidencia local junto al ZIP, fuera del archivo de entrega.

| Evidencia | Resultado |
| --- | --- |
| Empaquetador | Commit limpio 2e8f0f2df475614d09eeb0a3ff747dd1553aecc7; delivery.packagingCommit. |
| Motor reutilizado | rc.3, sourceCommit aebc40bf7a449262f22a9533d8ff9fcf8c9f6d8d; no recompilado para el ZIP. |
| SHA256 del JAR | 981b8bc400ea124dda66a5dadee372546005a326a2c32186ad14851a70ef942b, intacto. |
| Atribuciones | 35 bibliotecas y loader; avisos, textos y ocho archivos originales de fuentes preservados. |
| Integridad | 194 archivos, ZIP con checksum externo y extracción nueva verificados. |
| JAR extraído | Nueve casos: ayuda, perfiles, equivalencia, compatible, ruptura, revisión, entrada inválida, JSON y Markdown. Códigos 0/1/2/3 esperados. |
| Datos privados | No aparecen domicilio/NIF en los archivos textuales ni ficha, aviso o privacidad en el paquete. |
| Licencia | Cláusulas del documento aprobado de 06a9a3b en LICENSE.txt; sin notas internas ni borrador de términos. No genera una suscripción pagada. |
| Estado | LEMON_SQUEEZY_TEST_ONLY; privacidad PENDING_NOT_INCLUDED; sin aprobación de venta real o publicación pública. |

README explica CLI y Action local para consumidores privados. DELIVERY-STATUS.md declara las comprobaciones pendientes, sin presentar privacidad como definitiva. Los borradores personales permanecen en dist/legal-setup/; la identidad del aviso fue validada por Fran. Ver [procedimiento](test-store-delivery.md).

Cuando el titular autorice adjuntar a la ficha de prueba, elegir solo el ZIP anterior; no logs, evidencia externa, extracted/ ni ficha privada. Las descargas de compras de prueba están deshabilitadas en Lemon Squeezy: esta evidencia local no valida entrega comercial por tienda. rc.2 y rc.3 originales se conservan intactos.

## Candidato de atribuciones: 0.1.0-rc.3

Las decisiones posteriores de precio con impuestos incluidos, proveedor en España y uso profesional/personal están en el [borrador actualizado](product-license-draft.md). No se modifica este ZIP: sus términos reflejan el commit de origen y siguen sin ser una licencia efectiva. Tras aprobación, generar otro candidato con el texto definitivo.

Candidato generado el 20 de octubre de 2026 desde el commit limpio **aebc40bf7a449262f22a9533d8ff9fcf8c9f6d8d** de codex/public-distribution-licenses. Incluye las atribuciones implementadas en 1d0ac98 y el contacto público confirmado: Francisco Arce Ruiz, rayopuyi5@gmail.com. No hay merge a main, release pública ni licencia definitiva aprobada en esta entrega.

Directorio local: `D:\Projects\openapi-breaking-change-guard\dist\0.1.0-rc.3-aebc40bf`.

| Evidencia | Resultado |
| --- | --- |
| mvnd offline clean verify / Maven estándar offline verify | 963 pruebas Java por build, sin fallos. |
| Pruebas Node del candidato | 93 correctas, incluidas ocho regresiones de atribuciones. |
| Receta CI | Diez comprobaciones correctas, códigos 0/1/2/3 conservados. |
| Atribuciones | 35 bibliotecas y loader fijados por hashes; textos completos, avisos y ocho archivos de fuentes. |
| Integridad | ZIP, paquete y copia extraída verificados; manifiesto cubre textos/fuentes y borrador de términos. |
| Estado comercial | TECHNICAL_EVIDENCE_COMPLETE; producto OWNER_APPROVAL_PENDING y publicación no aprobada. |

ZIP: `openapi-guard-0.1.0-rc.3.zip`. La unidad de entrega es el paquete completo; no publicar guard.jar aislado, porque los nuevos avisos y fuentes van junto al ejecutable.

ZIP SHA256:

```text
d83e6ae25cd70143c23c218eda7d22533a3baea5ff99c832eb0acbcc692e8b60
```

JAR SHA256:

```text
981b8bc400ea124dda66a5dadee372546005a326a2c32186ad14851a70ef942b
```

Comprobar una ruptura desde la raíz, sin compilar:

```powershell
java -jar dist/0.1.0-rc.3-aebc40bf/package/guard.jar --profile=full --format=text dist/0.1.0-rc.3-aebc40bf/package/examples/base.yaml dist/0.1.0-rc.3-aebc40bf/package/examples/breaking.yaml
```

Se espera código 1. Los avisos están en package/THIRD-PARTY-NOTICES.md, y las fuentes/textos en package/third-party/licenses/. build-proof.json y los logs conservan el procedimiento local; remotePrValidated y publicationApproved siguen en false. La evidencia de la PR anterior no se atribuye a este nuevo ZIP.

El intento del 19 de octubre en dist/0.1.0-rc.3-1d0ac982 quedó incompleto, sin ZIP ni proof; se conserva como intento no válido. No se sobrescribe ni se presenta como candidato verificado.

**Para autorizar distribución pública:** revisar el [borrador de licencia](product-license-draft.md), completar los datos legales/impuestos pendientes y aprobar texto y destino de publicación. La preparación técnica de terceros y el correo confirmado no conceden por sí solos la licencia del producto.

---

## Candidato anterior: 0.1.0-rc.2

Generado el **16 de octubre de 2026, 14:44:48 Europe/Madrid**, desde un checkout limpio del commit **b6e32d4cdb07a439ddcde47aed97ee53125aaa9d**, integrado y subido a main. Los cambios documentales posteriores no alteran ese origen. La versión anterior rc.1 se conserva más abajo como histórico; no contiene todas las mejoras de PR.

Directorio local: `D:\Projects\openapi-breaking-change-guard\dist\0.1.0-rc.2-b6e32d4c`.

| Archivo/directorio | Contenido |
| --- | --- |
| openapi-guard-0.1.0-rc.2.zip y .zip.sha256 | Paquete congelado y checksum externo. |
| build-proof.json | Commit/version, hash del JAR y procedimiento local completado. |
| package/ e installed/ | Original y copia extraída; 99 archivos verificados en cada una. |
| pilot-kit/ | Action con JAR, workflow y contratos sintéticos de aceptación. |
| mvnd-verify.log y maven-verify.log | 963 pruebas Java por build: 937 habituales y 26 del ejecutable; sin fallos. |
| node-tests.log y ci-tests.log | 85 pruebas Node y diez comprobaciones CI, sin fallos. |

SHA256 del ZIP:

```text
db6ac0e4e3a6c37306debabf783d1fb0265266ddff348eb40fbdd2372220fda7
```

SHA256 del JAR:

```text
c5e37c66c429b744bd3cdffe85cfaf098247e55365f4c7e7613b223391d3ac3c
```

Ejecutar sin compilar desde la raíz:

```powershell
java -jar dist/0.1.0-rc.2-b6e32d4c/package/guard.jar --profile=full --format=text dist/0.1.0-rc.2-b6e32d4c/package/examples/base.yaml dist/0.1.0-rc.2-b6e32d4c/package/examples/breaking.yaml
```

Se espera código 1 por ruptura. El kit procede del mismo paquete; no descargar herramientas ni reconstruir en el consumidor. No copiar los binarios al repositorio del motor ni modificar el ZIP congelado.

### Evidencia remota y límites de este cierre

El consumidor ya tiene remoto y [PR de prueba](https://github.com/fr14arru/openapi-breaking-change-guard-test-project/pull/1). Fran ha mostrado su ejecución con cuatro hallazgos, check satisfactorio cuando se permiten rupturas y comentario de bot visible. Último commit consumidor conocido: 7f0c66af1f2730431990f8d278484d931f1dcea6. El comentario observado identifica baseline 4bdd2f1 y merge candidato 66ecc12.

Esa ejecución usó un paquete preparado antes de rc.2; no se etiqueta como prueba remota del ZIP nuevo. El proof mantiene remotePrValidated=false y publicationApproved=false. Faltan el resto de escenarios remotos, licencia efectiva, [cierre de terceros](third-party-review.md), instalación pública y pruebas comerciales. Los términos dentro del ZIP reflejan su commit de origen: son borradores del piloto privado y no sirven como licencia pública ni oferta vigente.

**Cierre alcanzado:** versión técnica estable y reproducible mediante el procedimiento registrado para evaluación local, con procedencia e integridad. **Cierre pendiente:** distribución y venta públicas. La nueva estrategia y los pasos de aceptación están en [lanzamiento comercial](commercial-launch.md); ya no se exige reunir tres equipos piloto. No se ha publicado una release ni activado cobros.

## Histórico: 0.1.0-rc.1

Candidato generado el 12 de octubre de 2026 a las 23:37 (Europe/Madrid). Versión: **0.1.0-rc.1**. Código y scripts: commit **4fd4fdbdb32320fa6657bf5250741b655a65b8ef**. Esta página registra una ejecución completada, no una publicación ni aprobación comercial.

### Uso histórico de rc.1

El registro inicial de rc.1 precede a la publicación de la PR del consumidor. Sus ZIP/JAR conservan los hashes originales; no se modifica su contenido. Los kits posteriores incluyen .gitattributes para preservar los bytes de .guard-action tras un checkout de Git en Windows.

Abrir `dist/0.1.0-rc.1-4fd4fdbd/pilot-kit/README.md`. El kit incluye workflow de PR, Action con JAR, contrato base y variantes. Llevarlo a un repositorio GitHub privado de prueba, crear main con el contrato base y abrir una PR desde una variante. [Pasos y matriz](candidate-roadmap.md).

Comprobar una ruptura localmente desde la raíz:

```powershell
java -jar dist/0.1.0-rc.1-4fd4fdbd/package/guard.jar --profile=full --format=text dist/0.1.0-rc.1-4fd4fdbd/package/examples/base.yaml dist/0.1.0-rc.1-4fd4fdbd/package/examples/breaking.yaml
```

El código esperado es 1. No hace falta compilar el consumidor ni desplegar un servicio.

### Archivos históricos de rc.1

Todo está bajo `dist/0.1.0-rc.1-4fd4fdbd/`, conservado fuera de target:

| Archivo/directorio | Uso |
| --- | --- |
| openapi-guard-0.1.0-rc.1.zip y .zip.sha256 | Transporte privado y checksum externo, por un canal confiable. |
| build-proof.json | Commit, versión, hash del JAR y procedimiento verificado. |
| package/ | Paquete original con manifiesto v2 y evidencia de bibliotecas. |
| installed/ | Copia extraída del ZIP y verificada de nuevo. |
| pilot-kit/ | Kit completo para el repositorio de prueba. |
| mvnd-verify.log, maven-verify.log | Verificación Java del candidato. |
| node-tests.log, ci-tests.log | Pruebas de adaptadores, paquete, kit y CI. |

SHA256 del ZIP:

```text
2d7a3d95da3ed4767e9ce2ffc2f9967958e0d51dd761447db98a92f7c3f250df
```

SHA256 del JAR:

```text
b38271de26ab9f7a0e5766239377b4f48d4810fe6f7d1f1b010ba4dbd399c2b6
```

No versionar estos binarios en Git del motor. Una actualización documental posterior no cambia la procedencia del candidato registrado aquí. Una reconstrucción puede producir hashes distintos y no es el mismo paquete verificado.

### Evidencia histórica de rc.1

| Comprobación | Resultado |
| --- | --- |
| mvnd offline clean verify, rc.1 | 960 pruebas Java, sin fallos. |
| Maven estándar offline verify, rc.1 | 960 pruebas Java, sin fallos. |
| Adaptadores, integridad, procedencia y kit | 66 pruebas Node, sin fallos. |
| Receta CI con JAR rc.1 | Diez comprobaciones correctas, códigos 0/1/2/3. |
| Paquete y copia extraída | 98 archivos cubiertos y verificados por integridad. |
| Bibliotecas de ejecución | 35 inventariadas con hashes y evidencia embebida. |
| Kit en repositorios Git independientes | Ocho escenarios correctos. |

Se verifican equivalencia, compatibilidad, ruptura, revisión, inválido, eliminación, renombrado y ausencia de cambios de contrato. Es evidencia local del adaptador/binario, no una ejecución real de GitHub ni ausencia universal de falsos positivos.

### Puertas registradas al generar rc.1

- En ese momento faltaba una PR real; la evidencia obtenida después se recoge en rc.2.
- Fran completa y aprueba términos, proveedor/cliente, precio, repositorios, fechas y soporte. El borrador no concede derechos por sí solo.
- Se revisan atribuciones/licencias de bibliotecas y loader antes de distribución comercial a terceros. El inventario no es clearance legal.
- Se aprueba el destino/contenido de una release, artefacto externo o listado. No hubo merge nuevo a main, tag ni publicación al generar rc.1.

El proof de rc.1 conserva remotePrValidated=false y publicationApproved=false. La oferta privada original ha sido sustituida por la [propuesta de autoservicio](pilot-offer.md), sin cobros ni suscripciones activados.
