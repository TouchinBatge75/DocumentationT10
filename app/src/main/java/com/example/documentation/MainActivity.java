package com.example.documentation;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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


import java.io.OutputStream;
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
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> nombres = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializar ListView y adaptador
        listView = findViewById(R.id.lista_manuales);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nombres);
        listView.setAdapter(adapter);

        cargarManualesLocales();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String nombreManual = nombres.get(position);
            java.io.File archivoManual = new java.io.File(getFilesDir(), "Manuales/" + nombreManual);

            if (archivoManual.exists()) {
                Intent intent = new Intent(MainActivity.this, VisorManualActivity.class);
                intent.putExtra(VisorManualActivity.EXTRA_MANUAL_PATH, archivoManual.getAbsolutePath());
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Archivo no encontrado: " + nombreManual, Toast.LENGTH_SHORT).show();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String nombreArchivo = adapter.getItem(position);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Eliminar manual")
                        .setMessage("¿Deseas eliminar \"" + nombreArchivo + "\"?")
                        .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                eliminarManual(nombreArchivo);
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();

                return true;
            }
        });





        // Configurar opciones de Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Asignar acción al botón
        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(view -> iniciarAutenticacionGoogle());
    }

    private void eliminarManual(String nombreArchivo) {
        java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales");
        java.io.File archivo = new  java.io.File(carpeta, nombreArchivo);

        if (archivo.exists()) {
            if (archivo.delete()) {
                Toast.makeText(this, "Archivo eliminado", Toast.LENGTH_SHORT).show();
                adapter.remove(nombreArchivo);
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "No se pudo eliminar el archivo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void cargarManualesLocales() {
        java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales");
        if (!carpeta.exists()) return;

        java.io.File[] archivos = carpeta.listFiles();
        if (archivos == null || archivos.length == 0) return;

        List<String> nombresLocales = new ArrayList<>();
        for (java.io.File archivo : archivos) {
            nombresLocales.add(archivo.getName());
        }

        adapter.clear();
        adapter.addAll(nombresLocales);
        adapter.notifyDataSetChanged();
    }

    private void iniciarAutenticacionGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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
                e.printStackTrace(); // Fuerza la impresión en LogCat
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

                        }
                        nuevosNombres.add(file.getName());
                    }

                    guardarHistorial(nuevoHistorial);

                    runOnUiThread(() -> {
                        adapter.clear();
                        adapter.addAll(nuevosNombres);
                        adapter.notifyDataSetChanged();
                        Toast.makeText(this, "Sincronización completa.", Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No hay archivos en la carpeta", Toast.LENGTH_SHORT).show()
                    );
                }
            } catch (Exception e) {
                Log.e(TAG, "Error al listar archivos", e);
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
                        Toast.makeText(this, "Archivo descargado: " + archivoDrive.getName(), Toast.LENGTH_SHORT).show()
                );
            } catch (Exception e) {
                Log.e(TAG, "Error descargando archivo: " + archivoDrive.getName(), e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error descargando archivo: " + archivoDrive.getName(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }


}
