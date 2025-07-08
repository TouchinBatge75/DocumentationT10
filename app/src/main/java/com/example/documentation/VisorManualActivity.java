package com.example.documentation;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;

public class VisorManualActivity extends AppCompatActivity {

    public static final String EXTRA_MANUAL_PATH = "manual_path";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_manual);

        TextView textoManual = findViewById(R.id.texto_manual);

        // Obtenemos la ruta del archivo PDF que nos pasaron desde el Intent
        String rutaArchivo = getIntent().getStringExtra(EXTRA_MANUAL_PATH);

        if (rutaArchivo != null) {
            try {
                // Abrimos el archivo PDF local
                File archivo = new File(rutaArchivo);

                // Cargamos el PDF con PdfBox
                PDDocument document = PDDocument.load(archivo);

                // Creamos un extractor de texto
                PDFTextStripper stripper = new PDFTextStripper();

                // Extraemos todo el texto del PDF
                String texto = stripper.getText(document);

                // Cerramos el documento
                document.close();

                // Mostramos el texto extraído en el TextView
                textoManual.setText(texto);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error al leer el manual", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Ruta del manual no válida", Toast.LENGTH_SHORT).show();
        }
    }
}
