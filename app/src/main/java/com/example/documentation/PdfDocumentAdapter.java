package com.example.documentation;

import android.content.Context;
import android.graphics.pdf.PdfRenderer;
import android.os.ParcelFileDescriptor;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.graphics.pdf.PdfDocument;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.print.PageRange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class PdfDocumentAdapter extends PrintDocumentAdapter {

    private final Context context;
    private final String path;
    private PrintAttributes printAttributes;
    private PdfRenderer renderer;
    private ParcelFileDescriptor fileDescriptor;

    public PdfDocumentAdapter(Context context, String path) {
        this.context = context;
        this.path = path;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle metadata) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        // Validar que newAttributes y su mediaSize no sean null
        PrintAttributes.MediaSize mediaSize = null;
        if (newAttributes != null) {
            mediaSize = newAttributes.getMediaSize();
        }

        if (mediaSize == null) {
            // Asignar mediaSize por defecto (A4)
            mediaSize = PrintAttributes.MediaSize.ISO_A4;
            PrintAttributes.Builder builder = new PrintAttributes.Builder()
                    .setMediaSize(mediaSize)
                    .setResolution(new PrintAttributes.Resolution("id", "name", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS);
            printAttributes = builder.build();
        } else {
            printAttributes = newAttributes;
        }

        // Obtener número de páginas del PDF para mostrar en el layout
        int pageCount = 0;
        try {
            fileDescriptor = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fileDescriptor);
            pageCount = renderer.getPageCount();
            renderer.close();
            fileDescriptor.close();
            renderer = null;
            fileDescriptor = null;
        } catch (IOException e) {
            e.printStackTrace();
            // En caso de error, dejar pageCount en 0
        }

        PrintDocumentInfo info = new PrintDocumentInfo.Builder("manual.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(pageCount)
                .build();

        // true = el layout cambió y se debe refrescar
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(PageRange[] pages,
                        ParcelFileDescriptor destination,
                        CancellationSignal cancellationSignal,
                        WriteResultCallback callback) {

        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            return;
        }

        PdfRenderer renderer = null;
        ParcelFileDescriptor fileDescriptor = null;
        PrintedPdfDocument pdfDoc = null;

        try {
            fileDescriptor = ParcelFileDescriptor.open(new File(path), ParcelFileDescriptor.MODE_READ_ONLY);
            renderer = new PdfRenderer(fileDescriptor);

            pdfDoc = new PrintedPdfDocument(context, printAttributes);

            final int pageCount = renderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {

                if (cancellationSignal.isCanceled()) {
                    callback.onWriteCancelled();
                    pdfDoc.close();
                    renderer.close();
                    fileDescriptor.close();
                    return;
                }

                PdfRenderer.Page page = renderer.openPage(i);

                PdfDocument.Page pdfPage = pdfDoc.startPage(i);

                Canvas canvas = pdfPage.getCanvas();
                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);
                canvas.drawBitmap(bitmap, 0, 0, null);

                pdfDoc.finishPage(pdfPage);
                page.close();
            }

            pdfDoc.writeTo(new FileOutputStream(destination.getFileDescriptor()));

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});

        } catch (Exception e) {
            callback.onWriteFailed(e.toString());
        } finally {
            try {
                if (pdfDoc != null) pdfDoc.close();
            } catch (Exception e) { /* Ignorar */ }
            try {
                if (renderer != null) renderer.close();
            } catch (Exception e) { /* Ignorar */ }
            try {
                if (fileDescriptor != null) fileDescriptor.close();
            } catch (Exception e) { /* Ignorar */ }
        }
    }
}
