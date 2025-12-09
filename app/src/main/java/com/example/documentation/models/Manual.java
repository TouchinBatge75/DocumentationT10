package com.example.documentation.models;

import java.io.Serializable;

public class Manual implements Serializable {
    public String nombre;
    public String rutaRelativa;
    public boolean esCarpeta = false;
    public boolean descargado = false;
    public String idDrive;
    public boolean esHeaderSeccion = false;
    public String tipoHeader;

    // NUEVO: Campos para tipo de archivo
    public String tipoArchivo = "";
    public String rutaCompleta = "";
    public String mimeType = "";

    // CONSTRUCTORES:

    // Constructor vacío
    public Manual() {}

    // Constructor básico (nombre + ruta)
    public Manual(String nombre, String rutaRelativa) {
        this.nombre = nombre;
        this.rutaRelativa = rutaRelativa;
        this.tipoArchivo = detectarTipoArchivo();
    }

    // Constructor para headers
    public Manual(String nombre, String rutaRelativa, String tipoHeader) {
        this.nombre = nombre;
        this.rutaRelativa = rutaRelativa;
        this.esHeaderSeccion = true;
        this.tipoHeader = tipoHeader;
        this.tipoArchivo = "header";
    }

    // Constructor para DriveRepository (idDrive, nombre, esCarpeta, mimeType, rutaRelativa)
    public Manual(String idDrive, String nombre, boolean esCarpeta, String mimeType, String rutaRelativa) {
        this.idDrive = idDrive;
        this.nombre = nombre;
        this.esCarpeta = esCarpeta;
        this.mimeType = mimeType;
        this.rutaRelativa = rutaRelativa;
        this.tipoArchivo = detectarTipoArchivo();
    }
    // En Manual.java, agrega este método:
    public String obtenerNombreLimpio() {
        if (nombre == null) return "";

        // Eliminar dobles puntos y espacios al final antes de .pdf
        String nombreLimpio = nombre.trim();

        // Corregir "..pdf" a ".pdf"
        nombreLimpio = nombreLimpio.replace("..pdf", ".pdf");
        nombreLimpio = nombreLimpio.replace("..PDF", ".PDF");

        // Eliminar múltiples puntos consecutivos
        while (nombreLimpio.contains("..")) {
            nombreLimpio = nombreLimpio.replace("..", ".");
        }

        return nombreLimpio;
    }

    // Modifica detectarTipoArchivo para usar nombre limpio:
    public String detectarTipoArchivo() {
        if (esCarpeta) return "carpeta";
        if (esHeaderSeccion) return "header";

        String nombreParaAnalizar = obtenerNombreLimpio();

        if (nombreParaAnalizar.contains(".")) {
            int lastDotIndex = nombreParaAnalizar.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < nombreParaAnalizar.length() - 1) {
                String extension = nombreParaAnalizar.substring(lastDotIndex + 1).toLowerCase();

                switch (extension) {
                    case "pdf": return "pdf";
                    case "mp4": case "avi": case "mkv": case "mov": case "wmv": return "video";
                    case "jpg": case "jpeg": case "png": case "gif": case "bmp": case "webp": return "imagen";
                    case "mp3": case "wav": case "ogg": case "m4a": return "audio";
                    case "txt": case "doc": case "docx": return "texto";
                    default: return "desconocido";
                }
            }
        }
        return "desconocido";
    }
    // Método para obtener el icono
    public String obtenerIcono() {
        switch (tipoArchivo) {
            case "pdf":
                return "📄";
            case "video":
                return "🎬";
            case "imagen":
                return "🖼️";
            case "audio":
                return "🎵";
            case "texto":
                return "📝";
            case "carpeta":
                return "📁";
            default:
                return "📎";
        }
    }
}
