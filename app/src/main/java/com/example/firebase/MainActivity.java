package com.example.firebase;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mapa;
    private Marker marker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DatabaseReference usuariosRef;
    private String usuarioID;
    private boolean usuarioCreado = false;

    private Spinner spinnerUsuarios;
    private TextView txtLatitud, txtLongitud, txtDireccion;
    private Map<String, Marker> markers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar Firebase
        usuariosRef = FirebaseDatabase.getInstance().getReference("Usuarios");
        usuarioID = UUID.randomUUID().toString();

        // Inicializar componentes del layout
        spinnerUsuarios = findViewById(R.id.spinnerUsuarios);
        txtLatitud = findViewById(R.id.txtLatitud);
        txtLongitud = findViewById(R.id.txtLongitud);
        txtDireccion = findViewById(R.id.txtDireccion);

        // Inicializar mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // Inicializar FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Crear el usuario una sola vez
        crearUsuario();

        // Configurar actualizaciones de ubicación
        setupLocationUpdates();

        // Cargar usuarios existentes en el Spinner
        cargarUsuarios();

        // Configurar el listener del Spinner
        configurarSpinnerListener();
    }

    private void configurarSpinnerListener() {
        spinnerUsuarios.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String usuarioSeleccionado = (String) parent.getItemAtPosition(position);
                mostrarUbicacionUsuario(usuarioSeleccionado);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No hacer nada
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mapa = googleMap;
        mapa.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mapa.setMyLocationEnabled(true);
        }
    }

    private void crearUsuario() {
        if (!usuarioCreado) {
            String nombreUsuario = "Usuario " + usuarioID.substring(0, 4);

            Map<String, Object> userData = new HashMap<>();
            userData.put("nombre", nombreUsuario);
            userData.put("latitud", 0.0);
            userData.put("longitud", 0.0);
            userData.put("direccion", "");

            usuariosRef.child(usuarioID).setValue(userData).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    usuarioCreado = true;
                    Toast.makeText(MainActivity.this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Error al crear usuario", Toast.LENGTH_SHORT).show();
                }
            });
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
                    Location location = locationResult.getLastLocation();
                    actualizarPosicionEnFirebase(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void actualizarPosicionEnFirebase(Location location) {
        if (usuarioCreado) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("latitud", location.getLatitude());
            updates.put("longitud", location.getLongitude());

            usuariosRef.child(usuarioID).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> {
                        obtenerDireccion(location.getLatitude(), location.getLongitude());
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(MainActivity.this,
                                "Error al actualizar ubicación", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void obtenerDireccion(double lat, double lng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String direccion = addresses.get(0).getAddressLine(0);

                usuariosRef.child(usuarioID).child("direccion").setValue(direccion)
                        .addOnSuccessListener(aVoid -> {
                            actualizarUI(lat, lng, direccion);
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(MainActivity.this,
                                    "Error al actualizar dirección", Toast.LENGTH_SHORT).show();
                        });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void actualizarUI(double lat, double lng, String direccion) {
        LatLng latLng = new LatLng(lat, lng);

        Marker userMarker = markers.get(usuarioID);
        if (userMarker == null) {
            userMarker = mapa.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Tu Posición")
                    .snippet(direccion));
            markers.put(usuarioID, userMarker);
        } else {
            userMarker.setPosition(latLng);
            userMarker.setSnippet(direccion);
        }

        mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

        txtLatitud.setText(String.format("Latitud: %.5f", lat));
        txtLongitud.setText(String.format("Longitud: %.5f", lng));
        txtDireccion.setText("Dirección: " + direccion);
    }

    private void cargarUsuarios() {
        usuariosRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> listaUsuarios = new ArrayList<>();

                for (DataSnapshot usuarioSnapshot : snapshot.getChildren()) {
                    String nombre = usuarioSnapshot.child("nombre").getValue(String.class);
                    Double latitud = usuarioSnapshot.child("latitud").getValue(Double.class);
                    Double longitud = usuarioSnapshot.child("longitud").getValue(Double.class);
                    String direccion = usuarioSnapshot.child("direccion").getValue(String.class);

                    if (nombre != null && latitud != null && longitud != null && direccion != null) {
                        listaUsuarios.add(nombre);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_item, listaUsuarios);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerUsuarios.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al cargar usuarios: " +
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarUbicacionUsuario(String nombreUsuarioSeleccionado) {
        // Ocultar todos los marcadores
        for (Marker marker : markers.values()) {
            marker.setVisible(false);
        }

        usuariosRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot usuarioSnapshot : snapshot.getChildren()) {
                    String nombre = usuarioSnapshot.child("nombre").getValue(String.class);
                    if (nombre != null && nombre.equals(nombreUsuarioSeleccionado)) {
                        String userId = usuarioSnapshot.getKey();
                        Double lat = usuarioSnapshot.child("latitud").getValue(Double.class);
                        Double lng = usuarioSnapshot.child("longitud").getValue(Double.class);
                        String direccion = usuarioSnapshot.child("direccion").getValue(String.class);

                        if (lat != null && lng != null) {
                            LatLng latLng = new LatLng(lat, lng);

                            Marker userMarker = markers.get(userId);
                            if (userMarker == null) {
                                userMarker = mapa.addMarker(new MarkerOptions()
                                        .position(latLng)
                                        .title(nombreUsuarioSeleccionado)
                                        .snippet(direccion));
                                markers.put(userId, userMarker);
                            } else {
                                userMarker.setPosition(latLng);
                                userMarker.setTitle(nombreUsuarioSeleccionado);
                                userMarker.setSnippet(direccion);
                                userMarker.setVisible(true);
                            }

                            mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

                            txtLatitud.setText(String.format("Latitud: %.5f", lat));
                            txtLongitud.setText(String.format("Longitud: %.5f", lng));
                            txtDireccion.setText("Dirección: " + direccion);
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al mostrar ubicación: " +
                        error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        // Limpiar todos los marcadores
        for (Marker marker : markers.values()) {
            marker.remove();
        }
        markers.clear();

        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
}