# Ubicaciones del contrato candidato en consola

Los mensajes generados por la herramienta están en inglés: cabeceras, motivos de reglas, diagnósticos y ayuda. Los formatos `json` y `ci` llevan sangría y saltos de línea; no hace falta copiarlos a un formateador. El formato `text` mantiene un hallazgo por renglón. Las rutas, nombres y fragmentos del contrato conservan sus caracteres originales; la consola debe admitir UTF-8 para mostrarlos correctamente. Traducir los mensajes no convierte esos datos a ASCII.

```powershell
mvnd -o clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=text src/test/resources/openapi/parameters-baseline.yaml src/test/resources/openapi/parameters-required.yaml
```

`anterior.yaml` y `nuevo.yaml` son nombres ilustrativos. INPUT_READ_FAILED significa que no se pudo leer una entrada: identifica su ruta y recomienda comprobar existencia, permisos, UTF-8 y límite de 5 MiB. Todavía no se compararon los contratos.

## Presentación

Cada hallazgo ocupa un renglón y se numera consecutivamente: primero rupturas, después diagnósticos de revisión. La numeración es de presentación, no un ID estable. El resumen de líneas aparece al principio, antes de severidad, regla, operación, motivo, ruta absoluta candidata `:línea:columna` y fragmento. Las posiciones empiezan en 1. Ejemplo ilustrativo:

```text
Error 1 - Lines 9 | [ERROR] PARAMETER_BECAME_REQUIRED | GET /pets | The parameter limit in query becomes required. | D:\Contratos\nuevo.yaml:9:7 | L9: required: true
```

La referencia permite usar filtros de navegación de editores/consolas. Que sea clicable depende de su configuración; no se ha certificado navegación automática en IntelliJ ni en todas las terminales. No se añaden dependencias del IDE ni enlaces de terminal.

El resumen combina definición y uso candidatos, ordena y elimina líneas duplicadas. Presenta hasta ocho líneas y avisa si el contexto se trunca. Para schemas se muestran también fragmentos indexados del nodo, como `L13: ... ; L14: ...`, dentro del mismo renglón. Las líneas son contexto de navegación: las pruebas pueden depender de restricciones combinadas y no se atribuye toda ruptura a una keyword inventada. No es un diff completo.

Con refs internas se señala la definición en components del candidato y su uso candidato. Las rupturas no imprimen posiciones del baseline. Cuando desaparece un elemento, se señala el ancestro existente más cercano con `[container; element absent]`: navegación de contexto, no una supuesta línea del elemento eliminado. Un endpoint eliminado puede señalar paths.

Los diagnósticos candidate incorporan posiciones disponibles. Diagnósticos baseline/comparison conservan origen y pointer; no se atribuyen al candidato sin evidencia. Para entradas ilegibles o documentos sin índice fiable no se inventan coordenadas.

Un error de lectura se presenta como `Error 1 - Lines unavailable | Review: INPUT_READ_FAILED ...`, con la ruta real de la entrada que falló. En el ejemplo con anterior.yaml inexistente, el fallo está en el archivo anterior: no hay una línea culpable en el contrato nuevo ni llegó a ejecutarse la comparación. Error N identifica un hallazgo que necesita atención; Review conserva la distinción frente a una ruptura demostrada.

## Acentos en PowerShell

El JAR emite UTF-8. Si aparecen caracteres como `Revisi├│n`, la terminal está interpretando esos bytes con otra codificación. Antes de ejecutar el JAR, configurar la salida de esta sesión:

```powershell
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
```

Esto no modifica permanentemente la configuración del sistema. Los informes guardados con --output siguen siendo UTF-8. La navegación automática de IntelliJ sigue dependiendo de sus filtros de consola.

## Código y garantías

CandidateSource está en adapter.cli. Indexa marcas originales de Jackson YAML (también JSON), claves RFC 6901 y arrays con dependencias existentes. No es otro parser semántico OpenAPI: no resuelve refs, lee archivos ni cambia compatibilidad. Reutiliza el texto leído para comparar. Dominio y aplicación siguen siendo Java puro.

Se usan marcas de inicio de token (`currentTokenLocation`), no posiciones del modelo ya normalizado. Ver [API oficial de Jackson](https://fasterxml.github.io/jackson-core/javadoc/2.13/com/fasterxml/jackson/core/JsonParser.html).

Profundidad máxima 128, aliases rechazados, duplicados detectados y presupuesto de 100000 posiciones. Si falla se descarta todo el índice, preservando el informe del motor. Los controles de terminal y saltos de línea de datos externos se reemplazan por espacios.

SourceOutputTest cubre YAML/JSON, CRLF, escapes, arrays, refs/uso, obligatoriedad, endpoint/método eliminado, entradas ilegibles, equivalencia, varios hallazgos y documentos sin índice fiable. Las tres pruebas iniciales fallaron antes de implementar. HumanOutputTest sustituye la expectativa antigua del pointer eliminado por el contenedor candidato. PackagedCliIT verifica navegación y un renglón por hallazgo con el JAR real.

Text cambia su presentación humana. JSON conserva esquema, procedencia, IDs, perfil y códigos para automatización; Markdown conserva su presentación anterior. No se añaden reglas ni dependencias.
