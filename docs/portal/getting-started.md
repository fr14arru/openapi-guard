# Primer recorrido como desarrollador

## 1. Entender el resultado con ejemplos

Para comenzar sin escoger contratos: `pwsh -NoProfile -ExecutionPolicy Bypass -File .\guard.ps1 -Demo`. Usa el perfil full y un ejemplo compatible. La política Bypass afecta solo a ese proceso. Después seguir la [ruta de lectura](../reading-roadmap.md).

Desde la raíz, con Corretto 25 y mvnd disponibles y dependencias Maven aprobadas ya cacheadas:

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=text examples/catalog/base.yaml examples/catalog/breaking.yaml
$LASTEXITCODE
```

Se esperan cuatro hallazgos y código 1. Comparar base.yaml con compatible.yaml produce código 0; con review.yaml produce código 3. Un archivo inválido puede producir código 2. Los scripts están documentados en [build y CI](../code/build.md).

## 2. Explorar los contratos

Abrir el catálogo interactivo del portal, filtrar por petición/respuesta, familia y resultado, y seleccionar un ejemplo. Cada caso muestra baseline/candidate completos y expectativa explícita. Los 146 casos del corpus llevan perfil parameters; los ejemplos ampliados indican su propio perfil. No interpretar un verde de operations como una validación de schemas.

El portal explica comparaciones precalculadas: no ejecuta Java en el navegador ni envía contratos a un servicio. Para analizar tus propios archivos, utilizar la CLI local.

## 3. Seguir el código que produjo el resultado

Leer [flujo de ejecución](../code/runtime.md), luego abrir el explorador de archivos. Para schemas, empezar por SchemaValuesRule, continuar con SchemaInclusion y terminar con SchemaValuesReader. Para códigos de respuesta, revisar ResponsePolicyRule y ResponseContractsReader.

El explorador permite buscar archivos y contenido, seleccionar un paquete, ver líneas y abrir la explicación del componente. Los enlaces del inventario abren el código local del portal, sin depender de GitHub.

## 4. Ejecutar una batería acotada

```powershell
mvnd -o '-Dtest=SchemaInclusionTest' test
mvnd -o '-Dtest=ObjectSchemaComparisonTest' test
pwsh -NoProfile -File scripts/test-ci.ps1
```

El script exige JAR existente. Para verificar también los 25 casos del JAR, utilizar verify. Ver [qué prueban los oráculos](../code/tests.md).

## 5. Revisar una mejora antes de tocar producción

Plantear un contrato mínimo válido, decidir el resultado esperado considerando dirección y restricciones juntas, y escribir la prueba roja. Un resultado UNKNOWN no se corrige cambiando el test a verde: hace falta demostrar la semántica o conservar la revisión. Ver [mantenimiento](../code/maintenance.md).
