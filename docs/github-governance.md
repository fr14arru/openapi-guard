# Gobierno del repositorio y protección de main

Fran (`fr14arru`) mantiene el repositorio oficial. El flujo recomendado para otras personas es fork, rama y pull request. MIT también permite clonar, modificar y redistribuir sin contribuir al original. Hacer público un repositorio no da permisos de escritura a sus lectores.

## Configuración pendiente de comprobar en GitHub

1. En Settings → Collaborators, revisar usuarios y aplicaciones con escritura o administración. Conservar solo los accesos deseados por Fran.
2. En Settings → Rules → Rulesets, crear una regla activa para `main` que restrinja las actualizaciones a los actores autorizados mediante bypass. Revisar qué administradores pueden usarlo: no equivale a «solo Fran» si existen otros administradores o aplicaciones con ese permiso.
3. Bloquear borrados y force pushes. Si el propietario puede eludir la restricción de actualización, usar reglas separadas sin bypass para esos bloqueos.
4. Configurar las comprobaciones obligatorias con sus nombres reales después de observar una ejecución. No exigir una aprobación que impida al único mantenedor integrar sus propias PR sin un mecanismo deliberado de bypass.
5. Probar una PR de fork y la integración por el propietario antes de declarar cerrada la protección.

`.github/CODEOWNERS` solicita revisión de Fran, pero **no protege main por sí solo**. Las reglas remotas no están activadas por añadir este archivo. La sesión de navegador disponible no está autenticada en GitHub; queda pendiente comprobar y aplicar la configuración remota.

La apertura también requiere resolver la privacidad del historial, descrita en [transición MIT](open-source.md). No se ha cambiado la visibilidad ni reescrito ningún commit.

Referencias: [reglas disponibles de GitHub](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/available-rules-for-rulesets) y [ramas protegidas](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches).
