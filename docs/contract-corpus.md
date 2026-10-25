# Corpus sintético de contratos

El usuario confirmó que no dispone de contratos reales y solicitó ejemplos generados. Se construye un corpus reproducible de 60 contratos pequeños JSON y un manifiesto con 146 comparaciones en `src/test/resources/openapi/corpus/`. Los dos ejemplos adicionales `parameters-baseline.yaml` y `parameters-required.yaml` prueban herencia y referencias desde el JAR.

No son contratos de clientes ni evidencia de ventas. No es posible enumerar todos los documentos OpenAPI: la cobertura se expresa mediante una matriz finita del perfil y sus límites.

## Matriz

| Familia | Comparaciones cubiertas |
| --- | --- |
| Presencia escalar | Producto de 3 ubicaciones (query/header/cookie), 4 tipos (integer/string/number/boolean) y 3 estados antes × 3 después (ausente/opcional/obligatorio): 108 pares |
| Path | Identidad de parámetro obligatorio, declaración opcional/required ausente inválidas, plantilla renombrada |
| Herencia | Required heredado, sustitución local, lista local vacía |
| Identidad | Casing de header equivalente, casing de query distinto, parámetros reordenados |
| Referencias | Inline/ref equivalente, required cambiado en componente, referencia externa/rota/cíclica |
| Entrada inválida | Duplicados exactos/de header, required textual, ubicación desconocida, lista null, schema y content simultáneos |
| Presencia sin cobertura | Objetos, arrays, content e identidad de esos contratos: revisión visible |
| Serialización | Defaults explícitos equivalentes y cambio de explode: revisión |
| Exclusiones | Headers especiales ignorados; required de respuesta excluido |
| Operaciones | Endpoint/método eliminado, altas y supresión de hallazgos de parámetros al eliminar una operación |
| Dialecto | Obligatorio nuevo en 3.1 y comparación entre familias rechazada |

Los 38 pares adicionales completan las familias que no pertenecen al producto de estados escalares. Otros tests cubren los ocho métodos HTTP, YAML/JSON equivalentes, límites de recursos, UTF-8, errores de archivos, ausencia de fetch HTTP, punteros y determinismo.

## Oráculo y ejecución

`cases.json` enumera id, baseline, candidate, status, verdict, reglas esperadas y diagnóstico cuando aplica. Las expectativas están definidas por escenario; no se calculan a partir del comparador. `ContractCorpusTest` exige IDs únicos, carga cada par real y verifica el informe esperado y la repetibilidad. No usa otra herramienta de diff como oráculo.

```powershell
mvnd '-Dtest=ContractCorpusTest' test
mvnd clean verify
mvn verify
```

Para revisar un par manualmente, pasar sus archivos a la CLI. Ejemplos:

```powershell
# Compatible: relajar obligatoriedad
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/query-required.json src/test/resources/openapi/corpus/query-optional.json
# Ruptura: añadir un parámetro obligatorio
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/empty.json src/test/resources/openapi/corpus/header-required.json
# Revisión: retirar un parámetro
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/cookie-optional.json src/test/resources/openapi/corpus/empty.json
# Inválido: required textual
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar src/test/resources/openapi/corpus/empty.json src/test/resources/openapi/corpus/invalid-required.json
```

Cada proceso termina con código 0/1/3/2 respectivamente. Un informe compatible limita su afirmación a [parameter-presence-profile.md](parameter-presence-profile.md).

## Trabajo futuro

Añadir un par mínimo por cada defecto confirmado. Mantener el corpus sintético aunque se incorpore uno real autorizado; no publicar contratos ajenos ni confundir un corpus de ejemplo con una validación de piloto. Antes de venta sigue siendo necesaria evidencia de uso y falsos positivos sobre contratos representativos.
