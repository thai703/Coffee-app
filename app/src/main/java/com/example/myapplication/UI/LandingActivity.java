package com.example.myapplication.UI;

import android.os.Bundle;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;

public class LandingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_landing);

        findViewById(R.id.btn_login).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.myapplication.UI.Login.LoginActivity.class));
        });

        findViewById(R.id.btn_register).setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.myapplication.UI.Login.RegisterActivity.class));
        });
    }
}
