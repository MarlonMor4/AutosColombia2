package com.example.autoscolombia;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.autoscolombia.models.Pago;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Iteración 3 – Gestión de Pagos (Mensualidades)
 *
 * Puede abrirse de dos formas:
 *  1. Desde el Menú principal → campos vacíos, modo libre
 *  2. Desde Gestión de Usuarios → placa y nombre pre-cargados del usuario seleccionado
 *
 * Funcionalidades:
 *  - Registrar pago mensual (placa, nombre, monto, método)
 *  - Ver todos los pagos con estado ACTIVO / VENCIDO
 *  - Buscar pagos por placa
 *  - Resumen estadístico (total recaudado, activos, vencidos)
 */
public class GestionPagosActivity extends AppCompatActivity {

    // Claves para Intent extras (usadas desde RegistroUsuarioActivity)
    public static final String EXTRA_PLACA  = "extra_placa";
    public static final String EXTRA_NOMBRE = "extra_nombre";

    // ── UI ────────────────────────────────────────────────────────────────────
    private EditText etPlacaPago, etNombreCliente, etMonto, etBuscarPlaca;
    private Spinner  spinnerMetodo;
    private Button   btnRegistrarPago, btnBuscarPlaca, btnVerTodos;
    private ListView lvPagos;
    private com.google.android.material.card.MaterialCardView cardResumen;
    private TextView txtResumen;

    // ── Firebase ──────────────────────────────────────────────────────────────
    private DatabaseReference dbPagos;

