package com.example.documentation;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    private TextView textoManual;
    private float currentTextSize = 16f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        textoManual = findViewById(R.id.texto_manual);
        textoManual.setTextSize(currentTextSize);

        String rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            File archivo = new File(rutaArchivo);
            if (archivo.exists()) {
                try {
                    PdfReader reader = new PdfReader(rutaArchivo);
                    StringBuilder texto = new StringBuilder();
                    for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                        texto.append(PdfTextExtractor.getTextFromPage(reader, i));
                        texto.append("\n\n");
                    }
                    reader.close();
                    textoManual.setText(texto.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error al leer el PDF", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Archivo no encontrado", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Ruta inválida", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_visor_manual, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();


        if (id == R.id.action_text_increase) {
            currentTextSize += 2f;
            textoManual.setTextSize(currentTextSize);
            return true;
        } else if (id == R.id.action_text_decrease) {
            currentTextSize = Math.max(10f, currentTextSize - 2f);
            textoManual.setTextSize(currentTextSize);
            return true;
        } else if (id == R.id.action_search) {
            Toast.makeText(this, "Función de búsqueda no implementada aún", Toast.LENGTH_SHORT).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
