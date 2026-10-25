# Instrucciones de desarrollo

OpenAPI Guard es software libre bajo MIT. Mantener las licencias y avisos de terceros; las fuentes y textos originales de licensing/ no deben modificarse ni normalizarse.

## Diseño

- Leer docs/architecture.md, docs/mvp-scope.md y docs/portal/roadmap.md.
- Java 25 sin preview; Maven estándar, mvnd recomendado. Spring Boot solo para arranque y configuración de la CLI no web.
- Dominio y aplicación Java puro, sin Spring, Swagger ni E/S en las reglas. Swagger Parser queda detrás de adapter.openapi.
- Arquitectura hexagonal y DDD pragmáticos: un módulo y límites útiles, sin infraestructura innecesaria.
- Peticiones: old incluido en new. Respuestas: new incluido en old. Nunca declarar compatible un análisis incompleto.
- Conservar perfiles versionados, incertidumbre, facetas excluidas y códigos de salida 0/1/2/3.

## Calidad

- TDD: demostrar el fallo antes de implementar y añadir regresiones pequeñas por dirección.
- Ejecutar mvnd test durante el desarrollo y mvn verify antes de entregar cambios relevantes.
- Para scripts/Action, construir el JAR antes de node --test scripts/*.test.mjs. No ejecutar clean en paralelo con lectores de target.
- Mantener README, guías del código, catálogo de reglas y Javadoc; ejecutar scripts/javadoc.ps1 -SkipBuild.
- Al cambiar el portal, regenerar contenido, probar, contrastar sus casos y compilar Docusaurus. No editar .generated.
- Salida de la herramienta en inglés, conservando Unicode del contrato. Documentación en español.
- No añadir dependencias o herramientas sin justificar necesidad, licencia y mantenimiento en la PR.

## Contribuciones y seguridad

- Contribuir mediante fork y PR; Conventional Commits en inglés. Solo el mantenedor integra en main.
- No publicar contratos confidenciales, credenciales, datos privados ni artefactos locales. No hacer force push ni reescribir historial ajeno.
- CODEOWNERS no activa protecciones: las reglas de main deben configurarse y comprobarse en GitHub.
- Mantener permisos mínimos en Actions. No usar pull_request_target para ejecutar código de contribuciones.
- Revisar CONTRIBUTING.md. Contacto público: rayopuyi5@gmail.com.
