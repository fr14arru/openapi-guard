# Gobierno del repositorio y protección de main

Fran (`fr14arru`) mantiene el repositorio oficial. El flujo recomendado para otras personas es fork, rama y pull request. MIT también permite clonar, modificar y redistribuir sin contribuir al original. Hacer público un repositorio no da permisos de escritura a sus lectores.

## Estado comprobado en GitHub

El 25 de octubre de 2026 la API pública confirma que main está protegida y que estas reglas activas se aplican exactamente a refs/heads/main:

| Ruleset | ID | Reglas |
| --- | --- | --- |
| protect-main | 25832626 | Restrict deletions; Block force pushes |
| main-maintainer-only | 25832795 | Restrict updates |

El propietario comunica que configuró Repository admin como excepción solo para las actualizaciones. La API pública no muestra los actores de bypass ni los miembros administradores: no permite certificar que Fran sea el único autorizado. Si es el único administrador y no hay otras excepciones, esa configuración limita las actualizaciones a él. Las dos reglas separadas permiten mantener los bloqueos de borrado y force push sin esa excepción.

## Lista de comprobación para mantener las reglas

1. En Settings → Collaborators, revisar usuarios y aplicaciones con escritura o administración. Conservar solo los accesos deseados por Fran.
2. En Settings → Rules → Rulesets, crear una regla activa para `main` que restrinja las actualizaciones a los actores autorizados mediante bypass. Revisar qué administradores pueden usarlo: no equivale a «solo Fran» si existen otros administradores o aplicaciones con ese permiso.
3. Bloquear borrados y force pushes. Si el propietario puede eludir la restricción de actualización, usar reglas separadas sin bypass para esos bloqueos.
4. Configurar las comprobaciones obligatorias con sus nombres reales después de observar una ejecución. No exigir una aprobación que impida al único mantenedor integrar sus propias PR sin un mecanismo deliberado de bypass.
5. Probar una PR de fork y la integración por el propietario antes de declarar cerrada la protección.

`.github/CODEOWNERS` solicita revisión de Fran, pero **no protege main por sí solo**. Las reglas remotas no se activan por añadir este archivo; su estado verificado consta arriba.

La apertura se realizó con una copia limpia, descrita en [transición MIT](open-source.md). No se hizo público ni se reescribió el historial del repositorio original.

Referencias: [reglas disponibles de GitHub](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets) y [ramas protegidas](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).
