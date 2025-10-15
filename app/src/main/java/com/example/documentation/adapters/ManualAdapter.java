package com.example.documentation.adapters;

import android.content.Intent;
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

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class ManualAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<Manual> items;
    private MainActivity mainActivity;

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
            nombre.setText(item.esCarpeta ? item.nombre : item.nombre.replace(".pdf", ""));

            if (item.rutaRelativa != null && !item.rutaRelativa.isEmpty()) {
                ruta.setText("Ruta: " + item.rutaRelativa);
                ruta.setVisibility(View.VISIBLE);
            } else {
                ruta.setVisibility(View.GONE);
            }

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

            actualizarBoton(item, position);
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
            File archivoLocal = new File(mainActivity.getFilesDir(),
                    "Manuales/" + (item.rutaRelativa.isEmpty() ? "" : item.rutaRelativa + "/") + item.nombre + ".pdf");
            if (archivoLocal.exists()) {
                Intent intent = new Intent(mainActivity, VisorManualActivity.class);
                intent.putExtra(VisorManualActivity.EXTRA_MANUAL_PATH, archivoLocal.getAbsolutePath());
                mainActivity.startActivity(intent);
            } else {
                item.descargado = false;
                Toast.makeText(mainActivity, "Archivo no encontrado", Toast.LENGTH_SHORT).show();
            }
        }
    }
}