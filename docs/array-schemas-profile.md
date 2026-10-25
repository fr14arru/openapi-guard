# Arrays: perfil D08

Seleccionar `--profile=arrays` activa `operations-parameters-array-schemas-v1`: reúne operaciones, parámetros, cuerpos, valores, objetos y arrays homogéneos. Los perfiles anteriores conservan su semántica y sus identificadores; `--profile=schemas` sigue representando D07 y da revisión para arrays. El perfil predeterminado sigue siendo parámetros.

Se comparan `items`, `minItems` y `maxItems` conjuntamente. Petición: old ⊆ new; respuesta: new ⊆ old. Reducir el tipo o enum de los elementos puede romper una petición, mientras ampliarlo puede romper una respuesta. Una longitud excluida requiere demostrar que existe un array anterior con esa longitud. Si sus elementos son opacos y no se demuestra esa existencia, se solicita revisión.

Un array limitado a cero elementos no necesita comparar elementos inalcanzables. En 3.1, `items: false` permite únicamente arrays vacíos; si además minItems es positivo, el esquema es contradictorio y no se certifica. En 3.0 un type array necesita items; en 3.1 su ausencia deja los elementos sin restricción. No inferir type array a partir de items. Las restricciones solo afectan a la rama array cuando no hay type explícito.

Se admiten arrays de objetos y arrays anidados, con referencias internas acíclicas y proyección de propiedades por dirección. Las ubicaciones conservan la definición anidada y el uso original. Se mantienen presupuesto de normalización, caché por dirección y ausencia de resolución externa. Ciclos, composición, enums compuestos, uniqueItems, contains, prefixItems y otras restricciones no soportadas producen cobertura parcial.

Fuentes semánticas: [Schema Object de OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#schema-object) y [validación JSON Schema 2020-12](https://json-schema.org/draft/2020-12/json-schema-validation).

## Demostración

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=arrays --format=text src/test/resources/openapi/arrays-old.yaml src/test/resources/openapi/arrays-new.yaml
$LASTEXITCODE
```

Dos rupturas por items: petición number → integer y respuesta integer → number; código 1. La comparación inversa devuelve código 0 dentro del perfil. JSON es el formato predeterminado y conserva los códigos 0/1/2/3.

## Evidencia TDD

Quince casos nuevos fallaron por ausencia del perfil antes de implementarlo. Regresiones rojas/verdes adicionales cubren selección CLI, longitud con elementos opacos y retirada de tipo sin demostrar elementos válidos. El oráculo independiente verifica 324 pares de arrays con intervalos de longitud 0–2 y enums booleanos, usando muestras de todas las clases de elementos; no usa el comparador para decidir las expectativas. Las pruebas del JAR verifican ambos dialectos, dirección, ubicación, comparación inversa y salida determinista.
