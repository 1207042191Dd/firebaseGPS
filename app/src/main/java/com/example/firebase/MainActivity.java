package com.example.firebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference ubicacionRef;
    private TextView txtLatitud, txtLongitud;
    private Button btnIrAMainActivity2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        ubicacionRef = FirebaseDatabase.getInstance().getReference("ubicacion");

        // Inicializar componentes del layout
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);

        // Botón para ir a MainActivity2
        btnIrAMainActivity2 = findViewById(R.id.btnIrAMainActivity2);
        btnIrAMainActivity2.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MainActivity2.class);
            startActivity(intent);
        });

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Configurar actualizaciones de ubicación
        setupLocationUpdates();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mapa.setMyLocationEnabled(true);
            mapa.getUiSettings().setZoomControlsEnabled(true);
            mapa.getUiSettings().setCompassEnabled(true);
        }
    }

    @SuppressLint("MissingPermission")
    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    actualizarUbicacion(locationResult.getLastLocation());
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void actualizarUbicacion(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        // Actualizar Firebase
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitud", lat);
        updates.put("longitud", lng);

        ubicacionRef.setValue(updates)
                .addOnSuccessListener(aVoid -> actualizarUI(lat, lng))
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this,
                        "Error al actualizar ubicación", Toast.LENGTH_SHORT).show());
    }

    private void actualizarUI(double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);

        // Limpiar mapa y agregar nuevo marcador
        mapa.clear();
        mapa.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Mi Posición"));

        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        // Actualizar TextViews
        txtLatitud.setText(String.format("Latitud: %.5f", lat));
        txtLongitud.setText(String.format("Longitud: %.5f", lng));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}
