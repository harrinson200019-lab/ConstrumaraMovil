package com.example.construmaramovil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientesActivity extends AppCompatActivity {

    private EditText etNombre, etDocumento, etTelefono;
    private EditText etCorreo, etDireccion;
    private TextView btnRegistrar, btnConsultar, tvResultado;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Handler handler =
            new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clientes);

        etNombre = findViewById(R.id.etNombre);
        etDocumento = findViewById(R.id.etDocumento);
        etTelefono = findViewById(R.id.etTelefono);
        etCorreo = findViewById(R.id.etCorreo);
        etDireccion = findViewById(R.id.etDireccion);

        btnRegistrar = findViewById(R.id.btnRegistrar);
        btnConsultar = findViewById(R.id.btnConsultar);
        tvResultado = findViewById(R.id.tvResultado);

        btnRegistrar.setOnClickListener(v -> registrarCliente());
        btnConsultar.setOnClickListener(v -> consultarClientes());
    }

    private void consultarClientes() {

        btnConsultar.setEnabled(false);
        tvResultado.setText("Consultando clientes...");

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

                    org.json.JSONArray clientes =
                            new org.json.JSONArray(respuesta.toString());

                    StringBuilder texto = new StringBuilder();

                    if (clientes.length() == 0) {
                        texto.append("No hay clientes registrados.");
                    } else {

                        texto.append("CLIENTES REGISTRADOS\n\n");

                        for (int i = 0; i < clientes.length(); i++) {

                            JSONObject cliente = clientes.getJSONObject(i);

                            texto.append("Nombre: ")
                                    .append(cliente.optString("nombre"))
                                    .append("\n");

                            texto.append("Documento: ")
                                    .append(cliente.optString("documento"))
                                    .append("\n");

                            texto.append("Teléfono: ")
                                    .append(cliente.optString("telefono"))
                                    .append("\n");

                            texto.append("Correo: ")
                                    .append(cliente.optString("correo"))
                                    .append("\n");

                            texto.append("Dirección: ")
                                    .append(cliente.optString("direccion"))
                                    .append("\n\n");
                        }
                    }

                    String resultado = texto.toString();

                    handler.post(() -> tvResultado.setText(resultado));

                } else {

                    handler.post(() ->
                            tvResultado.setText(
                                    "No se pudieron consultar los clientes. Código: " + codigo
                            )
                    );
                }

            } catch (Exception e) {

                android.util.Log.e("CONSTRUMARA", "Error al consultar clientes", e);

                handler.post(() ->
                        tvResultado.setText("Error al consultar: " + e.getMessage())
                );

            } finally {

                if (conexion != null) {
                    conexion.disconnect();
                }

                handler.post(() -> btnConsultar.setEnabled(true));
            }
        });
    }

    private void registrarCliente() {

        String nombre = etNombre.getText().toString().trim();
        String documento = etDocumento.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String correo = etCorreo.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();

        if (nombre.isEmpty() || documento.isEmpty()) {
            tvResultado.setText(
                    "El nombre y el documento son obligatorios."
            );
            return;
        }

        btnRegistrar.setEnabled(false);
        tvResultado.setText("Registrando cliente...");

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.CLIENTES_URL);

                android.util.Log.d("CONSTRUMARA", "URL utilizada: " + url);

                conexion = (HttpURLConnection) url.openConnection();
                conexion.setRequestMethod("POST");
                conexion.setInstanceFollowRedirects(false);
                conexion.setUseCaches(false);
                conexion.setRequestProperty(
                        "Content-Type", "application/json; charset=UTF-8"
                );
                conexion.setRequestProperty(
                        "Accept", "application/json"
                );
                conexion.setDoOutput(true);
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                JSONObject datos = new JSONObject();
                datos.put("nombre", nombre);
                datos.put("documento", documento);
                datos.put("telefono", telefono);
                datos.put("correo", correo);
                datos.put("direccion", direccion);

                try (OutputStream salida = conexion.getOutputStream()) {
                    salida.write(
                            datos.toString().getBytes(StandardCharsets.UTF_8)
                    );
                }

                int codigo = conexion.getResponseCode();

                handler.post(() -> {
                    btnRegistrar.setEnabled(true);

                    if (codigo == 201) {
                        tvResultado.setText(
                                "¡Cliente registrado correctamente en Construmara!"
                        );

                        etNombre.setText("");
                        etDocumento.setText("");
                        etTelefono.setText("");
                        etCorreo.setText("");
                        etDireccion.setText("");

                    } else if (codigo == 409) {
                        tvResultado.setText(
                                "Ya existe un cliente con ese documento."
                        );
                    } else if (codigo == 400) {
                        tvResultado.setText(
                                "Verifica los datos obligatorios."
                        );
                    } else {
                        tvResultado.setText(
                                "No se pudo registrar. Código: " + codigo
                        );
                    }
                });

            } catch (Exception e) {

                android.util.Log.e("CONSTRUMARA", "Error al registrar cliente", e);

                handler.post(() -> {
                    btnRegistrar.setEnabled(true);
                    tvResultado.setText(
                            "Error de conexión: " + e.toString()
                    );
                });

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
        executor.shutdown();
    }
}
