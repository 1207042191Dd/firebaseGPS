package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity2 extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private DatabaseReference ubicacionRef;
    private TextView txtLatitud, txtLongitud;
    private Button btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        // Inicializar Firebase
        ubicacionRef = FirebaseDatabase.getInstance().getReference("ubicacion");

        // Inicializar vistas
        txtLatitud = findViewById(R.id.txtLatitud2);
        txtLongitud = findViewById(R.id.txtLongitud2);
        btnVolver = findViewById(R.id.btnVolver);

        // Configurar el botón para volver a MainActivity
        btnVolver.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity2.this, MainActivity.class);
            startActivity(intent);
            finish(); // Cierra MainActivity2 para liberar recursos
        });

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Escuchar cambios en Firebase
        ubicacionRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Double lat = snapshot.child("latitud").getValue(Double.class);
                    Double lng = snapshot.child("longitud").getValue(Double.class);

                    if (lat != null && lng != null) {
                        // Actualizar textos de latitud y longitud
                        txtLatitud.setText("Latitud: " + lat);
                        txtLongitud.setText("Longitud: " + lng);

                        // Actualizar el mapa
                        actualizarMarcadorMapa(lat, lng);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity2.this, "Error al cargar ubicación", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mapa.getUiSettings().setZoomControlsEnabled(true);
    }

    private void actualizarMarcadorMapa(double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);

        // Limpiar mapa y agregar nuevo marcador
        mapa.clear();
        mapa.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Posición desde Firebase"));

        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
    }
}