# Mantener y ejecutar el portal Docusaurus

El sitio está en website/. La documentación fuente permanece en docs/; el código y fixtures se leen del repositorio. `.generated/` contiene páginas e índices derivados y no se versiona. Maven no necesita Node ni Docusaurus.

## Herramientas y dependencias

Node 20 o superior y pnpm. Docusaurus core, preset-classic y theme-mermaid 3.10.2; React/ReactDOM 18.3.1. Node y pnpm ya existen en el entorno de esta sesión. Estas versiones y sus transitivas fueron autorizadas e instaladas el 10 de octubre de 2026, con lockfile y sin scripts de instalación. Nuevas versiones o herramientas siguen requiriendo aprobación. Ver [instalación oficial](https://docusaurus.io/docs/installation) y [diagramas](https://docusaurus.io/docs/markdown-features/diagrams).

Desde website/, una vez autorizadas las dependencias:

```powershell
pnpm install --frozen-lockfile --ignore-scripts --store-dir .pnpm-store
pnpm test
pnpm prepare:content
pnpm verify:cases
pnpm build
pnpm test:build
pnpm serve --port 3000
```

Abrir http://localhost:3000. Para editar y ver cambios: `pnpm start --port 3000`; después de cambios en fuentes fuera de website, reiniciar start para regenerar el índice. Los servidores se limitan a 127.0.0.1. Serve presenta el resultado construido; ejecutar build después de modificar documentación. Ctrl+C detiene el servidor local. verify:cases usa Bypass únicamente en su proceso PowerShell.

Los diagramas usan Dagre, incluido en Mermaid. El primer build de Docusaurus 3.10.2 intentó resolver el import de @mermaid-js/layout-elk aunque no estaba habilitado. El plugin local dagreOnly declara un fallback deshabilitado para ese módulo opcional; no modifica dependencias ni habilita layouts ELK. Verificar los diagramas al actualizar Docusaurus.

Tras existir el lockfile, las instalaciones repetidas usan `pnpm install --frozen-lockfile --ignore-scripts --store-dir .pnpm-store`. No se ejecutan scripts de instalación de dependencias. Las actualizaciones a nuevas versiones/transitivas siguen sujetas a aprobación. El sitio no está publicado y no incorpora analytics, CDN ni buscador remoto.

## Archivos y responsabilidades

| Archivo/grupo | Responsabilidad |
| --- | --- |
| package.json y pnpm-lock.yaml | Dependencias aisladas y resolución reproducible del sitio. |
| docusaurus.config.js | Idioma español, Mermaid, navegación, tema, límites locales y enlaces estrictos. |
| sidebars.js | Orden de lectura y referencia de todas las guías. |
| scripts/content.mjs | Clasificación, enlaces al explorador y reescritura portable de Markdown. |
| scripts/prepare-content.mjs | Lee fuentes, genera guías e índices de código/casos/búsqueda. |
| scripts/scenarios.mjs | Contratos pequeños y expectativas independientes para casos ampliados. |
| scripts/content.test.mjs | Regresiones de enlaces, clasificación y estructura de escenarios. |
| scripts/site-build.test.mjs | Regresiones del HTML de producción: conserva las opciones de familia, responsabilidad y tipo de contenido. |
| scripts/verify-cases.ps1 | Contrasta estado, veredicto, reglas y diagnóstico esperado contra el motor ya compilado, con un verificador temporal en target. |
| src/pages | Portada, catálogo de casos, explorador de código y búsqueda local. |
| src/components | Presentación común de veredictos y navegación de documentación. |
| src/css/custom.css | Presentación adaptable, foco visible, colores y tema oscuro. |

El explorador muestra las fuentes textuales del repositorio, no archivos locales arbitrarios. No incorpora .git, target, credenciales ni directorios de dependencias. Las guías enlazan páginas locales; archivos del repositorio abren la vista de código. Un enlace local no resoluble falla la preparación, y Docusaurus falla el build ante enlaces internos rotos.

content.mjs convierte separadores Windows a rutas relativas portables, resuelve enlaces con un conjunto de archivos conocidos y conserva ejemplos literales entre fences. fileGroup asigna la responsabilidad y explanationFor dirige al capítulo correspondiente. prepare-content.mjs recorre solo carpetas del producto/documentación, omite resultados y dependencias, fija slugs explícitos y genera catalog.json con archivos, escenarios e índice textual.

El componente común expone useQueryValue para selección por URL y Verdict para presentar estado/veredicto. CodeExplorer filtra por grupo y texto, relaciona tests por nombre de clase y limita inicialmente a 1500 líneas con una opción de lectura completa. Cases filtra familia/resultado/texto y conserva expectativas y contratos originales. Search busca a partir de dos caracteres y muestra hasta 60 resultados. Las selecciones por URL permiten compartir enlaces locales; no modifican el repositorio.

## Añadir contenido sin duplicarlo

Modificar docs/ y repetir build. Un nuevo capítulo debe estar en sidebars.js o enlazado desde una guía; los índices se regeneran. Añadir una expectativa de escenario en scenarios.mjs exige comprobarla contra el motor, además de verificar estructura/IDs con pnpm test. El catálogo histórico se obtiene directamente de cases.json y los fixtures.

No declarar un escenario verificado solo porque se renderiza. Los tests del generador no prueban semántica OpenAPI; el motor y sus baterías siguen siendo la autoridad de ejecución, con expectativas revisadas de forma independiente.

Los filtros usan value explícito en cada option y convierten los conjuntos con Array.from. En la primera comprobación del navegador, el cliente agrupaba todas las etiquetas en una sola opción aunque el HTML del servidor las separaba; se producían errores de hidratación. Array.from conserva las opciones al cargar el cliente. Las tres regresiones de test:build comprueban valores seleccionables en el HTML, pero no sustituyen la comprobación en navegador. Mantener también max-width en los controles para evitar desbordamiento. XML utiliza la gramática markup incorporada en Prism; no añadir xml a additionalLanguages porque no existe un módulo prism-xml.

Verificación del 10 de octubre de 2026: siete pruebas del generador, tres del HTML construido y 218 comparaciones contra el motor pasan. El build estricto termina sin enlaces rotos. En el navegador se comprobaron los diagramas, los contratos y expectativas, la lectura de fuentes, el filtrado por responsabilidad/familia y la búsqueda por tipo; tras la corrección no aparecieron nuevos errores de consola ni desbordamiento horizontal en la vista comprobada.

`verify:cases` necesita el classpath de los informes JUnit de un verify Java previo. Utiliza el lanzador de fuentes de Java 25 y dependencias ya compiladas/cacheadas, sin instalar un framework de test adicional. El verificador temporal se regenera y no forma parte del código del motor.
