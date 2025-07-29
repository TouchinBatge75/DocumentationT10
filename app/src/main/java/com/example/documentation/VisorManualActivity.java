package com.example.documentation;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    private ImageView pdfImageView;
    private Button btnPrev, btnNext, btnZoomIn, btnZoomOut;
    private TextView tvPageIndicator;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        // Configuración de la Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Inicialización de vistas
        initViews();

        // Cargar el PDF
        cargarPDF();
    }

    private void initViews() {
        pdfImageView = findViewById(R.id.pdfImageView);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnZoomIn = findViewById(R.id.btnZoomIn);
        btnZoomOut = findViewById(R.id.btnZoomOut);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        // Configuración de listeners
        btnPrev.setOnClickListener(v -> mostrarPaginaAnterior());
        btnNext.setOnClickListener(v -> mostrarPaginaSiguiente());
        btnZoomIn.setOnClickListener(v -> aplicarZoom(true));
        btnZoomOut.setOnClickListener(v -> aplicarZoom(false));
    }

    private void cargarPDF() {
        rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            File file = new File(rutaArchivo);
            if (file.exists()) {
                try {
                    abrirRenderer(file);
                    pageCount = pdfRenderer.getPageCount();

                    // Calcular escala inicial basada en el ancho de pantalla
                    DisplayMetrics displayMetrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                    int screenWidth = displayMetrics.widthPixels;

                    // Abrir página temporalmente para obtener dimensiones
                    PdfRenderer.Page firstPage = pdfRenderer.openPage(0);
                    minScale = (float) screenWidth / firstPage.getWidth();
                    maxScale = minScale * 3.0f; // Zoom máximo 3x el tamaño ajustado
                    scaleFactor = minScale; // Establecer escala inicial
                    firstPage.close();

                    mostrarPagina(currentPageIndex);
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
        if (pdfRenderer == null || index < 0 || index >= pdfRenderer.getPageCount()) {
            return;
        }

        // Cerrar página actual si existe
        if (currentPage != null) {
            currentPage.close();
        }

        try {
            currentPage = pdfRenderer.openPage(index);

            // Calcular dimensiones ajustadas
            int width = (int) (currentPage.getWidth() * scaleFactor);
            int height = (int) (currentPage.getHeight() * scaleFactor);

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            pdfImageView.setImageBitmap(bitmap);
            tvPageIndicator.setText(String.format("Página %d / %d", index + 1, pageCount));

            // Actualizar estado de los botones
            btnPrev.setEnabled(index > 0);
            btnNext.setEnabled(index < pageCount - 1);
            btnZoomOut.setEnabled(scaleFactor > minScale);
            btnZoomIn.setEnabled(scaleFactor < maxScale);

        } catch (Exception e) {
            manejarError("Error al mostrar página", e);
        }
    }

    private void aplicarZoom(boolean zoomIn) {
        if (zoomIn) {
            scaleFactor = Math.min(scaleFactor + SCALE_STEP, maxScale);
        } else {
            scaleFactor = Math.max(scaleFactor - SCALE_STEP, minScale);
        }
        mostrarPagina(currentPageIndex);
    }

    private void mostrarPaginaAnterior() {
        if (currentPageIndex > 0) {
            currentPageIndex--;
            mostrarPagina(currentPageIndex);
        }
    }

    private void mostrarPaginaSiguiente() {
        if (currentPageIndex < pageCount - 1) {
            currentPageIndex++;
            mostrarPagina(currentPageIndex);
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

    // Menú e impresión
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_visor_manual, menu);
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
                printManager.print("Documentación", printAdapter, new PrintAttributes.Builder().build());
            } else {
                Toast.makeText(this, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Impresión no soportada en esta versión de Android", Toast.LENGTH_SHORT).show();
        }
    }
}