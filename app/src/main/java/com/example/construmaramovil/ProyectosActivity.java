
package com.example.construmaramovil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProyectosActivity extends AppCompatActivity {

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    private Spinner spClientes;

    private EditText etNombreProyecto, etTipoObra;
    private EditText etDescripcion, etPresupuesto, etEstado;
    private EditText etFechaInicio, etFechaFin;

    private TextView btnRegistrarProyecto;
    private TextView btnConsultarProyectos;
    private TextView tvResultadoProyectos;

    private ArrayList<Cliente> listaClientes = new ArrayList<>();

    private static class Cliente {

        int id;
        String nombre;

        Cliente(int id, String nombre) {
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
        setContentView(R.layout.activity_proyectos);

        spClientes = findViewById(R.id.spClientes);

        etNombreProyecto = findViewById(R.id.etNombreProyecto);
        etTipoObra = findViewById(R.id.etTipoObra);
        etDescripcion = findViewById(R.id.etDescripcion);
        etPresupuesto = findViewById(R.id.etPresupuesto);
        etEstado = findViewById(R.id.etEstado);
        etFechaInicio = findViewById(R.id.etFechaInicio);
        etFechaFin = findViewById(R.id.etFechaFin);

        btnRegistrarProyecto = findViewById(R.id.btnRegistrarProyecto);
        btnConsultarProyectos = findViewById(R.id.btnConsultarProyectos);
        tvResultadoProyectos = findViewById(R.id.tvResultadoProyectos);

        btnRegistrarProyecto.setOnClickListener(v -> registrarProyecto());

        btnConsultarProyectos.setOnClickListener(v -> consultarProyectos());

        cargarClientes();
    }

    private void cargarClientes() {

        tvResultadoProyectos.setText("Cargando clientes...");

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.CLIENTES_URL);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setRequestProperty("Accept", "application/json");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                int codigo = conexion.getResponseCode();

                if (codigo == HttpURLConnection.HTTP_OK) {

                    StringBuilder respuesta = new StringBuilder();

                    try (java.io.BufferedReader lector =
                                 new java.io.BufferedReader(
                                         new java.io.InputStreamReader(
                                                 conexion.getInputStream(),
                                                 StandardCharsets.UTF_8))) {

                        String linea;

                        while ((linea = lector.readLine()) != null) {
                            respuesta.append(linea);
                        }
                    }

                    JSONArray clientes =
                            new JSONArray(respuesta.toString());

                    ArrayList<Cliente> nuevosClientes = new ArrayList<>();

                    nuevosClientes.add(
                            new Cliente(0, "Selecciona un cliente")
                    );

                    for (int i = 0; i < clientes.length(); i++) {

                        JSONObject cliente = clientes.getJSONObject(i);

                        int id = cliente.optInt("id_cliente");
                        String nombre = cliente.optString("nombre");

                        nuevosClientes.add(new Cliente(id, nombre));
                    }

                    handler.post(() -> {

                        listaClientes.clear();
                        listaClientes.addAll(nuevosClientes);

                        ArrayAdapter<Cliente> adaptador =
                                new ArrayAdapter<>(
                                        ProyectosActivity.this,
                                        android.R.layout.simple_spinner_item,
                                        listaClientes
                                );

                        adaptador.setDropDownViewResource(
                                android.R.layout.simple_spinner_dropdown_item
                        );

                        spClientes.setAdapter(adaptador);

                        if (listaClientes.size() > 1) {
                            tvResultadoProyectos.setText(
                                    "Selecciona el cliente para el proyecto."
                            );
                        } else {
                            tvResultadoProyectos.setText(
                                    "No hay clientes registrados."
                            );
                        }
                    });

                } else {

                    handler.post(() ->
                            tvResultadoProyectos.setText(
                                    "No se pudieron cargar los clientes. Código: "
                                            + codigo
                            )
                    );
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "CONSTRUMARA",
                        "Error al cargar clientes",
                        e
                );

                handler.post(() ->
                        tvResultadoProyectos.setText(
                                "Error al cargar clientes: " + e.getMessage()
                        )
                );

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    private void registrarProyecto() {

        int posicion = spClientes.getSelectedItemPosition();

        if (posicion <= 0 || listaClientes.size() <= posicion) {
            tvResultadoProyectos.setText(
                    "Debes seleccionar un cliente registrado."
            );
            return;
        }

        int clienteId = listaClientes.get(posicion).id;

        String nombre = etNombreProyecto.getText().toString().trim();
        String tipoObra = etTipoObra.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String presupuesto = etPresupuesto.getText().toString().trim();
        String estado = etEstado.getText().toString().trim();
        String fechaInicio = etFechaInicio.getText().toString().trim();
        String fechaFin = etFechaFin.getText().toString().trim();

        if (nombre.isEmpty() || tipoObra.isEmpty()) {
            tvResultadoProyectos.setText(
                    "El nombre y el tipo de obra son obligatorios."
            );
            return;
        }

        double valorPresupuesto;

        try {
            valorPresupuesto = presupuesto.isEmpty()
                    ? 0.0
                    : Double.parseDouble(presupuesto);

            if (valorPresupuesto < 0) {
                tvResultadoProyectos.setText(
                        "El presupuesto no puede ser negativo."
                );
                return;
            }

        } catch (NumberFormatException e) {
            tvResultadoProyectos.setText(
                    "El presupuesto debe ser un valor numérico."
            );
            return;
        }

        btnRegistrarProyecto.setEnabled(false);
        tvResultadoProyectos.setText("Registrando proyecto...");

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.PROYECTOS_URL);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                );
                conexion.setRequestProperty("Accept", "application/json");
                conexion.setDoOutput(true);
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                JSONObject datos = new JSONObject();

                datos.put("id_cliente", clienteId);
                datos.put("nombre", nombre);
                datos.put("tipo_obra", tipoObra);
                datos.put("descripcion",
                        descripcion.isEmpty() ? JSONObject.NULL : descripcion);
                datos.put("presupuesto", valorPresupuesto);
                datos.put("estado", estado.isEmpty() ? "Pendiente" : estado);
                datos.put("fecha_inicio",
                        fechaInicio.isEmpty() ? JSONObject.NULL : fechaInicio);
                datos.put("fecha_fin",
                        fechaFin.isEmpty() ? JSONObject.NULL : fechaFin);

                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(
                            datos.toString().getBytes(StandardCharsets.UTF_8)
                    );
                }

                int codigo = conexion.getResponseCode();

                handler.post(() -> {

                    btnRegistrarProyecto.setEnabled(true);

                    if (codigo == 201) {

                        tvResultadoProyectos.setText(
                                "¡Proyecto registrado correctamente en Construmara!"
                        );

                        spClientes.setSelection(0);

                        etNombreProyecto.setText("");
                        etTipoObra.setText("");
                        etDescripcion.setText("");
                        etPresupuesto.setText("");
                        etEstado.setText("");
                        etFechaInicio.setText("");
                        etFechaFin.setText("");

                    } else if (codigo == 400) {

                        tvResultadoProyectos.setText(
                                "Verifica los datos y el cliente seleccionado."
                        );

                    } else {

                        tvResultadoProyectos.setText(
                                "No se pudo registrar el proyecto. Código: "
                                        + codigo
                        );
                    }
                });

            } catch (Exception e) {

                android.util.Log.e(
                        "CONSTRUMARA",
                        "Error al registrar proyecto",
                        e
                );

                handler.post(() -> {
                    btnRegistrarProyecto.setEnabled(true);

                    tvResultadoProyectos.setText(
                            "Error de conexión: " + e.getMessage()
                    );
                });

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }
            }
        });
    }

    private void consultarProyectos() {

        btnConsultarProyectos.setEnabled(false);
        tvResultadoProyectos.setText("Consultando proyectos...");

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.PROYECTOS_URL);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("GET");
                conexion.setRequestProperty("Accept", "application/json");
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                int codigo = conexion.getResponseCode();

                if (codigo == HttpURLConnection.HTTP_OK) {

                    StringBuilder respuesta = new StringBuilder();

                    try (java.io.BufferedReader lector =
                                 new java.io.BufferedReader(
                                         new java.io.InputStreamReader(
                                                 conexion.getInputStream(),
                                                 StandardCharsets.UTF_8))) {

                        String linea;

                        while ((linea = lector.readLine()) != null) {
                            respuesta.append(linea);
                        }
                    }

                    JSONArray proyectos =
                            new JSONArray(respuesta.toString());

                    StringBuilder texto = new StringBuilder();

                    if (proyectos.length() == 0) {

                        texto.append("No hay proyectos registrados.");

                    } else {

                        texto.append("PROYECTOS REGISTRADOS\n\n");

                        for (int i = 0; i < proyectos.length(); i++) {

                            JSONObject proyecto = proyectos.getJSONObject(i);

                            texto.append("Proyecto: ")
                                    .append(proyecto.optString("nombre"))
                                    .append("\n");

                            texto.append("Cliente: ")
                                    .append(proyecto.optString("nombre_cliente"))
                                    .append("\n");

                            texto.append("Tipo de obra: ")
                                    .append(proyecto.optString("tipo_obra"))
                                    .append("\n");

                            texto.append("Descripción: ")
                                    .append(proyecto.optString("descripcion"))
                                    .append("\n");

                            texto.append("Presupuesto: ")
                                    .append(proyecto.optString("presupuesto"))
                                    .append("\n");

                            texto.append("Estado: ")
                                    .append(proyecto.optString("estado"))
                                    .append("\n");

                            texto.append("Fecha de inicio: ")
                                    .append(proyecto.optString("fecha_inicio"))
                                    .append("\n");

                            texto.append("Fecha de finalización: ")
                                    .append(proyecto.optString("fecha_fin"))
                                    .append("\n\n");
                        }
                    }

                    String resultado = texto.toString();

                    handler.post(() ->
                            tvResultadoProyectos.setText(resultado)
                    );

                } else {

                    handler.post(() ->
                            tvResultadoProyectos.setText(
                                    "No se pudieron consultar los proyectos. Código: "
                                            + codigo
                            )
                    );
                }

            } catch (Exception e) {

                android.util.Log.e(
                        "CONSTRUMARA",
                        "Error al consultar proyectos",
                        e
                );

                handler.post(() ->
                        tvResultadoProyectos.setText(
                                "Error al consultar: " + e.getMessage()
                        )
                );

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }

                handler.post(() ->
                        btnConsultarProyectos.setEnabled(true)
                );
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
