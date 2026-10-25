# Casos de uso y límites de cobertura

El portal permite recorrer las comparaciones del corpus histórico y ejemplos ampliados de los perfiles implementados. No existe una lista finita que contenga todos los contratos posibles: pueden variar arbitrariamente rutas, schemas, valores, profundidad y restricciones. Aquí se cubren familias de comportamiento y sus fronteras, con acceso adicional a todos los tests y fixtures.

## Casos operativos del producto

| Necesidad del desarrollador | Cómo utilizarlo |
| --- | --- |
| Revisar un cambio antes de publicar API | CLI con anterior/candidato, profile full y format text. |
| Bloquear un job CI ante ruptura | JSON y código 1; tratar 2 como error y 3 como revisión. |
| Analizar solo facetas de una adopción inicial | Seleccionar un perfil histórico y comunicar su alcance. |
| Obtener un informe almacenado localmente | compare-contracts.ps1 preserva resultado JSON UTF-8. |
| Investigar una regla | Catálogo de casos → contrato → clase de dominio → test. |
| Reutilizar motor en una aplicación Java | CompareContracts con SwaggerContractAnalyzer, sin arrancar Spring. |
| Revisar el código sin IDE | Explorador del portal, referencias y Javadoc generado. |
| Preparar futura integración GitHub | Receta CI disponible; Action/App todavía propuestas. |

## Familias de comparación

| Familia | Casos de ruptura/compatibilidad | Incertidumbre/errores |
| --- | --- | --- |
| Operaciones | Ocho métodos; endpoint completo; adiciones y equivalencia | Plantillas renombradas/ambiguas, Path Items referenciados. |
| Parámetros | Nuevo required, relajación, herencia/override, headers | Retirada documental, content/serialización opaca, referencias inválidas. |
| Cuerpos y formatos | Obligatorio, opcional, cobertura de media types | Retirada documental, métodos ambiguos, patrones/encoding. |
| Valores | Integer/number, enum, null, límites por dirección | Schema contradictorio, dialecto desconocido y assertions no soportadas. |
| Objetos | Required, apertura, propiedad opcional añadida/retirada | additionalProperties con schema y cambios de acceso. |
| Arrays | Items, anidamiento, min/max y arrays solo vacíos | UniqueItems, tuplas, items opacos o sin existencia probada. |
| Respuestas | Retirar código o formato concreto | Adiciones, default, rangos y negociación incierta. |
| Referencias | Inline/ref equivalentes, compartidas por dirección | Ciclos como revisión; externa/inexistente como entrada fallida. |
| Fronteras | UTF-8, rutas con espacios, determinismo | Alias, claves duplicadas, exceso de bytes/profundidad/presupuesto. |

En petición se prueba anterior ⊆ candidato. En respuesta, candidato ⊆ anterior. La política de disponibilidad de respuestas es una faceta distinta. Se incluyen ejemplos de BREAKING/PARTIAL para mostrar que una ruptura demostrada no elimina las demás lagunas.

## Cómo leer cada caso del catálogo

Cada caso muestra ID, origen, perfil, familia, contratos anteriores/nuevos, estado, veredicto, reglas esperadas y diagnóstico aplicable. El código de salida se deriva del estado y veredicto: FAILED → 2, BREAKING → 1, COMPATIBLE → 0, resto → 3. El corpus conserva expectativas independientes en su manifest.

Los tests construidos en Java cubren además presupuestos, Unicode, DAGs, referencias/proyección compleja y universos finitos de oráculos. Abrir [la batería completa](../code/tests.md) o [el inventario](../code/inventory.md) para esos casos; el navegador no pretende sustituir su ejecución.

## Lo que aún no se puede hacer

No se suben archivos al portal para analizarlos, no se resuelven recursos remotos y no se certifica JSON Schema completo. Tampoco hay cuentas, cobros, panel de equipos, API web, GitHub App o integración automática con pull requests. Son posibles evoluciones, no capacidades actuales.
