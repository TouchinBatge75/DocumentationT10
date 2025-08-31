package com.example.documentation;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.github.chrisbanes.photoview.PhotoView;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";
    private PhotoView pdfImageView;

    private Button btnPrev, btnNext;
    private TextView tvPageIndicator;

    private String textoCompletoPdf;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;

    private int currentPageIndex = 0;
    private int pageCount = 0;
    private float scaleFactor = 1.0f;
    private final float SCALE_STEP = 0.2f;

    private float minScale = 1.0f;
    private float maxScale = 3.0f;
    private String rutaArchivo;

    // Variables para navegación de búsqueda
    private LinearLayout searchNavigationLayout;
    private TextView tvSearchIndicator;
    private Button btnPrevResult, btnNextResult;
    private List<Integer> searchResults = new ArrayList<>();
    private int currentSearchIndex = -1;
    private String currentSearchQuery = "";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        initViews();
        cargarPDF();

        EditText pageInput = findViewById(R.id.pageInput);
        TextView pageTotal = findViewById(R.id.pageTotal);

        pageInput.setText(String.valueOf(currentPageIndex + 1));
        pageTotal.setText("/" + pageCount);


        pageInput.setOnEditorActionListener((v, actionId, event) -> {
            String input = pageInput.getText().toString();
            if (!input.isEmpty()) {
                int nuevaPagina = Integer.parseInt(input) - 1; // Convertir a índice 0-based
                if (nuevaPagina >= 0 && nuevaPagina < pageCount) {
                    mostrarPagina(nuevaPagina);
                } else {
                    Toast.makeText(this, "Página fuera de rango", Toast.LENGTH_SHORT).show();
                    pageInput.setText(String.valueOf(currentPageIndex + 1)); // Restaurar página actual
                }
            }
            return true;
        });

        // Listener para detectar taps en tercios de la pantalla
        pdfImageView.setOnViewTapListener((view, x, y) -> {
            int width = view.getWidth();

            if (x < width / 3f) {
                mostrarPaginaAnterior();
            } else if (x > (width * 2f / 3f)) {
                mostrarPaginaSiguiente();
            }
        });
    }

    private void initViews() {
        pdfImageView = findViewById(R.id.pdfImageView);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        // Nuevos elementos para navegación de búsqueda
        btnPrevResult = findViewById(R.id.btnPrevResult);
        btnNextResult = findViewById(R.id.btnNextResult);
        searchNavigationLayout = findViewById(R.id.searchNavigationLayout);
        tvSearchIndicator = findViewById(R.id.tvSearchIndicator);

        btnPrev.setOnClickListener(v -> mostrarPaginaAnterior());
        btnNext.setOnClickListener(v -> mostrarPaginaSiguiente());

        // Listeners para navegación de búsqueda
        btnPrevResult.setOnClickListener(v -> navegarResultadoAnterior());
        btnNextResult.setOnClickListener(v -> navegarResultadoSiguiente());
    }

    private void cargarPDF() {
        rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            File file = new File(rutaArchivo);
            if (file.exists()) {
                try {
                    abrirRenderer(file);
                    pageCount = pdfRenderer.getPageCount();

                    // Actualizar el indicador de página
                    actualizarIndicadorPagina();

                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenWidth = displayMetrics.widthPixels;

                    PdfRenderer.Page firstPage = pdfRenderer.openPage(0);
                    minScale = (float) screenWidth / firstPage.getWidth();
                    maxScale = minScale * 3.0f;
                    scaleFactor = minScale;
                    firstPage.close();

                    mostrarPagina(currentPageIndex);

                    // Extraer texto en segundo plano para búsqueda
                    new Thread(() -> {
                        ExtractorPDF extractor = new ExtractorPDF(this);
                        try {
                            textoCompletoPdf = extractor.extractText(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this, "Error al cargar texto para búsqueda", Toast.LENGTH_SHORT).show());
                        }
                    }).start();

                } catch (IOException e) {
                    manejarError("Error al abrir PDF", e);
                }
            } else {
                Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Ruta inválida", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void abrirRenderer(File file) throws IOException {
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
    }

    private void mostrarPagina(int index) {
        if (index < 0 || index >= pageCount) return;

        try {
            if (currentPage != null) {
                currentPage.close();
            }

            currentPage = pdfRenderer.openPage(index);

            Bitmap bitmap = Bitmap.createBitmap(
                    currentPage.getWidth(),
                    currentPage.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            bitmap.eraseColor(Color.WHITE);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            pdfImageView.setImageBitmap(bitmap);

            pdfImageView.setScale(1.0f, false);

            currentPageIndex = index;
            actualizarIndicadorPagina();

            //Actualizar input y total
            EditText pageInput = findViewById(R.id.pageInput);
            TextView pageTotal = findViewById(R.id.pageTotal);
            pageInput.setText(String.valueOf(currentPageIndex + 1));
            pageTotal.setText("/" + pageCount);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al mostrar página", Toast.LENGTH_SHORT).show();
        }
    }


    private void actualizarIndicadorPagina() {
        if (tvPageIndicator != null) {
            tvPageIndicator.setText((currentPageIndex + 1) + " / " + pageCount);
        }
    }

    private void mostrarPaginaAnterior() {
        if (currentPageIndex > 0) {
            mostrarPagina(currentPageIndex - 1);
        }
    }

    private void mostrarPaginaSiguiente() {
        if (currentPageIndex < pageCount - 1) {
            mostrarPagina(currentPageIndex + 1);
        }
    }

    // Métodos para navegación de búsqueda
    private void navegarResultadoAnterior() {
        if (!searchResults.isEmpty() && currentSearchIndex > 0) {
            currentSearchIndex--;
            int pagina = searchResults.get(currentSearchIndex);
            mostrarPagina(pagina);
            actualizarIndicadorBusqueda();
        }
    }

    private void navegarResultadoSiguiente() {
        if (!searchResults.isEmpty() && currentSearchIndex < searchResults.size() - 1) {
            currentSearchIndex++;
            int pagina = searchResults.get(currentSearchIndex);
            mostrarPagina(pagina);
            actualizarIndicadorBusqueda();
        }
    }

    private void actualizarIndicadorBusqueda() {
        if (!searchResults.isEmpty()) {
            String indicador = (currentSearchIndex + 1) + " / " + searchResults.size();
            tvSearchIndicator.setText(indicador);
        }
    }

    private void mostrarBotonesBusqueda(boolean mostrar) {
        if (mostrar && !searchResults.isEmpty()) {
            searchNavigationLayout.setVisibility(View.VISIBLE);
            actualizarIndicadorBusqueda();
        } else {
            searchNavigationLayout.setVisibility(View.GONE);
            // Limpiar resultados cuando se oculta
            searchResults.clear();
            currentSearchIndex = -1;
        }
    }


    private void buscarPalabraEnPdf(String palabraClave) {
        if (textoCompletoPdf != null && palabraClave != null && !palabraClave.isEmpty()) {
            currentSearchQuery = palabraClave;
            new Thread(() -> {
                try {
                    ExtractorPDF extractor = new ExtractorPDF(this);
                    List<Integer> resultados = extractor.buscarTodasLasPaginas(palabraClave, rutaArchivo);

                    runOnUiThread(() -> {
                        if (resultados != null && !resultados.isEmpty()) {
                            searchResults = resultados;
                            currentSearchIndex = 0;

                            Toast.makeText(this, "Encontradas " + resultados.size() + " coincidencias", Toast.LENGTH_SHORT).show();
                            mostrarBotonesBusqueda(true);
                            mostrarPagina(resultados.get(0));
                            actualizarIndicadorBusqueda();
                        } else {
                            Toast.makeText(this, "No se encontró la palabra", Toast.LENGTH_SHORT).show();
                            searchResults.clear();
                            mostrarBotonesBusqueda(false);
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error al buscar palabra", Toast.LENGTH_SHORT).show();
                        mostrarBotonesBusqueda(false);
                    });
                }
            }).start();
        } else {
            mostrarBotonesBusqueda(false);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_visor_manual, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setQueryHint("Buscar en el PDF...");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                buscarPalabraEnPdf(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_print) {
            imprimirPDF();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void imprimirPDF() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            PrintManager printManager = (PrintManager) getSystemService(PRINT_SERVICE);
            if (printManager != null && rutaArchivo != null) {
                PrintDocumentAdapter printAdapter = new PdfDocumentAdapter(this, rutaArchivo);
                PrintAttributes printAttributes = new PrintAttributes.Builder()
                        .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();
                printManager.print("Documentación", printAdapter, printAttributes);
            } else {
                Toast.makeText(this, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Impresión no soportada en esta versión de Android", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cerrarRecursos();
    }

    private void cerrarRecursos() {
        try {
            if (currentPage != null) {
                currentPage.close();
                currentPage = null;
            }
            if (pdfRenderer != null) {
                pdfRenderer.close();
                pdfRenderer = null;
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
                parcelFileDescriptor = null;
            }
        } catch (IOException e) {
            manejarError("Error al cerrar recursos", e);
        }
    }

    private void manejarError(String mensaje, Exception e) {
        e.printStackTrace();
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
        finish();
    }
}