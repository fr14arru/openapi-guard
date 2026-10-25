# Revisión crítica de la planificación

Fecha: 2026-10-08. Revisión histórica; posteriormente se ha autorizado exclusivamente la estructura inicial. Ver [desarrollo local](development.md).

## Evaluación y correcciones

| Aspecto | Problema en el plan original | Corrección |
| --- | --- | --- |
| Sobreingeniería | Nueve paquetes, puertos de entrada/salida, DTOs y grafo completo antes de una comparación útil | Cuatro paquetes, caso de uso concreto y un puerto de análisis; modelo propio solo para lo necesario |
| DDD | Objetos valor acertados, pero riesgo de copiar OpenAPI y crear agregados sin invariantes útiles | Lenguaje de compatibilidad y resultados propios; no entidades persistentes ni repositorios ficticios |
| Hexagonal | Independencia correcta; demasiadas abstracciones para un único consumidor | CLI como adaptador conductor, puerto donde se aísla motor/parser, inyección por constructor y configuración Boot en el borde |
| Spring Boot | Excluirlo por simplicidad no tenía en cuenta la experiencia del desarrollador | Boot desde el inicio para CLI no web; dominio y aplicación Java puros, comprobables sin contexto Spring |
| Requests/responses | Inclusiones bien orientadas; retirada de cuerpo etiquetada con excesiva certeza y retirada de código siempre no bloqueante | Cuerpo retirado revisable; códigos requieren examinar fallback y semántica documentada; distinguir datos de negociación |
| Cobertura | Cualquier seguridad/servidor presente podía volver indeterminado el análisis completo | Declarar facetas excluidas del producto y comparar datos comprometidos; opacidad en esos datos sí impide resultado verde |
| Dependencias | Faltaba evidencia de mantenimiento y licencia, y se favorecía implícitamente construir motor | Primero prueba de reutilización; tabla de releases/licencias con límites de evidencia |
| Venta | CLI y piloto llegaban tras 20 tareas; 3.0/3.1 y muchos constraints obligatorios antes de validar demanda | CLI en la segunda tarea, feedback desde entonces, primera versión 3.0 y ampliaciones por bloqueadores reales |

El recorte de 3.1, límites y recursividad semántica es una propuesta comercial, no una decisión aprobada por el usuario. Si los contratos iniciales requieren esas capacidades, deben adelantarse. Una CLI demasiado restrictiva también puede ser un mal MVP: el corpus real decide el mínimo, no la comodidad técnica.

## Semántica que se mantiene

Petición `old ⊆ new`; respuesta `new ⊆ old`. Reducir un enum restringe peticiones y restringe salidas: efectos opuestos. Required false→true rompe entradas, mientras required true→false puede romper la garantía de una salida. Para propiedades, comprobar objetos abiertos y restricciones conjuntas; no basta interpretar el diff como eliminación = ruptura.

Las inclusiones describen un contrato de datos, no todos los comportamientos de un servidor. La ausencia de parámetro/cuerpo/código no determina de forma universal qué hace una implementación. Negociación de contenido y disponibilidad son dimensiones distintas. Las reglas de revisión conservan la incertidumbre en lugar de convertir una política comercial en una demostración.

La especificación permite no enumerar todas las respuestas y da precedencia a respuestas específicas sobre rangos. Por eso eliminar un código específico puede cambiar la restricción efectiva bajo `default`/rangos. Referencia: [Responses Object de OpenAPI 3.0.4](https://spec.openapis.org/oas/v3.0.4.html#responses-object). La elección de revisión es política propuesta del producto.

## Riesgos principales

1. Perfil demasiado pequeño: muchos contratos usan límites/composición y podrían no obtener una conclusión útil. Revisar contratos reales antes de vender y ampliar solo bloqueadores.
2. Falsos positivos y falsos verdes por objetos abiertos, dirección o garantías de terceros. Corpus adversarial, cobertura explícita y revisión semántica de discrepancias.
3. Diferenciación insuficiente frente a herramientas gratuitas. Validar pago por onboarding, claridad, soporte o políticas; no atribuir valor comercial al mero hecho de construir otro diff.
4. Licencias transitivas y artefactos exactos pendientes. Apache-2.0 en los candidatos principales permite uso comercial bajo sus obligaciones; revisar redistribución/NOTICE y dependencias al fijar versiones. Ver [evidencia de arquitectura](architecture.md#mantenimiento-y-licencias).
5. No hay prueba local todavía de Java 25, ausencia de E/S externa ni fidelidad del parser/motor. Las fuentes documentales no sustituyen D01.

## Estado de aprobación

Se autorizó revisar documentación y hacer commit; posteriormente el usuario decidió incluir Spring Boot desde el inicio por familiaridad laboral. Esa elección sustituye la propuesta de posponer Boot. Ahora se autoriza solo la estructura inicial: POM, paquetes y prueba JUnit 5. La prueba de parser/motor, comparación y siguientes tareas siguen fuera de esta entrega. Las descargas requieren aprobación separada, y publicación/contacto comercial no están autorizados.

Ver [arquitectura revisada](architecture.md), [alcance](mvp-scope.md), [roadmap](roadmap.md) y [reglas del repositorio](../AGENTS.md).
