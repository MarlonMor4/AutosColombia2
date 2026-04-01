package com.example.autoscolombia.models;

/**
 * Modelo de pago mensual del parqueadero Autos Colombia.
 * Representa una mensualidad registrada para un vehículo/cliente.
 */
public class Pago {

    public String id;              // ID único generado por Firebase
    public String placa;           // Placa del vehículo
    public String nombre;          // Nombre del cliente
    public double monto;           // Monto pagado (en pesos colombianos)
    public long fechaPago;         // Timestamp de cuando se realizó el pago
    public long fechaVencimiento;  // Timestamp de vencimiento (30 días después del pago)
    public String metodoPago;      // "EFECTIVO" o "TRANSFERENCIA"

    // Constructor vacío requerido por Firebase
    public Pago() {}

    // Constructor completo
    public Pago(String id, String placa, String nombre, double monto,
                long fechaPago, long fechaVencimiento, String metodoPago) {
        this.id = id;
        this.placa = placa;
        this.nombre = nombre;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.fechaVencimiento = fechaVencimiento;
        this.metodoPago = metodoPago;
    }

    /**
     * Indica si la mensualidad está activa (no vencida).
     */
    public boolean isActivo() {
        return System.currentTimeMillis() <= fechaVencimiento;
    }
}
