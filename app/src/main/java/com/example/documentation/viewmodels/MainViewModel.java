package com.example.documentation.viewmodels;

import androidx.lifecycle.ViewModel;

import com.example.documentation.models.Manual;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainViewModel extends ViewModel {
    // Estado de navegación que queremos preservar
    public String currentFolderID = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";
    public String currentRelativePath = "";
    public Stack<String> pilaCarpetas = new Stack<>();
    public Stack<String> pilaRutas = new Stack<>();
    public boolean isAuthenticated = false;
    public String buscarDespuesDeAutenticar = "";

    // Cache de datos de Drive
    public List<Manual> cacheCompletoDrive = new ArrayList<>();

    public MainViewModel() {
        // Inicializar solo si está vacío
        if (pilaCarpetas.isEmpty()) {
            pilaCarpetas.push(currentFolderID);
            pilaRutas.push("");
        }
    }

    // Método para limpiar búsqueda y volver al estado normal
    public void resetToRoot() {
        pilaCarpetas.clear();
        pilaRutas.clear();
        currentFolderID = "11a5MPz8K1vFk7HhblB3DGW21CTRZn-uW";
        currentRelativePath = "";
        pilaCarpetas.push(currentFolderID);
        pilaRutas.push("");
    }
}