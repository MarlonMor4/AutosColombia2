package com.example.autoscolombia;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Celda;
import com.example.autoscolombia.models.Movimiento;
import com.google.firebase.database.*;

public class EntradaSalidaActivity extends AppCompatActivity {

    EditText txtPlaca;
    Button btnEntrada, btnSalida;
    DatabaseReference dbCeldas, dbMovimientos;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrada_salida);

        txtPlaca = findViewById(R.id.txtPlaca);
        btnEntrada = findViewById(R.id.btnEntrada);
        btnSalida = findViewById(R.id.btnSalida);

        dbCeldas = FirebaseDatabase.getInstance().getReference("celdas");
        dbMovimientos = FirebaseDatabase.getInstance().getReference("movimientos");

        btnEntrada.setOnClickListener(v -> registrarEntrada());
        btnSalida.setOnClickListener(v -> registrarSalida());
    }

    private void registrarEntrada() {
        String placa = txtPlaca.getText().toString().trim();
        if(placa.isEmpty()){ Toast.makeText(this,"Ingrese placa",Toast.LENGTH_SHORT).show(); return; }

        dbCeldas.orderByChild("estado").equalTo("LIBRE")
                .limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        if(!snapshot.exists()){
                            Toast.makeText(EntradaSalidaActivity.this,"No hay celdas libres",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for(DataSnapshot data:snapshot.getChildren()){
                            // Actualizar la celda
                            data.getRef().child("estado").setValue("OCUPADA");
                            data.getRef().child("placa").setValue(placa);

                            // Registrar movimiento
                            String id = dbMovimientos.push().getKey();
                            dbMovimientos.child(id)
                                    .setValue(new Movimiento(id, placa,"ENTRADA",System.currentTimeMillis()));

                            Toast.makeText(EntradaSalidaActivity.this,"Entrada registrada en " + data.getValue(Celda.class).numero,Toast.LENGTH_SHORT).show();
                            txtPlaca.setText("");
                            return;
                        }
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void registrarSalida() {
        String placa = txtPlaca.getText().toString().trim();
        if(placa.isEmpty()){ Toast.makeText(this,"Ingrese placa",Toast.LENGTH_SHORT).show(); return; }

        dbCeldas.orderByChild("placa").equalTo(placa)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(DataSnapshot snapshot) {
                        if(!snapshot.exists()){
                            Toast.makeText(EntradaSalidaActivity.this,"Vehículo no encontrado",Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for(DataSnapshot data:snapshot.getChildren()){
                            // Liberar celda
                            data.getRef().child("estado").setValue("LIBRE");
                            data.getRef().child("placa").setValue("");

                            // Registrar movimiento
                            String id = dbMovimientos.push().getKey();
                            dbMovimientos.child(id)
                                    .setValue(new Movimiento(id, placa,"SALIDA",System.currentTimeMillis()));

                            Toast.makeText(EntradaSalidaActivity.this,"Salida registrada de " + data.getValue(Celda.class).numero,Toast.LENGTH_SHORT).show();
                            txtPlaca.setText("");
                            return;
                        }
                    }

                    @Override public void onCancelled(DatabaseError error) {}
                });
    }
}