# Evidencia de terceros conservada para distribución

Inventario técnico preparado el 19 de octubre de 2026. catalog.json fija los 35 JAR de ejecución, el loader separado, sus hashes y los archivos de evidencia. Los originales proceden de los JAR de rc.2 y de las fuentes de las mismas versiones que ya estaban en la caché Maven. No se han descargado dependencias nuevas.

- files/: licencias, avisos, POM y cabeceras originales. Cada entrada del catálogo conserva el origen dentro del artefacto. Los archivos License.class del modelo Swagger no son textos y se excluyen.
- sources/: ocho archivos de fuentes Maven originales, sin modificar, con URL de procedencia y SHA256 en el catálogo. Incluyen Logback Core/Classic 1.5.34 y Jakarta Annotations 2.1.1 para entregar sus fuentes bajo EPL-2.0. También se conservan fuentes de los otros componentes cuyo JAR no aportaba texto de licencia.
- catalog.json: expresiones observadas y opción de distribución preparada. Para Logback y Jakarta Annotations se prepara EPL-2.0; no se eliminan las alternativas ni avisos originales. Los derechos de subcomponentes siguen en sus textos íntegros.

.gitattributes conserva los bytes de todo este directorio. No reformatear, traducir ni normalizar estos textos. Editar el catálogo exige revisar la versión, sus archivos y las obligaciones correspondientes; no recalcular hashes automáticamente para aceptar una actualización desconocida.

El empaquetador valida el conjunto completo y los hashes antes de copiar. Añade THIRD-PARTY-NOTICES.md, fuentes y PRODUCT-LICENSE-STATUS.txt; el manifiesto de integridad cubre todos ellos. Un componente desconocido o evidencia modificada hace fallar la preparación. No se consulta la red ni la caché del consumidor.

La unidad de entrega preparada es el ZIP completo. No se han embebido estas nuevas atribuciones/fuentes en el JAR ejecutable: publicar guard.jar aislado perdería documentación que ahora acompaña al paquete. Una futura Action pública debe instalar y conservar el conjunto completo, no descargar solo el JAR.

El fingerprint del loader es SHA256 del índice de clases: líneas ordenadas por ruta, cada una ruta|SHA256, unidas con LF sin salto final. Identifica código que no aparece entre los JAR anidados. Los hashes no son firmas ni pruebas de recompilación reproducible.

Esto completa la recopilación técnica, no concede licencia al código propio ni autoriza publicación/venta. El titular debe revisar especialmente las obligaciones del distribuidor comercial EPL y aprobar su licencia. Ver [revisión](../docs/third-party-review.md) y [borrador del producto](../docs/product-license-draft.md).
