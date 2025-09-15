package com.example.documentation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import com.google.gson.Gson;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;

//Cuenta donde se guardan los manuales: manuales615@gmail.com - M@nuales975

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private static final String TAG = "MainActivity";
    private GoogleSignInClient mGoogleSignInClient;
    private Drive mDriveService;
    private RecyclerView recyclerView;
    private ManualAdapter adapter;
    private List<ItemDrive> items = new ArrayList<>();
    private GoogleSignInAccount currentAccount;
    private String folderID = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";
    private String currentRelativePath = "";

    private Stack<String> pilaCarpetas = new Stack<>();
    private Stack<String> pilaRutas = new Stack<>();

    // Clase interna
    public static class ItemDrive {
        public String id;
        public String name;
        public boolean esCarpeta;
        public boolean descargado;
        public String rutaRelativa;

        public ItemDrive(String id, String name, boolean esCarpeta, String rutaRelativa) {
            this.id = id;
            this.name = name;
            this.esCarpeta = esCarpeta;
            this.descargado = false;
            this.rutaRelativa = rutaRelativa;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pilaCarpetas.push(folderID);
        pilaRutas.push("");

        recyclerView = findViewById(R.id.rv_manuales);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManualAdapter(items, mDriveService);
        recyclerView.setAdapter(adapter);

        cargarManualesLocales(""); // Cargar desde la raíz

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(view -> iniciarAutenticacionGoogle());

        Button btnBuscar = findViewById(R.id.btn_search_manual);
        btnBuscar.setOnClickListener(v -> mostrarOpcionesBusqueda());

    }

    private void mostrarOpcionesBusqueda() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buscar manual")
                .setItems(new String[]{"Por nombre", "Por selectores"}, (dialog, which) -> {
                    if (which == 0) {
                        buscarPorNombre();
                    } else if (which == 1) {
                        buscarPorSelectores();
                    }
                });
        builder.show();
    }

    private void buscarPorNombre() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buscar manual por nombre");

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton("Buscar", (dialog, which) -> {
            String query = input.getText().toString();
            filtrarPorNombre(query);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void filtrarPorNombre(String query) {
        String q = query.toLowerCase().trim();
        List<ItemDrive> filtrados = new ArrayList<>();

        for (ItemDrive item : items) {
            // Busca tanto en carpetas como en archivos, insensible a mayúsculas
            if (item.name.toLowerCase().contains(q)) {
                filtrados.add(item);
            }
        }
        adapter.updateData(filtrados);
    }

    private void buscarPorSelectores() {
        // Extraer opciones únicas de los items
        Set<String> tipos = new HashSet<>();
        Set<String> marcas = new HashSet<>();
        Set<String> modelos = new HashSet<>();

        for (ItemDrive item : items) {
            if (!item.esCarpeta && !item.rutaRelativa.isEmpty()) {
                String[] partes = item.rutaRelativa.split("/");
                if (partes.length > 0) tipos.add(partes[0]);
                if (partes.length > 1) marcas.add(partes[1]);
                if (partes.length > 2) modelos.add(partes[2]);
            }
        }

        // Crear AlertDialog con 3 Spinners
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selectores, null);
        Spinner spinnerTipo = dialogView.findViewById(R.id.spinner_tipo);
        Spinner spinnerMarca = dialogView.findViewById(R.id.spinner_marca);
        Spinner spinnerModelo = dialogView.findViewById(R.id.spinner_modelo);

        // Cargar sets en los spinners
        spinnerTipo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(tipos)));
        spinnerMarca.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(marcas)));
        spinnerModelo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>(modelos)));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buscar manual por selectores");
        builder.setView(dialogView);
        builder.setPositiveButton("Filtrar", (d, w) -> {
            String tipo = spinnerTipo.getSelectedItem() != null ? spinnerTipo.getSelectedItem().toString() : "";
            String marca = spinnerMarca.getSelectedItem() != null ? spinnerMarca.getSelectedItem().toString() : "";
            String modelo = spinnerModelo.getSelectedItem() != null ? spinnerModelo.getSelectedItem().toString() : "";
            filtrarPorSelectores(tipo, marca, modelo);
        });
        builder.setNegativeButton("Cancelar", (d, w) -> d.dismiss());
        builder.show();
    }

    private void filtrarPorSelectores(String tipo, String marca, String modelo) {
        List<ItemDrive> filtrados = new ArrayList<>();
        for (ItemDrive item : items) {
            if (!item.esCarpeta) {
                String[] partes = item.rutaRelativa.split("/");
                if ((tipo.isEmpty() || (partes.length > 0 && partes[0].equals(tipo))) &&
                        (marca.isEmpty() || (partes.length > 1 && partes[1].equals(marca))) &&
                        (modelo.isEmpty() || (partes.length > 2 && partes[2].equals(modelo)))) {
                    filtrados.add(item);
                }
            }
        }
        adapter.updateData(filtrados);
    }

    @Override
    public void onBackPressed() {
        if (pilaCarpetas.size() > 1) {
            // Remover la carpeta actual (la estamos saliendo)
            pilaCarpetas.pop();
            pilaRutas.pop();

            // Recuperar la carpeta anterior
            String carpetaAnterior = pilaCarpetas.peek();
            String rutaAnterior = pilaRutas.peek();

            // Actualizar variables actuales
            this.folderID = carpetaAnterior;
            this.currentRelativePath = rutaAnterior;

            // SOLO cargar archivos locales al navegar atrás
            cargarManualesLocales(rutaAnterior);

            // Y solo si estamos autenticados, también cargar de Drive
            if (currentAccount != null && mDriveService != null) {
                listarCarpetaDrive(folderID, rutaAnterior);
            }
        } else {
            super.onBackPressed();
        }
    }

    private void eliminarCarpeta(ItemDrive item) {
        java.io.File carpetaLocal = new java.io.File(getFilesDir(),
                "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.name);

        if (carpetaLocal.exists() && carpetaLocal.isDirectory()) {
            boolean eliminada = eliminarRecursivamente(carpetaLocal);

            if (eliminada) {
                Toast.makeText(this, "Carpeta eliminada: " + item.name, Toast.LENGTH_SHORT).show();
                // Recargar la vista actual
                cargarManualesLocales(currentRelativePath);
                if (currentAccount != null) {
                    listarCarpetaDrive(folderID, currentRelativePath);
                }
            } else {
                Toast.makeText(this, "No se pudo eliminar la carpeta", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Helper para borrar carpetas recursivamente
    private boolean eliminarRecursivamente(java.io.File archivo) {
        if (archivo.isDirectory()) {
            java.io.File[] hijos = archivo.listFiles();
            if (hijos != null) {
                for (java.io.File hijo : hijos) {
                    eliminarRecursivamente(hijo);
                }
            }
        }
        return archivo.delete();
    }
    private class ManualAdapter extends RecyclerView.Adapter<ManualAdapter.ViewHolder> {
        private List<ItemDrive> items;
        private Drive driveService; // Agregar esta variable

        public ManualAdapter(List<ItemDrive> items, Drive driveService) {
            this.items = items;
            this.driveService = driveService;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manual, parent, false);
            return new ViewHolder(view);
        }

        public void setDriveService(Drive driveService) {
            this.driveService = driveService;
        }


        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ItemDrive item = items.get(position);
            holder.nombre.setText(item.esCarpeta
                    ? item.name
                    : item.name.replace(".pdf", ""));

            holder.itemView.setOnLongClickListener(v -> {
                if (item.esCarpeta) {
                    MainActivity.this.mostrarDialogoConfirmacionEliminacionCarpeta(item);
                    return true;
                } else if (item.descargado) {
                    mostrarDialogoConfirmacionEliminacion(item, holder);
                    return true;
                }
                return false;
            });

            // Configurar el botón según el estado del item
            actualizarBoton(holder, item, position);
        }

        private void actualizarBoton(ViewHolder holder, ItemDrive item, int position) {
            // Asegurar que el botón sea visible y la ProgressBar oculta por defecto
            holder.btnAccion.setVisibility(View.VISIBLE);
            holder.progressCircular.setVisibility(View.GONE);
            holder.btnAccion.setEnabled(true);

            if (item.esCarpeta) {
                holder.btnAccion.setText("Abrir carpeta");
                holder.btnAccion.setOnClickListener(v -> {
                    String nuevaRuta = item.rutaRelativa.isEmpty() ? item.name : item.rutaRelativa + "/" + item.name;
                    MainActivity.this.abrirCarpeta(item.id, nuevaRuta);
                });
            } else {
                if (item.descargado) {
                    holder.btnAccion.setText("Abrir");
                    holder.btnAccion.setOnClickListener(v -> abrirArchivo(item));
                } else {
                    holder.btnAccion.setText("Descargar");
                    holder.btnAccion.setOnClickListener(v -> descargarArchivo(item, holder, position));
                }
            }
        }

        public void descargarArchivo(ItemDrive item, ViewHolder holder, int position) {
            // Verificar que el servicio de Drive esté disponible
            if (driveService == null) {
                Toast.makeText(MainActivity.this, "Error: Servicio de Drive no disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            // Ocultar botón y mostrar ProgressBar
            holder.btnAccion.setVisibility(View.GONE);
            holder.progressCircular.setVisibility(View.VISIBLE);

            new Thread(() -> {
                try {
                    // Crear la estructura de carpetas si es necesario
                    java.io.File carpetaDestino = new java.io.File(getFilesDir(),
                            "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa));
                    if (!carpetaDestino.exists()) {
                        carpetaDestino.mkdirs();
                    }

                    java.io.File archivoLocal = new java.io.File(carpetaDestino, item.name + ".pdf");

                    FileOutputStream output = new FileOutputStream(archivoLocal);

                    // USAR driveService DEL ADAPTER (no MainActivity.this.mDriveService)
                    driveService.files().get(item.id).executeMediaAndDownloadTo(output);
                    output.close();

                    // Descarga completada
                    item.descargado = true;

                    runOnUiThread(() -> {
                        // Ocultar ProgressBar y mostrar botón
                        holder.progressCircular.setVisibility(View.GONE);
                        holder.btnAccion.setVisibility(View.VISIBLE);

                        // Actualizar el botón a "Abrir"
                        holder.btnAccion.setText("Abrir");
                        holder.btnAccion.setEnabled(true);

                        // Configurar el listener para abrir el archivo
                        holder.btnAccion.setOnClickListener(v -> abrirArchivo(item));

                        Toast.makeText(MainActivity.this, "Descargado: " + item.name, Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        // Ocultar ProgressBar y mostrar botón
                        holder.progressCircular.setVisibility(View.GONE);
                        holder.btnAccion.setVisibility(View.VISIBLE);

                        // Si hay error, volver a "Descargar"
                        holder.btnAccion.setText("Descargar");
                        holder.btnAccion.setEnabled(true);

                        // Restaurar el listener de descarga
                        holder.btnAccion.setOnClickListener(v -> descargarArchivo(item, holder, position));

                        Toast.makeText(MainActivity.this, "Error al descargar " + item.name + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error al descargar", e);
                    });
                }
            }).start();
        }


        public void eliminarArchivo(ItemDrive item, ManualAdapter.ViewHolder holder, int position) {
            java.io.File archivoLocal = new java.io.File(getFilesDir(),
                    "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.name + ".pdf");

            if (archivoLocal.exists() && archivoLocal.delete()) {
                item.descargado = false;

                // Asegurar que se vea el botón y no la ProgressBar
                holder.btnAccion.setVisibility(View.VISIBLE);
                holder.progressCircular.setVisibility(View.GONE);

                // Actualizar el botón inmediatamente
                holder.btnAccion.setText("Descargar");
                holder.btnAccion.setEnabled(true);
                holder.btnAccion.setOnClickListener(v -> descargarArchivo(item, holder, position));

                Toast.makeText(MainActivity.this, "Archivo eliminado: " + item.name, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Error al eliminar el archivo", Toast.LENGTH_SHORT).show();
            }
        }

        private void abrirArchivo(ItemDrive item) {
            java.io.File archivoLocal = new java.io.File(getFilesDir(),
                    "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.name + ".pdf");
            if (archivoLocal.exists()) {
                Intent intent = new Intent(MainActivity.this, VisorManualActivity.class);
                intent.putExtra(VisorManualActivity.EXTRA_MANUAL_PATH, archivoLocal.getAbsolutePath());
                startActivity(intent);
            } else {
                // Si el archivo no existe, actualizar el estado
                item.descargado = false;
                notifyItemChanged(items.indexOf(item));
                Toast.makeText(MainActivity.this, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        public void updateData(List<ItemDrive> nuevosItems) {
            items.clear();
            items.addAll(nuevosItems);
            notifyDataSetChanged();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView nombre;
            Button btnAccion;
            ProgressBar progressCircular;
            RelativeLayout progressContainer;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                nombre = itemView.findViewById(R.id.nombre_manual);
                btnAccion = itemView.findViewById(R.id.btnAccion);
                progressCircular = itemView.findViewById(R.id.progress_circular);
                progressContainer = itemView.findViewById(R.id.progress_container);
            }
        }
    }private void mostrarDialogoConfirmacionEliminacion(ItemDrive item, ManualAdapter.ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION) return;

        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar el archivo \"" + item.name + "\"?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    adapter.eliminarArchivo(item, holder, position);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void mostrarDialogoConfirmacionEliminacionCarpeta(ItemDrive item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar carpeta")
                .setMessage("¿Seguro que quieres eliminar la carpeta \"" + item.name + "\" y todo su contenido?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    eliminarCarpeta(item);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }


    private void cargarManualesLocales(String rutaRelativa) {
        new Thread(() -> {
            java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales/" + rutaRelativa);
            List<ItemDrive> itemsLocales = new ArrayList<>();

            if (carpeta.exists()) {
                recorrerCarpeta(carpeta, rutaRelativa, itemsLocales);
            }

            runOnUiThread(() -> {
                items.clear();
                items.addAll(itemsLocales);
                adapter.updateData(itemsLocales);
                currentRelativePath = rutaRelativa;
            });
        }).start();
    }

    private void recorrerCarpeta(java.io.File carpetaActual, String rutaRelativa, List<ItemDrive> lista) {
        java.io.File[] archivos = carpetaActual.listFiles();
        if (archivos != null) {
            for (java.io.File archivo : archivos) {
                if (archivo.isFile() && archivo.getName().endsWith(".pdf")) {
                    String nombreArchivo = archivo.getName().substring(0, archivo.getName().length() - 4);
                    ItemDrive item = new ItemDrive("", nombreArchivo, false, rutaRelativa);
                    item.descargado = true;
                    lista.add(item);
                } else if (archivo.isDirectory()) {
                    // Para carpetas locales, mostrarlas también
                    ItemDrive item = new ItemDrive("", archivo.getName(), true, rutaRelativa);
                    lista.add(item);
                }
            }
        }
    }

    private void listarCarpetaDrive(String folderId, String rutaRelativa) {
        // Verificar que el servicio de Drive esté inicializado
        if (mDriveService == null) {
            Log.d(TAG, "Servicio de Drive no inicializado");
            return;
        }

        new Thread(() -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("'" + folderId + "' in parents and trashed=false")
                        .setFields("files(id, name, mimeType, parents)")
                        .execute();

                List<ItemDrive> itemsDrive = new ArrayList<>();
                if (result.getFiles() != null) {
                    for (File f : result.getFiles()) {
                        boolean esCarpeta = "application/vnd.google-apps.folder".equals(f.getMimeType());

                        boolean descargado = false;
                        String nombreArchivo = f.getName();

                        if (!esCarpeta) {
                            // Asegurar extensión .pdf
                            if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
                                nombreArchivo += ".pdf";
                            }

                            java.io.File archivoLocal = new java.io.File(getFilesDir(),
                                    "Manuales/" + (rutaRelativa.isEmpty() ? "" : rutaRelativa + "/") + nombreArchivo);
                            descargado = archivoLocal.exists();
                        }

                        // Para mostrar en UI quitamos .pdf solo si es archivo
                        String nombreMostrar = esCarpeta ? nombreArchivo : nombreArchivo.replace(".pdf", "");

                        ItemDrive item = new ItemDrive(f.getId(), nombreMostrar, esCarpeta, rutaRelativa);
                        item.descargado = descargado;
                        itemsDrive.add(item);
                    }
                }

                runOnUiThread(() -> {
                    adapter.updateData(itemsDrive);
                    currentRelativePath = rutaRelativa; // Actualizar la ruta actual
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al listar carpeta de Drive: " + e.getMessage());
                // Evitamos Toast molesto, solo log
            }
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
                Log.e(TAG, "Error al iniciar sesión: " + e.toString());
            }
        }
    }

    private void obtenerManualesDesdeDrive(GoogleSignInAccount account) {
        this.currentAccount = account;

        HttpTransport transport = new NetHttpTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                this, Collections.singleton(DriveScopes.DRIVE_READONLY));
        credential.setSelectedAccount(account.getAccount());

        mDriveService = new Drive.Builder(transport, jsonFactory, credential)
                .setApplicationName("Documentation App")
                .build();

        Log.d(TAG, "Servicio de Drive inicializado correctamente.");

        // Actualizar el adapter con el servicio
        adapter.setDriveService(mDriveService);

        listarCarpetaDrive(folderID, "");
    }

    public void abrirCarpeta(String nuevaCarpetaID, String nuevaRutaRelativa) {

        if (!folderID.equals(nuevaCarpetaID) || !currentRelativePath.equals(nuevaRutaRelativa)) {
            pilaCarpetas.push(folderID);
            pilaRutas.push(currentRelativePath);
        }

        // Actualizar a la nueva carpeta
        this.folderID = nuevaCarpetaID;
        this.currentRelativePath = nuevaRutaRelativa;

        // Cargar contenido
        cargarManualesLocales(nuevaRutaRelativa);

        if (currentAccount != null && mDriveService != null) {
            listarCarpetaDrive(folderID, nuevaRutaRelativa);
        }
    }

    public static class ArchivoDescargado {
        private String id;
        private String name;
        private String modifiedTime;

        public ArchivoDescargado() {
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