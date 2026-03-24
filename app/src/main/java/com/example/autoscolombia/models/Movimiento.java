package com.example.autoscolombia.models;


public class Movimiento {
    public String id;
    public String placa;
    public String tipo;
    public long timestamp;

    public Movimiento() {}

    public Movimiento(String id, String placa, String tipo, long timestamp) {
        this.id = id;
        this.placa = placa;
        this.tipo = tipo;
        this.timestamp = timestamp;
    }
}