// File: models/Celda.java
package com.example.autoscolombia.models;

public class Celda {
    public String id;
    public String numero;
    public String estado; // LIBRE / OCUPADA
    public String placa;  // qué vehículo ocupa

    public Celda() {}

    public Celda(String id, String numero, String estado, String placa) {
        this.id = id;
        this.numero = numero;
        this.estado = estado;
        this.placa = placa;
    }
}