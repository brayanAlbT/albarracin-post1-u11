package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReporteBajoStockService {

    private final InventarioService inventarioService;

    public ReporteBajoStockService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    public String generarReporte() {
        List<Producto> bajos = inventarioService.obtenerProductosBajoStock();
        if (bajos.isEmpty()) {
            return "Sin productos bajo stock minimo.";
        }
        StringBuilder sb = new StringBuilder("=== REPORTE BAJO STOCK ===\n");
        for (Producto p : bajos) {
            sb.append(String.format("- %s (stock: %d, minimo: %d)%n",
                    p.getNombre(), p.getStock(), p.getStockMinimo()));
        }
        return sb.toString();
    }
}