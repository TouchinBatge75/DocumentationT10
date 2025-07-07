package com.example.documentation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

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
        TextView estadoManual = convertView.findViewById(R.id.estado_manual);

        //Obtenemos manual actual
        Manual manual = listaManuales.get(position);

        //Verificamos si ya tenemos el arrchivo descargado localmente
        File archivo = new File(context.getFilesDir(), manual.Nombre+".pdf");

        if(archivo.exists())
        {
            estadoManual.setText("Abrir");
            estadoManual.setTextColor(0xFF2E7D32);//verde;
        }
        else
        {
            estadoManual.setText("Descargar");
            estadoManual.setTextColor(0xFF1565C0);//Azul;
        }

        return convertView;
    }
}