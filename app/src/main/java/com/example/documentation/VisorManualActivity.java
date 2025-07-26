package com.example.documentation;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import java.io.File;
import java.io.IOException;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    private ImageView pdfImageView;
    private Button btnPrev, btnNext;
    private TextView tvPageIndicator;

    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private ParcelFileDescriptor parcelFileDescriptor;

    private int currentPageIndex = 0;
    private int pageCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        pdfImageView = findViewById(R.id.pdfImageView);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvPageIndicator = findViewById(R.id.tvPageIndicator);

        String rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            File file = new File(rutaArchivo);
            if (file.exists()) {
                try {
                    openRenderer(file);
                    pageCount = pdfRenderer.getPageCount();
                    showPage(currentPageIndex);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al abrir PDF", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Ruta inválida", Toast.LENGTH_LONG).show();
        }

        btnPrev.setOnClickListener(v -> {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                showPage(currentPageIndex);
            }
        });

        btnNext.setOnClickListener(v -> {
            if (currentPageIndex < pageCount - 1) {
                currentPageIndex++;
                showPage(currentPageIndex);
            }
        });
    }

    private void openRenderer(File file) throws IOException {
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(parcelFileDescriptor);
    }

    private void showPage(int index) {
        if (pdfRenderer == null || index < 0 || index >= pdfRenderer.getPageCount()) {
            return;
        }

        // Cierra página anterior
        if (currentPage != null) {
            currentPage.close();
        }

        currentPage = pdfRenderer.openPage(index);

        // Tamaño del bitmap según densidad y tamaño de página
        int width = getResources().getDisplayMetrics().densityDpi / 72 * currentPage.getWidth();
        int height = getResources().getDisplayMetrics().densityDpi / 72 * currentPage.getHeight();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Color.WHITE);
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        pdfImageView.setImageBitmap(bitmap);

        tvPageIndicator.setText(String.format("Página %d / %d", index + 1, pageCount));

        // Actualiza estado de botones
        btnPrev.setEnabled(index > 0);
        btnNext.setEnabled(index < pageCount - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (currentPage != null) currentPage.close();
            if (pdfRenderer != null) pdfRenderer.close();
            if (parcelFileDescriptor != null) parcelFileDescriptor.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Opcional: implementa búsqueda o zoom en otra iteración.
}
