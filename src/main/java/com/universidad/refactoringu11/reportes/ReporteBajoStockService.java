package com.universidad.refactoringu11.reportes;

import com.universidad.refactoringu11.inventario.InventarioService;
import com.universidad.refactoringu11.inventario.NotificadorBajoStock;
import com.universidad.refactoringu11.inventario.Producto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ReporteBajoStockService implements NotificadorBajoStock {

    private final InventarioService inventarioService;
    private final List<String> alertasRegistradas = new ArrayList<>();

    public ReporteBajoStockService(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @Override
    public void notificar(Producto producto, int cantidadReorden) {
        String alerta = String.format("ALERTA [%s]: stock=%d, reordenar=%d",
                producto.getNombre(), producto.getStock(), cantidadReorden);
        alertasRegistradas.add(alerta);
    }

    public List<String> getAlertasRegistradas() {
        return Collections.unmodifiableList(alertasRegistradas);
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