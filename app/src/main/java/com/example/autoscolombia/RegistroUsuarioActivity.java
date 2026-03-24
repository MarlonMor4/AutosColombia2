package com.example.autoscolombia;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.autoscolombia.models.Usuario;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class RegistroUsuarioActivity extends AppCompatActivity {

    EditText txtNombre, txtPlaca;
    Button btnGuardar, btnVer;
    DatabaseReference database;
    ListView lvUsuarios; // Para mostrar usuarios y permitir selección
    ArrayAdapter<String> adapter;
    String usuarioSeleccionadoId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuario);

        txtNombre = findViewById(R.id.txtNombre);
        txtPlaca = findViewById(R.id.txtPlaca);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnVer = findViewById(R.id.btnVer);
        lvUsuarios = findViewById(R.id.lvUsuarios); // Nuevo ListView en layout

        database = FirebaseDatabase.getInstance().getReference("usuarios");

        btnGuardar.setOnClickListener(v -> {
            String nombre = txtNombre.getText().toString().trim();
            String placa = txtPlaca.getText().toString().trim();

            if(nombre.isEmpty() || placa.isEmpty()){
                Toast.makeText(this,"Complete los campos",Toast.LENGTH_SHORT).show();
                return;
            }

            if(usuarioSeleccionadoId == null){
                // Registrar nuevo usuario
                String id = database.push().getKey();
                database.child(id).setValue(new Usuario(nombre, placa));
                Toast.makeText(this,"Usuario registrado",Toast.LENGTH_SHORT).show();
            } else {
                // Editar usuario existente
                database.child(usuarioSeleccionadoId).child("nombre").setValue(nombre);
                database.child(usuarioSeleccionadoId).child("placa").setValue(placa);
                Toast.makeText(this,"Usuario actualizado",Toast.LENGTH_SHORT).show();
                usuarioSeleccionadoId = null;
            }

            txtNombre.setText("");
            txtPlaca.setText("");
        });

        btnVer.setOnClickListener(v -> cargarUsuarios());
    }

    private void cargarUsuarios(){
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> lista = new ArrayList<>();
                final List<String> ids = new ArrayList<>();
                for(DataSnapshot data: snapshot.getChildren()){
                    Usuario u = data.getValue(Usuario.class);
                    lista.add(u.nombre + " - " + u.placa);
                    ids.add(data.getKey());
                }
                adapter = new ArrayAdapter<>(RegistroUsuarioActivity.this,
                        android.R.layout.simple_list_item_1, lista);
                lvUsuarios.setAdapter(adapter);

                lvUsuarios.setOnItemClickListener((parent, view, position, id) -> {
                    usuarioSeleccionadoId = ids.get(position);
                    database.child(usuarioSeleccionadoId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snap) {
                            Usuario u = snap.getValue(Usuario.class);
                            txtNombre.setText(u.nombre);
                            txtPlaca.setText(u.placa);
                        }
                        @Override public void onCancelled(DatabaseError error) {}
                    });
                });

                lvUsuarios.setOnItemLongClickListener((parent, view, position, id) -> {
                    String idUsuario = ids.get(position);
                    database.child(idUsuario).removeValue();
                    Toast.makeText(RegistroUsuarioActivity.this,"Usuario eliminado",Toast.LENGTH_SHORT).show();
                    cargarUsuarios();
                    return true;
                });
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
}