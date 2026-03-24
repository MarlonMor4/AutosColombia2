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

        // Inicializar celdas si no existen
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if(!snapshot.exists()){
                    for(int i=1;i<=maxCeldas;i++){
                        String id = db.push().getKey();
                        db.child(id).setValue(new Celda(id,"C"+i,"LIBRE",""));
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });

        // Mostrar celdas y actualizar en tiempo real
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaCeldas.clear();
                List<String> display = new ArrayList<>();
                for(DataSnapshot data:snapshot.getChildren()){
                    Celda celda = data.getValue(Celda.class);
                    if(celda != null){
                        listaCeldas.add(celda);
                        String texto = celda.numero + " - " + celda.estado;
                        if(!celda.placa.isEmpty()) texto += " (" + celda.placa + ")";
                        display.add(texto);
                    }
                }
                adapter = new ArrayAdapter<>(GestionCeldasActivity.this,
                        android.R.layout.simple_list_item_1, display);
                lvCeldas.setAdapter(adapter);
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}