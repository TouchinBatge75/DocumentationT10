// Manual.java
package com.example.documentation;

public class Manual {
    public String idDrive;       // ID en Drive
    public String Nombre;        // Nombre del archivo o carpeta
    public boolean esCarpeta;    // true si es carpeta
    public String enlace;        // URL de descarga (solo para archivos)
    public String rutaRelativa;  // Carpeta relativa dentro de "Manuales"

    public Manual(String idDrive, String Nombre, boolean esCarpeta, String enlace, String rutaRelativa) {
        this.idDrive = idDrive;
        this.Nombre = Nombre;
        this.esCarpeta = esCarpeta;
        this.enlace = enlace;
        this.rutaRelativa = rutaRelativa;
    }
}

