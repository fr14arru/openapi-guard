# Contribuir

Se puede utilizar, modificar y redistribuir conforme a [MIT](LICENSE). Las dependencias y fuentes de terceros mantienen sus licencias originales.

1. Abre una issue para explicar el problema o la mejora; no adjuntes contratos confidenciales ni credenciales.
2. Haz un fork y crea una rama de trabajo.
3. Para comportamientos nuevos, añade primero una prueba que falle y después la implementación mínima. Usa contratos sintéticos pequeños y cubre ambas direcciones cuando corresponda.
4. Ejecuta `mvnd test` y `mvn verify`. Para empaquetado/Action, ejecuta también `node --test scripts/*.test.mjs` con el JAR ya construido. Consulta [desarrollo](docs/development.md) para herramientas y dependencias.
5. Actualiza README y documentación. Mantén dominio y aplicación como Java puro; no añadas infraestructura sin necesidad.
6. Crea commits Conventional Commits en inglés y abre una PR desde tu fork. El mantenedor revisa e integra los cambios; no se concede acceso directo de escritura por contribuir.

Solo envía trabajo que tengas derecho a aportar bajo MIT. Conserva las atribuciones de terceros. No hay SLA ni garantía de aceptación o plazos de revisión. El contacto público es rayopuyi5@gmail.com.

La organización del repositorio y las protecciones deseadas están en [gobierno de main](docs/github-governance.md). La licencia permite también clones y redistribuciones independientes; el fork es el flujo recomendado para contribuir al repositorio oficial.
