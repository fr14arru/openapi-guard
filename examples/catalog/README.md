# Catálogo SaaS sintético

Son ejemplos generados, no contratos de clientes. Representan una API de productos/pedidos con componentes compartidos, parámetros, objetos, arrays anidados y cuerpos obligatorios.

| Comparación desde base.yaml | Resultado esperado en full |
| --- | --- |
| compatible.yaml | COMPLETED/COMPATIBLE, código 0: cantidad de petición hasta 200, enum de respuesta más estrecho y menos productos permitidos |
| breaking.yaml | COMPLETED/BREAKING, código 1: cantidad de petición hasta 10, enum de respuesta ampliado y más productos posibles |
| review.yaml | PARTIAL/INDETERMINATE, código 3: pattern en productId requiere análisis no implementado |

El caso breaking devuelve cuatro hallazgos: expansión de la lista de productos, restricción de CreateOrder.quantity, y expansión de Order.status en los dos usos de respuesta. Un componente compartido se analiza por uso, no se deduplica perdiendo operaciones afectadas.

```powershell
java -jar target/openapi-breaking-change-guard-0.1.0-SNAPSHOT.jar --profile=full --format=text examples/catalog/base.yaml examples/catalog/breaking.yaml
$LASTEXITCODE
pwsh -NoProfile -File scripts/test-ci.ps1
```

Los diez casos del script verifican también compatible/review y preservan códigos e informes JSON UTF-8. La batería finita no prueba todos los contratos OpenAPI ni sustituye la validación comercial.
