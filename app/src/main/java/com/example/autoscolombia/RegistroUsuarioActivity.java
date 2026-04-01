package com.example.autoscolombia;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Usuario;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RegistroUsuarioActivity extends AppCompatActivity {

    EditText txtNombre, txtPlaca;
    Button btnGuardar, btnVer, btnPagarMensualidad;
    DatabaseReference database, dbPagos;
    ListView lvUsuarios;
    MaterialCardView cardMensualidad;
    TextView txtEstadoMensualidad;
    ArrayAdapter<String> adapter;

    String usuarioSeleccionadoId   = null;
    String nombreSeleccionado      = "";
    String placaSeleccionada       = "";

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro_usuario);

        txtNombre            = findViewById(R.id.txtNombre);
        txtPlaca             = findViewById(R.id.txtPlaca);
        btnGuardar           = findViewById(R.id.btnGuardar);
        btnVer               = findViewById(R.id.btnVer);
        lvUsuarios           = findViewById(R.id.lvUsuarios);
        cardMensualidad      = findViewById(R.id.cardMensualidad);
        txtEstadoMensualidad = findViewById(R.id.txtEstadoMensualidad);
        btnPagarMensualidad  = findViewById(R.id.btnPagarMensualidad);

        database = FirebaseDatabase.getInstance().getReference("usuarios");
        dbPagos  = FirebaseDatabase.getInstance().getReference("pagos");

        btnGuardar.setOnClickListener(v -> guardarOEditarUsuario());
        btnVer    .setOnClickListener(v -> cargarUsuarios());

        // Al pulsar "Pagar Mensualidad" abre GestionPagosActivity
        // con la placa y el nombre del usuario ya pre-cargados
        btnPagarMensualidad.setOnClickListener(v -> {
            Intent intent = new Intent(this, GestionPagosActivity.class);
            intent.putExtra(GestionPagosActivity.EXTRA_PLACA,  placaSeleccionada);
            intent.putExtra(GestionPagosActivity.EXTRA_NOMBRE, nombreSeleccionado);
            startActivity(intent);
        });

        cargarUsuarios();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Al regresar de GestionPagosActivity, refrescar el estado de mensualidad
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onResume() {
        super.onResume();
        if (!placaSeleccionada.isEmpty()) {
            verificarMensualidad(placaSeleccionada);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void guardarOEditarUsuario() {
        String nombre = txtNombre.getText().toString().trim();
        String placa  = txtPlaca.getText().toString().trim().toUpperCase();

        if (nombre.isEmpty() || placa.isEmpty()) {
            Toast.makeText(this, "Complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (usuarioSeleccionadoId == null) {
            String id = database.push().getKey();
            database.child(id).setValue(new Usuario(nombre, placa));
            Toast.makeText(this, "Usuario registrado correctamente", Toast.LENGTH_SHORT).show();
        } else {
            database.child(usuarioSeleccionadoId).child("nombre").setValue(nombre);
            database.child(usuarioSeleccionadoId).child("placa").setValue(placa);
            Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show();
            usuarioSeleccionadoId = null;
        }

        txtNombre.setText("");
        txtPlaca.setText("");
        ocultarCardMensualidad();
        cargarUsuarios();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void cargarUsuarios() {
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> lista = new ArrayList<>();
                final List<String> ids = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Usuario u = data.getValue(Usuario.class);
                    if (u != null && u.nombre != null && u.placa != null) {
                        lista.add(u.nombre + " — " + u.placa);
                        ids.add(data.getKey());
                    }
                }

                adapter = new ArrayAdapter<>(RegistroUsuarioActivity.this,
                        android.R.layout.simple_list_item_1, lista);
                lvUsuarios.setAdapter(adapter);

                // Toque corto → cargar en campos para editar + mostrar estado mensualidad
                lvUsuarios.setOnItemClickListener((parent, view, position, id) -> {
                    usuarioSeleccionadoId = ids.get(position);
                    database.child(usuarioSeleccionadoId)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot snap) {
                                    Usuario u = snap.getValue(Usuario.class);
                                    if (u != null) {
                                        nombreSeleccionado = u.nombre != null ? u.nombre : "";
                                        placaSeleccionada  = u.placa  != null ? u.placa  : "";
                                        txtNombre.setText(nombreSeleccionado);
                                        txtPlaca .setText(placaSeleccionada);
                                        // Mostrar tarjeta de mensualidad
                                        verificarMensualidad(placaSeleccionada);
                                    }
                                }
                                @Override public void onCancelled(DatabaseError error) {}
                            });
                    Toast.makeText(RegistroUsuarioActivity.this,
                            "Usuario seleccionado. Edite y presione Guardar.", Toast.LENGTH_SHORT).show();
                });

                // Toque largo → eliminar
                lvUsuarios.setOnItemLongClickListener((parent, view, position, id) -> {
                    String idUsuario = ids.get(position);
                    database.child(idUsuario).removeValue();
                    Toast.makeText(RegistroUsuarioActivity.this,
                            "Usuario eliminado", Toast.LENGTH_SHORT).show();
                    usuarioSeleccionadoId = null;
                    ocultarCardMensualidad();
                    cargarUsuarios();
                    return true;
                });
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consulta Firebase para saber si la placa tiene mensualidad activa
    // ─────────────────────────────────────────────────────────────────────────
    private void verificarMensualidad(String placa) {
        cardMensualidad.setVisibility(View.VISIBLE);
        txtEstadoMensualidad.setText("Consultando mensualidad...");

        dbPagos.orderByChild("placa").equalTo(placa)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long ahora = System.currentTimeMillis();
                        long mejorVencimiento = 0; // la mensualidad más reciente activa

                        for (DataSnapshot data : snapshot.getChildren()) {
                            Object rawVenc = data.child("fechaVencimiento").getValue();
                            if (rawVenc instanceof Number) {
                                long venc = ((Number) rawVenc).longValue();
                                if (venc > ahora && venc > mejorVencimiento) {
                                    mejorVencimiento = venc;
                                }
                            }
                        }

                        if (mejorVencimiento > 0) {
                            // ✅ Mensualidad activa
                            String fechaVenc = sdf.format(new Date(mejorVencimiento));
                            txtEstadoMensualidad.setText(
                                    "✅ Mensualidad ACTIVA\nVence el: " + fechaVenc +
                                    "\nEste vehículo sale SIN COBRO del parqueadero.");
                            txtEstadoMensualidad.setTextColor(0xFF2E7D32); // verde
                            btnPagarMensualidad.setText("🔄  RENOVAR MENSUALIDAD");
                        } else {
                            // ❌ Sin mensualidad o vencida
                            txtEstadoMensualidad.setText(
                                    "❌ Sin mensualidad activa\n" +
                                    "Este vehículo paga por horas al salir.");
                            txtEstadoMensualidad.setTextColor(0xFFC62828); // rojo
                            btnPagarMensualidad.setText("💳  PAGAR MENSUALIDAD");
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void ocultarCardMensualidad() {
        cardMensualidad.setVisibility(View.GONE);
        placaSeleccionada  = "";
        nombreSeleccionado = "";
    }
}
