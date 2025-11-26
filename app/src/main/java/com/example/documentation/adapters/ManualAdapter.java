package com.example.documentation.adapters;

import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.documentation.R;
import com.example.documentation.models.Manual;
import com.example.documentation.activities.MainActivity;
import com.example.documentation.activities.VisorManualActivity;
import com.example.documentation.activities.VisorMultimediaActivity;

import java.io.File;
import java.util.List;

public class ManualAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Manual> items;
    private MainActivity mainActivity;
    private static final String TAG = "ManualAdapter";

    private static final int TIPO_ITEM_NORMAL = 0;
    private static final int TIPO_HEADER_SECCION = 1;

    public ManualAdapter(List<Manual> items, MainActivity mainActivity) {
        this.items = items;
        this.mainActivity = mainActivity;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).esHeaderSeccion ? TIPO_HEADER_SECCION : TIPO_ITEM_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TIPO_HEADER_SECCION) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_header_seccion, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manual, parent, false);
            return new ItemViewHolder(view, mainActivity);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Manual item = items.get(position);

        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item);
        } else if (holder instanceof ItemViewHolder) {
            ((ItemViewHolder) holder).bind(item, position);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<Manual> nuevosItems) {
        items.clear();
        items.addAll(nuevosItems);
        notifyDataSetChanged();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvHeader;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvHeader = itemView.findViewById(R.id.tv_header_seccion);
        }

        public void bind(Manual item) {
            tvHeader.setText(item.nombre);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView nombre;
        TextView ruta;
        Button btnAccion;
        ProgressBar progressCircular;
        MainActivity mainActivity;

        public ItemViewHolder(@NonNull View itemView, MainActivity mainActivity) {
            super(itemView);
            this.mainActivity = mainActivity;
            nombre = itemView.findViewById(R.id.nombre_manual);
            ruta = itemView.findViewById(R.id.ruta_manual);
            btnAccion = itemView.findViewById(R.id.btnAccion);
            progressCircular = itemView.findViewById(R.id.progress_circular);
        }

        public void bind(Manual item, int position) {
            // Detectar tipo de archivo automáticamente
            item.tipoArchivo = item.detectarTipoArchivo();

            // Mostrar nombre con icono según el tipo
            String icono = item.obtenerIcono();
            String nombreMostrar = item.esCarpeta ? item.nombre : item.nombre;
            nombre.setText(icono + " " + nombreMostrar);

            if (item.rutaRelativa != null && !item.rutaRelativa.isEmpty()) {
                ruta.setText("Ruta: " + item.rutaRelativa);
                ruta.setVisibility(View.VISIBLE);
            } else {
                ruta.setVisibility(View.GONE);
            }

            // Configurar clic largo para eliminar
            itemView.setOnLongClickListener(v -> {
                if (item.esCarpeta) {
                    mainActivity.mostrarDialogoConfirmacionEliminacionCarpeta(item);
                    return true;
                } else if (item.descargado) {
                    mainActivity.mostrarDialogoConfirmacionEliminacion(item, position);
                    return true;
                }
                return false;
            });

            // Configurar clic normal en el item
            itemView.setOnClickListener(v -> {
                onItemClick(item, position);
            });

            actualizarBoton(item, position);
        }

        private void onItemClick(Manual item, int position) {
            if (item.esCarpeta) {
                // Navegar a carpeta
                String nuevaRuta = item.rutaRelativa.isEmpty() ? item.nombre : item.rutaRelativa + "/" + item.nombre;
                mainActivity.abrirCarpeta(item.idDrive, nuevaRuta);
            } else if (item.descargado) {
                // Abrir archivo descargado
                abrirArchivo(item);
            } else {
                // Descargar si no está descargado
                descargarArchivo(item, position);
            }
        }

        private void actualizarBoton(Manual item, int position) {
            btnAccion.setVisibility(View.VISIBLE);
            progressCircular.setVisibility(View.GONE);
            btnAccion.setEnabled(true);

            if (item.esCarpeta) {
                btnAccion.setText("Abrir carpeta");
                btnAccion.setOnClickListener(v -> {
                    String nuevaRuta = item.rutaRelativa.isEmpty() ? item.nombre : item.rutaRelativa + "/" + item.nombre;
                    mainActivity.abrirCarpeta(item.idDrive, nuevaRuta);
                });
            } else {
                if (item.descargado) {
                    btnAccion.setText("Abrir");
                    btnAccion.setOnClickListener(v -> abrirArchivo(item));
                } else {
                    btnAccion.setText("Descargar");
                    btnAccion.setOnClickListener(v -> descargarArchivo(item, position));
                }
            }
        }

        public void descargarArchivo(Manual item, int position) {
            if (mainActivity.getDriveRepository() == null) {
                Toast.makeText(mainActivity, "Error: Servicio de Drive no disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            btnAccion.setVisibility(View.GONE);
            progressCircular.setVisibility(View.VISIBLE);

            new Thread(() -> {
                boolean exito = mainActivity.getDriveRepository().descargarArchivo(item);

                mainActivity.runOnUiThread(() -> {
                    if (exito) {
                        item.descargado = true;
                        progressCircular.setVisibility(View.GONE);
                        btnAccion.setVisibility(View.VISIBLE);
                        btnAccion.setText("Abrir");
                        btnAccion.setEnabled(true);
                        btnAccion.setOnClickListener(v -> abrirArchivo(item));

                        mainActivity.actualizarItemDescargado(item, position);
                        Toast.makeText(mainActivity, "Descargado: " + item.nombre, Toast.LENGTH_SHORT).show();

                        // Abrir automáticamente después de descargar
                        abrirArchivo(item);
                    } else {
                        progressCircular.setVisibility(View.GONE);
                        btnAccion.setVisibility(View.VISIBLE);
                        btnAccion.setText("Descargar");
                        btnAccion.setEnabled(true);
                        btnAccion.setOnClickListener(v -> descargarArchivo(item, position));

                        Toast.makeText(mainActivity, "Error al descargar", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        }

        public void eliminarArchivo(Manual item, int position) {
            boolean eliminado = mainActivity.getLocalFilesRepository().eliminarArchivo(item);

            if (eliminado) {
                item.descargado = false;

                btnAccion.setVisibility(View.VISIBLE);
                progressCircular.setVisibility(View.GONE);
                btnAccion.setText("Descargar");
                btnAccion.setEnabled(true);
                btnAccion.setOnClickListener(v -> descargarArchivo(item, position));

                mainActivity.actualizarItemEliminado(item, position);
                Toast.makeText(mainActivity, "Archivo eliminado: " + item.nombre, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(mainActivity, "Error al eliminar el archivo", Toast.LENGTH_SHORT).show();
            }
        }

        private void abrirArchivo(Manual item) {
            // Obtener la ruta completa del archivo
            File archivoLocal = obtenerArchivoLocal(item);

            Log.d(TAG, "=== INTENTANDO ABRIR ARCHIVO ===");
            Log.d(TAG, "Nombre: " + item.nombre);
            Log.d(TAG, "Tipo detectado: " + item.tipoArchivo);
            Log.d(TAG, "Ruta local: " + archivoLocal.getAbsolutePath());
            Log.d(TAG, "¿Existe? " + archivoLocal.exists());

            if (archivoLocal.exists() && archivoLocal.length() > 0) {
                // Configurar la ruta completa y tipo de archivo en el objeto
                item.rutaCompleta = archivoLocal.getAbsolutePath();
                item.tipoArchivo = item.detectarTipoArchivo();

                Log.d(TAG, "Ruta completa: " + item.rutaCompleta);
                Log.d(TAG, "Tipo final: " + item.tipoArchivo);

                // Abrir según el tipo de archivo
                switch (item.tipoArchivo) {
                    case "pdf":
                        Log.d(TAG, "Abriendo como PDF...");
                        abrirPdf(item);
                        break;
                    case "video":
                        Log.d(TAG, "Abriendo como VIDEO...");
                        abrirConVisorMultimedia(item);
                        break;
                    case "imagen":
                        Log.d(TAG, "Abriendo como IMAGEN...");
                        abrirConVisorMultimedia(item);
                        break;
                    case "texto":
                        Log.d(TAG, "Abriendo como TEXTO...");
                        abrirConVisorMultimedia(item);
                        break;
                    case "audio":
                    case "desconocido":
                        Log.d(TAG, "Abriendo con app externa...");
                        abrirConAppExterna(item);
                        break;
                    default:
                        Log.e(TAG, "Tipo de archivo no soportado: " + item.tipoArchivo);
                        Toast.makeText(mainActivity, "Tipo de archivo no soportado: " + item.tipoArchivo, Toast.LENGTH_SHORT).show();
                        break;
                }
            } else {
                Log.e(TAG, "❌ El archivo no existe o está vacío");
                item.descargado = false;
                Toast.makeText(mainActivity, "El archivo no se encuentra. Intenta descargarlo nuevamente.", Toast.LENGTH_SHORT).show();
                actualizarBoton(item, getAdapterPosition());
            }
        }

        private File obtenerArchivoLocal(Manual item) {
            try {
                // Construir la ruta completa usando el nombre real del archivo
                File directorioBase = mainActivity.getFilesDir();
                String rutaCompleta = "Manuales/" +
                        (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") +
                        item.nombre; // Usar el nombre real, NO forzar .pdf

                File archivoLocal = new File(directorioBase, rutaCompleta);

                Log.d(TAG, "Buscando archivo local: " + archivoLocal.getAbsolutePath());
                Log.d(TAG, "¿Existe el archivo? " + archivoLocal.exists());

                if (archivoLocal.exists()) {
                    Log.d(TAG, "Tamaño del archivo: " + archivoLocal.length() + " bytes");
                }

                return archivoLocal;

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener archivo local: " + e.getMessage());
                return new File(item.nombre); // Fallback
            }
        }

        private void abrirPdf(Manual item) {
            Intent intent = new Intent(mainActivity, VisorManualActivity.class);
            intent.putExtra("archivo_path", item.rutaCompleta);
            mainActivity.startActivity(intent);
        }

        private void abrirConVisorMultimedia(Manual item) {
            Intent intent = new Intent(mainActivity, VisorMultimediaActivity.class);
            intent.putExtra("manual", item);
            mainActivity.startActivity(intent);
        }

        private void abrirConAppExterna(Manual item) {
            try {
                File file = new File(item.rutaCompleta);
                if (file.exists()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(android.net.Uri.fromFile(file), obtenerMimeType(item));
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    if (intent.resolveActivity(mainActivity.getPackageManager()) != null) {
                        mainActivity.startActivity(intent);
                    } else {
                        Toast.makeText(mainActivity, "No hay app para abrir este tipo de archivo", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(mainActivity, "El archivo no existe", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(mainActivity, "Error al abrir archivo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        private String obtenerMimeType(Manual item) {
            switch (item.tipoArchivo) {
                case "pdf":
                    return "application/pdf";
                case "video":
                    return "video/*";
                case "imagen":
                    return "image/*";
                case "audio":
                    return "audio/*";
                case "texto":
                    return "text/plain";
                default:
                    return "*/*";
            }
        }
    }
}