    // ── Datos internos ────────────────────────────────────────────────────────
    private final List<Pago>   listaPagos   = new ArrayList<>();
    private final List<String> listaDisplay = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private static final long TREINTA_DIAS_MS = 30L * 24 * 60 * 60 * 1000;

    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion_pagos);

        // Referencias UI
        etPlacaPago      = findViewById(R.id.etPlacaPago);
        etNombreCliente  = findViewById(R.id.etNombreCliente);
        etMonto          = findViewById(R.id.etMonto);
        etBuscarPlaca    = findViewById(R.id.etBuscarPlaca);
        spinnerMetodo    = findViewById(R.id.spinnerMetodo);
        btnRegistrarPago = findViewById(R.id.btnRegistrarPago);
        btnBuscarPlaca   = findViewById(R.id.btnBuscarPlaca);
        btnVerTodos      = findViewById(R.id.btnVerTodos);
        lvPagos          = findViewById(R.id.lvPagos);
        cardResumen      = findViewById(R.id.cardResumen);
        txtResumen       = findViewById(R.id.txtResumen);

        // Firebase
        dbPagos = FirebaseDatabase.getInstance().getReference("pagos");

        // Spinner métodos de pago
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"EFECTIVO", "TRANSFERENCIA"});
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMetodo.setAdapter(spinnerAdapter);

        // ListView adapter
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaDisplay);
        lvPagos.setAdapter(adapter);

        // ── Pre-cargar datos si viene desde Gestión de Usuarios ──────────────
        String placaExtra  = getIntent().getStringExtra(EXTRA_PLACA);
        String nombreExtra = getIntent().getStringExtra(EXTRA_NOMBRE);
        if (placaExtra != null && !placaExtra.isEmpty()) {
            etPlacaPago.setText(placaExtra);
            etPlacaPago.setEnabled(false); // la placa no se puede cambiar en este flujo
        }
        if (nombreExtra != null && !nombreExtra.isEmpty()) {
            etNombreCliente.setText(nombreExtra);
            etNombreCliente.setEnabled(false);
        }
        // Si vino con datos, buscar sus pagos anteriores automáticamente
        if (placaExtra != null && !placaExtra.isEmpty()) {
            etBuscarPlaca.setText(placaExtra);
            buscarPorPlaca(placaExtra);
        }

        // Listeners
        btnRegistrarPago.setOnClickListener(v -> registrarPago());
        btnBuscarPlaca  .setOnClickListener(v -> {
            String p = etBuscarPlaca.getText().toString().trim().toUpperCase();
            if (!p.isEmpty()) buscarPorPlaca(p);
            else toast("Ingrese una placa para buscar");
        });
        btnVerTodos.setOnClickListener(v -> cargarTodosPagos());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REGISTRAR PAGO
    // ─────────────────────────────────────────────────────────────────────────
    private void registrarPago() {
        String placa    = etPlacaPago.getText().toString().trim().toUpperCase();
        String nombre   = etNombreCliente.getText().toString().trim();
        String montoStr = etMonto.getText().toString().trim();
        String metodo   = spinnerMetodo.getSelectedItem().toString();

        if (placa.isEmpty())  { toast("Ingrese la placa del vehículo"); return; }
        if (nombre.isEmpty()) { toast("Ingrese el nombre del cliente");  return; }
        if (montoStr.isEmpty()){ toast("Ingrese el monto a pagar");      return; }

        double monto;
        try {
            monto = Double.parseDouble(montoStr);
        } catch (NumberFormatException e) {
            toast("El monto ingresado no es válido");
            return;
        }
        if (monto <= 0) { toast("El monto debe ser mayor a cero"); return; }

        long ahora       = System.currentTimeMillis();
        long vencimiento = ahora + TREINTA_DIAS_MS;
        String id        = dbPagos.push().getKey();

        Pago pago = new Pago(id, placa, nombre, monto, ahora, vencimiento, metodo);

        dbPagos.child(id).setValue(pago)
                .addOnSuccessListener(unused -> {
                    toast("✅ Mensualidad registrada. Vence: " +
                            sdf.format(new Date(vencimiento)));
                    // Limpiar sólo monto (placa/nombre pueden estar fijos por Intent)
                    etMonto.setText("150000");
                    buscarPorPlaca(placa); // refrescar historial de la misma placa
                })
                .addOnFailureListener(e -> toast("Error al guardar: " + e.getMessage()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUSCAR PAGOS POR PLACA
    // ─────────────────────────────────────────────────────────────────────────
    private void buscarPorPlaca(String placa) {
        dbPagos.orderByChild("placa").equalTo(placa)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        listaPagos.clear();
                        listaDisplay.clear();

                        if (!snapshot.exists()) {
                            toast("No hay pagos registrados para: " + placa);
                            cardResumen.setVisibility(View.GONE);
                            adapter.notifyDataSetChanged();
                            return;
                        }
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Pago p = data.getValue(Pago.class);
                            if (p != null) {
                                listaPagos.add(p);
                                listaDisplay.add(formatearPago(p));
                            }
                        }
                        mostrarResumen();
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(DatabaseError error) {
                        toast("Error al buscar: " + error.getMessage());
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARGAR TODOS LOS PAGOS
    // ─────────────────────────────────────────────────────────────────────────
    private void cargarTodosPagos() {
        dbPagos.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaPagos.clear();
                listaDisplay.clear();

                if (!snapshot.exists()) {
                    toast("No hay pagos registrados aún");
                    cardResumen.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                    return;
                }
                for (DataSnapshot data : snapshot.getChildren()) {
                    Pago p = data.getValue(Pago.class);
                    if (p != null) {
                        listaPagos.add(p);
                        listaDisplay.add(formatearPago(p));
                    }
                }
                mostrarResumen();
                adapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError error) {
                toast("Error al cargar pagos: " + error.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    private String formatearPago(Pago p) {
        String estado    = p.isActivo() ? "✅ ACTIVO" : "❌ VENCIDO";
        String fechaPago = sdf.format(new Date(p.fechaPago));
        String fechaVenc = sdf.format(new Date(p.fechaVencimiento));
        String monto     = String.format(Locale.getDefault(), "$%,.0f", p.monto);
        return estado + " | " + p.placa + " – " + p.nombre +
               "\nPago: " + fechaPago + "  Vence: " + fechaVenc +
               "  " + monto + " (" + p.metodoPago + ")";
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void mostrarResumen() {
        int activos = 0, vencidos = 0;
        double totalRecaudado = 0;
        for (Pago p : listaPagos) {
            totalRecaudado += p.monto;
            if (p.isActivo()) activos++; else vencidos++;
        }
        txtResumen.setText("Total registros: " + listaPagos.size() +
                "\n✅ Activos: " + activos +
                "   ❌ Vencidos: " + vencidos +
                "\n💰 Total recaudado: $" +
                String.format(Locale.getDefault(), "%,.0f", totalRecaudado));
        cardResumen.setVisibility(View.VISIBLE);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
