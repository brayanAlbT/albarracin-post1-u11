package com.universidad.refactoringu11.inventario;

import com.universidad.refactoringu11.reportes.ReporteBajoStockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class InventarioService {

    private final List<Producto> productos = new ArrayList<>();
    private final List<String> movimientos = new ArrayList<>();
    private ReporteBajoStockService reporteBajoStockService;

    @Autowired(required = false)
    public void setReporteBajoStockService(ReporteBajoStockService reporteBajoStockService) {
        this.reporteBajoStockService = reporteBajoStockService;
    }

    public void agregarProducto(Producto producto) {
        productos.add(producto);
    }

    public List<Producto> obtenerProductosBajoStock() {
        return productos.stream()
                .filter(p -> p.getStock() < p.getStockMinimo())
                .toList();
    }

    public List<String> getMovimientos() {
        return Collections.unmodifiableList(movimientos);
    }

    public String actualizarInventario(Long productoId, int cantidad, String tipoMovimiento) {
        if (!esMovimientoValido(productoId, cantidad, tipoMovimiento)) {
            return "ERROR_DATOS_INVALIDOS";
        }
        Producto producto = buscarProducto(productoId);
        if (producto == null) {
            return "ERROR_PRODUCTO_NO_EXISTE";
        }
        int nuevoStock = calcularNuevoStock(producto, cantidad, tipoMovimiento);
        if (nuevoStock < 0) {
            return "ERROR_STOCK_INSUFICIENTE";
        }
        aplicarActualizacionYRegistrar(producto, cantidad, tipoMovimiento, nuevoStock);
        return generarResultadoMovimiento(producto, nuevoStock);
    }

    private boolean esMovimientoValido(Long productoId, int cantidad, String tipoMovimiento) {
        return productoId != null && cantidad != 0
                && ("ENTRADA".equals(tipoMovimiento) || "SALIDA".equals(tipoMovimiento));
    }

    private Producto buscarProducto(Long productoId) {
        return productos.stream()
                .filter(p -> p.getId().equals(productoId))
                .findFirst()
                .orElse(null);
    }

    private int calcularNuevoStock(Producto producto, int cantidad, String tipoMovimiento) {
        return "ENTRADA".equals(tipoMovimiento)
                ? producto.getStock() + cantidad
                : producto.getStock() - cantidad;
    }

    private void aplicarActualizacionYRegistrar(Producto producto, int cantidad,
                                                String tipoMovimiento, int nuevoStock) {
        producto.setStock(nuevoStock);
        String registro = tipoMovimiento + " de " + cantidad + " unidades de "
                + producto.getNombre() + " -> stock final: " + nuevoStock;
        movimientos.add(registro);
        System.out.println(registro);
    }

    private String generarResultadoMovimiento(Producto producto, int nuevoStock) {
        if (nuevoStock < producto.getStockMinimo()) {
            int cantidadReorden = calcularCantidadReorden(producto, nuevoStock);
            if (reporteBajoStockService != null) {
                System.out.println(reporteBajoStockService.generarReporte());
            }
            System.out.println("ALERTA: " + producto.getNombre()
                    + " bajo stock minimo. Reordenar " + cantidadReorden + " unidades.");
            return "OK_BAJO_STOCK_REORDEN_" + cantidadReorden;
        }
        return "OK_" + nuevoStock;
    }

    private int calcularCantidadReorden(Producto producto, int nuevoStock) {
        return (producto.getStockMinimo() * 3) - nuevoStock;
    }
}