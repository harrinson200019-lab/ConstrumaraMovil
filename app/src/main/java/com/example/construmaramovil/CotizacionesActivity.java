package com.example.construmaramovil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CotizacionesActivity extends AppCompatActivity {

    private Spinner spProyectos;
    private EditText etFechaCotizacion;
    private EditText etDescripcionCotizacion;
    private EditText etTotalCotizacion;
    private EditText etEstadoCotizacion;
    private TextView tvResultadoCotizaciones;

    private ArrayList<Proyecto> listaProyectos = new ArrayList<>();

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    // Clase para almacenar los proyectos.
    private static class Proyecto {
        int id;
        String nombre;

        Proyecto(int id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cotizaciones);

        spProyectos = findViewById(R.id.spProyectos);
        etFechaCotizacion = findViewById(R.id.etFechaCotizacion);
        etDescripcionCotizacion = findViewById(R.id.etDescripcionCotizacion);
        etTotalCotizacion = findViewById(R.id.etTotalCotizacion);
        etEstadoCotizacion = findViewById(R.id.etEstadoCotizacion);

        TextView btnRegistrar = findViewById(R.id.btnRegistrarCotizacion);
        TextView btnConsultar = findViewById(R.id.btnConsultarCotizaciones);

        tvResultadoCotizaciones = findViewById(R.id.tvResultadoCotizaciones);

        cargarProyectos();

        btnRegistrar.setOnClickListener(v -> registrarCotizacion());

        btnConsultar.setOnClickListener(v -> consultarCotizaciones());
    }

    // Cargar proyectos desde la API.
    private void cargarProyectos() {

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.PROYECTOS_URL);
                conexion = (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conexion.getInputStream())
                );

                StringBuilder respuesta = new StringBuilder();
                String linea;

                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                reader.close();

                JSONArray proyectosJSON = new JSONArray(respuesta.toString());

                ArrayList<Proyecto> proyectos = new ArrayList<>();

                proyectos.add(new Proyecto(0, "Selecciona un proyecto"));

                for (int i = 0; i < proyectosJSON.length(); i++) {

                    JSONObject objeto = proyectosJSON.getJSONObject(i);

                    int id = objeto.getInt("id_proyecto");
                    String nombre = objeto.getString("nombre");

                    proyectos.add(new Proyecto(id, nombre));
                }

                handler.post(() -> {

                    listaProyectos.clear();
                    listaProyectos.addAll(proyectos);

                    ArrayAdapter<Proyecto> adapter = new ArrayAdapter<>(
                            CotizacionesActivity.this,
                            android.R.layout.simple_spinner_item,
                            listaProyectos
                    );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spProyectos.setAdapter(adapter);

                });

            } catch (Exception e) {

                handler.post(() ->
                        Toast.makeText(
                                CotizacionesActivity.this,
                                "Error al cargar proyectos: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    // Registrar una cotización.
    private void registrarCotizacion() {

        int posicion = spProyectos.getSelectedItemPosition();

        if (posicion <= 0 || posicion >= listaProyectos.size()) {
            Toast.makeText(this, "Selecciona un proyecto", Toast.LENGTH_SHORT).show();
            return;
        }

        Proyecto proyectoSeleccionado = listaProyectos.get(posicion);

        String fecha = etFechaCotizacion.getText().toString().trim();
        String descripcion = etDescripcionCotizacion.getText().toString().trim();
        String totalTexto = etTotalCotizacion.getText().toString().trim();
        String estado = etEstadoCotizacion.getText().toString().trim();

        if (fecha.isEmpty()) {
            etFechaCotizacion.setError("Ingresa la fecha");
            return;
        }

        double total = 0;

        try {
            if (!totalTexto.isEmpty()) {
                total = Double.parseDouble(totalTexto);
            }
        } catch (NumberFormatException e) {
            etTotalCotizacion.setError("Ingresa un total válido");
            return;
        }

        if (estado.isEmpty()) {
            estado = "Borrador";
        }

        final double totalFinal = total;
        final String estadoFinal = estado;

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.COTIZACIONES_URL);
                conexion = (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexion.setDoOutput(true);
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                JSONObject datos = new JSONObject();

                datos.put("id_proyecto", proyectoSeleccionado.id);
                datos.put("fecha", fecha);
                datos.put("descripcion",
                        descripcion.isEmpty() ? JSONObject.NULL : descripcion);
                datos.put("total", totalFinal);
                datos.put("estado", estadoFinal);

                OutputStream output = conexion.getOutputStream();
                output.write(datos.toString().getBytes("UTF-8"));
                output.close();

                int codigo = conexion.getResponseCode();

                InputStream input = codigo >= 200 && codigo < 400
                        ? conexion.getInputStream()
                        : conexion.getErrorStream();

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(input)
                );

                StringBuilder respuesta = new StringBuilder();
                String linea;

                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                reader.close();

                JSONObject resultado = new JSONObject(respuesta.toString());

                String mensaje = resultado.optString(
                        "mensaje",
                        "Respuesta recibida del servidor"
                );

                handler.post(() -> {

                    Toast.makeText(
                            CotizacionesActivity.this,
                            mensaje,
                            Toast.LENGTH_LONG
                    ).show();

                    tvResultadoCotizaciones.setText(mensaje);

                    if (codigo == 201) {
                        etFechaCotizacion.setText("");
                        etDescripcionCotizacion.setText("");
                        etTotalCotizacion.setText("");
                        etEstadoCotizacion.setText("");
                        spProyectos.setSelection(0);
                    }

                });

            } catch (Exception e) {

                handler.post(() ->
                        Toast.makeText(
                                CotizacionesActivity.this,
                                "Error al registrar: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    // Consultar todas las cotizaciones.
    private void consultarCotizaciones() {

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.COTIZACIONES_URL);
                conexion = (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("GET");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conexion.getInputStream())
                );

                StringBuilder respuesta = new StringBuilder();
                String linea;

                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                reader.close();

                JSONArray cotizaciones = new JSONArray(respuesta.toString());

                StringBuilder resultado = new StringBuilder();

                if (cotizaciones.length() == 0) {
                    resultado.append("No hay cotizaciones registradas.");
                }

                for (int i = 0; i < cotizaciones.length(); i++) {

                    JSONObject objeto = cotizaciones.getJSONObject(i);

                    resultado.append("COTIZACIÓN ")
                            .append(objeto.optInt("id_cotizacion"))
                            .append("\n");

                    resultado.append("Proyecto: ")
                            .append(objeto.optString("nombre_proyecto"))
                            .append("\n");

                    resultado.append("Fecha: ")
                            .append(objeto.optString("fecha"))
                            .append("\n");

                    resultado.append("Descripción: ")
                            .append(objeto.optString("descripcion"))
                            .append("\n");

                    resultado.append("Total: $")
                            .append(objeto.optString("total"))
                            .append("\n");

                    resultado.append("Estado: ")
                            .append(objeto.optString("estado"))
                            .append("\n");

                    resultado.append("----------------------------\n\n");
                }

                String textoFinal = resultado.toString();

                handler.post(() ->
                        tvResultadoCotizaciones.setText(textoFinal)
                );

            } catch (Exception e) {

                handler.post(() ->
                        Toast.makeText(
                                CotizacionesActivity.this,
                                "Error al consultar cotizaciones: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {
                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
