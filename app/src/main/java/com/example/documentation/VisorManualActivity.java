package com.example.documentation;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import com.google.android.material.appbar.MaterialToolbar;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    private TextView textoManual;
    private float currentTextSize = 16f;
    private String textoOriginal = ""; // Para mantener el texto sin resaltar

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
                    textoOriginal = texto.toString(); // Guarda el texto original
                    textoManual.setText(textoOriginal);
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

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setQueryHint("Buscar en el texto...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                resaltarPalabra(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                resaltarPalabra(newText);
                return true;
            }
        });

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
        }

        return super.onOptionsItemSelected(item);
    }

    private void resaltarPalabra(String palabra) {
        if (textoOriginal == null) return;

        SpannableString spannable = new SpannableString(textoOriginal);

        if (palabra != null && !palabra.isEmpty()) {
            String contenidoLower = textoOriginal.toLowerCase();
            String palabraLower = palabra.toLowerCase();

            int index = contenidoLower.indexOf(palabraLower);
            while (index >= 0) {
                spannable.setSpan(
                        new BackgroundColorSpan(Color.YELLOW),
                        index,
                        index + palabra.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                index = contenidoLower.indexOf(palabraLower, index + palabra.length());
            }
        }

        textoManual.setText(spannable);
    }
}
