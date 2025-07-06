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

public class splash_activity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);        //Set a design to XML screen

        Animation fadeIn = AnimationUtils.loadAnimation(this,R.anim.fade_in);//Load the animation

        ImageView logo = findViewById(R.id.logo);//Find the ImageView with id 'logo'
        TextView titulo = findViewById(R.id.tituloApp);//Find the TextView with id 'tituloApp'

        logo.startAnimation(fadeIn);//Vanish animation on the logo
        titulo.startAnimation(fadeIn);//Vanish animation on the title text

        //Create a 2.5 seconds delay on the splash activity
        new Handler().postDelayed(()->
        {
            startActivity(new Intent(splash_activity.this, MainActivity.class)); //Start the MainActivity
            finish();//Close the splash activity
        },2500);
    }
}