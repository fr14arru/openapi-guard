# OpenAPI Breaking Change Guard

Compara contratos OpenAPI y detecta cambios que pueden romper a sus consumidores, tanto en terminal como en pull requests de GitHub. Código propio gratuito bajo [MIT](LICENSE), sin licencia de pago ni cuenta de producto. Las bibliotecas conservan sus licencias.

## Ejecutarlo

Requisitos de desarrollo: Amazon Corretto Java 25 y Maven 3.10.0; se recomienda mvnd. No requiere base de datos ni Docker. Desde la raíz:

```powershell
mvnd clean verify
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=text examples/catalog/base.yaml examples/catalog/breaking.yaml
```

El ejemplo incompatible termina con código 1: es el resultado esperado. Para comprobar compatibilidad, sustituye `breaking.yaml` por `compatible.yaml`. `--help` muestra las opciones. Maven estándar funciona con `mvn clean verify`.

Los formatos disponibles son texto, JSON con sangría, Markdown y CI. Usa `--profile=full` para toda la cobertura actual; el perfil predeterminado conserva la cobertura histórica de parámetros.

| Código | Resultado |
| --- | --- |
| 0 | Compatible dentro del perfil analizado |
| 1 | Ruptura demostrada |
| 2 | Error de entrada o ejecución |
| 3 | Revisión necesaria; análisis incierto |

## Qué analiza

Operaciones eliminadas, parámetros obligatorios, cuerpos y formatos de petición, tipos, enums, nullabilidad, límites, propiedades, obligatoriedad, objetos abiertos/cerrados, readOnly/writeOnly, arrays/items y política de disponibilidad de respuestas. Peticiones y respuestas se comparan en direcciones distintas. Cada informe declara exclusiones e incertidumbre: **no certifica compatibilidad global del API**. Ver [alcance y límites](docs/mvp-scope.md).

Los hallazgos incluyen reglas, motivos y posiciones del contrato candidato. Una eliminación puede señalar el contenedor superviviente; las líneas son ayudas de navegación. Ver [ubicaciones](docs/source-locations.md).

## GitHub Actions

La [guía rápida](docs/github-actions-quickstart.md) incluye un workflow con informes, anotaciones y comentario actualizable en la conversación de la PR. `fail-on-breaking: 'false'` permite rupturas intencionadas; entradas inválidas y revisión pendiente siguen fallando. Publicar comentarios requiere `pull-requests: write` y se omite en forks.

Actualmente se utiliza un paquete local con Action y JAR. Todavía no hay una release pública MIT ni una referencia remota de instalación publicada. Hacer público el repositorio no genera esos artefactos automáticamente.

El código está publicado en [fr14arru/openapi-guard](https://github.com/fr14arru/openapi-guard), con un historial nuevo y el repositorio original conservado privado. Las dos reglas de main están activas: restringen actualizaciones y bloquean borrados y force pushes. Ver [estado verificado](docs/github-governance.md).

La validación de la copia exportada detectó y corrigió una preparación de test dependiente de LF; la regresión comprueba ahora LF y CRLF explícitamente.

## Documentación y colaboración

- [Guías de usuario e instalación](https://github.com/fr14arru/OpenAPI-Breaking-Change-Guard-Docs).
- [Inicio y próximos pasos de código abierto](docs/open-source.md).
- [Guía para leer el código](docs/code/README.md) y [arquitectura](docs/architecture.md).
- [Roadmap actual](docs/portal/roadmap.md).
- [Portal Docusaurus local](docs/portal/documentation-site.md).
- [Contribuir mediante fork y PR](CONTRIBUTING.md) y [gobierno de main](docs/github-governance.md).
- [Atribuciones y fuentes de terceros](docs/third-party-review.md).

Contacto público: rayopuyi5@gmail.com. Las aportaciones económicas serán voluntarias y no desbloquearán funciones; el enlace todavía está pendiente. Los documentos comerciales anteriores se conservan como historial y no son condiciones de esta distribución MIT.
