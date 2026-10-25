# Cómo mejorar el producto y facilitar la revisión

Estas son propuestas para después de revisar y validar lo existente. El portal implementa las ayudas de lectura; las propuestas de motor y SaaS siguen sujetas a alcance y autorización.

## Ayudas de comprensión que incorpora el portal

- Un recorrido por objetivos: entender el producto → interpretar un informe → seguir la regla → revisar su test.
- Diagramas para llamadas y fases; ninguna librería del adaptador aparece como dependencia del dominio.
- Código íntegro local con líneas, filtros de paquetes y búsqueda de contenido, unido a explicaciones.
- Catálogo filtrable con anteriores/candidatos y expectativas; distingue ejemplos históricos de los ampliados.
- Búsqueda local de documentación, código y casos, sin servicio de indexación externo.
- Markdown como fuente única; el sitio transforma enlaces y genera índices al preparar el contenido.

## Mejoras futuras de experiencia

| Propuesta | Beneficio | Verificación útil |
| --- | --- | --- |
| Informe que reúna una vista compacta por operación | Revisar muchos usos sin perder detalle | Un desarrollador localiza causa y consumidores afectados. |
| Opción explícita para política CI de revisión | Decidir si código 3 bloquea sin inventar compatibilidad | Guía y pruebas del flujo CI seleccionado. |
| Distribución y arranque más simples | Menos fricción para equipos nuevos | Instalación desde entorno limpio y notices revisados. |
| Ejemplos reales anonimizados y autorizados | Evaluar qué reglas aportan valor | Comparación manual, falsos positivos y lagunas medidos. |

No conviene reorganizar los cuatro paquetes ni introducir una interfaz por regla solo para que el código parezca más arquitectónico. Tampoco sustituir incertidumbre por una heurística optimista para reducir el número de diagnósticos.

## Mejoras futuras del motor

Elegir por frecuencia en contratos de piloto: composición, additionalProperties con schema, serialización, fallback o negociación. Cada una necesita una política explícita y pruebas conjuntas/direccionales. Antes de ampliar, medir cuántos usos quedan indeterminados y si la mejora elimina un bloqueo real.

Mantener un módulo mientras siga siendo fácil compilar y revisar. Separar módulos cuando aparezcan necesidades distintas de distribución o dependencias, no por calendario. Mantener dominio y aplicación invocables sin Boot.

## Criterio práctico para priorizar

Primero, una entrega que otro desarrollador pueda ejecutar y comprender. Después, evidencia de utilidad real y uso repetido. Finalmente, automatizar la fricción observada. La infraestructura central tiene sentido cuando exista una necesidad que la CLI y CI local no satisfagan, con coste operativo y aislamiento definidos.

Consultar [roadmap](roadmap.md) para dependencias y [mantenimiento del código](../code/maintenance.md) para el procedimiento TDD.
