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
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;
import java.io.IOException;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.github.chrisbanes.photoview.PhotoView;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";
    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;
    private PhotoView pdfImageView;

    private Button btnPrev, btnNext;
    private TextView tvPageIndicator;

    private String textoCompletoPdf;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage; // Cambiado el nombre para consistencia
    private ParcelFileDescriptor parcelFileDescriptor;

    private int currentPageIndex = 0;
    private int pageCount = 0;
    private float scaleFactor = 1.0f;
    private final float SCALE_STEP = 0.2f;

    // Variables corregidas - eliminadas las duplicadas
    private float minScale = 1.0f;
    private float maxScale = 3.0f;
    private String rutaArchivo;

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
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        btnPrev.setOnClickListener(v -> mostrarPaginaAnterior());
        btnNext.setOnClickListener(v -> mostrarPaginaSiguiente());
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

            // Renderizar la página en el bitmap
            bitmap.eraseColor(Color.WHITE); // Fondo blanco
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            pdfImageView.setImageBitmap(bitmap);

            // Resetear zoom al cambiar de página
            pdfImageView.setScale(1.0f, false);

            currentPageIndex = index;
            actualizarIndicadorPagina();

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

    private void buscarPalabraEnPdf(String palabraClave) {
        if (textoCompletoPdf != null && palabraClave != null && !palabraClave.isEmpty()) {
            new Thread(() -> {
                try {
                    ExtractorPDF extractor = new ExtractorPDF(this);
                    int pagina = extractor.buscarPaginaQueContiene(palabraClave, rutaArchivo);
                    runOnUiThread(() -> {
                        if (pagina >= 0) {
                            Toast.makeText(this, "Palabra encontrada en página " + (pagina + 1), Toast.LENGTH_SHORT).show();
                            mostrarPagina(pagina);
                        } else {
                            Toast.makeText(this, "No se encontró la palabra", Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this, "Error al buscar palabra", Toast.LENGTH_SHORT).show());
                }
            }).start();
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