package com.example.autoscolombia.models;


public class Movimiento {
    public String id;
    public String placa;
    public String tipo; // ENTRADA o SALIDA
    public long fecha;  // Guardado en milisegundos (System.currentTimeMillis())

    public Movimiento() {}

    public Movimiento(String id, String placa, String tipo, long fecha) {
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.fecha = fecha;
    }
}