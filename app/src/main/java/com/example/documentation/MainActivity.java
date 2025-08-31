package com.example.documentation;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.common.reflect.TypeToken;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

//Cuenta donde se guardan los manuales: manuales615@gmail.com - M@nuales975
public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private static final String TAG = "MainActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private Drive mDriveService;
    private RecyclerView recyclerView;
    private ManualAdapter adapter;
    private List<String> nombres = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar RecyclerView
        recyclerView = findViewById(R.id.rv_manuales);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManualAdapter(nombres);
        recyclerView.setAdapter(adapter);

        cargarManualesLocales();

        // Configurar opciones de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Asignar acción al botón de descargar
        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(view -> iniciarAutenticacionGoogle());

        // Asignar acción al botón de búsqueda (puedes implementar la funcionalidad)
        Button btnBuscar = findViewById(R.id.btn_search_manual);
        btnBuscar.setOnClickListener(view -> {
            Toast.makeText(this, "Funcionalidad de búsqueda por implementar", Toast.LENGTH_SHORT).show();
        });
    }

    // Adaptador para RecyclerView
    private class ManualAdapter extends RecyclerView.Adapter<ManualAdapter.ViewHolder> {
        private List<String> manuales;

        public ManualAdapter(List<String> manuales) {
            this.manuales = manuales;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(android.R.layout.simple_list_item_1, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String nombreManual = manuales.get(position);
            holder.textView.setText(nombreManual);

            holder.itemView.setOnClickListener(v -> {
                java.io.File archivoManual = new java.io.File(getFilesDir(), "Manuales/" + nombreManual);
                if (archivoManual.exists()) {
                    Intent intent = new Intent(MainActivity.this, VisorManualActivity.class);
                    intent.putExtra(VisorManualActivity.EXTRA_MANUAL_PATH, archivoManual.getAbsolutePath());
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, "Archivo no encontrado: " + nombreManual, Toast.LENGTH_SHORT).show();
                }
            });

            holder.itemView.setOnLongClickListener(v -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminar manual")
                        .setMessage("¿Deseas eliminar \"" + nombreManual + "\"?")
                        .setPositiveButton("Eliminar", (dialog, which) -> eliminarManual(nombreManual))
                        .setNegativeButton("Cancelar", null)
                        .show();
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return manuales.size();
        }

        public void updateData(List<String> nuevosManuales) {
            manuales.clear();
            manuales.addAll(nuevosManuales);
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(android.R.id.text1);
            }
        }
    }

    private void eliminarManual(String nombreArchivo) {
        java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales");
        java.io.File archivo = new java.io.File(carpeta, nombreArchivo);

        if (archivo.exists()) {
            if (archivo.delete()) {
                Toast.makeText(this, "Archivo eliminado", Toast.LENGTH_SHORT).show();
                cargarManualesLocales(); // Recargar la lista
            } else {
                Toast.makeText(this, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cargarManualesLocales() {
        new Thread(() -> {
            java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales");
            List<String> nombresLocales = new ArrayList<>();

            if (carpeta.exists()) {
                java.io.File[] archivos = carpeta.listFiles();
                if (archivos != null) {
                    for (java.io.File archivo : archivos) {
                        nombresLocales.add(archivo.getName());
                    }
                }
            }

            runOnUiThread(() -> {
                nombres.clear();
                nombres.addAll(nombresLocales);
                if (adapter != null) {
                    adapter.updateData(nombresLocales);
                }
            });
        }).start();
    }

    private void iniciarAutenticacionGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Toast.makeText(this, "Autenticado como: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                    obtenerManualesDesdeDrive(account);
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error completo al iniciar sesión: " + e.toString());
                e.printStackTrace();
            }
        }
    }

    private void obtenerManualesDesdeDrive(GoogleSignInAccount account) {
        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE_READONLY));
        credential.setSelectedAccount(account.getAccount());

        mDriveService = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("Documentation App")
                .build();

        Log.d(TAG, "Servicio de Drive inicializado correctamente.");

        // ID puro de la carpeta (sin "?usp=sharing")
        String folderId = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";

        new Thread(() -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("'" + folderId + "' in parents and trashed = false")
                        .setFields("files(id, name, mimeType, modifiedTime)")
                        .execute();

                List<File> files = result.getFiles();
                List<ArchivoDescargado> historial = cargarHistorial();
                List<ArchivoDescargado> nuevoHistorial = new ArrayList<>(historial);
                List<String> nuevosNombres = new ArrayList<>();

                if (files != null && !files.isEmpty()) {
                    for (File file : files) {
                        if (esNuevoOEditado(file, historial)) {
                            descargarYGuardarArchivo(file);

                            ArchivoDescargado nuevo = new ArchivoDescargado(
                                    file.getId(),
                                    file.getName(),
                                    file.getModifiedTime().toStringRfc3339()
                            );

                            // Actualizar historial
                            for (Iterator<ArchivoDescargado> iterator = nuevoHistorial.iterator(); iterator.hasNext();) {
                                ArchivoDescargado a = iterator.next();
                                if (a.getId().equals(nuevo.getId())) {
                                    iterator.remove();
                                }
                            }
                            nuevoHistorial.add(nuevo);
                        }
                        nuevosNombres.add(file.getName());
                    }

                    guardarHistorial(nuevoHistorial);

                    runOnUiThread(() -> {
                        nombres.clear();
                        nombres.addAll(nuevosNombres);
                        adapter.updateData(nuevosNombres);
                        Toast.makeText(MainActivity.this, "Sincronización completa.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this, "No hay archivos en la carpeta", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al listar archivos", e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error al conectar con Drive", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private List<ArchivoDescargado> cargarHistorial() {
        java.io.File file = new java.io.File(getFilesDir(), "historial.json");
        if (!file.exists()) return new ArrayList<>();

        try (FileReader reader = new FileReader(file)) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<ArchivoDescargado>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private void guardarHistorial(List<ArchivoDescargado> archivos) {
        try (FileWriter writer = new FileWriter(new java.io.File(getFilesDir(), "historial.json"))) {
            new Gson().toJson(archivos, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean esNuevoOEditado(File fileDrive, List<ArchivoDescargado> historial) {
        for (ArchivoDescargado local : historial) {
            if (fileDrive.getId().equals(local.getId())) {
                return !fileDrive.getModifiedTime().toStringRfc3339().equals(local.getModifiedTime());
            }
        }
        return true;
    }

    private void descargarYGuardarArchivo(File archivoDrive) {
        new Thread(() -> {
            try {
                // Crear carpeta "Manuales" dentro del almacenamiento privado de la app
                java.io.File folder = new java.io.File(getFilesDir(), "Manuales");
                if (!folder.exists()) {
                    folder.mkdirs();
                }

                // Archivo local con el mismo nombre que el archivo de Drive
                java.io.File archivoLocal = new java.io.File(folder, archivoDrive.getName());

                // Descargar el archivo y guardar en archivoLocal
                FileOutputStream outputStream = new FileOutputStream(archivoLocal);
                mDriveService.files().get(archivoDrive.getId()).executeMediaAndDownloadTo(outputStream);
                outputStream.close();

                Log.d(TAG, "Archivo descargado y guardado en: " + archivoLocal.getAbsolutePath());

                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Archivo descargado: " + archivoDrive.getName(), Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error descargando archivo: " + archivoDrive.getName(), e);
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "Error descargando archivo: " + archivoDrive.getName(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    // Clase interna para representar los archivos descargados en el historial
    public static class ArchivoDescargado {
        private String id;
        private String name;
        private String modifiedTime;

        public ArchivoDescargado() {
            // Constructor vacío necesario para Gson
        }

        public ArchivoDescargado(String id, String name, String modifiedTime) {
            this.id = id;
            this.name = name;
            this.modifiedTime = modifiedTime;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getModifiedTime() {
            return modifiedTime;
        }
    }
}