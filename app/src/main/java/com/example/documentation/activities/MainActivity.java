package com.example.documentation.activities;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.documentation.R;
import com.example.documentation.adapters.ManualAdapter;
import com.example.documentation.models.Manual;
import com.example.documentation.repositories.DriveRepository;
import com.example.documentation.repositories.LocalFilesRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.example.documentation.viewmodels.MainViewModel;
import androidx.lifecycle.ViewModelProvider;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1000;
    private static final String TAG = "MainActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private Drive mDriveService;
    private DriveRepository driveRepository;
    private LocalFilesRepository localFilesRepository;
    private RecyclerView recyclerView;
    private ManualAdapter adapter;
    private List<Manual> items = new ArrayList<>();
    private List<Manual> todosLosItems = new ArrayList<>();
    private List<Manual> cacheCompletoDrive = new ArrayList<>();
    private String buscarDespuesDeAutenticar = "";
    private GoogleSignInAccount currentAccount;
    private String folderID = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";
    private String currentRelativePath = "";
    private Stack<String> pilaCarpetas = new Stack<>();
    private Stack<String> pilaRutas = new Stack<>();
    private MainViewModel viewModel;

    // Métodos para que el adapter acceda a los repositories
    public DriveRepository getDriveRepository() {
        return driveRepository;
    }

    public LocalFilesRepository getLocalFilesRepository() {
        return localFilesRepository;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        this.folderID = viewModel.currentFolderID;
        this.currentRelativePath = viewModel.currentRelativePath;
        this.pilaCarpetas = viewModel.pilaCarpetas;
        this.pilaRutas = viewModel.pilaRutas;
        this.buscarDespuesDeAutenticar = viewModel.buscarDespuesDeAutenticar;
        this.cacheCompletoDrive = viewModel.cacheCompletoDrive;

        // 3. INICIALIZAR SOLO SI LAS PILAS ESTÁN VACÍAS (EVITA DUPLICADOS)
        if (pilaCarpetas.isEmpty()) {
            pilaCarpetas.push(folderID);
            pilaRutas.push("");


            viewModel.pilaCarpetas = this.pilaCarpetas;
            viewModel.pilaRutas = this.pilaRutas;
        }


        localFilesRepository = new LocalFilesRepository(this);

        recyclerView = findViewById(R.id.rv_manuales);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ManualAdapter(items, this);
        recyclerView.setAdapter(adapter);


        sincronizarListaCompleta();


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

    private void filtrarPorNombre(String query) {
        String q = query.toLowerCase().trim();

        if (q.isEmpty()) {
            sincronizarListaCompleta();
            Toast.makeText(this, "Mostrando vista de carpetas", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "Buscando: '" + q + "'");

        if (currentAccount == null || driveRepository == null) {
            Log.d(TAG, "No autenticado. Iniciando autenticación automática...");
            buscarDespuesDeAutenticar = q;
            iniciarAutenticacionGoogle();
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

                    if (!buscarDespuesDeAutenticar.isEmpty()) {
                        Log.d(TAG, "Autenticación exitosa. Esperando cache de Drive...");
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

    private void buscarPorSelectores() {
        if (currentAccount == null || driveRepository == null) {
            Toast.makeText(this, "Primero conéctate a Google Drive", Toast.LENGTH_SHORT).show();
            iniciarAutenticacionGoogle();
            return;
        }
        obtenerCarpetasNivel1();
    }

    private void obtenerCarpetasNivel1() {
        new Thread(() -> {
            List<String> carpetasNivel1 = driveRepository.obtenerCarpetasNivel1(folderID);

            runOnUiThread(() -> {
                if (carpetasNivel1.isEmpty()) {
                    Toast.makeText(this, "Error al cargar carpetas", Toast.LENGTH_SHORT).show();
                } else {
                    mostrarSelectorNivel1(carpetasNivel1);
                }
            });
        }).start();
    }

    private void mostrarSelectorNivel1(List<String> tipos) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_selectores, null);
        Spinner spinnerTipo = dialogView.findViewById(R.id.spinner_tipo);
        Spinner spinnerMarca = dialogView.findViewById(R.id.spinner_marca);
        Spinner spinnerModelo = dialogView.findViewById(R.id.spinner_modelo);

        ArrayAdapter<String> adapterTipo = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tipos);
        spinnerTipo.setAdapter(adapterTipo);

        spinnerMarca.setEnabled(false);
        spinnerModelo.setEnabled(false);
        spinnerMarca.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
        spinnerModelo.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));

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
            List<String> marcas = driveRepository.obtenerCarpetasNivel2(folderID, tipoSeleccionado);

            runOnUiThread(() -> {
                ArrayAdapter<String> adapterMarca = new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, marcas);
                spinnerMarca.setAdapter(adapterMarca);
                spinnerMarca.setEnabled(true);

                spinnerModelo.setAdapter(new ArrayAdapter<>(MainActivity.this,
                        android.R.layout.simple_spinner_dropdown_item, new ArrayList<>()));
                spinnerModelo.setEnabled(false);

                spinnerMarca.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String marcaSeleccionada = parent.getItemAtPosition(position).toString();
                        obtenerCarpetasNivel3(tipoSeleccionado, marcaSeleccionada, spinnerModelo);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {}
                });
            });
        }).start();
    }

    private void obtenerCarpetasNivel3(String tipoSeleccionado, String marcaSeleccionada, Spinner spinnerModelo) {
        new Thread(() -> {
            // Primero necesitamos obtener el ID del tipo
            String idTipo = obtenerIdCarpeta(folderID, tipoSeleccionado);
            if (idTipo == null) return;

            List<String> modelos = driveRepository.obtenerCarpetasNivel3(idTipo, marcaSeleccionada);

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
        }).start();
    }

    private String obtenerIdCarpeta(String parentId, String nombreCarpeta) {
        try {
            com.google.api.services.drive.model.FileList result = mDriveService.files().list()
                    .setQ("name = '" + nombreCarpeta + "' and '" + parentId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    .execute();

            if (!result.getFiles().isEmpty()) {
                return result.getFiles().get(0).getId();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener ID de carpeta: " + e.getMessage());
        }
        return null;
    }

    private void ejecutarBusquedaPorCarpetas(String tipo, String marca, String modelo) {
        if (tipo.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un tipo", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            String folderIdDestino = driveRepository.navegarACarpeta(folderID, tipo, marca, modelo);
            final String rutaFinal = tipo + (marca.isEmpty() ? "" : "/" + marca) + (modelo.isEmpty() ? "" : "/" + modelo);

            runOnUiThread(() -> {
                abrirCarpeta(folderIdDestino, rutaFinal);
                Toast.makeText(MainActivity.this, "Navegando a: " + rutaFinal, Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    private void verificarYEjecutarBusqueda() {
        if (cacheCompletoDrive.isEmpty()) {
            Log.d(TAG, "Cache de Drive aún vacío. Reintentando en 1 segundo...");
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

    private boolean coincideConSelectores(Manual item, String tipo, String marca, String modelo) {
        String[] partes = item.rutaRelativa.split("/");
        return (tipo.isEmpty() || (partes.length > 0 && partes[0].equals(tipo))) &&
                (marca.isEmpty() || (partes.length > 1 && partes[1].equals(marca))) &&
                (modelo.isEmpty() || (partes.length > 2 && partes[2].equals(modelo)));
    }

    private void irARaiz() {
        Log.d(TAG, "=== VOLVIENDO AL INICIO ===");

        // Le decimos al ViewModel que reinicie todo al estado inicial
        viewModel.resetToRoot();

        // Sincronizamos nuestra actividad con lo que acaba de hacer el ViewModel
        this.folderID = viewModel.currentFolderID;
        this.currentRelativePath = viewModel.currentRelativePath;
        this.pilaCarpetas = viewModel.pilaCarpetas;
        this.pilaRutas = viewModel.pilaRutas;

        // Actualizamos la pantalla con la nueva información
        sincronizarListaCompleta();
        Toast.makeText(this, "🏠 Volviendo al inicio", Toast.LENGTH_SHORT).show();
    }

    private void actualizarBotonInicio() {
        Button btnInicio = findViewById(R.id.btn_inicio);

        boolean estaEnRaizPura = currentRelativePath.isEmpty() && pilaCarpetas.size() <= 1;

        boolean esModoBusqueda;

        if (items.isEmpty()) {
            esModoBusqueda = false;
        } else {
            boolean tieneHeader = items.get(0).esHeaderSeccion;

            // CORRECCIÓN: Reemplazar Stream por bucle tradicional
            boolean hayCarpetas = false;
            for (Manual item : items) {
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

            viewModel.currentFolderID = this.folderID;
            viewModel.currentRelativePath = this.currentRelativePath;
            viewModel.pilaCarpetas = this.pilaCarpetas;
            viewModel.pilaRutas = this.pilaRutas;

            sincronizarListaCompleta();
            actualizarBotonInicio();
        } else {
            super.onBackPressed();
        }
    }

    // Métodos públicos para que el adapter los use
    public void mostrarDialogoConfirmacionEliminacion(Manual item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Confirmar eliminación")
                .setMessage("¿Estás seguro de que quieres eliminar el archivo \"" + item.nombre + "\"?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    ManualAdapter.ItemViewHolder viewHolder = (ManualAdapter.ItemViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
                    if (viewHolder != null) {
                        viewHolder.eliminarArchivo(item, position);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    public void mostrarDialogoConfirmacionEliminacionCarpeta(Manual item) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar carpeta")
                .setMessage("¿Seguro que quieres eliminar la carpeta \"" + item.nombre + "\" y todo su contenido?")
                .setPositiveButton("Sí", (dialog, which) -> eliminarCarpeta(item))
                .setNegativeButton("No", null)
                .show();
    }

    private void eliminarCarpeta(Manual item) {
        boolean eliminada = localFilesRepository.eliminarCarpeta(item);
        if (eliminada) {
            Toast.makeText(this, "Carpeta eliminada: " + item.nombre, Toast.LENGTH_SHORT).show();
            sincronizarListaCompleta();
        } else {
            Toast.makeText(this, "No se pudo eliminar la carpeta", Toast.LENGTH_SHORT).show();
        }
    }

    public void actualizarItemDescargado(Manual itemDescargado, int position) {
        if (position >= 0 && position < items.size()) {
            items.get(position).descargado = true;
            adapter.notifyItemChanged(position);
        }

        for (Manual item : cacheCompletoDrive) {
            if (item.idDrive.equals(itemDescargado.idDrive)) {
                item.descargado = true;
                break;
            }
        }

        for (Manual item : todosLosItems) {
            if (item.nombre.equals(itemDescargado.nombre) && item.rutaRelativa.equals(itemDescargado.rutaRelativa)) {
                item.descargado = true;
                break;
            }
        }

        Log.d(TAG, "Item actualizado: " + itemDescargado.nombre + " - Descargado: true");
    }

    public void actualizarItemEliminado(Manual itemEliminado, int position) {
        if (position >= 0 && position < items.size()) {
            items.get(position).descargado = false;
            adapter.notifyItemChanged(position);
        }

        for (Manual item : cacheCompletoDrive) {
            if (item.idDrive.equals(itemEliminado.idDrive)) {
                item.descargado = false;
                break;
            }
        }

        for (Manual item : todosLosItems) {
            if (item.nombre.equals(itemEliminado.nombre) && item.rutaRelativa.equals(itemEliminado.rutaRelativa)) {
                item.descargado = false;
                break;
            }
        }

        Log.d(TAG, "Item actualizado: " + itemEliminado.nombre + " - Descargado: false");
    }

    private void cargarManualesLocales(String rutaRelativa) {
        new Thread(() -> {
            List<Manual> itemsLocales = localFilesRepository.cargarManualesLocales(rutaRelativa);

            runOnUiThread(() -> {
                todosLosItems.clear();
                todosLosItems.addAll(itemsLocales);

                items.clear();
                items.addAll(itemsLocales);
                adapter.updateData(itemsLocales);
                currentRelativePath = rutaRelativa;

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
            List<Manual> resultadosBusqueda = localFilesRepository.buscarArchivosLocalesRecursivo(q);
            List<Manual> driveEncontrados = new ArrayList<>();

            if (currentAccount != null) {
                for (Manual item : cacheCompletoDrive) {
                    if (!item.esCarpeta && item.nombre.toLowerCase().contains(q)) {
                        driveEncontrados.add(item);
                        Log.d(TAG, "✓ ENCONTRADO EN DRIVE: " + item.nombre);
                    }
                }
            }

            runOnUiThread(() -> {
                List<Manual> resultadosOrganizados = new ArrayList<>();

                if (!resultadosBusqueda.isEmpty()) {
                    resultadosOrganizados.add(new Manual("📁 MANUALES LOCALES (" + resultadosBusqueda.size() + ")", "local"));
                    resultadosOrganizados.addAll(resultadosBusqueda);
                }

                if (!driveEncontrados.isEmpty()) {
                    resultadosOrganizados.add(new Manual("☁️ MANUALES EN DRIVE (" + driveEncontrados.size() + ")", "drive"));
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
            });
        }).start();
    }

    private void ejecutarBusquedaSoloLocales(String query) {
        new Thread(() -> {
            List<Manual> resultadosBusqueda = localFilesRepository.buscarArchivosLocalesRecursivo(query);

            runOnUiThread(() -> {
                List<Manual> resultadosOrganizados = new ArrayList<>();

                if (!resultadosBusqueda.isEmpty()) {
                    resultadosOrganizados.add(new Manual("📁 MANUALES LOCALES (" + resultadosBusqueda.size() + ")", "local"));
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
        if (driveRepository == null) return;

        new Thread(() -> {
            List<Manual> itemsDrive = driveRepository.listarCarpeta(folderId, rutaRelativa);

            runOnUiThread(() -> {
                items.clear();
                items.addAll(itemsDrive);
                adapter.updateData(itemsDrive);
                currentRelativePath = rutaRelativa;
                actualizarBotonInicio();
            });
        }).start();
    }

    private void sincronizarListaCompleta() {
        Log.d(TAG, "=== SINCRONIZANDO LISTA COMPLETA ===");

        viewModel.currentFolderID = this.folderID;
        viewModel.currentRelativePath = this.currentRelativePath;
        viewModel.pilaCarpetas = this.pilaCarpetas;
        viewModel.pilaRutas = this.pilaRutas;
        viewModel.buscarDespuesDeAutenticar = this.buscarDespuesDeAutenticar;

        if (currentAccount != null && driveRepository != null) {
            Log.d(TAG, "🔄 MODO: Drive + Locales");
            listarCarpetaDrive(folderID, currentRelativePath);

            new Thread(() -> {
                Log.d(TAG, "🔄 Iniciando carga del cache de Drive...");
                List<Manual> cacheCompleto = driveRepository.cargarTodoDriveRecursivo(folderID, "");

                runOnUiThread(() -> {
                    cacheCompletoDrive.clear();
                    cacheCompletoDrive.addAll(cacheCompleto);
                    viewModel.cacheCompletoDrive = cacheCompletoDrive;
                    Log.d(TAG, "✅ Cache Drive actualizado: " + cacheCompleto.size() + " archivos");
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

        // Inicializar DriveRepository después de tener mDriveService
        driveRepository = new DriveRepository(mDriveService, this);

        sincronizarListaCompleta();
        actualizarBotonInicio();
    }

    public void abrirCarpeta(String nuevaCarpetaID, String nuevaRutaRelativa) {
        Log.d(TAG, "=== ABRIENDO CARPETA ===");

        if (!folderID.equals(nuevaCarpetaID) || !currentRelativePath.equals(nuevaRutaRelativa)) {
            pilaCarpetas.push(folderID);
            pilaRutas.push(currentRelativePath);
        }

        this.folderID = nuevaCarpetaID;
        this.currentRelativePath = nuevaRutaRelativa;

        viewModel.currentFolderID = this.folderID;
        viewModel.currentRelativePath = this.currentRelativePath;
        viewModel.pilaCarpetas = this.pilaCarpetas;
        viewModel.pilaRutas = this.pilaRutas;

        sincronizarListaCompleta();
        actualizarBotonInicio();
    }
}