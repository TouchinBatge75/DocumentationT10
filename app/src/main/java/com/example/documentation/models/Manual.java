package com.example.documentation.models;

public class Manual {
    public String idDrive;
    public String nombre;
    public boolean esCarpeta;
    public String enlace;
    public String rutaRelativa;
    public boolean descargado;
    public boolean esHeaderSeccion;
    public String tipoSeccion;

    // Constructor principal
    public Manual(String idDrive, String nombre, boolean esCarpeta, String enlace, String rutaRelativa) {
        this.idDrive = idDrive;
        this.nombre = nombre;
        this.esCarpeta = esCarpeta;
        this.enlace = enlace;
        this.rutaRelativa = rutaRelativa;
        this.descargado = false;
        this.esHeaderSeccion = false;
        this.tipoSeccion = "";
    }

    // Constructor para headers
    public Manual(String nombreSeccion, String tipoSeccion) {
        this.idDrive = "";
        this.nombre = nombreSeccion;
        this.esCarpeta = false;
        this.enlace = "";
        this.rutaRelativa = "";
        this.descargado = false;
        this.esHeaderSeccion = true;
        this.tipoSeccion = tipoSeccion;
    }

    // Constructor vacío
    public Manual() {}
}