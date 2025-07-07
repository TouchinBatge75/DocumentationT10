package com.example.documentation;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

// This class stores the name and link of each manual
class Manual {
    String Nombre;
    String enlace;

    Manual(String Nombre, String enlace) {
        this.Nombre = Nombre;
        this.enlace = enlace;
    }
}

public class MainActivity extends AppCompatActivity {
    // ArrayList to store all the manuals read from the .txt file
    private ArrayList<Manual> listaManuales = new ArrayList<>();

    // Direct download link to the .txt file hosted in Google Drive
    private static final String LINK_TXT = ""; // Aquí pon el enlace tipo: https://drive.google.com/uc?export=download&id=...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Loads the layout of the activity

        // Start downloading the manual list when the app launches
        descargarListaManuales();
    }

    // This method downloads and reads the manual list from the .txt file
    private void descargarListaManuales() {
        new Thread(() -> {
            try {
                // Connect to the URL where the .txt file is stored
                URL url = new URL(LINK_TXT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // Create a reader to read the file line by line
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String linea;

                // Read each line of the file
                while ((linea = reader.readLine()) != null) {
                    if (!linea.trim().isEmpty() && linea.contains("|")) {
                        // Separate the name and link using "|"
                        String[] partes = linea.split("\\|");
                        if (partes.length == 2) {
                            String nombre = partes[0].trim();
                            String enlace = partes[1].trim();
                            listaManuales.add(new Manual(nombre, enlace)); // Save the manual in the list
                        }
                    }
                }

                reader.close(); // Close the reader

                // Display how many manuals were loaded
                runOnUiThread(() -> Toast.makeText(this, "Manuales encontrados: " + listaManuales.size(), Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error al leer el archivo de manuales", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
