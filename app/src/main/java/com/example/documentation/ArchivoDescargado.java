package com.example.documentation;

public class ArchivoDescargado {
    private String id;
    private String nombre;
    private String modifiedTime;

    public ArchivoDescargado() {}

    public ArchivoDescargado(String id, String nombre, String modifiedTime) {
        this.id = id;
        this.nombre = nombre;
        this.modifiedTime = modifiedTime;
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public String getModifiedTime() { return modifiedTime; }

    public void setId(String id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setModifiedTime(String modifiedTime) { this.modifiedTime = modifiedTime; }
}
