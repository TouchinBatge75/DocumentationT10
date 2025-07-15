package com.example.documentation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

import android.content.Intent;
import android.util.Log;
import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.services.drive.DriveScopes;

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
    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private ArrayList<Manual> listaManuales = new ArrayList<>(); //los datos vendrán desde Drive

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_READONLY))  // Solo lectura
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

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

    private void iniciarAutenticacionGoogle() { //inicia la autenticacion
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                Toast.makeText(this, "Autenticado como: " + account.getEmail(), Toast.LENGTH_SHORT).show();

                // Aquí va el siguiente paso: acceder a Drive y listar archivos
                obtenerManualesDesdeDrive(account);
            } else {
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void obtenerManualesDesdeDrive(GoogleSignInAccount account) {
        // Aquí haremos la conexión con Google Drive API usando la cuenta autenticada.
    }


}



