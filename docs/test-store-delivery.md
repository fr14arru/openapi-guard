> **Cambio de modelo (22 de octubre de 2026):** el código propio pasa a MIT y uso gratuito. Las condiciones comerciales, precios y aprobaciones pendientes que aparecen más abajo son historial, no requisitos vigentes de esta distribución. Las evidencias y licencias de terceros se conservan. Estado y siguientes pasos: [transición MIT](open-source.md).

# Paquete para probar la entrega en Lemon Squeezy

Esta entrega permite preparar la ficha y el archivo de un producto en **modo de prueba**, sin abrir ventas. La licencia del motor ya tiene su redacción aprobada; privacidad y aviso legal siguen separados del paquete. No subir nada automáticamente ni presentar este ZIP como una release comercial pública.

## Qué cambia y qué conserva

Se reutiliza el binario de rc.3 verificado, sin recompilarlo ni modificar sus bytes. Su versión, sourceCommit, build-proof.json y SHA256 se conservan. El código de empaquetado nuevo se identifica aparte mediante `delivery.packagingCommit`; nunca se atribuye a ese commit la compilación anterior del motor.

Contenido nuevo:

- LICENSE.txt: cláusulas de usuario extraídas del documento exacto aprobado, sin ficha interna ni instrucciones de revisión.
- README.txt: requisitos, ejemplos ejecutables, CLI, guía de la Action y límites.
- DELIVERY-STATUS.md: modo de prueba y comprobaciones pendientes antes de venta.
- PRODUCT-LICENSE-STATUS.txt: texto aprobado para entrega de prueba; no afirma que falte aprobación de las cláusulas ya leídas.

Se conservan las bibliotecas, el loader, avisos, textos completos y archivos originales de fuentes de terceros. PILOT-TERMS-DRAFT.txt se excluye. No se copian privacidad, aviso legal ni la ficha con datos del titular: no hay un barrido del directorio privado ni de todos los documentos del repositorio.

## Preparación reproducible

Desde un checkout limpio con Node y PowerShell existentes:

```powershell
node scripts/prepare-test-delivery.mjs dist/0.1.0-rc.3-aebc40bf/package dist/lemon-squeezy-test/package
pwsh -NoProfile -File scripts/archive-evaluation.ps1 -Package dist/lemon-squeezy-test/package -Archive dist/lemon-squeezy-test/openapi-guard-test.zip
```

Los destinos deben ser nuevos. En una entrega registrada se elige un directorio versionado bajo dist, no se reutiliza ni se sobrescribe el anterior. Si falla una copia, puede quedar un directorio parcial que no debe utilizarse. Los errores de entrada y licencia se comprueban antes de crear el destino; la integridad final debe superar la verificación antes de archivar.

El empaquetador solo admite archivos de ejecución conocidos, las evidencias embebidas y los archivos catalogados de atribuciones. La integridad válida del origen no autoriza introducir un documento privado adicional. Se exige prueba de build coherente y catálogo con bytes intactos.

La licencia se vincula al commit de aprobación `06a9a3b` y al blob Git `e1496cb7a22a32ae80ea615f92c2757c56182503` del documento de revisión. Se normaliza CRLF a LF antes de verificar ese identificador. Cambiar el texto de origen obliga a revisar su aprobación; no basta renombrarlo LICENSE. Las únicas transformaciones son retirar notas internas y ajustar rótulos de revisión, sin alterar cláusulas de uso.

## Verificación de la entrega real

1. Verificar el conjunto cerrado de archivos y las atribuciones.
2. Crear ZIP y checksum externo con archive-evaluation.ps1.
3. Extraer con install-evaluation.ps1 a un directorio nuevo comprobando ese checksum y la integridad interior.
4. Ejecutar el JAR extraído: ayuda, equivalencia, compatible, ruptura, revisión y entrada inválida; contrastar códigos 0/1/2/3 y texto/JSON/Markdown.
5. Comprobar que domicilio/NIF y los documentos privados no aparecen en la entrega. No imprimir sus valores al registrar la comprobación.

Los hashes internos detectan alteraciones; no sustituyen un canal confiable para el hash externo ni una firma del editor. Las pruebas locales del ZIP no prueban la descarga desde Lemon Squeezy ni el comportamiento en todos los runners.

## Qué se mantiene pendiente y fuera del ZIP

La identidad del aviso legal fue validada por Fran. Privacidad es un borrador local para Gmail, GitHub y Lemon Squeezy, sin marketing ni analítica propia. Faltan sus verificaciones de datos de pedidos, conservación por finalidad, bases y condiciones/transferencias de los servicios. Ver [preparación legal](legal-setup.md).

Antes de venta: completar y publicar información legal en el destino autorizado, fijar vigencia, preparar confirmación de condiciones y validar checkout, renovación, cancelación y entrega por tienda. Las descargas de compras de prueba están deshabilitadas por Lemon Squeezy. No confundir una prueba de checkout con la entrega real del archivo.

La receta actual de Action con el motor copiado en .guard-action se limita a consumidores privados. Para consumidores públicos, no publicar ese JAR en Git: el instalador en runner sigue pendiente. No se instala un servidor de licencias ni se modifica el motor para este empaquetado.

La ruta y checksum del ZIP efectivamente generado se registran en [el registro de entrega](pilot-delivery.md). No adjuntar archivos a Lemon Squeezy ni cambiar la tienda a modo real sin la instrucción correspondiente del titular.
