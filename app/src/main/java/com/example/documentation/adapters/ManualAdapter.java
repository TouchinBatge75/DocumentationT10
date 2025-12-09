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
import java.util.ArrayList;
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
                // ❌ FALTA: Guardar la ruta original antes de modificar
                String nombreOriginal = item.nombre;
                String rutaRelativaOriginal = item.rutaRelativa;

                boolean exito = mainActivity.getDriveRepository().descargarArchivo(item);

                mainActivity.runOnUiThread(() -> {
                    if (exito) {
                        // ✅ Asegurar que el item tenga los datos correctos
                        item.descargado = true;

                        // ✅ Obtener la ruta real después de descargar
                        File archivoLocal = obtenerArchivoLocal(item);
                        if (archivoLocal.exists()) {
                            item.rutaCompleta = archivoLocal.getAbsolutePath();
                            Log.d(TAG, "✅ Ruta actualizada después de descargar: " + item.rutaCompleta);
                        }

                        progressCircular.setVisibility(View.GONE);
                        btnAccion.setVisibility(View.VISIBLE);
                        btnAccion.setText("Abrir");
                        btnAccion.setEnabled(true);
                        btnAccion.setOnClickListener(v -> abrirArchivo(item));

                        mainActivity.actualizarItemDescargado(item, position);
                        Toast.makeText(mainActivity, "Descargado: " + item.nombre, Toast.LENGTH_SHORT).show();


                    } else {
                        // ❌ Restaurar nombre original si falló
                        item.nombre = nombreOriginal;
                        item.rutaRelativa = rutaRelativaOriginal;

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
            Log.d(TAG, "Nombre original: " + item.nombre);
            Log.d(TAG, "Nombre limpio: " + item.obtenerNombreLimpio());
            Log.d(TAG, "Tipo detectado inicial: " + item.tipoArchivo);
            Log.d(TAG, "Ruta local: " + archivoLocal.getAbsolutePath());
            Log.d(TAG, "¿Existe? " + archivoLocal.exists());

            if (archivoLocal.exists() && archivoLocal.length() > 0) {
                // ✅ FORZAR DETECCIÓN CORRECTA BASADA EN EL ARCHIVO REAL
                String nombreArchivoReal = archivoLocal.getName();

                // Detectar tipo basado en el nombre REAL del archivo
                String tipoReal = "desconocido";
                if (nombreArchivoReal.toLowerCase().endsWith(".pdf")) {
                    tipoReal = "pdf";
                } else if (nombreArchivoReal.toLowerCase().matches(".*\\.(mp4|avi|mkv|mov|wmv)$")) {
                    tipoReal = "video";
                } else if (nombreArchivoReal.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                    tipoReal = "imagen";
                }

                Log.d(TAG, "Nombre real del archivo: " + nombreArchivoReal);
                Log.d(TAG, "Tipo real detectado: " + tipoReal);

                // Configurar la ruta completa y tipo de archivo en el objeto
                item.rutaCompleta = archivoLocal.getAbsolutePath();
                item.tipoArchivo = tipoReal; // Usar el tipo detectado del archivo real

                Log.d(TAG, "Ruta completa: " + item.rutaCompleta);
                Log.d(TAG, "Tipo final asignado: " + item.tipoArchivo);

                // Abrir según el tipo de archivo
                switch (item.tipoArchivo) {
                    case "pdf":
                        Log.d(TAG, "✅ Abriendo como PDF...");
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

        // En ManualAdapter.java, agrega este método:
        private String detectarTipoDesdeNombreArchivo(String nombreArchivo) {
            if (nombreArchivo == null) return "desconocido";

            nombreArchivo = nombreArchivo.toLowerCase().trim();

            // Limpiar dobles puntos
            nombreArchivo = nombreArchivo.replace("..pdf", ".pdf");

            if (nombreArchivo.endsWith(".pdf")) {
                return "pdf";
            } else if (nombreArchivo.matches(".*\\.(mp4|avi|mkv|mov|wmv)$")) {
                return "video";
            } else if (nombreArchivo.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                return "imagen";
            } else if (nombreArchivo.matches(".*\\.(mp3|wav|ogg|m4a)$")) {
                return "audio";
            } else if (nombreArchivo.matches(".*\\.(txt|doc|docx)$")) {
                return "texto";
            }

            return "desconocido";
        }
        private File obtenerArchivoLocal(Manual item) {
            try {
                File directorioBase = mainActivity.getFilesDir();
                String rutaBase = "Manuales/" +
                        (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/");

                Log.d(TAG, "🔍 Buscando archivo en: " + rutaBase);

                // Lista de nombres a probar (en orden de prioridad)
                List<File> archivosAPrueba = new ArrayList<>();

                // 1. Nombre exacto del item
                archivosAPrueba.add(new File(directorioBase, rutaBase + item.nombre));

                // 2. Si el item no tiene .pdf pero es tipo PDF
                if (item.tipoArchivo.equals("pdf") && !item.nombre.toLowerCase().endsWith(".pdf")) {
                    archivosAPrueba.add(new File(directorioBase, rutaBase + item.nombre + ".pdf"));
                }

                // 3. Si el item tiene .pdf, probar sin extensión
                if (item.nombre.toLowerCase().endsWith(".pdf")) {
                    String nombreSinExtension = item.nombre.substring(0, item.nombre.length() - 4);
                    archivosAPrueba.add(new File(directorioBase, rutaBase + nombreSinExtension));
                }

                // 4. Buscar cualquier archivo que coincida (sin importar extensión)
                File carpeta = new File(directorioBase, rutaBase);
                if (carpeta.exists() && carpeta.isDirectory()) {
                    File[] archivosEnCarpeta = carpeta.listFiles();
                    if (archivosEnCarpeta != null) {
                        for (File archivo : archivosEnCarpeta) {
                            if (archivo.isFile()) {
                                String nombreArchivo = archivo.getName();
                                String nombreSinExt = nombreArchivo.replace(".pdf", "").replace(".PDF", "");
                                if (nombreSinExt.equals(item.nombre) ||
                                        nombreArchivo.equals(item.nombre) ||
                                        nombreSinExt.equals(item.nombre.replace(".pdf", ""))) {
                                    archivosAPrueba.add(archivo);
                                }
                            }
                        }
                    }
                }

                // Probar cada posibilidad
                for (File archivo : archivosAPrueba) {
                    if (archivo.exists() && archivo.isFile() && archivo.length() > 0) {
                        Log.d(TAG, "✅ Archivo encontrado: " + archivo.getAbsolutePath());
                        Log.d(TAG, "✅ Tamaño: " + archivo.length() + " bytes");
                        return archivo;
                    }
                }

                // Si no se encontró, devolver la ruta más probable
                File rutaMasProbable = new File(directorioBase, rutaBase + item.nombre +
                        (item.tipoArchivo.equals("pdf") ? ".pdf" : ""));
                Log.e(TAG, "❌ Archivo no encontrado. Ruta esperada: " + rutaMasProbable.getAbsolutePath());
                return rutaMasProbable;

            } catch (Exception e) {
                Log.e(TAG, "Error en obtenerArchivoLocal: " + e.getMessage(), e);
                return new File(item.nombre);
            }
        }
        private void abrirPdf(Manual item) {
            File archivo = obtenerArchivoLocal(item);

            if (archivo.exists()) {
                Log.d(TAG, "✅ Archivo confirmado: " + archivo.getAbsolutePath());
                Log.d(TAG, "✅ Tamaño: " + archivo.length() + " bytes");

                Intent intent = new Intent(mainActivity, VisorManualActivity.class);
                intent.putExtra(VisorManualActivity.EXTRA_MANUAL_PATH, archivo.getAbsolutePath());

                // Depuración: verificar extra
                Log.d(TAG, "✅ Pasando extra: " + VisorManualActivity.EXTRA_MANUAL_PATH +
                        " = " + archivo.getAbsolutePath());

                mainActivity.startActivity(intent);
            } else {
                Log.e(TAG, "❌ Archivo no existe para abrir: " + archivo.getAbsolutePath());
                Toast.makeText(mainActivity, "El archivo no existe: " + archivo.getName(), Toast.LENGTH_SHORT).show();
            }
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