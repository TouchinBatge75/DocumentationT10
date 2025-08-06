package com.example.documentation;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;

public class ExtractorPDF {

    private Context context;

    public ExtractorPDF(Context context) {
        this.context = context;
        // Inicializar recursos PdfBox para Android, muy importante para evitar errores
        PDFBoxResourceLoader.init(context);
    }

    public String extractText(File pdfFile) throws IOException {
        PDDocument document = PDDocument.load(pdfFile);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }

    public int buscarPaginaQueContiene(String palabra, String rutaArchivo) throws IOException {
        File file = new File(rutaArchivo);
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();

        int totalPages = document.getNumberOfPages();
        for (int i = 0; i < totalPages; i++) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String textoPagina = stripper.getText(document);
            if (textoPagina.toLowerCase().contains(palabra.toLowerCase())) {
                document.close();
                return i;
            }
        }

        document.close();
        return -1;
    }
}
