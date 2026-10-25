# Cómo funciona SchemaInclusion

El método público `compare(source, target)` intenta probar `source ⊆ target` en el fragmento soportado. No decide por sí mismo si se trata de petición o respuesta: SchemaValuesRule establece la dirección. Cada llamada crea una `Evaluation` con caches propias; no se comparte estado mutable entre comparaciones públicas.

`Result` incluye `Outcome` (INCLUDED, EXCLUDED o UNKNOWN), motivo, pointers source/target e `incomplete`. Este último campo es independiente del outcome: puede existir una ruptura demostrada y además restricciones de otros subárboles que no se han podido analizar. La sobrecarga de dos argumentos marca UNKNOWN como incompleto. `Interval` representa límites numéricos y calcula si el intervalo es vacío o singleton.

## Orden de la prueba

1. `Evaluation.compare` consulta la cache por identidad de la pareja de nodos. `compute` produce el resultado; se rellenan pointers ausentes con las definiciones actuales y se añade la incertidumbre de ambos árboles.
2. Restricciones desconocidas en el nodo raíz dan UNKNOWN. Si no se ha demostrado satisfacibilidad de propiedades requeridas de objetos, tampoco se da una prueba definitiva.
3. `effectiveTypes` elimina ramas imposibles por límites numéricos, longitudes o arrays. Un number restringido a un único entero se trata como integer para la inclusión. Un esquema sin valores válidos se comunica conservadoramente como UNKNOWN; no se utiliza la inclusión vacía para dar verde.
4. `finiteValues` calcula un dominio escalar enumerable cuando puede: enum filtrado por restricciones, boolean, null, string vacío o intervalos enteros pequeños. Todos sus valores deben satisfacer conjuntamente el destino. Enum no anula tipos ni límites.
5. Los tipos de source deben estar cubiertos por target; integer se admite dentro de number. Si un tipo array exige items cuya existencia no está demostrada, no se inventa un contraejemplo de array.
6. Si target tiene enum, `enumWitness` busca o demuestra un valor fuera del enum y `enumCovers` intenta probar cobertura completa. Si ninguna prueba es suficiente, devuelve UNKNOWN.
7. Se comparan longitudes de strings, intervalos de integer/number, propiedades de objetos e items/longitudes de arrays. La primera ruptura demostrada se devuelve como explicación de ese uso. No se promete enumerar todos los motivos de ruptura dentro del mismo schema.

## Números, strings y enum

`interval` convierte límites exclusivos/inclusivos de integer a extremos enteros inclusivos con CEILING/FLOOR y un ajuste de unidad cuando hace falta. Para number conserva los extremos decimales y su exclusividad. `contains(target, source)` prueba ambos extremos, considerando límites ausentes como no acotados.

`finiteValues` enumera intervalos enteros con diferencia menor que 1024; para dominios mayores, la cobertura de enum puede demostrarse mediante recuento de valores distintos que cumplen **también** los límites. Los números son BigDecimal canonizados; no se compara 1 y 1.0 como strings distintos.

`enumWitness` puede demostrar que un dominio continuo, estructural o entero no acotado no cabe en un enum finito. Para strings busca testigos repetidos de puntos de código entre 32 y 1055, solo si la longitud mínima es como máximo 1024. Es una búsqueda acotada, no un solver de strings general. No encontrar un testigo no demuestra compatibilidad. `enumCovers` solo devuelve cierto para dominios cuya cobertura puede justificarse completamente. `predicate` preconstruye un conjunto de enum y aplica las restricciones escalares.

Las longitudes se interpretan en puntos de código Unicode, no unidades UTF-16. Por ejemplo, `🐈` tiene longitud uno a estos efectos.

## Objetos

`shape` da un objeto abierto y sin propiedades cuando no hay restricciones estructurales normalizadas. `unrestricted` crea un schema de todos los tipos para un nombre adicional permitido; mantiene un pointer al contenedor real.

`objectInclusion` sigue este orden:

1. Un cambio de readOnly/writeOnly de una propiedad común requiere revisar su comportamiento efectivo.
2. Si target exige una propiedad que source puede omitir, existe una ruptura de inclusión.
3. Si source admite nombres adicionales y target cierra el objeto, hay una ruptura.
4. Para cada nombre de la unión ordenada de propiedades: un nombre imposible en un source cerrado no produce un contraejemplo; un nombre permitido en source y prohibido en target sí lo produce. Si un lado no declara la propiedad pero permite adicionales, se compara contra `unrestricted`.
5. Se compara cada schema hijo. El motivo se acompaña del nombre de propiedad y los pointers del hijo. Si no hay ruptura pero queda algún UNKNOWN, se conserva la incertidumbre.

Por eso eliminar una propiedad de respuesta no equivale siempre a una ruptura: importan required, apertura del objeto y los valores permitidos para nombres adicionales. La proyección direccional de flags ocurre antes, en el adaptador.

`objectSatisfiable` revisa propiedades requeridas. Una requerida no declarada en un objeto cerrado es imposible; una requerida con schema opaco no permite demostrar existencia. No se prueban restricciones de objeto que no forman parte del fragmento soportado.

## Arrays

`arrayShape` interpreta ausencia de forma como array sin restricción de items, mínimo 0 y máximo Integer.MAX_VALUE. `arrayMaximum` reduce el máximo efectivo a cero cuando se demuestra que ningún item es satisfacible. Esto permite analizar un array vacío aunque sus items sean imposibles.

`arrayInclusion` comprueba longitudes y luego inclusión de items. Para probar una longitud excluida necesita un array válido: si hacen falta elementos y su existencia es opaca, devuelve UNKNOWN. El array vacío proporciona por sí mismo un testigo para mínimo nuevo mayor que cero. Si source solo admite el vacío, no compara items inalcanzables. Los arrays no vacíos necesitan items satisfacibles antes de demostrar inclusión o ruptura recursiva.

Los arrays son homogéneos. Tuplas, uniqueItems, contains y otras restricciones no implementadas son lagunas, incluso si el texto coincide en ambos contratos.

## Satisfacibilidad, incertidumbre y rendimiento

`satisfiable` usa una cache por identidad y `calculateSatisfiable` intenta encontrar una rama válida escalar, objeto o array. INCLUDED significa existencia probada en ese cálculo; EXCLUDED, imposibilidad demostrada; UNKNOWN, evidencia insuficiente. No confundir este uso interno de Outcome con el outcome de inclusión de dos schemas.

`uncertainty` recorre restricciones desconocidas y subárboles comprometidos. No recorre items si el máximo efectivo es cero. Memoriza por identidad para no expandir repetidamente grafos de componentes compartidos. `Pair.equals/hashCode` usa identidad de nodos, evitando coste de igualdad estructural recursiva.

El adaptador corta ciclos y profundidad/budget antes de llegar al dominio; no deben construirse grafos cíclicos artificiales confiando en que estas caches sean un detector general de ciclos. La normalización cacheada y la evaluación cacheada solucionan problemas diferentes. Los oráculos finitos de [tests](tests.md) verifican el fragmento elegido; no prueban toda la semántica JSON Schema.
