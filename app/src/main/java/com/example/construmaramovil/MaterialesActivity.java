package com.example.construmaramovil;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaterialesActivity extends AppCompatActivity {

    private EditText etNombreMaterial;
    private EditText etDescripcionMaterial;
    private EditText etUnidadMaterial;
    private EditText etCantidadMaterial;
    private EditText etPrecioMaterial;

    private TextView tvResultadoMateriales;

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materiales);

        etNombreMaterial = findViewById(R.id.etNombreMaterial);
        etDescripcionMaterial = findViewById(R.id.etDescripcionMaterial);
        etUnidadMaterial = findViewById(R.id.etUnidadMaterial);
        etCantidadMaterial = findViewById(R.id.etCantidadMaterial);
        etPrecioMaterial = findViewById(R.id.etPrecioMaterial);

        TextView btnRegistrar = findViewById(R.id.btnRegistrarMaterial);
        TextView btnConsultar = findViewById(R.id.btnConsultarMateriales);

        tvResultadoMateriales = findViewById(R.id.tvResultadoMateriales);

        btnRegistrar.setOnClickListener(v -> registrarMaterial());

        btnConsultar.setOnClickListener(v -> consultarMateriales());
    }

    // Registrar un material en la base de datos.
    private void registrarMaterial() {

        String nombre = etNombreMaterial.getText().toString().trim();
        String descripcion = etDescripcionMaterial.getText().toString().trim();
        String unidad = etUnidadMaterial.getText().toString().trim();
        String cantidadTexto = etCantidadMaterial.getText().toString().trim();
        String precioTexto = etPrecioMaterial.getText().toString().trim();

        if (nombre.isEmpty()) {
            etNombreMaterial.setError("Ingresa el nombre del material");
            return;
        }

        if (unidad.isEmpty()) {
            etUnidadMaterial.setError("Ingresa la unidad de medida");
            return;
        }

        double cantidad = 0;
        double precio = 0;

        try {
            if (!cantidadTexto.isEmpty()) {
                cantidad = Double.parseDouble(cantidadTexto);
            }

            if (!precioTexto.isEmpty()) {
                precio = Double.parseDouble(precioTexto);
            }

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Ingresa cantidades y precios válidos",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final double cantidadFinal = cantidad;
        final double precioFinal = precio;

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.MATERIALES_URL);
                conexion = (HttpURLConnection) url.openConnection();

                conexion.setRequestMethod("POST");
                conexion.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conexion.setDoOutput(true);
                conexion.setConnectTimeout(10000);
                conexion.setReadTimeout(10000);

                JSONObject datos = new JSONObject();

                datos.put("nombre", nombre);
                datos.put("descripcion",
                        descripcion.isEmpty() ? JSONObject.NULL : descripcion);
                datos.put("unidad", unidad);
                datos.put("cantidad", cantidadFinal);
                datos.put("precio_unitario", precioFinal);

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
                            MaterialesActivity.this,
                            mensaje,
                            Toast.LENGTH_LONG
                    ).show();

                    tvResultadoMateriales.setText(mensaje);

                    if (codigo == 201) {
                        etNombreMaterial.setText("");
                        etDescripcionMaterial.setText("");
                        etUnidadMaterial.setText("");
                        etCantidadMaterial.setText("");
                        etPrecioMaterial.setText("");
                    }
                });

            } catch (Exception e) {

                handler.post(() ->
                        Toast.makeText(
                                MaterialesActivity.this,
                                "Error al registrar material: " + e.getMessage(),
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

    // Consultar todos los materiales registrados.
    private void consultarMateriales() {

        executor.execute(() -> {

            HttpURLConnection conexion = null;

            try {
                URL url = new URL(ConexionAPI.MATERIALES_URL);
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

                JSONArray materiales = new JSONArray(respuesta.toString());

                StringBuilder resultado = new StringBuilder();

                if (materiales.length() == 0) {
                    resultado.append("No hay materiales registrados.");
                }

                for (int i = 0; i < materiales.length(); i++) {

                    JSONObject objeto = materiales.getJSONObject(i);

                    resultado.append("MATERIAL ")
                            .append(objeto.optInt("id_material"))
                            .append("\n");

                    resultado.append("Nombre: ")
                            .append(objeto.optString("nombre"))
                            .append("\n");

                    resultado.append("Descripción: ")
                            .append(objeto.optString("descripcion"))
                            .append("\n");

                    resultado.append("Unidad: ")
                            .append(objeto.optString("unidad"))
                            .append("\n");

                    resultado.append("Cantidad: ")
                            .append(objeto.optString("cantidad"))
                            .append("\n");

                    resultado.append("Precio unitario: $")
                            .append(objeto.optString("precio_unitario"))
                            .append("\n");

                    resultado.append("----------------------------\n\n");
                }

                String textoFinal = resultado.toString();

                handler.post(() ->
                        tvResultadoMateriales.setText(textoFinal)
                );

            } catch (Exception e) {

                handler.post(() ->
                        Toast.makeText(
                                MaterialesActivity.this,
                                "Error al consultar materiales: " + e.getMessage(),
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
