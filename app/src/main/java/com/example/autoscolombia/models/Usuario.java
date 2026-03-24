package com.example.autoscolombia.models;

public class Usuario {

    // Campos obligatorios
    public String nombre;   // Nombre del usuario
    public String placa;    // Placa del vehículo asociado

    // Constructor vacío requerido por Firebase
    public Usuario() {}

    // Constructor con parámetros
    public Usuario(String nombre, String placa) {
        this.nombre = nombre;
        this.placa = placa;
    }
}