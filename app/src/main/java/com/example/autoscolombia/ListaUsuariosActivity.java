// File: ListaUsuariosActivity.java
package com.example.autoscolombia;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Usuario;
import com.google.firebase.database.*;

public class ListaUsuariosActivity extends AppCompatActivity {

    TextView txtLista;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_usuarios);

        txtLista = findViewById(R.id.txtLista);
        database = FirebaseDatabase.getInstance().getReference("usuarios");

        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String datos = "";
                for (DataSnapshot data : snapshot.getChildren()) {
                    Usuario u = data.getValue(Usuario.class);
                    if(u != null)
                        datos += u.nombre + " - " + u.placa + "\n";
                }
                txtLista.setText(datos);
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }
}