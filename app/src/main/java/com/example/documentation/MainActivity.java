package com.example.documentation;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import androidx.core.content.ContextCompat;
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

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private static final String TAG = "MainActivity";
    private static final int TIPO_ITEM_NORMAL = 0;
    private static final int TIPO_HEADER_SECCION = 1;

    private GoogleSignInClient mGoogleSignInClient;
    private Drive mDriveService;
    private RecyclerView recyclerView;
    private ManualAdapter adapter;
    private List<ItemDrive> items = new ArrayList<>();
    private List<ItemDrive> todosLosItems = new ArrayList<>();
    private List<ItemDrive> cacheCompletoDrive = new ArrayList<>();
    private String buscarDespuesDeAutenticar = "";
    private GoogleSignInAccount currentAccount;
    private String folderID = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";
    private String currentRelativePath = "";
    private Stack<String> pilaCarpetas = new Stack<>();
    private Stack<String> pilaRutas = new Stack<>();

    public static class ItemDrive {
        public String id;
        public String name;
        public boolean esCarpeta;
        public boolean descargado;
        public String rutaRelativa;
        public boolean esHeaderSeccion; // Nuevo: para identificar headers
        public String tipoSeccion; // "local" o "drive"

        public ItemDrive(String id, String name, boolean esCarpeta, String rutaRelativa) {
            this.id = id;
            this.name = name;
            this.esCarpeta = esCarpeta;
            this.descargado = false;
            this.rutaRelativa = rutaRelativa;
            this.esHeaderSeccion = false;
            this.tipoSeccion = "";
        }

        // Constructor para headers de sección
        public ItemDrive(String nombreSeccion, String tipoSeccion) {
            this.id = "";
            this.name = nombreSeccion;
            this.esCarpeta = false;
            this.descargado = false;
            this.rutaRelativa = "";
            this.esHeaderSeccion = true;
            this.tipoSeccion = tipoSeccion;
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

        adapter = new ManualAdapter(items);
        recyclerView.setAdapter(adapter);

        cargarManualesLocales("");


        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY))
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(view -> iniciarAutenticacionGoogle());

        Button btnBuscar = findViewById(R.id.btn_search_manual);
        btnBuscar.setOnClickListener(v -> mostrarOpcionesBusqueda());

        Button btnInicio = findViewById(R.id.btn_inicio);
        btnInicio.setOnClickListener(v -> irARaiz());
        actualizarBotonInicio();
    }

    private void mostrarOpcionesBusqueda() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buscar manual")
                .setItems(new String[]{"Por nombre", "Por selectores", "Limpiar búsqueda"}, (dialog, which) -> {
                    if (which == 0) {
                        buscarPorNombre();
                    } else if (which == 1) {
                        buscarPorSelectores();
                    } else if (which == 2) {
                        limpiarBusqueda();
                    }
                });
        builder.show();
    }

    private void limpiarBusqueda() {
        sincronizarListaCompleta();
        Toast.makeText(this, "Volviendo a vista de carpetas", Toast.LENGTH_SHORT).show();
        actualizarBotonInicio();
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

    // Agrega esta variable con las otras variables de instancia


    private void filtrarPorNombre(String query) {
        String q = query.toLowerCase().trim();

        if (q.isEmpty()) {
            sincronizarListaCompleta();
            Toast.makeText(this, "Mostrando vista de carpetas", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Buscando: '" + q + "'");


        if (currentAccount == null || mDriveService == null) {
            Log.d(TAG, "No autenticado. Iniciando autenticación automática...");
            buscarDespuesDeAutenticar = q; // Guardar búsqueda
            iniciarAutenticacionGoogle();   // Autenticar automáticamente
            return;
        }

        ejecutarBusquedaCompleta(q);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    obtenerManualesDesdeDrive(account);

                    // ✅ CORRECCIÓN: Esperar a que el cache de Drive esté completamente cargado
                    if (!buscarDespuesDeAutenticar.isEmpty()) {
                        Log.d(TAG, "Autenticación exitosa. Esperando cache de Drive...");

                        // Verificar periódicamente si el cache está listo
                        new android.os.Handler().postDelayed(() -> {
                            verificarYEjecutarBusqueda();
                        }, 1000);
                    }
                }
            } catch (ApiException e) {
                Log.e(TAG, "Error en autenticación automática: " + e.getMessage());
                Toast.makeText(this, "Error al conectar con Google Drive", Toast.LENGTH_SHORT).show();

                if (!buscarDespuesDeAutenticar.isEmpty()) {
                    ejecutarBusquedaSoloLocales(buscarDespuesDeAutenticar);
                    buscarDespuesDeAutenticar = "";
                }
            }
        }
    }

    private void buscarArchivosLocalesRecursivo(java.io.File carpeta, String rutaRelativa, String query, List<ItemDrive> resultados) {
        if (carpeta.exists() && carpeta.isDirectory()) {
            java.io.File[] archivos = carpeta.listFiles();
            if (archivos != null) {
                for (java.io.File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".pdf")) {
                        String nombreArchivo = archivo.getName().replace(".pdf", "");
                        if (nombreArchivo.toLowerCase().contains(query)) {
                            ItemDrive item = new ItemDrive("", nombreArchivo, false, rutaRelativa);
                            item.descargado = true;
                            resultados.add(item);
                            Log.d(TAG, "✓ ENCONTRADO LOCAL: " + nombreArchivo + " en " + rutaRelativa);
                        }
                    } else if (archivo.isDirectory()) {
                        String nuevaRuta = rutaRelativa.isEmpty() ? archivo.getName() : rutaRelativa + "/" + archivo.getName();
                        buscarArchivosLocalesRecursivo(archivo, nuevaRuta, query, resultados);
                    }
                }
            }
        }
    }
    private void buscarPorSelectores() {
        if (currentAccount == null || mDriveService == null) {
            Toast.makeText(this, "Primero conéctate a Google Drive", Toast.LENGTH_SHORT).show();
            iniciarAutenticacionGoogle();
            return;
        }
        obtenerCarpetasNivel1();
    }

    private void obtenerCarpetasNivel1() {
        new Thread(() -> {
            try {
                List<String> carpetasNivel1 = new ArrayList<>();

                // Listar carpetas directamente en la raíz de tu folderID
                FileList result = mDriveService.files().list()
                        .setQ("'" + folderID + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        .setFields("files(id, name)")
                        .execute();

                for (File file : result.getFiles()) {
                    carpetasNivel1.add(file.getName());
                }

                runOnUiThread(() -> mostrarSelectorNivel1(carpetasNivel1));

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener carpetas nivel 1: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "Error al cargar carpetas", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarSelectorNivel1(List<String> tipos) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selectores, null);
        Spinner spinnerTipo = dialogView.findViewById(R.id.spinner_tipo);
        Spinner spinnerMarca = dialogView.findViewById(R.id.spinner_marca);
        Spinner spinnerModelo = dialogView.findViewById(R.id.spinner_modelo);

        // Configurar spinner de tipo
        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapterTipo);

        // Deshabilitar los otros spinners inicialmente
        spinnerMarca.setEnabled(false);
        spinnerModelo.setEnabled(false);
        spinnerMarca.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
        spinnerModelo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));

        // Cuando se selecciona un tipo, cargar marcas
        spinnerTipo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String tipoSeleccionado = parent.getItemAtPosition(position).toString();
                obtenerCarpetasNivel2(tipoSeleccionado, spinnerMarca, spinnerModelo);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buscar por categorías");
        builder.setView(dialogView);
        builder.setPositiveButton("Buscar", (d, w) -> {
            String tipo = spinnerTipo.getSelectedItem() != null ? spinnerTipo.getSelectedItem().toString() : "";
            String marca = spinnerMarca.getSelectedItem() != null ? spinnerMarca.getSelectedItem().toString() : "";
            String modelo = spinnerModelo.getSelectedItem() != null ? spinnerModelo.getSelectedItem().toString() : "";
            ejecutarBusquedaPorCarpetas(tipo, marca, modelo);
        });
        builder.setNegativeButton("Cancelar", (d, w) -> d.dismiss());
        builder.show();
    }
    private void obtenerCarpetasNivel2(String tipoSeleccionado, Spinner spinnerMarca, Spinner spinnerModelo) {
        new Thread(() -> {
            try {
                // Primero encontrar el ID de la carpeta del tipo seleccionado
                String queryTipo = "name = '" + tipoSeleccionado + "' and '" + folderID + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                FileList resultTipo = mDriveService.files().list().setQ(queryTipo).execute();

                if (resultTipo.getFiles().isEmpty()) return;

                String idTipo = resultTipo.getFiles().get(0).getId();
                List<String> marcas = new ArrayList<>();

                // Obtener carpetas dentro del tipo (marcas)
                FileList resultMarcas = mDriveService.files().list()
                        .setQ("'" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        .setFields("files(name)")
                        .execute();

                for (File file : resultMarcas.getFiles()) {
                    marcas.add(file.getName());
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapterMarca = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, marcas);
                    spinnerMarca.setAdapter(adapterMarca);
                    spinnerMarca.setEnabled(true);

                    // Resetear modelo
                    spinnerModelo.setAdapter(new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
                    spinnerModelo.setEnabled(false);

                    // Configurar listener para marcas
                    spinnerMarca.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String marcaSeleccionada = parent.getItemAtPosition(position).toString();
                            obtenerCarpetasNivel3(idTipo, marcaSeleccionada, spinnerModelo);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener marcas: " + e.getMessage());
            }
        }).start();
    }
    private void obtenerCarpetasNivel3(String idTipo, String marcaSeleccionada, Spinner spinnerModelo) {
        new Thread(() -> {
            try {
                // Encontrar ID de la marca seleccionada
                String queryMarca = "name = '" + marcaSeleccionada + "' and '" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                FileList resultMarca = mDriveService.files().list().setQ(queryMarca).execute();

                if (resultMarca.getFiles().isEmpty()) return;

                String idMarca = resultMarca.getFiles().get(0).getId();
                List<String> modelos = new ArrayList<>();

                // Obtener carpetas dentro de la marca (modelos)
                FileList resultModelos = mDriveService.files().list()
                        .setQ("'" + idMarca + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                        .setFields("files(name)")
                        .execute();

                for (File file : resultModelos.getFiles()) {
                    modelos.add(file.getName());
                }

                runOnUiThread(() -> {
                    if (!modelos.isEmpty()) {
                        ArrayAdapter<String> adapterModelo = new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_dropdown_item, modelos);
                        spinnerModelo.setAdapter(adapterModelo);
                        spinnerModelo.setEnabled(true);
                    } else {
                        spinnerModelo.setEnabled(false);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener modelos: " + e.getMessage());
            }
        }).start();
    }

    private void ejecutarBusquedaPorCarpetas(String tipo, String marca, String modelo) {
        if (tipo.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un tipo", Toast.LENGTH_SHORT).show();
            return;
        }

        // Navegar directamente a la carpeta seleccionada
        new Thread(() -> {
            try {
                String currentFolderId = folderID;

                // Navegar al tipo
                String queryTipo = "name = '" + tipo + "' and '" + currentFolderId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                FileList resultTipo = mDriveService.files().list().setQ(queryTipo).execute();
                if (resultTipo.getFiles().isEmpty()) return;
                String idTipo = resultTipo.getFiles().get(0).getId();

                // Si hay marca seleccionada, navegar a ella
                if (!marca.isEmpty()) {
                    String queryMarca = "name = '" + marca + "' and '" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                    FileList resultMarca = mDriveService.files().list().setQ(queryMarca).execute();
                    if (!resultMarca.getFiles().isEmpty()) {
                        idTipo = resultMarca.getFiles().get(0).getId();

                        // Si hay modelo seleccionado, navegar a él
                        if (!modelo.isEmpty()) {
                            String queryModelo = "name = '" + modelo + "' and '" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                            FileList resultModelo = mDriveService.files().list().setQ(queryModelo).execute();
                            if (!resultModelo.getFiles().isEmpty()) {
                                idTipo = resultModelo.getFiles().get(0).getId();
                            }
                        }
                    }
                }

                final String finalFolderId = idTipo;
                final String rutaFinal = tipo + (marca.isEmpty() ? "" : "/" + marca) + (modelo.isEmpty() ? "" : "/" + modelo);

                runOnUiThread(() -> {
                    abrirCarpeta(finalFolderId, rutaFinal);
                    Toast.makeText(MainActivity.this, "Navegando a: " + rutaFinal, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error en búsqueda por carpetas: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error al navegar", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void filtrarPorSelectores(String tipo, String marca, String modelo) {
        List<ItemDrive> resultados = new ArrayList<>();
        List<ItemDrive> localesFiltrados = new ArrayList<>();
        List<ItemDrive> driveFiltrados = new ArrayList<>();

        // Filtrar locales
        for (ItemDrive item : todosLosItems) {
            if (!item.esCarpeta && coincideConSelectores(item, tipo, marca, modelo)) {
                localesFiltrados.add(item);
            }
        }

        // Filtrar Drive
        for (ItemDrive item : cacheCompletoDrive) {
            if (!item.esCarpeta && !item.descargado && coincideConSelectores(item, tipo, marca, modelo)) {
                // Verificar que no esté en locales
                boolean existeEnLocales = false;
                for (ItemDrive local : localesFiltrados) {
                    if (local.name.equals(item.name) && local.rutaRelativa.equals(item.rutaRelativa)) {
                        existeEnLocales = true;
                        break;
                    }
                }
                if (!existeEnLocales) {
                    driveFiltrados.add(item);
                }
            }
        }

        // Organizar con secciones
        if (!localesFiltrados.isEmpty()) {
            resultados.add(new ItemDrive("📁 MANUALES LOCALES (" + localesFiltrados.size() + ")", "local"));
            resultados.addAll(localesFiltrados);
        }

        if (!driveFiltrados.isEmpty()) {
            resultados.add(new ItemDrive("☁️ MANUALES EN DRIVE (" + driveFiltrados.size() + ")", "drive"));
            resultados.addAll(driveFiltrados);
        }

        if (resultados.isEmpty()) {
            Toast.makeText(this, "No se encontraron manuales con los filtros seleccionados", Toast.LENGTH_SHORT).show();
        }

        items.clear();
        items.addAll(resultados);
        adapter.updateData(resultados);
    }

    private void verificarYEjecutarBusqueda() {
        if (cacheCompletoDrive.isEmpty()) {
            Log.d(TAG, "Cache de Drive aún vacío. Reintentando en 1 segundo...");
            // Reintentar después de 1 segundo
            new android.os.Handler().postDelayed(() -> {
                verificarYEjecutarBusqueda();
            }, 1000);
        } else {
            Log.d(TAG, "✅ Cache de Drive listo! Ejecutando búsqueda: " + buscarDespuesDeAutenticar);
            ejecutarBusquedaCompleta(buscarDespuesDeAutenticar);
            buscarDespuesDeAutenticar = "";

            Toast.makeText(MainActivity.this,
                    "Búsqueda completada en Drive y locales",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private boolean coincideConSelectores(ItemDrive item, String tipo, String marca, String modelo) {
        String[] partes = item.rutaRelativa.split("/");
        return (tipo.isEmpty() || (partes.length > 0 && partes[0].equals(tipo))) &&
                (marca.isEmpty() || (partes.length > 1 && partes[1].equals(marca))) &&
                (modelo.isEmpty() || (partes.length > 2 && partes[2].equals(modelo)));
    }
    private void irARaiz() {
        Log.d(TAG, "=== NAVEGANDO A RAÍZ ===");
        Log.d(TAG, "FolderID actual: " + folderID);
        Log.d(TAG, "CurrentPath actual: " + currentRelativePath);
        Log.d(TAG, "Tamaño pila carpetas: " + pilaCarpetas.size());
        Log.d(TAG, "Tamaño pila rutas: " + pilaRutas.size());

        // Limpiar completamente las pilas de navegación
        pilaCarpetas.clear();
        pilaRutas.clear();

        // Restablecer a los valores iniciales de la raíz
        String folderIDRaiz = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW"; // ID de la carpeta raíz de Drive
        this.folderID = folderIDRaiz;
        this.currentRelativePath = "";

        // Volver a inicializar las pilas con la raíz
        pilaCarpetas.push(folderIDRaiz);
        pilaRutas.push("");

        Log.d(TAG, "Nuevo FolderID: " + folderID);
        Log.d(TAG, "Nuevo CurrentPath: " + currentRelativePath);
        Log.d(TAG, "Nueva pila carpetas: " + pilaCarpetas.size());
        Log.d(TAG, "Nueva pila rutas: " + pilaRutas.size());

        // Mostrar contenido de la raíz
        sincronizarListaCompleta();

        Toast.makeText(this, "🏠 Volviendo al inicio", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Navegación resetada. Carpeta actual: raíz");
    }

    private void actualizarBotonInicio() {
        Button btnInicio = findViewById(R.id.btn_inicio);

        boolean estaEnRaizPura = currentRelativePath.isEmpty() && pilaCarpetas.size() <= 1;

        boolean esModoBusqueda;

        if (items.isEmpty()) {
            esModoBusqueda = false;
        } else {
            boolean tieneHeader = items.get(0).esHeaderSeccion;

            boolean hayCarpetas = false;
            for (ItemDrive item : items) {
                if (item.esCarpeta) {
                    hayCarpetas = true;
                    break;
                }
            }

            esModoBusqueda = tieneHeader || !hayCarpetas;
        }

        if (estaEnRaizPura && !esModoBusqueda) {
            btnInicio.setEnabled(false);
            btnInicio.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.darker_gray)));
            btnInicio.setText("🏠 En inicio");
            Log.d(TAG, "Botón INICIO: Deshabilitado (en raíz)");
        } else {
            btnInicio.setEnabled(true);
            btnInicio.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, android.R.color.holo_blue_light)));
            btnInicio.setText("🏠 Inicio");
            Log.d(TAG, "Botón INICIO: Habilitado (ruta: " + currentRelativePath +
                    ", pila: " + pilaCarpetas.size() + ")");
        }
    }

    @Override
    public void onBackPressed() {
        if (pilaCarpetas.size() > 1) {
            pilaCarpetas.pop();
            pilaRutas.pop();

            String carpetaAnterior = pilaCarpetas.peek();
            String rutaAnterior = pilaRutas.peek();

            this.folderID = carpetaAnterior;
            this.currentRelativePath = rutaAnterior;

            sincronizarListaCompleta();
            actualizarBotonInicio();
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
                sincronizarListaCompleta();
            } else {
                Toast.makeText(this, "No se pudo eliminar la carpeta", Toast.LENGTH_SHORT).show();
            }
        }
    }

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

    private class ManualAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private List<ItemDrive> items;

        public ManualAdapter(List<ItemDrive> items) {
            this.items = items;
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).esHeaderSeccion ? TIPO_HEADER_SECCION : TIPO_ITEM_NORMAL;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TIPO_HEADER_SECCION) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_header_seccion, parent, false);
                return new HeaderViewHolder(view);
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_manual, parent, false);
                return new ItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            ItemDrive item = items.get(position);

            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).bind(item);
            } else if (holder instanceof ItemViewHolder) {
                ((ItemViewHolder) holder).bind(item, position);
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

        public class HeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvHeader;

            public HeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvHeader = itemView.findViewById(R.id.tv_header_seccion);
            }

            public void bind(ItemDrive item) {
                tvHeader.setText(item.name);
            }
        }

        public class ItemViewHolder extends RecyclerView.ViewHolder {
            TextView nombre;
            TextView ruta;
            Button btnAccion;
            ProgressBar progressCircular;

            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                nombre = itemView.findViewById(R.id.nombre_manual);
                ruta = itemView.findViewById(R.id.ruta_manual);
                btnAccion = itemView.findViewById(R.id.btnAccion);
                progressCircular = itemView.findViewById(R.id.progress_circular);

            }

            public void bind(ItemDrive item, int position) {
                nombre.setText(item.esCarpeta ? item.name : item.name.replace(".pdf", ""));

                if (item.rutaRelativa != null && !item.rutaRelativa.isEmpty()) {
                    ruta.setText("Ruta: " + item.rutaRelativa);
                    ruta.setVisibility(View.VISIBLE);
                } else {
                    ruta.setVisibility(View.GONE);
                }

                itemView.setOnLongClickListener(v -> {
                    if (item.esCarpeta) {
                        MainActivity.this.mostrarDialogoConfirmacionEliminacionCarpeta(item);
                        return true;
                    } else if (item.descargado) {
                        mostrarDialogoConfirmacionEliminacion(item, position);
                        return true;
                    }
                    return false;
                });

                actualizarBoton(item, position);
            }

            private void actualizarBoton(ItemDrive item, int position) {
                btnAccion.setVisibility(View.VISIBLE);
                progressCircular.setVisibility(View.GONE);
                btnAccion.setEnabled(true);

                if (item.esCarpeta) {
                    btnAccion.setText("Abrir carpeta");
                    btnAccion.setOnClickListener(v -> {
                        String nuevaRuta = item.rutaRelativa.isEmpty() ? item.name : item.rutaRelativa + "/" + item.name;
                        MainActivity.this.abrirCarpeta(item.id, nuevaRuta);
                    });
                } else {
                    if (item.descargado) {
                        btnAccion.setText("Abrir");
                        btnAccion.setOnClickListener(v -> abrirArchivo(item));
                    } else {
                        btnAccion.setText("Descargar");
                        btnAccion.setOnClickListener(v -> descargarArchivo(item, position));
                    }
                }
            }

            public void descargarArchivo(ItemDrive item, int position) {
                if (mDriveService == null) {
                    Toast.makeText(MainActivity.this, "Error: Servicio de Drive no disponible", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnAccion.setVisibility(View.GONE);
                progressCircular.setVisibility(View.VISIBLE);

                new Thread(() -> {
                    try {
                        java.io.File carpetaDestino = new java.io.File(getFilesDir(),
                                "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa));
                        if (!carpetaDestino.exists()) {
                            carpetaDestino.mkdirs();
                        }

                        java.io.File archivoLocal = new java.io.File(carpetaDestino, item.name + ".pdf");
                        FileOutputStream output = new FileOutputStream(archivoLocal);

                        mDriveService.files().get(item.id).executeMediaAndDownloadTo(output);
                        output.close();

                        item.descargado = true;

                        runOnUiThread(() -> {
                            progressCircular.setVisibility(View.GONE);
                            btnAccion.setVisibility(View.VISIBLE);
                            btnAccion.setText("Abrir");
                            btnAccion.setEnabled(true);
                            btnAccion.setOnClickListener(v -> abrirArchivo(item));

                            actualizarItemDescargado(item, position);

                            Toast.makeText(MainActivity.this, "Descargado: " + item.name, Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> {
                            progressCircular.setVisibility(View.GONE);
                            btnAccion.setVisibility(View.VISIBLE);
                            btnAccion.setText("Descargar");
                            btnAccion.setEnabled(true);
                            btnAccion.setOnClickListener(v -> descargarArchivo(item, position));

                            Toast.makeText(MainActivity.this, "Error al descargar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }).start();
            }

            public void eliminarArchivo(ItemDrive item, int position) {
                java.io.File archivoLocal = new java.io.File(getFilesDir(),
                        "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.name + ".pdf");

                if (archivoLocal.exists() && archivoLocal.delete()) {
                    item.descargado = false;

                    btnAccion.setVisibility(View.VISIBLE);
                    progressCircular.setVisibility(View.GONE);
                    btnAccion.setText("Descargar");
                    btnAccion.setEnabled(true);
                    btnAccion.setOnClickListener(v -> descargarArchivo(item, position));

                    actualizarItemEliminado(item, position);

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
                    item.descargado = false;
                    Toast.makeText(MainActivity.this, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void mostrarDialogoConfirmacionEliminacion(ItemDrive item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar el archivo \"" + item.name + "\"?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    ((ManualAdapter.ItemViewHolder) recyclerView.findViewHolderForAdapterPosition(position))
                            .eliminarArchivo(item, position);
                })
                .setNegativeButton("No", null)
                .show();
    }
    private void actualizarItemDescargado(ItemDrive itemDescargado, int position) {
        // 1. Actualizar el estado del item en la lista actual
        if (position >= 0 && position < items.size()) {
            items.get(position).descargado = true;
            adapter.notifyItemChanged(position);
        }


        for (ItemDrive item : cacheCompletoDrive) {
            if (item.id.equals(itemDescargado.id)) {
                item.descargado = true;
                break;
            }
        }


        for (ItemDrive item : todosLosItems) {
            if (item.name.equals(itemDescargado.name) && item.rutaRelativa.equals(itemDescargado.rutaRelativa)) {
                item.descargado = true;
                break;
            }
        }

        Log.d(TAG, "Item actualizado: " + itemDescargado.name + " - Descargado: true");
    }
    private void actualizarItemEliminado(ItemDrive itemEliminado, int position) {
        // 1. Actualizar el estado del item en la lista actual
        if (position >= 0 && position < items.size()) {
            items.get(position).descargado = false;
            adapter.notifyItemChanged(position);
        }

        for (ItemDrive item : cacheCompletoDrive) {
            if (item.id.equals(itemEliminado.id)) {
                item.descargado = false;
                break;
            }
        }

        for (ItemDrive item : todosLosItems) {
            if (item.name.equals(itemEliminado.name) && item.rutaRelativa.equals(itemEliminado.rutaRelativa)) {
                item.descargado = false;
                break;
            }
        }

        Log.d(TAG, "Item actualizado: " + itemEliminado.name + " - Descargado: false");
    }

    private void mostrarDialogoConfirmacionEliminacionCarpeta(ItemDrive item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar carpeta")
                .setMessage("¿Seguro que quieres eliminar la carpeta \"" + item.name + "\" y todo su contenido?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarCarpeta(item))
                .setNegativeButton("No", null)
                .show();
    }

    private void cargarManualesLocales(String rutaRelativa) {
        new Thread(() -> {
            java.io.File carpeta = new java.io.File(getFilesDir(), "Manuales/" + rutaRelativa);
            List<ItemDrive> itemsLocales = new ArrayList<>();

            Log.d(TAG, "=== CARGANDO LOCALES ===");
            Log.d(TAG, "Ruta relativa: " + rutaRelativa);
            Log.d(TAG, "Ruta absoluta: " + carpeta.getAbsolutePath());
            Log.d(TAG, "Carpeta existe: " + carpeta.exists());

            if (carpeta.exists()) {
                java.io.File[] archivos = carpeta.listFiles();
                Log.d(TAG, "Número de elementos: " + (archivos != null ? archivos.length : 0));

                if (archivos != null) {
                    // Primero agregar carpetas
                    for (java.io.File archivo : archivos) {
                        if (archivo.isDirectory()) {
                            ItemDrive itemCarpeta = new ItemDrive("", archivo.getName(), true, rutaRelativa);
                            itemsLocales.add(itemCarpeta);
                            Log.d(TAG, "✓ Carpeta: " + archivo.getName());
                        }
                    }

                    // Luego agregar archivos PDF
                    for (java.io.File archivo : archivos) {
                        if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".pdf")) {
                            String nombreArchivo = archivo.getName().replace(".pdf", "");
                            ItemDrive item = new ItemDrive("", nombreArchivo, false, rutaRelativa);
                            item.descargado = true;
                            itemsLocales.add(item);
                            Log.d(TAG, "✓ PDF: " + nombreArchivo);
                        }
                    }
                }
            } else {
                Log.d(TAG, "❌ La carpeta no existe: " + carpeta.getAbsolutePath());
            }

            runOnUiThread(() -> {
                todosLosItems.clear();
                todosLosItems.addAll(itemsLocales);

                items.clear();
                items.addAll(itemsLocales);
                adapter.updateData(itemsLocales);
                currentRelativePath = rutaRelativa;

                Log.d(TAG, "=== CARPETA CARGADA: " + itemsLocales.size() + " elementos ===");

                if (itemsLocales.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Carpeta vacía", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Cargados: " + itemsLocales.size() + " elementos",
                            Toast.LENGTH_SHORT).show();
                }

                actualizarBotonInicio();
            });
        }).start();
    }

    private void ejecutarBusquedaCompleta(String q) {
        new Thread(() -> {
            List<ItemDrive> resultadosBusqueda = new ArrayList<>();
            List<ItemDrive> driveEncontrados = new ArrayList<>();

            // 1. BUSCAR EN ARCHIVOS LOCALES
            java.io.File carpetaRaiz = new java.io.File(getFilesDir(), "Manuales");
            if (carpetaRaiz.exists()) {
                buscarArchivosLocalesRecursivo(carpetaRaiz, "", q, resultadosBusqueda);
            }

            // 2. BUSCAR EN CACHE DE DRIVE (si está autenticado)
            if (currentAccount != null && mDriveService != null) {
                for (ItemDrive item : cacheCompletoDrive) {
                    if (!item.esCarpeta) {
                        String nombreBusqueda = item.name.toLowerCase();
                        if (nombreBusqueda.contains(q)) {
                            driveEncontrados.add(item);
                            Log.d(TAG, "✓ ENCONTRADO EN DRIVE: " + item.name);
                        }
                    }
                }
            }

            // 3. ORGANIZAR RESULTADOS
            runOnUiThread(() -> {
                List<ItemDrive> resultadosOrganizados = new ArrayList<>();

                if (!resultadosBusqueda.isEmpty()) {
                    resultadosOrganizados.add(new ItemDrive("📁 MANUALES LOCALES (" + resultadosBusqueda.size() + ")", "local"));
                    resultadosOrganizados.addAll(resultadosBusqueda);
                }

                if (!driveEncontrados.isEmpty()) {
                    resultadosOrganizados.add(new ItemDrive("☁️ MANUALES EN DRIVE (" + driveEncontrados.size() + ")", "drive"));
                    resultadosOrganizados.addAll(driveEncontrados);
                }

                if (resultadosOrganizados.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No se encontró: '" + q + "'", Toast.LENGTH_LONG).show();
                } else {
                    String mensaje = "Encontrados: " + resultadosBusqueda.size() + " locales";
                    if (!driveEncontrados.isEmpty()) {
                        mensaje += ", " + driveEncontrados.size() + " en Drive";
                    }
                    Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                }

                items.clear();
                items.addAll(resultadosOrganizados);
                adapter.updateData(resultadosOrganizados);
                actualizarBotonInicio();

                Log.d(TAG, "Resultados búsqueda - Locales: " + resultadosBusqueda.size() + ", Drive: " + driveEncontrados.size());
            });
        }).start();
    }

    private void ejecutarBusquedaSoloLocales(String query) {
        String q = query.toLowerCase().trim();

        new Thread(() -> {
            List<ItemDrive> resultadosBusqueda = new ArrayList<>();

            java.io.File carpetaRaiz = new java.io.File(getFilesDir(), "Manuales");
            if (carpetaRaiz.exists()) {
                buscarArchivosLocalesRecursivo(carpetaRaiz, "", q, resultadosBusqueda);
            }

            runOnUiThread(() -> {
                List<ItemDrive> resultadosOrganizados = new ArrayList<>();

                if (!resultadosBusqueda.isEmpty()) {
                    resultadosOrganizados.add(new ItemDrive("📁 MANUALES LOCALES (" + resultadosBusqueda.size() + ")", "local"));
                    resultadosOrganizados.addAll(resultadosBusqueda);
                }

                if (resultadosOrganizados.isEmpty()) {
                    Toast.makeText(MainActivity.this, "No se encontró: '" + query + "'", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this,
                            "Encontrados: " + resultadosBusqueda.size() + " locales",
                            Toast.LENGTH_SHORT).show();
                }

                items.clear();
                items.addAll(resultadosOrganizados);
                adapter.updateData(resultadosOrganizados);
                actualizarBotonInicio();
            });
        }).start();
    }

    private void listarCarpetaDrive(String folderId, String rutaRelativa) {
        if (mDriveService == null) return;

        new Thread(() -> {
            try {
                FileList result = mDriveService.files().list()
                        .setQ("'" + folderId + "' in parents and trashed=false")
                        .setFields("files(id, name, mimeType)")
                        .execute();

                List<ItemDrive> itemsDrive = new ArrayList<>();
                if (result.getFiles() != null) {
                    for (File f : result.getFiles()) {
                        boolean esCarpeta = "application/vnd.google-apps.folder".equals(f.getMimeType());
                        boolean descargado = false;
                        String nombreArchivo = f.getName();

                        if (!esCarpeta) {
                            if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
                                nombreArchivo += ".pdf";
                            }
                            java.io.File archivoLocal = new java.io.File(getFilesDir(),
                                    "Manuales/" + (rutaRelativa.isEmpty() ? "" : rutaRelativa + "/") + nombreArchivo);
                            descargado = archivoLocal.exists();
                        }

                        String nombreMostrar = esCarpeta ? nombreArchivo : nombreArchivo.replace(".pdf", "");
                        ItemDrive item = new ItemDrive(f.getId(), nombreMostrar, esCarpeta, rutaRelativa);
                        item.descargado = descargado;
                        itemsDrive.add(item);
                    }
                }

                runOnUiThread(() -> {
                    items.clear();
                    items.addAll(itemsDrive);
                    adapter.updateData(itemsDrive);
                    currentRelativePath = rutaRelativa;
                    actualizarBotonInicio();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error al listar carpeta de Drive: " + e.getMessage());
            }
        }).start();
    }

    private void cargarTodoDriveRecursivo(String folderId, String ruta, List<ItemDrive> acumulador) {
        if (mDriveService == null) return;

        try {
            FileList result = mDriveService.files().list()
                    .setQ("'" + folderId + "' in parents and trashed=false")
                    .setFields("files(id, name, mimeType)")
                    .execute();

            if (result.getFiles() != null) {
                Log.d(TAG, "📂 Cargando carpeta Drive: " + folderId + " - Elementos: " + result.getFiles().size());

                for (File f : result.getFiles()) {
                    boolean esCarpeta = "application/vnd.google-apps.folder".equals(f.getMimeType());
                    String nuevaRuta = ruta.isEmpty() ? f.getName() : ruta + "/" + f.getName();

                    if (esCarpeta) {
                        Log.d(TAG, "📁 Carpeta encontrada: " + f.getName());
                        cargarTodoDriveRecursivo(f.getId(), nuevaRuta, acumulador);
                    } else {
                        String nombreArchivo = f.getName();
                        if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
                            nombreArchivo += ".pdf";
                        }
                        java.io.File archivoLocal = new java.io.File(getFilesDir(), "Manuales/" + nuevaRuta);
                        boolean descargado = archivoLocal.exists();

                        ItemDrive item = new ItemDrive(f.getId(), nombreArchivo.replace(".pdf", ""), false, ruta);
                        item.descargado = descargado;
                        acumulador.add(item);

                        Log.d(TAG, "✅ Archivo Drive agregado al cache: " + item.name + " - Ruta: " + ruta);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error en carga recursiva de Drive: " + e.getMessage());
        }
    }



    private void sincronizarListaCompleta() {
        Log.d(TAG, "=== SINCRONIZANDO LISTA COMPLETA ===");
        Log.d(TAG, "FolderID: " + folderID);
        Log.d(TAG, "CurrentPath: " + currentRelativePath);
        Log.d(TAG, "CurrentAccount: " + (currentAccount != null));
        Log.d(TAG, "DriveService: " + (mDriveService != null));
        Log.d(TAG, "Cache actual: " + cacheCompletoDrive.size() + " archivos");

        if (currentAccount != null && mDriveService != null) {
            Log.d(TAG, "🔄 MODO: Drive + Locales");

            // Cargar vista normal de carpeta actual
            listarCarpetaDrive(folderID, currentRelativePath);

            // CACHE COMPLETO EN BACKGROUND
            new Thread(() -> {
                Log.d(TAG, "🔄 Iniciando carga del cache de Drive...");
                List<ItemDrive> cacheCompleto = new ArrayList<>();
                cargarTodoDriveRecursivo(folderID, "", cacheCompleto);

                runOnUiThread(() -> {
                    cacheCompletoDrive.clear();
                    cacheCompletoDrive.addAll(cacheCompleto);
                    Log.d(TAG, "✅ Cache Drive actualizado: " + cacheCompleto.size() + " archivos");

                    // ✅ NUEVO: Verificar archivos en el cache
                    for (int i = 0; i < Math.min(5, cacheCompletoDrive.size()); i++) {
                        ItemDrive item = cacheCompletoDrive.get(i);
                        Log.d(TAG, "📄 Cache[" + i + "]: " + item.name + " - Ruta: " + item.rutaRelativa);
                    }

                    actualizarBotonInicio();
                });
            }).start();

        } else {
            Log.d(TAG, "📱 MODO: Solo Locales");
            cargarManualesLocales(currentRelativePath);
        }
        actualizarBotonInicio();
    }

    private void iniciarAutenticacionGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
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

        sincronizarListaCompleta();
        actualizarBotonInicio();
    }

    public void abrirCarpeta(String nuevaCarpetaID, String nuevaRutaRelativa) {
        Log.d(TAG, "=== ABRIENDO CARPETA ===");
        Log.d(TAG, "Carpeta ID: " + nuevaCarpetaID);
        Log.d(TAG, "Ruta: " + nuevaRutaRelativa);

        if (!folderID.equals(nuevaCarpetaID) || !currentRelativePath.equals(nuevaRutaRelativa)) {
            pilaCarpetas.push(folderID);
            pilaRutas.push(currentRelativePath);
            Log.d(TAG, "Agregado a pila. Tamaño pila: " + pilaCarpetas.size());
        }

        this.folderID = nuevaCarpetaID;
        this.currentRelativePath = nuevaRutaRelativa;

        Log.d(TAG, "Navegando a: " + nuevaRutaRelativa);
        sincronizarListaCompleta();
        actualizarBotonInicio();
    }
}