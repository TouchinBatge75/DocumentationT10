package com.example.documentation.repositories;

import android.content.Context;
import android.util.Log;

import com.example.documentation.models.Manual;
import com.example.documentation.repositories.DriveRepository;
import com.example.documentation.repositories.LocalFilesRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BusquedaCategoriasHelper {
    private static final String TAG = "BusquedaCategoriasHelper";

    private DriveRepository driveRepository;
    private LocalFilesRepository localFilesRepository;
    private boolean tieneInternet;

    public BusquedaCategoriasHelper(DriveRepository driveRepo, LocalFilesRepository localRepo, boolean tieneInternet) {
        this.driveRepository = driveRepo;
        this.localFilesRepository = localRepo;
        this.tieneInternet = tieneInternet;
    }

    // Obtener tipos (combinando Drive y locales)
    public List<String> obtenerTipos(String folderID) {
        Set<String> tiposSet = new HashSet<>();

        // Obtener tipos de locales
        List<String> tiposLocales = localFilesRepository.obtenerTiposLocales();
        tiposSet.addAll(tiposLocales);

        // Obtener tipos de Drive (si hay internet)
        if (tieneInternet && driveRepository != null) {
            List<String> tiposDrive = driveRepository.obtenerCarpetasNivel1(folderID);
            tiposSet.addAll(tiposDrive);
        }

        List<String> tiposCombinados = new ArrayList<>(tiposSet);
        java.util.Collections.sort(tiposCombinados);
        return tiposCombinados;
    }

    // Obtener marcas (combinando)
    public List<String> obtenerMarcas(String folderID, String tipo) {
        Set<String> marcasSet = new HashSet<>();

        // Obtener marcas de locales
        List<String> marcasLocales = localFilesRepository.obtenerMarcasLocales(tipo);
        marcasSet.addAll(marcasLocales);

        // Obtener marcas de Drive
        if (tieneInternet && driveRepository != null) {
            List<String> marcasDrive = driveRepository.obtenerCarpetasNivel2(folderID, tipo);
            marcasSet.addAll(marcasDrive);
        }

        List<String> marcasCombinadas = new ArrayList<>(marcasSet);
        java.util.Collections.sort(marcasCombinadas);
        return marcasCombinadas;
    }

    // Obtener modelos (combinando)
    public List<String> obtenerModelos(String folderID, String tipo, String marca) {
        Set<String> modelosSet = new HashSet<>();

        // Obtener modelos de locales
        List<String> modelosLocales = localFilesRepository.obtenerModelosLocales(tipo, marca);
        modelosSet.addAll(modelosLocales);

        // Obtener modelos de Drive
        if (tieneInternet && driveRepository != null) {
            // Necesitamos obtener el ID del tipo primero
            String idTipo = obtenerIdTipo(folderID, tipo);
            if (idTipo != null) {
                List<String> modelosDrive = driveRepository.obtenerCarpetasNivel3(idTipo, marca);
                modelosSet.addAll(modelosDrive);
            }
        }

        List<String> modelosCombinados = new ArrayList<>(modelosSet);
        java.util.Collections.sort(modelosCombinados);
        return modelosCombinados;
    }

    // Ejecutar búsqueda combinada
    public List<Manual> buscarPorCategorias(String folderID, String tipo, String marca, String modelo) {
        List<Manual> resultados = new ArrayList<>();

        // Buscar en locales
        List<Manual> locales = localFilesRepository.buscarPorCategoriasLocales(tipo, marca, modelo);
        if (!locales.isEmpty()) {
            resultados.add(new Manual("📁 MANUALES LOCALES (" + locales.size() + ")", "", "local"));
            resultados.addAll(locales);
        }

        // Buscar en Drive (si hay internet)
        if (tieneInternet && driveRepository != null) {
            try {
                String folderIdDestino = driveRepository.navegarACarpeta(folderID, tipo, marca, modelo);

                // Listar archivos en esa carpeta
                String rutaRelativa = construirRutaRelativa(tipo, marca, modelo);
                List<Manual> driveResultados = driveRepository.listarCarpeta(folderIdDestino, rutaRelativa);

                if (!driveResultados.isEmpty()) {
                    resultados.add(new Manual("☁️ MANUALES EN DRIVE (" + driveResultados.size() + ")", "", "drive"));
                    resultados.addAll(driveResultados);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error buscando en Drive: " + e.getMessage());
            }
        }

        return resultados;
    }

    private String obtenerIdTipo(String folderID, String tipo) {
        // Este método debería estar en DriveRepository, lo agregamos aquí temporalmente
        if (driveRepository != null && tieneInternet) {
            try {
                // Usar navegación para obtener el ID
                return driveRepository.navegarACarpeta(folderID, tipo, "", "");
            } catch (Exception e) {
                Log.e(TAG, "Error obteniendo ID de tipo: " + e.getMessage());
            }
        }
        return null;
    }

    private String construirRutaRelativa(String tipo, String marca, String modelo) {
        StringBuilder ruta = new StringBuilder(tipo);
        if (!marca.isEmpty()) {
            ruta.append("/").append(marca);
            if (!modelo.isEmpty()) {
                ruta.append("/").append(modelo);
            }
        }
        return ruta.toString();
    }
}