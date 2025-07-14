package com.example.documentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

// Clase para representar un manual
class Manual {
    String Nombre;
    String enlace;

    Manual(String Nombre, String enlace) {
        this.Nombre = Nombre;
        this.enlace = enlace;
    }
}

public class MainActivity extends AppCompatActivity {

    private ArrayList<Manual> listaManuales = new ArrayList<>(); // Se seguirá usando, pero los datos vendrán desde Drive

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Aquí eventualmente pondremos: iniciar autenticación y listar archivos desde Google Drive
        Toast.makeText(this, "Pulsa el botón para descargar manuales desde Drive", Toast.LENGTH_LONG).show();
        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(v -> {
            iniciarAutenticacionGoogle();
        });
    }

    // Este método lo usaremos después para llenar la lista una vez que descarguemos desde Google Drive
    public void mostrarManuales(ArrayList<Manual> manuales) {
        listaManuales.clear();
        listaManuales.addAll(manuales);

        ManualAdapter adapter = new ManualAdapter(this, listaManuales);
        ListView lista = findViewById(R.id.lista_manuales);
        lista.setAdapter(adapter);
    }
}
