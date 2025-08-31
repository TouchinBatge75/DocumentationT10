package com.example.documentation;


//Imports
import android.app.Activity;
import android.content.Intent;//for change activity/screen
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation; //Base class for animations
import android.view.animation.AnimationUtils; //For load animations on XML
import android.widget.ImageView;
import android.widget.TextView;

public class SplashActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        Animation fadeIn = AnimationUtils.loadAnimation(this,R.anim.fade_in);//Carga la animacion

        ImageView logo = findViewById(R.id.logo);//Busca la imagen para logo'
        TextView titulo = findViewById(R.id.tituloApp);//Busca el visor de texto con el titulo 'tituloApp'
        TextView titulo2 = findViewById(R.id.tituloApp2);
        TextView titulo3 = findViewById(R.id.tituloApp3);
        logo.startAnimation(fadeIn);//efecto de desvanecido en el logo
        titulo.startAnimation(fadeIn);//efecto de desvanecido en el texto
        titulo2.startAnimation(fadeIn);
        titulo3.startAnimation(fadeIn);
        //retraso de 2.5 sg para la splash Activity
        new Handler().postDelayed(()->
        {
            startActivity(new Intent(SplashActivity.this, MainActivity.class)); //Inicia la actividad Principal
            finish();//Cierra la splash Activity
        },2500);
    }
}