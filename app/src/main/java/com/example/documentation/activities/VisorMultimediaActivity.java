package com.example.documentation.activities;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.documentation.R;
import com.example.documentation.models.Manual;
import java.io.File;

public class VisorMultimediaActivity extends AppCompatActivity {

    private Manual manual;
    private VideoView videoView;
    private ImageView imageView;
    private TextView textView;
    private RelativeLayout layoutVideo;
    private ScrollView layoutImagen;
    private ScrollView layoutTexto;
    private LinearLayout layoutNoSoportado;
    private Button btnAbrirExterno;
    private static final String TAG = "VisorMultimedia";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_multimedia);

        Log.d(TAG, "Iniciando VisorMultimediaActivity");

        // Obtener el manual desde el intent
        manual = (Manual) getIntent().getSerializableExtra("manual");

        if (manual == null) {
            Log.e(TAG, "Error: manual es null");
            Toast.makeText(this, "Error: No se pudo cargar el archivo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d(TAG, "Archivo a abrir: " + manual.nombre + " - Tipo: " + manual.tipoArchivo);

        // Inicializar vistas
        initViews();

        // Configurar la interfaz según el tipo de archivo
        configurarVisor();

        // Configurar botones
        configurarBotones();
    }

    private void initViews() {
        videoView = findViewById(R.id.videoView);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.textView);
        layoutVideo = findViewById(R.id.layoutVideo);
        layoutImagen = findViewById(R.id.layoutImagen);
        layoutTexto = findViewById(R.id.layoutTexto);
        layoutNoSoportado = findViewById(R.id.layoutNoSoportado);
        btnAbrirExterno = findViewById(R.id.btnAbrirExterno);

        // Mostrar nombre del archivo en el título
        TextView tvTitulo = findViewById(R.id.tvTitulo);
        tvTitulo.setText(manual.nombre);

        Log.d(TAG, "Vistas inicializadas correctamente");
    }

    private void configurarVisor() {
        Log.d(TAG, "Configurando visor para tipo: " + manual.tipoArchivo);

        // Ocultar todos los layouts primero
        ocultarTodosLosLayouts();

        switch (manual.tipoArchivo) {
            case "video":
                configurarVideo();
                break;
            case "imagen":
                configurarImagen();
                break;
            case "texto":
                configurarTexto();
                break;
            case "pdf":
            case "audio":
                abrirConAppExterna();
                break;
            default:
                mostrarNoSoportado();
                break;
        }
    }

    private void configurarVideo() {
        Log.d(TAG, "Configurando video...");
        layoutVideo.setVisibility(View.VISIBLE);

        try {
            File videoFile = new File(manual.rutaCompleta);
            Log.d(TAG, "Ruta del video: " + manual.rutaCompleta);
            Log.d(TAG, "¿Existe el archivo? " + videoFile.exists());
            Log.d(TAG, "Tamaño del archivo: " + (videoFile.exists() ? videoFile.length() + " bytes" : "No existe"));

            if (videoFile.exists() && videoFile.length() > 0) {
                Uri videoUri = Uri.fromFile(videoFile);

                // Configurar listeners de error primero
                videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.e(TAG, "Error en VideoView - what: " + what + ", extra: " + extra);
                        Toast.makeText(VisorMultimediaActivity.this,
                                "Error al reproducir el video (código: " + what + ")", Toast.LENGTH_LONG).show();
                        return true;
                    }
                });

                videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        Log.d(TAG, "Video preparado correctamente, iniciando reproducción...");
                        videoView.start();
                    }
                });

                videoView.setVideoURI(videoUri);

                // Configurar controles de video
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                videoView.setMediaController(mediaController);

                Log.d(TAG, "Video configurado correctamente");

            } else {
                String mensaje = "El archivo de video no existe o está vacío";
                Log.e(TAG, mensaje);
                Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show();
                mostrarNoSoportado();
            }
        } catch (Exception e) {
            Log.e(TAG, "Excepción al configurar video: " + e.getMessage(), e);
            Toast.makeText(this, "Error crítico: " + e.getMessage(), Toast.LENGTH_LONG).show();
            mostrarNoSoportado();
        }
    }

    private void configurarImagen() {
        Log.d(TAG, "Configurando imagen...");
        layoutImagen.setVisibility(View.VISIBLE); // Ya no hay scrollViewContent

        try {
            File imageFile = new File(manual.rutaCompleta);
            Log.d(TAG, "Ruta de imagen: " + manual.rutaCompleta);

            if (imageFile.exists()) {
                Uri imageUri = Uri.fromFile(imageFile);
                imageView.setImageURI(imageUri);
                Log.d(TAG, "Imagen cargada correctamente");
            } else {
                Log.e(TAG, "El archivo de imagen no existe");
                Toast.makeText(this, "El archivo de imagen no existe", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar imagen: " + e.getMessage());
            Toast.makeText(this, "Error al cargar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarTexto() {
        Log.d(TAG, "Configurando texto...");
        layoutTexto.setVisibility(View.VISIBLE); // Ya no hay scrollViewContent

        try {
            File textFile = new File(manual.rutaCompleta);
            Log.d(TAG, "Ruta de texto: " + manual.rutaCompleta);

            if (textFile.exists()) {
                // Leer archivo de texto
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.FileReader(textFile)
                );
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                textView.setText(content.toString());
                Log.d(TAG, "Texto cargado correctamente - " + content.length() + " caracteres");
            } else {
                textView.setText("El archivo de texto no existe");
                Log.e(TAG, "El archivo de texto no existe");
            }
        } catch (Exception e) {
            String errorMsg = "Error al leer archivo: " + e.getMessage();
            textView.setText(errorMsg);
            Log.e(TAG, errorMsg);
        }
    }

    private void mostrarNoSoportado() {
        Log.d(TAG, "Mostrando layout no soportado");
        layoutNoSoportado.setVisibility(View.VISIBLE);
        TextView tvTipoNoSoportado = findViewById(R.id.tvTipoNoSoportado);
        tvTipoNoSoportado.setText("Tipo: " + manual.tipoArchivo + "\nArchivo: " + manual.nombre);
    }

    private void ocultarTodosLosLayouts() {
        layoutVideo.setVisibility(View.GONE);
        layoutImagen.setVisibility(View.GONE);
        layoutTexto.setVisibility(View.GONE);
        layoutNoSoportado.setVisibility(View.GONE);
    }

    private void abrirConAppExterna() {
        Log.d(TAG, "Abriendo con app externa...");

        try {
            File file = new File(manual.rutaCompleta);
            Log.d(TAG, "Ruta para app externa: " + manual.rutaCompleta);

            if (file.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(file);

                // Establecer el MIME type correcto
                String mimeType;
                switch (manual.tipoArchivo) {
                    case "pdf":
                        mimeType = "application/pdf";
                        break;
                    case "audio":
                        mimeType = "audio/*";
                        break;
                    default:
                        mimeType = "*/*";
                        break;
                }

                Log.d(TAG, "MIME Type: " + mimeType);
                intent.setDataAndType(uri, mimeType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    Log.d(TAG, "Iniciando actividad externa...");
                    startActivity(intent);
                    finish();
                } else {
                    Log.e(TAG, "No hay app para abrir este tipo de archivo");
                    Toast.makeText(this, "No hay app para abrir este tipo de archivo", Toast.LENGTH_SHORT).show();
                    mostrarNoSoportado();
                }
            } else {
                Log.e(TAG, "El archivo no existe para app externa");
                Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al abrir con app externa: " + e.getMessage());
            Toast.makeText(this, "Error al abrir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarBotones() {
        btnAbrirExterno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Botón abrir externo clickeado");
                abrirConAppExterna();
            }
        });

        // Botón de retroceso
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Botón retroceso clickeado");
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        if (manual != null && manual.tipoArchivo != null && manual.tipoArchivo.equals("video")) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (manual != null && manual.tipoArchivo != null && manual.tipoArchivo.equals("video")) {
            videoView.stopPlayback();
        }
    }
}