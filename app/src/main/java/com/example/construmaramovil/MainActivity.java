package com.example.construmaramovil;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView btnClientes = findViewById(R.id.btnClientes);
        TextView btnProyectos = findViewById(R.id.btnProyectos);
        TextView btnCotizaciones = findViewById(R.id.btnCotizaciones);
        TextView btnMateriales = findViewById(R.id.btnMateriales);

        btnClientes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ClientesActivity.class);
            startActivity(intent);
        });

        btnProyectos.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProyectosActivity.class);
            startActivity(intent);
        });

        btnCotizaciones.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CotizacionesActivity.class);
            startActivity(intent);
        });

        btnMateriales.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MaterialesActivity.class);
            startActivity(intent);
        });
    }
}
