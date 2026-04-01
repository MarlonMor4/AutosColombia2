package com.example.autoscolombia;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Movimiento;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.*;

public class EntradaSalidaActivity extends AppCompatActivity {

    private EditText txtPlaca;
    private Button btnEntrada, btnSalida, btnConfirmarPago;
    private TextView txtDetallePago;
    private MaterialCardView cardResultadoPago;
    private DatabaseReference dbCeldas, dbMovimientos, dbPagos;

    private String idCeldaEncontrada = "";
    private String placaActual       = "";

    // true = el vehículo tiene mensualidad activa → salida gratuita
    private boolean tienesMensualidad = false;

    private static final int TARIFA_POR_HORA = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrada_salida);

        txtPlaca          = findViewById(R.id.txtPlaca);
        btnEntrada        = findViewById(R.id.btnEntrada);
        btnSalida         = findViewById(R.id.btnSalida);
        btnConfirmarPago  = findViewById(R.id.btnConfirmarPago);
        txtDetallePago    = findViewById(R.id.txtDetallePago);
        cardResultadoPago = findViewById(R.id.cardResultadoPago);

        dbCeldas      = FirebaseDatabase.getInstance().getReference("celdas");
        dbMovimientos = FirebaseDatabase.getInstance().getReference("movimientos");
        dbPagos       = FirebaseDatabase.getInstance().getReference("pagos");

        btnEntrada      .setOnClickListener(v -> registrarEntrada());
        btnSalida       .setOnClickListener(v -> calcularCobro());
        btnConfirmarPago.setOnClickListener(v -> procesarSalidaFinal());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRAR ENTRADA
    // ─────────────────────────────────────────────────────────────────────────
    private void registrarEntrada() {
        String placa = txtPlaca.getText().toString().trim().toUpperCase();
        if (placa.isEmpty()) {
            mostrarToast("Por favor ingrese una placa");
            return;
        }

        // Verificar que la placa no esté ya dentro
        dbCeldas.orderByChild("placa").equalTo(placa)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            mostrarToast("La placa " + placa + " ya está en el parqueadero");
                            return;
                        }
                        asignarCelda(placa);
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    private void asignarCelda(String placa) {
        dbCeldas.orderByChild("estado").equalTo("LIBRE").limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mostrarToast("¡No hay celdas disponibles!");
                            return;
                        }
                        for (DataSnapshot data : snapshot.getChildren()) {
                            data.getRef().child("estado").setValue("OCUPADA");
                            data.getRef().child("placa").setValue(placa);

                            String idMov = dbMovimientos.push().getKey();
                            dbMovimientos.child(idMov).setValue(
                                    new Movimiento(idMov, placa, "ENTRADA", System.currentTimeMillis()));

                            mostrarToast("Entrada registrada: " + placa);
                            txtPlaca.setText("");
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CALCULAR COBRO
    // Lógica: primero verifica mensualidad; si está activa → gratis; si no → por horas
    // ─────────────────────────────────────────────────────────────────────────
    private void calcularCobro() {
        placaActual = txtPlaca.getText().toString().trim().toUpperCase();
        if (placaActual.isEmpty()) {
            mostrarToast("Ingrese una placa para calcular");
            return;
        }

        // 1️⃣ Encontrar la celda del vehículo
        dbCeldas.orderByChild("placa").equalTo(placaActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            mostrarToast("El vehículo no se encuentra en el parqueadero");
                            cardResultadoPago.setVisibility(View.GONE);
                            return;
                        }
                        for (DataSnapshot dataCelda : snapshot.getChildren()) {
                            idCeldaEncontrada = dataCelda.getKey();
                            // 2️⃣ Verificar mensualidad ANTES de calcular horas
                            verificarMensualidadYCobrar();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    /**
     * Consulta Firebase "pagos" para la placa actual.
     * Si encuentra una mensualidad vigente → muestra salida gratuita.
     * Si no → calcula cobro por horas normalmente.
     */
    private void verificarMensualidadYCobrar() {
        dbPagos.orderByChild("placa").equalTo(placaActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        long ahora          = System.currentTimeMillis();
                        long mejorVencimiento = 0;

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
                            // ✅ Tiene mensualidad activa → salida GRATIS
                            tienesMensualidad = true;
                            mostrarSalidaGratuita(mejorVencimiento);
                        } else {
                            // ❌ No tiene mensualidad → cobrar por horas
                            tienesMensualidad = false;
                            buscarTiempoEntrada();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        // En caso de error de red, continuar con cobro por horas
                        tienesMensualidad = false;
                        buscarTiempoEntrada();
                    }
                });
    }

    /** Muestra la tarjeta con mensaje de mensualidad activa (sin cobro). */
    private void mostrarSalidaGratuita(long fechaVencimiento) {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String fechaVenc = sdf.format(new java.util.Date(fechaVencimiento));

        cardResultadoPago.setVisibility(View.VISIBLE);
        txtDetallePago.setText(
                "Vehículo: " + placaActual +
                "\n\n✅ MENSUALIDAD ACTIVA" +
                "\nVence el: " + fechaVenc +
                "\n\n🎉 SALIDA SIN COBRO\n(Mensualidad al día)");
        btnConfirmarPago.setText("CONFIRMAR SALIDA (GRATIS)");
        btnConfirmarPago.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF6A1B9A));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Busca el tiempo de entrada para el cálculo por horas
    // ─────────────────────────────────────────────────────────────────────────
    private void buscarTiempoEntrada() {
        dbMovimientos.orderByChild("placa").equalTo(placaActual)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshotMov) {
                        long tiempoEntrada = 0;
                        for (DataSnapshot m : snapshotMov.getChildren()) {
                            String tipo = m.child("tipo").getValue(String.class);
                            if ("ENTRADA".equals(tipo)) {
                                Object rawFecha = m.child("fecha").getValue();
                                if (rawFecha instanceof Number) {
                                    long ts = ((Number) rawFecha).longValue();
                                    if (ts > tiempoEntrada) tiempoEntrada = ts;
                                }
                            }
                        }

                        if (tiempoEntrada == 0) {
                            mostrarToast("Error: Registro de entrada no encontrado");
                            return;
                        }

                        long tiempoTotalMillis = System.currentTimeMillis() - tiempoEntrada;
                        double horasDecimal    = tiempoTotalMillis / (1000.0 * 60 * 60);
                        int horasACobrar       = (int) Math.ceil(horasDecimal);
                        if (horasACobrar == 0) horasACobrar = 1;

                        int total = horasACobrar * TARIFA_POR_HORA;

                        cardResultadoPago.setVisibility(View.VISIBLE);
                        txtDetallePago.setText(
                                "Vehículo: " + placaActual +
                                "\nHoras de estancia: " + String.format("%.2f", horasDecimal) +
                                "\nHoras a cobrar: " + horasACobrar +
                                "\n\n💰 TOTAL A PAGAR: $" + String.format("%,d", total));
                        btnConfirmarPago.setText("CONFIRMAR PAGO Y SALIDA");
                        btnConfirmarPago.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(0xFF34A853));
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRMAR SALIDA (gratuita o con pago)
    // ─────────────────────────────────────────────────────────────────────────
    private void procesarSalidaFinal() {
        if (idCeldaEncontrada.isEmpty()) {
            mostrarToast("Primero calcule el cobro");
            return;
        }

        // Liberar celda
        dbCeldas.child(idCeldaEncontrada).child("estado").setValue("LIBRE");
        dbCeldas.child(idCeldaEncontrada).child("placa").setValue("");

        // Registrar movimiento de SALIDA
        String idSalida = dbMovimientos.push().getKey();
        dbMovimientos.child(idSalida).setValue(
                new Movimiento(idSalida, placaActual, "SALIDA", System.currentTimeMillis()));

        // Mensaje diferenciado según tipo de salida
        String msg = tienesMensualidad
                ? "✅ Salida registrada. Mensualidad activa — sin cobro."
                : "✅ Pago confirmado. Celda liberada.";
        mostrarToast(msg);

        // Limpiar estado
        cardResultadoPago.setVisibility(View.GONE);
        txtPlaca.setText("");
        btnConfirmarPago.setText("CONFIRMAR PAGO Y SALIDA");
        btnConfirmarPago.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF34A853));
        idCeldaEncontrada = "";
        placaActual       = "";
        tienesMensualidad = false;
    }

    private void mostrarToast(String mensaje) {
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
