package com.example.documentation;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import java.io.File;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        TextView textoManual = findViewById(R.id.texto_manual);

        String rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            File archivo = new File(rutaArchivo);
            if (archivo.exists()) {
                try {
                    // Leer el PDF
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
}
