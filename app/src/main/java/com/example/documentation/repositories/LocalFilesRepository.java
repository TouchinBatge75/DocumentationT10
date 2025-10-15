package com.example.documentation.repositories;

import android.content.Context;
import android.util.Log;

import com.example.documentation.models.Manual;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LocalFilesRepository {
    private static final String TAG = "LocalFilesRepository";
    private Context context;

    public LocalFilesRepository(Context context) {
        this.context = context;
    }

    public List<Manual> cargarManualesLocales(String rutaRelativa) {
        File carpeta = new File(context.getFilesDir(), "Manuales/" + rutaRelativa);
        List<Manual> itemsLocales = new ArrayList<>();

        Log.d(TAG, "=== CARGANDO LOCALES ===");
        Log.d(TAG, "Ruta relativa: " + rutaRelativa);
        Log.d(TAG, "Ruta absoluta: " + carpeta.getAbsolutePath());
        Log.d(TAG, "Carpeta existe: " + carpeta.exists());

        if (carpeta.exists()) {
            File[] archivos = carpeta.listFiles();
            Log.d(TAG, "Número de elementos: " + (archivos != null ? archivos.length : 0));

            if (archivos != null) {
                // Primero agregar carpetas
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        Manual itemCarpeta = new Manual("", archivo.getName(), true, "", rutaRelativa);
                        itemsLocales.add(itemCarpeta);
                        Log.d(TAG, "✓ Carpeta: " + archivo.getName());
                    }
                }

                // Luego agregar archivos PDF
                for (File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".pdf")) {
                        String nombreArchivo = archivo.getName().replace(".pdf", "");
                        Manual item = new Manual("", nombreArchivo, false, "", rutaRelativa);
                        item.descargado = true;
                        itemsLocales.add(item);
                        Log.d(TAG, "✓ PDF: " + nombreArchivo);
                    }
                }
            }
        } else {
            Log.d(TAG, "❌ La carpeta no existe: " + carpeta.getAbsolutePath());
        }

        Log.d(TAG, "=== CARPETA CARGADA: " + itemsLocales.size() + " elementos ===");
        return itemsLocales;
    }

    public List<Manual> buscarArchivosLocalesRecursivo(String query) {
        List<Manual> resultados = new ArrayList<>();
        File carpetaRaiz = new File(context.getFilesDir(), "Manuales");

        if (carpetaRaiz.exists()) {
            buscarArchivosLocalesRecursivo(carpetaRaiz, "", query.toLowerCase().trim(), resultados);
        }
        return resultados;
    }

    private void buscarArchivosLocalesRecursivo(File carpeta, String rutaRelativa, String query, List<Manual> resultados) {
        if (carpeta.exists() && carpeta.isDirectory()) {
            File[] archivos = carpeta.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".pdf")) {
                        String nombreArchivo = archivo.getName().replace(".pdf", "");
                        if (nombreArchivo.toLowerCase().contains(query)) {
                            Manual item = new Manual("", nombreArchivo, false, "", rutaRelativa);
                            item.descargado = true;
                            resultados.add(item);
                            Log.d(TAG, "✓ ENCONTRADO LOCAL: " + nombreArchivo + " en " + rutaRelativa);
                        }
                    } else if (archivo.isDirectory()) {
                        String nuevaRuta = rutaRelativa.isEmpty() ? archivo.getName() : rutaRelativa + "/" + archivo.getName();
                        buscarArchivosLocalesRecursivo(archivo, nuevaRuta, query, resultados);
                    }
                }
            }
        }
    }

    public boolean eliminarArchivo(Manual item) {
        File archivoLocal = new File(context.getFilesDir(),
                "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.nombre + ".pdf");
        return archivoLocal.exists() && archivoLocal.delete();
    }

    public boolean eliminarCarpeta(Manual item) {
        File carpetaLocal = new File(context.getFilesDir(),
                "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.nombre);

        if (carpetaLocal.exists() && carpetaLocal.isDirectory()) {
            return eliminarRecursivamente(carpetaLocal);
        }
        return false;
    }

    private boolean eliminarRecursivamente(File archivo) {
        if (archivo.isDirectory()) {
            File[] hijos = archivo.listFiles();
            if (hijos != null) {
                for (File hijo : hijos) {
                    eliminarRecursivamente(hijo);
                }
            }
        }
        return archivo.delete();
    }

    public boolean archivoExiste(Manual item) {
        File archivoLocal = new File(context.getFilesDir(),
                "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.nombre + ".pdf");
        return archivoLocal.exists();
    }
}