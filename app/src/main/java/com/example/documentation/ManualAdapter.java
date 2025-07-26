package com.example.documentation;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URL;
import java.net.HttpURLConnection;



import java.io.File;
import java.util.ArrayList;

public class ManualAdapter extends BaseAdapter
{
    private Context context;
    private ArrayList<Manual> listaManuales;
    public ManualAdapter(Context context, ArrayList<Manual> listaManuales)
    {
        this.context=context;
        this.listaManuales=listaManuales;
    }

    @Override
    public int getCount()
    {
        return listaManuales.size();//Cantidad de manuales
    }

    @Override
    public Object getItem(int position) {
        return listaManuales.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position; //No se usa ID reales, solo posicion
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView==null)
        {
            convertView=LayoutInflater.from(context).inflate(R.layout.item_manual, parent, false);
        }
        TextView nombreManual= convertView.findViewById(R.id.nombre_manual);


        //Obtenemos manual actual
        Manual manual = listaManuales.get(position);

        //Verificamos si ya tenemos el arrchivo descargado localmente
        File archivo = new File(context.getFilesDir(), manual.Nombre+".pdf");


        return convertView;
    }
    private void descargarManual(Manual manual, TextView estadoManual) {
        new Thread(() -> {
            try {
                URL url = new URL(manual.enlace);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                java.io.InputStream input = connection.getInputStream();
                File archivo = new File(context.getFilesDir(), manual.Nombre + ".pdf");
                java.io.FileOutputStream output = new java.io.FileOutputStream(archivo);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.close();
                input.close();

                ((MainActivity) context).runOnUiThread(() -> {
                    estadoManual.setText("Abrir");
                    estadoManual.setTextColor(0xFF2E7D32); // Verde
                    Toast.makeText(context, "Descargado: " + manual.Nombre, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                ((MainActivity) context).runOnUiThread(() ->
                        Toast.makeText(context, "Error al descargar " + manual.Nombre, Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

}