# Objetos: perfil D07

El perfil recomendado para el análisis ampliado es `operations-parameters-object-schemas-v1`, seleccionable mediante `--profile=schemas`. Reúne los incrementos D05/D06/D07, conservando los perfiles anteriores. El constructor Java predeterminado y la CLI sin opciones siguen usando presencia de parámetros; la ayuda declara esta diferencia y recomienda el perfil ampliado.

## Reglas y semántica

Se mantienen `REQUEST_SCHEMA_VALUES_RESTRICTED` y `RESPONSE_SCHEMA_VALUES_EXPANDED`: una inclusión incumplida por uso efectivo, con razón, dirección y punteros originales. Se devuelve el primer motivo demostrado por uso, con propiedades ordenadas; no se promete un inventario exhaustivo de todos los cambios de keywords. El ID depende del perfil y de la identidad semántica del uso, no del componente o índice físico.

Se comparan properties, required y additionalProperties booleano conjuntamente, incluyendo objetos anidados. Petición: old ⊆ new; respuesta: new ⊆ old. La ausencia de additionalProperties mantiene el objeto abierto. Properties sin type no implica type object; sus restricciones solo afectan a la rama objeto del dominio.

| Cambio aislado | Petición | Respuesta |
| --- | --- | --- |
| Propiedad opcional pasa a requerida | Ruptura | Puede restringir salidas sin romper |
| Propiedad requerida pasa a opcional | Puede ampliar entradas sin romper | Ruptura |
| Retirada de propiedad opcional con objeto abierto | Puede ampliar entradas | Puede ampliar valores de salida: ruptura |
| Retirada de propiedad opcional con objeto cerrado | Puede rechazar entradas: ruptura | Puede restringir salidas |
| Adición de propiedad opcional con objeto abierto | Su nueva restricción puede romper | Puede restringir salidas |
| Adición de propiedad con objeto cerrado | No afecta entradas que antes la tenían prohibida | Puede producir un nombre antes prohibido: ruptura |
| Objeto abierto pasa a cerrado | Ruptura por nombres adicionales | Puede restringir salidas |

Las conclusiones se prueban con el resto de restricciones: tipos/enums/límites de cada propiedad, presencia y espacio residual de nombres adicionales. No aplicar la tabla como un diff independiente de keywords. Las propiedades requeridas imposibles o no analizables producen revisión, evitando demostrar inclusiones a partir de esquemas contradictorios.

## readOnly/writeOnly

La proyección lógica de petición elimina las propiedades readOnly y su obligatoriedad; la de respuesta hace lo mismo con writeOnly. Las propiedades restantes conservan sus constraints y additionalProperties sigue aplicándose al espacio residual. Se conserva el modo de acceso original para detectar transiciones.

Cambiar el modo de acceso de una propiedad existente da revisión: una anotación readOnly/writeOnly no demuestra por sí sola cómo el servidor tratará un valor recibido o qué hará un cliente concreto. No inventar un rechazo de ejecución. Declarar ambos modos true es entrada inválida. Referencia: [Schema Object de OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#schema-object).

## Incertidumbre y límites

Arrays/items, composición, additionalProperties con schema, patternProperties, dependent constraints y enums compuestos permanecen parciales. Las referencias internas acíclicas conservan definición y uso; ciclos terminan con revisión. Los componentes se analizan por cada uso y dirección. No hay resolución externa, base de datos ni servicios.

Una ruptura demostrada puede coexistir con otras restricciones opacas: BREAKING con status PARTIAL, no COMPLETED. El informe conserva diagnósticos. No se certifican formatos no JSON, serializaciones de parámetros complejas, negociación de respuestas, seguridad ni comportamiento real del servidor. Ver [límites de valores](schema-values-profile.md).

La normalización tiene un presupuesto de 10.000 visitas a schemas por documento, además de tamaño/profundidad y cadenas de referencias. Superarlo crea un nodo opaco con diagnóstico y análisis parcial, nunca una certificación truncada. El recorrido es ordenado; las referencias acíclicas se reutilizan por dirección y la inclusión/satisfacibilidad/cobertura reutilizan resultados por identidad dentro de una sola evaluación. No hay caché global mutable ni expansión total de un grafo compartido. Un test compara un grafo de 32 niveles con dos referencias al nivel anterior, prueba equivalencia y una ruptura en su hoja dentro de cinco segundos.

## Demostración

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=schemas --format=text src/test/resources/openapi/objects-old.yaml src/test/resources/openapi/objects-new.yaml
```

Dos rupturas: name pasa a obligatorio en petición y deja de serlo en respuesta. Código 1. La comparación inversa devuelve código 0 dentro del perfil. Omitir `--format=text` devuelve JSON, con las mismas conclusiones y códigos 0/1/2/3.

TDD: 21 casos rojos antes de implementar objetos; un ciclo adicional rojo/verde para transiciones de acceso y otro para selección CLI. Un oráculo independiente prueba 196 pares de objetos abiertos/cerrados, opcionales/requeridos y enums booleanos, con muestras de todas las clases de valores relevantes para esa matriz. Los tests no usan el comparador para decidir sus expectativas. No se declara cobertura universal de OpenAPI a partir de ese corpus finito.
