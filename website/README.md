# Portal documental local

Guías españolas, roadmap, casos de uso, código completo y búsqueda local. Fuente Markdown en ../docs; configuración y componentes en este directorio. El build Java es independiente.

Requisitos: Node ≥20 y pnpm. Docusaurus/React y sus transitivas fueron autorizados el 10 de octubre de 2026. Las versiones exactas se fijan en package.json y pnpm-lock.yaml; futuras actualizaciones requieren su aprobación correspondiente.

```powershell
Set-Location website
pnpm install --frozen-lockfile --ignore-scripts --store-dir .pnpm-store
pnpm test
pnpm prepare:content
pnpm verify:cases
pnpm build
pnpm test:build
pnpm serve --port 3000
```

Abrir http://localhost:3000. Si ya están instaladas las dependencias y construido el sitio, basta `pnpm serve --port 3000`. Para detenerlo, Ctrl+C en esa terminal. `verify:cases` requiere el build Java y sus informes existentes; no ejecuta Maven ni descarga dependencias. Ejecuta PowerShell con Bypass solo en ese proceso, sin cambiar la política permanente. Para desarrollar: `pnpm start --port 3000`.

El explorador y la búsqueda leen archivos del repositorio al preparar contenido. El catálogo incluye las expectativas históricas y ejemplos generados; no ejecuta Java dentro del navegador. No se publican fuentes a un servidor externo.

Las carpetas node_modules, .generated, .docusaurus, build y .pnpm-store están ignoradas. Más detalles en [mantenimiento del portal](../docs/portal/documentation-site.md).

`test` comprueba siete casos del generador y la navegación. `test:build` exige un build previo y comprueba las opciones reales de los tres filtros en HTML de producción. `verify:cases` contrasta 218 pares con el motor. Además de esas baterías, revisar en el navegador catálogo, búsqueda, explorador y diagramas después de actualizar el portal.
