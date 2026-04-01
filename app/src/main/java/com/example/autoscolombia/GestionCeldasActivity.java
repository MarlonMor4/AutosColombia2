package com.example.autoscolombia;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Celda;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class GestionCeldasActivity extends AppCompatActivity {

    ListView lvCeldas;
    DatabaseReference db;
    List<Celda> listaCeldas = new ArrayList<>();
    ArrayAdapter<String> adapter;
    int maxCeldas = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_celdas);

        lvCeldas = findViewById(R.id.lvCeldas);
        db = FirebaseDatabase.getInstance().getReference("celdas");

        // Inicializar celdas si aún no existen en Firebase
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    for (int i = 1; i <= maxCeldas; i++) {
                        String id = db.push().getKey();
                        db.child(id).setValue(new Celda(id, "C" + i, "LIBRE", ""));
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        // Escuchar cambios en tiempo real
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaCeldas.clear();
                List<String> display = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Celda celda = data.getValue(Celda.class);
                    if (celda == null) continue;

                    // BUG FIX: celda.placa puede ser null; se usa texto seguro
                    String placaInfo = (celda.placa != null && !celda.placa.isEmpty())
                            ? " (" + celda.placa + ")" : "";

                    String icono = "OCUPADA".equals(celda.estado) ? "🔴" : "🟢";
                    String texto = icono + " " + celda.numero + " — " + celda.estado + placaInfo;

                    listaCeldas.add(celda);
                    display.add(texto);
                }

                adapter = new ArrayAdapter<>(GestionCeldasActivity.this,
                        android.R.layout.simple_list_item_1, display);
                lvCeldas.setAdapter(adapter);
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}
