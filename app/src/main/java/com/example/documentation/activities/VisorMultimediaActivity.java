package com.example.documentation.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    private LinearLayout layoutVideo, layoutImagen, layoutTexto, layoutNoSoportado;
    private Button btnAbrirExterno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visor_multimedia);

        // Obtener el manual desde el intent
        manual = (Manual) getIntent().getSerializableExtra("manual");

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
    }

    private void configurarVisor() {
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
        layoutVideo.setVisibility(View.VISIBLE);

        try {
            File videoFile = new File(manual.rutaCompleta);
            if (videoFile.exists()) {
                Uri videoUri = Uri.fromFile(videoFile);
                videoView.setVideoURI(videoUri);

                // Configurar controles de video
                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                videoView.setMediaController(mediaController);

                videoView.start();
            } else {
                Toast.makeText(this, "El archivo de video no existe", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al reproducir video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarImagen() {
        layoutImagen.setVisibility(View.VISIBLE);

        try {
            File imageFile = new File(manual.rutaCompleta);
            if (imageFile.exists()) {
                Uri imageUri = Uri.fromFile(imageFile);
                imageView.setImageURI(imageUri);
            } else {
                Toast.makeText(this, "El archivo de imagen no existe", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al cargar imagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void configurarTexto() {
        layoutTexto.setVisibility(View.VISIBLE);

        try {
            File textFile = new File(manual.rutaCompleta);
            if (textFile.exists()) {
                // Leer archivo de texto (esto es básico, para archivos pequeños)
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
            } else {
                textView.setText("El archivo de texto no existe");
            }
        } catch (Exception e) {
            textView.setText("Error al leer archivo: " + e.getMessage());
        }
    }

    private void abrirConAppExterna() {
        try {
            File file = new File(manual.rutaCompleta);
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

                intent.setDataAndType(uri, mimeType);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    finish(); // Cerrar esta actividad ya que se abre otra app
                } else {
                    Toast.makeText(this, "No hay app para abrir este tipo de archivo", Toast.LENGTH_SHORT).show();
                    mostrarNoSoportado();
                }
            } else {
                Toast.makeText(this, "El archivo no existe", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al abrir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void mostrarNoSoportado() {
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

    private void configurarBotones() {
        btnAbrirExterno.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirConAppExterna();
            }
        });

        // Botón de retroceso
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (manual.tipoArchivo.equals("video")) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (manual.tipoArchivo.equals("video")) {
            videoView.stopPlayback();
        }
    }
}