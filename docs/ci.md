# Uso en CI sin infraestructura propia

La CLI ya es un JAR Java 25 ejecutable. La receta no instala herramientas ni accede a proveedores; cualquier job con Corretto/Java 25 y Maven puede ejecutar verify y comparar dos archivos locales. Elegir explícitamente base y candidata: la base debe ser el contrato realmente publicado o el commit objetivo de integración, no dos copias del archivo modificado.

## PowerShell

La [integración Git/GitHub](git-integration.md) añade comparación entre commits y workflow de PR. Tiene pruebas locales; su ejecución remota y el check obligatorio en main requieren acceso a GitHub y validación separada. El workflow prepara herramientas en el runner, con aprobación de descargas; la receta local siguiente no instala herramientas.

La variante probada usa PowerShell 7, ya disponible en el entorno de desarrollo. Compilar y verificar primero; en el runner se pueden permitir las dependencias Maven aprobadas o usar caché y `-o`.

```powershell
mvn -o clean verify
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
pwsh -NoProfile -File scripts/compare-contracts.ps1 -Baseline 'contracts/base.yaml' -Candidate 'contracts/candidate.yaml' -Report 'target/contract-report.json'
exit $LASTEXITCODE
```

El script usa `full` por defecto; se puede seleccionar `-Profile arrays` u otro perfil histórico. Pasa rutas como argumentos independientes al proceso Java, admite espacios y escribe JSON UTF-8 sin BOM. Resume hallazgos/revisión para personas y conserva exactamente los códigos del motor. El timeout del proceso Java es de 60 segundos; un fallo de arranque, timeout o salida inválida da código 2. No sobrescribe contratos ni el JAR si se seleccionan como ruta del informe.

| Código | Decisión del job |
| --- | --- |
| 0 | Continuar: compatible solo dentro del perfil |
| 1 | Bloquear: ruptura de datos/disponibilidad documentada |
| 2 | Bloquear: corregir entrada, argumentos o ejecución |
| 3 | Bloquear certificación automática; revisar incertidumbre |

Guardar `target/contract-report.json` como artefacto del job incluso con códigos 1/2/3, si se produjo un informe válido. Los errores de lectura de contratos producen FAILED en JSON; errores del lanzador o argumentos pueden no producir informe. Usar una ruta nueva por ejecución y no consumir un informe anterior tras un fallo de arranque. No convertir un código 3 en éxito ni silenciar el código 1 con `continue-on-error`.

## Sin PowerShell

En un runner con shell POSIX y Java, no hace falta instalar PowerShell:

```sh
mvn -o clean verify || exit $?
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar \
  --profile=full --format=json "$BASELINE" "$CANDIDATE" > contract-report.json
result=$?
exit "$result"
```

Esta receta conserva el exit code y el JSON. La variante POSIX es documentación; la validación de proceso se realizó en Windows/PowerShell. Ahora hay un workflow preparado para GitHub; su ejecución remota se registra por separado en la guía de integración.

## Validación reproducible

```powershell
pwsh -NoProfile -File scripts/test-ci.ps1
```

Diez comprobaciones contra el JAR real: contratos equivalentes (0), retiradas (1), adiciones con revisión (3), archivo ausente (2), perfil inválido (2), JAR ausente (2), protección del contrato como ruta de informe (2), y tres variantes del catálogo SaaS sintético (0/1/3). Se usan rutas con espacios y JSON UTF-8 estricto. Las siete pruebas iniciales empezaron fallando por ausencia del script antes de implementarlo. No requieren Pester ni nuevas dependencias.

Los ejemplos son sintéticos. Los 146 pares históricos siguen validando su perfil de parámetros; no reinterpretarlos como cobertura de todos los perfiles nuevos. Para arrays y respuestas utilizar los ejemplos y tests específicos. El piloto con contratos reales y clientes sigue pendiente.

El catálogo de `examples/catalog/` contiene productos y pedidos, parámetros query/path, cuerpos obligatorios, objetos abiertos/cerrados, propiedades readOnly, arrays de objetos y componentes compartidos. Base → compatible amplía la cantidad aceptada y restringe resultados; base → breaking restringe cantidad y amplía enums/arrays de respuesta; base → review introduce pattern pendiente. Resultados esperados: 0/1/3 respectivamente. [Guía del catálogo](../examples/catalog/README.md).
