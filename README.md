# Post-contenido — Unidad 11: Refactorización Guiada por Pruebas (Code Smells, Paquetes y Acoplamiento)

**Curso:** Patrones de Diseño de Software (Código: 1156701-A) — Unidad 11  
**Semestre:** Sexto Semestre  
**Estudiante:** Brayan Smith Albarracín Tarazona  
**Institución:** Universidad Francisco de Paula Santander (UFPS)  

---

## 1. Descripción del Proyecto

Kata de refactorización guiada por pruebas sobre un servicio de gestión de inventario (`refactoring-u11`) en **Java 17** y **Spring Boot 3.2.5**.
- **Parte 1:** Blindaje mediante **pruebas de caracterización** previas (`InventarioServiceTest`, `ClasificadorProductoTest`, `EtiquetadoProductoTest`) y eliminación de dos *code smells* reales: un *Long Method* en `InventarioService` y un *Switch Statement* duplicado en `ClasificadorProducto` y `EtiquetadoProducto`.
- **Parte 2:** Detección mediante **ArchUnit** (`ArquitecturaPaquetesTest`) de una dependencia cíclica entre los paquetes `inventario` y `reportes`, resolución mediante **Inversión de Dependencias (DIP)** con la interfaz `NotificadorBajoStock`, y verificación con las mismas pruebas intactas de la Parte 1.

---

## 2. Parte 1 — Evidencia de Code Smells y Refactorización

| Code Smell | Evidencia Antes | Técnica Aplicada (Fowler) | Evidencia Después |
| :--- | :--- | :--- | :--- |
| **Long Method** (`InventarioService.actualizarInventario`) | ~45 líneas mezclando 6 responsabilidades: validación, búsqueda lineal, actualización de stock, cálculo de reorden, auditoría y alertas. | **Extract Method** (`esMovimientoValido`, `buscarProducto`, `calcularNuevoStock`, `aplicarActualizacionYRegistrar`, `generarResultadoMovimiento`, `calcularCantidadReorden`). | `actualizarInventario()` tiene exactamente **10 líneas** de orquestación clara; cada método privado tiene una sola responsabilidad. |
| **Switch Statements Duplicados** (`ClasificadorProducto` y `EtiquetadoProducto`) | Mismo `switch (producto.getTipo())` sobre las 4 constantes de `TipoProducto` repetido en dos clases distintas. | **Replace Conditional with Polymorphism** (métodos abstractos `calcularRecargo(precio)` y `generarEtiqueta(nombre)` en el enum `TipoProducto`). | **0 switches y 0 cadenas if-else** en todo el código; ambas clases delegan polimórficamente en una línea conservando su firma pública. |

Las 12 pruebas de caracterización (`6` en `InventarioServiceTest`, `4` en `ClasificadorProductoTest`, `2` en `EtiquetadoProductoTest`) se escribieron **antes** de refactorizar y permanecieron **sin ninguna modificación** tras ambos refactors.

---

## 3. Parte 2 — Diagnóstico y Resolución del Ciclo entre Paquetes

### Diagrama de Paquetes Antes vs. Después

```text
ANTES (Ciclo detectado por ArchUnit: inventario <-> reportes)
+--------------------+          importa ReporteBajoStockService         +--------------------+
|     inventario     | -----------------------------------------------> |      reportes      |
| InventarioService  | <----------------------------------------------- | ReporteBajoStock   |
+--------------------+           importa InventarioService              +--------------------+

DESPUÉS (Dependencia unidireccional mediante DIP: reportes -> inventario)
+---------------------------+                                           +--------------------+
|        inventario         |                                           |      reportes      |
| InventarioService         | <------- implementa / importa ----------- | ReporteBajoStock   |
| -> NotificadorBajoStock   |                                           | (implements        |
+---------------------------+                                           |  Notificador...)   |
                                                                        +--------------------+
```

- **Evidencia con ArchUnit (`ArquitecturaPaquetesTest`):** Antes de invertir la dependencia, `slices().matching("com.universidad.refactoringu11.(*)..").should().beFreeOfCycles()` detectaba el ciclo `inventario -> reportes -> inventario` aunque las pruebas funcionales pasaran. Tras extraer `NotificadorBajoStock` dentro de `inventario`, el paquete `inventario` no importa ninguna clase de `reportes` y la regla de arquitectura queda libre de ciclos.

---

## 4. Decisiones de Diseño (1 a 4)

1. **Decisión 1 — Extracción de cálculo de reorden como método privado vs. clase colaboradora (`CalculadoraReorden`):**  
   Se extrajo `calcularCantidadReorden(Producto, int)` como método privado dentro de `InventarioService` porque es una fórmula aritmética estable de una sola línea (`(stockMinimo * 3) - nuevoStock`) que no requiere colaboradores externos ni posee una razón de cambio independiente. Extraer una clase aparte introduciría sobre-ingeniería innecesaria; si en el futuro la política de reorden dependiera de proveedores o estacionalidad, el método privado ya está aislado y listo para promoverse a una estrategia.
2. **Decisión 2 — Polimorfismo dentro del enum `TipoProducto` vs. jerarquía de clases Strategy externas:**  
   Se implementaron `calcularRecargo(double)` y `generarEtiqueta(String)` como métodos abstractos en el propio enum `TipoProducto`. Dado que el catálogo de tipos (`LIBRO`, `ELECTRONICO`, `ROPA`, `ALIMENTO`) es cerrado, estable y sus reglas solo dependen de atributos del propio producto (sin beans inyectados por Spring), el enum polimórfico elimina ambos `switch` con mínima indirección y obliga en tiempo de compilación a que cualquier nuevo tipo implemente ambos comportamientos.
3. **Decisión 3 — Ubicación de `NotificadorBajoStock` en `inventario` vs. `reportes`:**  
   Siguiendo la regla de dependencias entre capas (Guía de Contenido, sección 4.3: *infraestructura/detalle depende del dominio; el dominio nunca depende de infraestructura*), `inventario` es el paquete de mayor nivel de abstracción (contiene las entidades `Producto`, `TipoProducto` y la lógica central de negocio), mientras `reportes` es un consumidor/detalle secundario. Ubicar `NotificadorBajoStock` en `inventario` garantiza que `inventario` solo dependa de sí mismo y que `reportes` dependa en un único sentido de `inventario`.
4. **Decisión 4 — Granularidad de `notificar(Producto, int cantidadReorden)` por evento vs. por lote (`List<Producto>`):**  
   `InventarioService.actualizarInventario()` procesa un movimiento individual por llamada; notificar por evento individual (`notificar(producto, cantidadReorden)`) permite capturar exactamente qué producto cayó bajo el mínimo y con qué cantidad calculada de reorden en el instante en que ocurre el movimiento, sin acoplar `ReporteBajoStockService` a reconstruir diferencias de estado. La alternativa por lote (`notificar(List<Producto>)`) sería preferible en procesos nocturnos (*batch*) que consolidan miles de movimientos de una sola vez para reducir llamadas de red o escrituras.

---

## 5. Cómo Ejecutar

```bash
# Compilar
mvn compile

# Ejecutar toda la suite (caracterización + arquitectura ArchUnit)
mvn test
```

---

## 6. Herramientas Utilizadas

- Java 17, Spring Boot 3.2.5
- JUnit 5, ArchUnit 1.3.0 (`archunit-junit5`)
- Apache Maven, Git, GitHub