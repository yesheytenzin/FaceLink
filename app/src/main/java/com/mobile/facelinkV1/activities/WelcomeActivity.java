package com.mobile.facelinkV1.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.mobile.facelinkV1.R;

public class WelcomeActivity extends AppCompatActivity {

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        auth = FirebaseAuth.getInstance();
        if(auth.getCurrentUser() != null){
            goToNextActivity();
        }

        setContentView(R.layout.activity_welcome);

        findViewById(R.id.welcomeBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToNextActivity();
            }
        });
    }

    void goToNextActivity(){
        startActivity(new Intent(WelcomeActivity.this, LoginActivity.class));
        finish();
    }
}