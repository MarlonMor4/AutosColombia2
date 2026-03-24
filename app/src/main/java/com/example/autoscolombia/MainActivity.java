// File: MainActivity.java
package com.example.autoscolombia;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    Button btnUsuarios, btnCeldas, btnEntradas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnUsuarios = findViewById(R.id.btnUsuarios);
        btnCeldas = findViewById(R.id.btnCeldas);
        btnEntradas = findViewById(R.id.btnEntradas);

        btnUsuarios.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroUsuarioActivity.class)));

        btnCeldas.setOnClickListener(v ->
                startActivity(new Intent(this, GestionCeldasActivity.class)));

        btnEntradas.setOnClickListener(v ->
                startActivity(new Intent(this, EntradaSalidaActivity.class)));
    }
}