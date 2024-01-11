package com.example.server;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    Button btnDangNhap;
    TextView txtSlogan;

    //firebase


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDangNhap = findViewById(R.id.btnDangNhap);
        txtSlogan = findViewById(R.id.txtSlogan);
        Typeface face = Typeface.createFromAsset(getAssets(),"fonts/Nabila.ttf");
        txtSlogan.setTypeface(face);

        btnDangNhap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent dangNhap = new Intent(MainActivity.this, DangNhap.class);
                startActivity(dangNhap);
            }
        });
    }
}