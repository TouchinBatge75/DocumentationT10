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
    public File obtenerArchivoLocal(Manual item) {
        try {
            File directorioBase = context.getFilesDir();
            String rutaBase = "Manuales/" +
                    (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/");

            Log.d(TAG, "🔍 Buscando archivo para: " + item.nombre);
            Log.d(TAG, "📁 Ruta base: " + rutaBase);

            // Lista de nombres posibles
            List<String> nombresPosibles = new ArrayList<>();

            // 1. Nombre exacto
            nombresPosibles.add(item.nombre);

            // 2. Con .pdf si es PDF
            if (item.tipoArchivo != null && item.tipoArchivo.equals("pdf") &&
                    !item.nombre.toLowerCase().endsWith(".pdf")) {
                nombresPosibles.add(item.nombre + ".pdf");
            }

            // 3. Con .PDF (mayúsculas)
            if (item.tipoArchivo != null && item.tipoArchivo.equals("pdf")) {
                nombresPosibles.add(item.nombre + ".PDF");
            }

            // 4. Sin extensión .pdf si la tiene
            if (item.nombre.toLowerCase().endsWith(".pdf")) {
                nombresPosibles.add(item.nombre.substring(0, item.nombre.length() - 4));
            }

            // Buscar cada posibilidad
            for (String nombre : nombresPosibles) {
                String rutaCompleta = rutaBase + nombre;
                File archivo = new File(directorioBase, rutaCompleta);

                if (archivo.exists() && archivo.isFile() && archivo.length() > 0) {
                    Log.d(TAG, "✅ Archivo encontrado: " + archivo.getAbsolutePath());
                    return archivo;
                }
            }

            // Si no se encuentra, devolver la ruta más probable
            String nombreFinal = item.nombre;
            if (item.tipoArchivo != null && item.tipoArchivo.equals("pdf") &&
                    !item.nombre.toLowerCase().endsWith(".pdf")) {
                nombreFinal = item.nombre + ".pdf";
            }

            return new File(directorioBase, rutaBase + nombreFinal);

        } catch (Exception e) {
            Log.e(TAG, "Error en obtenerArchivoLocal: " + e.getMessage());
            return new File(item.nombre);
        }
    }
    public List<String> obtenerTiposLocales() {
        List<String> tipos = new ArrayList<>();
        File carpetaRaiz = new File(context.getFilesDir(), "Manuales");

        if (carpetaRaiz.exists() && carpetaRaiz.isDirectory()) {
            File[] archivos = carpetaRaiz.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        tipos.add(archivo.getName());
                    }
                }
            }
        }
        return tipos;
    }

    public List<String> obtenerMarcasLocales(String tipo) {
        List<String> marcas = new ArrayList<>();
        File carpetaTipo = new File(context.getFilesDir(), "Manuales/" + tipo);

        if (carpetaTipo.exists() && carpetaTipo.isDirectory()) {
            File[] archivos = carpetaTipo.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        marcas.add(archivo.getName());
                    }
                }
            }
        }
        return marcas;
    }

    public List<String> obtenerModelosLocales(String tipo, String marca) {
        List<String> modelos = new ArrayList<>();
        File carpetaMarca = new File(context.getFilesDir(), "Manuales/" + tipo + "/" + marca);

        if (carpetaMarca.exists() && carpetaMarca.isDirectory()) {
            File[] archivos = carpetaMarca.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        modelos.add(archivo.getName());
                    }
                }
            }
        }
        return modelos;
    }

    public List<Manual> buscarPorCategoriasLocales(String tipo, String marca, String modelo) {
        List<Manual> resultados = new ArrayList<>();

        // Construir ruta base
        String rutaBase = "Manuales/";
        if (!tipo.isEmpty()) rutaBase += tipo + "/";
        if (!marca.isEmpty()) rutaBase += marca + "/";
        if (!modelo.isEmpty()) rutaBase += modelo + "/";

        File carpetaBusqueda = new File(context.getFilesDir(), rutaBase);

        if (carpetaBusqueda.exists()) {
            // Buscar PDFs en esta carpeta
            buscarPDFsEnCarpeta(carpetaBusqueda, tipo, marca, modelo, resultados);
        }

        return resultados;
    }

    private void buscarPDFsEnCarpeta(File carpeta, String tipo, String marca, String modelo, List<Manual> resultados) {
        if (carpeta.exists() && carpeta.isDirectory()) {
            File[] archivos = carpeta.listFiles();
            if (archivos != null) {
                for (File archivo : archivos) {
                    if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".pdf")) {
                        String nombreSinExtension = archivo.getName().replace(".pdf", "");

                        // Construir ruta relativa
                        String rutaRelativa = tipo;
                        if (!marca.isEmpty()) rutaRelativa += "/" + marca;
                        if (!modelo.isEmpty()) rutaRelativa += "/" + modelo;

                        Manual manual = new Manual("", nombreSinExtension, false, "application/pdf", rutaRelativa);
                        manual.descargado = true;
                        manual.rutaCompleta = archivo.getAbsolutePath();
                        resultados.add(manual);
                    } else if (archivo.isDirectory()) {
                        // Buscar recursivamente
                        buscarPDFsEnCarpeta(archivo, tipo, marca, modelo, resultados);
                    }
                }
            }
        }
    }
}