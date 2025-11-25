package com.example.documentation.repositories;

import android.content.Context;
import android.util.Log;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.example.documentation.models.Manual;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DriveRepository {
    private static final String TAG = "DriveRepository";
    private Drive driveService;
    private Context context;

    public DriveRepository(Drive driveService, Context context) {
        this.driveService = driveService;
        this.context = context;
    }

    public List<Manual> listarCarpeta(String folderId, String rutaRelativa) {
        List<Manual> itemsDrive = new ArrayList<>();
        if (driveService == null) return itemsDrive;

        try {
            FileList result = driveService.files().list()
                    .setQ("'" + folderId + "' in parents and trashed=false")
                    .setFields("files(id, name, mimeType)")
                    .execute();

            if (result.getFiles() != null) {
                for (File f : result.getFiles()) {
                    boolean esCarpeta = "application/vnd.google-apps.folder".equals(f.getMimeType());
                    boolean descargado = false;
                    String nombreArchivo = f.getName();

                    if (!esCarpeta) {
                        if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
                            nombreArchivo += ".pdf";
                        }
                        java.io.File archivoLocal = new java.io.File(context.getFilesDir(),
                                "Manuales/" + (rutaRelativa.isEmpty() ? "" : rutaRelativa + "/") + nombreArchivo);
                        descargado = archivoLocal.exists();
                    }

                    String nombreMostrar = esCarpeta ? nombreArchivo : nombreArchivo.replace(".pdf", "");
                    Manual item = new Manual(f.getId(), nombreMostrar, esCarpeta, "", rutaRelativa);
                    item.descargado = descargado;
                    itemsDrive.add(item);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al listar carpeta de Drive: " + e.getMessage());
        }
        return itemsDrive;
    }

    public List<Manual> cargarTodoDriveRecursivo(String folderId, String ruta) {
        List<Manual> acumulador = new ArrayList<>();
        if (driveService == null) return acumulador;

        try {
            FileList result = driveService.files().list()
                    .setQ("'" + folderId + "' in parents and trashed=false")
                    .setFields("files(id, name, mimeType)")
                    .execute();

            if (result.getFiles() != null) {
                Log.d(TAG, "Cargando carpeta Drive: " + folderId + " - Elementos: " + result.getFiles().size());

                for (File f : result.getFiles()) {
                    boolean esCarpeta = "application/vnd.google-apps.folder".equals(f.getMimeType());
                    String nuevaRuta = ruta.isEmpty() ? f.getName() : ruta + "/" + f.getName();

                    if (esCarpeta) {
                        Log.d(TAG, "Carpeta encontrada: " + f.getName());
                        acumulador.addAll(cargarTodoDriveRecursivo(f.getId(), nuevaRuta));
                    } else {
                        String nombreArchivo = f.getName();
                        if (!nombreArchivo.toLowerCase().endsWith(".pdf")) {
                            nombreArchivo += ".pdf";
                        }
                        java.io.File archivoLocal = new java.io.File(context.getFilesDir(), "Manuales/" + nuevaRuta);
                        boolean descargado = archivoLocal.exists();

                        Manual item = new Manual(f.getId(), nombreArchivo.replace(".pdf", ""), false, "", ruta);
                        item.descargado = descargado;
                        acumulador.add(item);

                        Log.d(TAG, "Archivo Drive agregado al cache: " + item.nombre + " - Ruta: " + ruta);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en carga recursiva de Drive: " + e.getMessage());
        }
        return acumulador;
    }

    public boolean descargarArchivo(Manual item) {
        if (driveService == null) return false;

        try {
            java.io.File carpetaDestino = new java.io.File(context.getFilesDir(),
                    "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa));
            if (!carpetaDestino.exists()) {
                carpetaDestino.mkdirs();
            }

            java.io.File archivoLocal = new java.io.File(carpetaDestino, item.nombre + ".pdf");
            FileOutputStream output = new FileOutputStream(archivoLocal);

            driveService.files().get(item.idDrive).executeMediaAndDownloadTo(output);
            output.close();
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error al descargar archivo: " + e.getMessage());
            return false;
        }
    }

    public List<String> obtenerCarpetasNivel1(String folderID) {
        List<String> carpetasNivel1 = new ArrayList<>();
        if (driveService == null) return carpetasNivel1;

        try {
            FileList result = driveService.files().list()
                    .setQ("'" + folderID + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    .setFields("files(id, name)")
                    .execute();

            for (File file : result.getFiles()) {
                carpetasNivel1.add(file.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener carpetas nivel 1: " + e.getMessage());
        }
        return carpetasNivel1;
    }

    public List<String> obtenerCarpetasNivel2(String folderID, String tipoSeleccionado) {
        List<String> marcas = new ArrayList<>();
        if (driveService == null) return marcas;

        try {
            String queryTipo = "name = '" + tipoSeleccionado + "' and '" + folderID + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
            FileList resultTipo = driveService.files().list().setQ(queryTipo).execute();

            if (resultTipo.getFiles().isEmpty()) return marcas;

            String idTipo = resultTipo.getFiles().get(0).getId();

            FileList resultMarcas = driveService.files().list()
                    .setQ("'" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    .setFields("files(name)")
                    .execute();

            for (File file : resultMarcas.getFiles()) {
                marcas.add(file.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener marcas: " + e.getMessage());
        }
        return marcas;
    }

    public List<String> obtenerCarpetasNivel3(String idTipo, String marcaSeleccionada) {
        List<String> modelos = new ArrayList<>();
        if (driveService == null) return modelos;

        try {
            String queryMarca = "name = '" + marcaSeleccionada + "' and '" + idTipo + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
            FileList resultMarca = driveService.files().list().setQ(queryMarca).execute();

            if (resultMarca.getFiles().isEmpty()) return modelos;

            String idMarca = resultMarca.getFiles().get(0).getId();

            FileList resultModelos = driveService.files().list()
                    .setQ("'" + idMarca + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    .setFields("files(name)")
                    .execute();

            for (File file : resultModelos.getFiles()) {
                modelos.add(file.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al obtener modelos: " + e.getMessage());
        }
        return modelos;
    }

    public String navegarACarpeta(String folderID, String tipo, String marca, String modelo) {
        if (driveService == null) return folderID;

        try {
            String currentFolderId = folderID;

            if (!tipo.isEmpty()) {
                String queryTipo = "name = '" + tipo + "' and '" + currentFolderId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                FileList resultTipo = driveService.files().list().setQ(queryTipo).execute();
                if (!resultTipo.getFiles().isEmpty()) {
                    currentFolderId = resultTipo.getFiles().get(0).getId();

                    if (!marca.isEmpty()) {
                        String queryMarca = "name = '" + marca + "' and '" + currentFolderId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                        FileList resultMarca = driveService.files().list().setQ(queryMarca).execute();
                        if (!resultMarca.getFiles().isEmpty()) {
                            currentFolderId = resultMarca.getFiles().get(0).getId();

                            if (!modelo.isEmpty()) {
                                String queryModelo = "name = '" + modelo + "' and '" + currentFolderId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false";
                                FileList resultModelo = driveService.files().list().setQ(queryModelo).execute();
                                if (!resultModelo.getFiles().isEmpty()) {
                                    currentFolderId = resultModelo.getFiles().get(0).getId();
                                }
                            }
                        }
                    }
                }
            }
            return currentFolderId;
        } catch (Exception e) {
            Log.e(TAG, "Error en navegación por carpetas: " + e.getMessage());
            return folderID;
        }
    }
}