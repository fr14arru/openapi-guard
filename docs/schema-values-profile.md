# Valores de esquemas: perfil D06

El perfil `operations-parameters-schema-values-v1` se selecciona con `--profile=schema-values`. Incluye operaciones, presencia de parámetros y cuerpos, formatos de petición y valores simples de parámetros efectivos y cuerpos JSON. El perfil predeterminado histórico se conserva; el perfil `--profile=schemas` añade también D07 (objetos).

Las reglas `REQUEST_SCHEMA_VALUES_RESTRICTED` y `RESPONSE_SCHEMA_VALUES_EXPANDED` prueban respectivamente `old ⊆ new` y `new ⊆ old`. Se analiza cada uso de un componente por dirección, operación, parámetro, código de respuesta y formato; los componentes sin uso no generan hallazgos. Un hallazgo representa una inclusión incumplida por uso, con explicación concreta, punteros de definición y uso, dirección y, cuando procede, código/formato. Los IDs son deterministas e independientes de índices físicos o ubicación de componentes.

## Restricciones soportadas

- Tipos escalares y relación matemática integer/number, incluyendo equivalencia 1/1.0.
- Enum escalar, intersección con tipo, null y límites. En 3.1 también const escalar y uniones de tipos.
- Nullable de 3.0 solo cuando hay type en el mismo objeto; no anula enum. En 3.1 null se expresa mediante type.
- Minimum/maximum y sus límites exclusivos por dialecto; intervalos enteros se redondean según su dominio.
- MinLength/maxLength, contando puntos de código Unicode.
- Referencias internas acíclicas a schemas, conservando procedencia y sin E/S externa.

No se implementa inclusión arbitraria de JSON Schema. Arrays, objetos en este perfil escalar, composición, pattern, multipleOf, enums compuestos, vocabularios no reconocidos o restricciones desconocidas dentro del uso producen revisión. Para objetos usar el perfil ampliado [--profile=schemas](object-schemas-profile.md). Ciclos y cadenas superiores a 64 referencias son parciales. Siblings de $ref con restricciones en 3.1 requieren composición y revisión; en 3.0 se conserva la semántica de Reference Object.

Esquemas contradictorios se revisan; no se concluye compatibilidad por conjuntos vacíos. Cuando un enum candidato se introduce sobre un dominio que no puede cubrirse o refutarse con el análisis disponible, se informa revisión. Los límites son un apoyo para comparar constraints conjuntas, no un motor general de satisfacibilidad.

Format se trata como anotación, y sus validaciones quedan excluidas explícitamente. Las referencias normativas son [Schema Object de OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#schema-object) y [validación JSON Schema 2020-12](https://json-schema.org/draft/2020-12/json-schema-validation). Cada dialecto se compara con su misma familia, sin conversión.

## Cobertura de usos

Solo se prueba semántica JSON para application/json y tipos concretos application/*+json; comodines, otros formatos, encoding y parámetros opacos requieren revisión. Las respuestas se comparan por códigos exactos y representaciones compartidas: altas/bajas de cobertura, default o rangos requieren revisión. No se interpreta negociación ni se ofrece todavía una política completa de códigos (D09). Los cuerpos de métodos con semántica incierta requieren revisión tanto en 3.0 como en 3.1.

No se resuelven contratos externos. Se conserva el límite de 5 MiB y profundidad 128; se añade un presupuesto de precisión/escala decimal de 1024 y longitud expresable con un entero Java no negativo. Excederlo da error explícito. No se usan flotantes binarios para las pruebas numéricas.

## Ejemplo

```powershell
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=schema-values --format=text src/test/resources/openapi/schema-values-old.yaml src/test/resources/openapi/schema-values-new.yaml
```

Dos rupturas: reducción del enum en petición y expansión integer→number en respuesta. La comparación inversa es compatible en el perfil. JSON sigue siendo el formato de automatización y mantiene códigos 0/1/2/3.

TDD: 17 casos fallaron antes de implementar el perfil y un caso CLI volvió a demostrar la fase roja antes de conectar su selección. Se incluyen matrices direccionales, constraints conjuntas, null, refs, ciclos y 36 comparaciones de intervalos enteros contrastadas con un oráculo exhaustivo independiente del parser y del comparador.
