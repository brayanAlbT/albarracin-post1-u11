# Refactoring U11 — Parte 1: Red de Seguridad y Refactorización Guiada por Evidencia

## Evidencia de Code Smells (Parte 1)
1. **Long Method en `InventarioService.actualizarInventario()`:** Superaba las 35 líneas mezclando validación de entrada, búsqueda lineal de producto, cálculo/actualización de stock, cálculo de cantidad de reorden, registro de movimiento y emisión de alertas. Tras aplicar **Extract Method**, `actualizarInventario()` se redujo a 10 líneas de orquestación clara.
2. **Switch Statements duplicados en `ClasificadorProducto` y `EtiquetadoProducto`:** Ambas clases repetían exactamente el mismo `switch (producto.getTipo())` sobre los 4 valores de `TipoProducto`. Se aplicó **Replace Conditional with Polymorphism** mediante métodos abstractos en el enum `TipoProducto`, eliminando por completo todo `switch` e `if/else` de ambas clases.

## Decisiones de diseño (Parte 1)
- **Decisión 1 (Cálculo de reorden):** Se extrajo `calcularCantidadReorden` como método privado dentro de `InventarioService` en lugar de una clase `CalculadoraReorden` independiente, dado que es una fórmula aritmética simple (`stockMinimo * 3 - nuevoStock`) sin colaboradores externos ni razón de cambio separada.
- **Decisión 2 (Polimorfismo en `TipoProducto`):** Se eligió encapsular ambos comportamientos como métodos abstractos en el enum `TipoProducto` en vez de crear una jerarquía de clases Strategy externas, porque el conjunto de categorías es cerrado, estable y no requiere inyección de dependencias de Spring.