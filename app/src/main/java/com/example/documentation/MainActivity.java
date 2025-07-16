package com.example.documentation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;


import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {



    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private ArrayList<Manual> listaManuales = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "Pulsa el botón para descargar manuales desde Drive", Toast.LENGTH_LONG).show();

        Button btnDescargar = findViewById(R.id.btn_descargar_manuales);
        btnDescargar.setOnClickListener(v -> iniciarAutenticacionGoogle());

        // Configuración de Google Sign-In con permiso de lectura de Drive
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id)) // ← Esto es crucial
                .requestScopes(new Scope("https://www.googleapis.com/auth/drive.readonly"))
                .build();

    }

    private void iniciarAutenticacionGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            if (task.isSuccessful()) {
                GoogleSignInAccount account = task.getResult();
                if (account != null) {
                    Toast.makeText(this, "Autenticado como: " + account.getEmail(), Toast.LENGTH_SHORT).show();
                    obtenerManualesDesdeDrive(account);
                }
            } else {
                Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void obtenerManualesDesdeDrive(GoogleSignInAccount account) {
        // Aquí vendrá el acceso a Google Drive para listar y descargar archivos
    }

    public void mostrarManuales(ArrayList<Manual> manuales) {
        listaManuales.clear();
        listaManuales.addAll(manuales);

        ManualAdapter adapter = new ManualAdapter(this, listaManuales);
        ListView lista = findViewById(R.id.lista_manuales);
        lista.setAdapter(adapter);
    }
}
