package com.mobile.facelinkV1.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.mobile.facelinkV1.R;
import com.mobile.facelinkV1.databinding.ActivityMainBinding;
import com.mobile.facelinkV1.models.User;
public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    FirebaseAuth auth;
    FirebaseDatabase database;


    String[] permissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
    private int requestCode = 1;
    User user;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        database =  FirebaseDatabase.getInstance();


        FirebaseUser currentUser = auth.getCurrentUser();
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                auth.signOut();


                // Notify user of successful logout
                Toast.makeText(MainActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

                // Redirect to the LoginActivity (or your login screen activity)
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);  // Clears the back stack
                startActivity(intent);
                finish();
            }
        });


        database.getReference().child("profiles")
                .child(currentUser.getUid())
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                user = snapshot.getValue(User.class);

                                Glide.with(MainActivity.this)
                                        .load(user.getProfile())
                                        .into(binding.profilePicture);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });



        binding.linkBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPermissionGranted()){

                        Intent  intent = new Intent(MainActivity.this,ConnectingActivity.class);
                        intent.putExtra("profile",user.getProfile());
                        startActivity(intent);
                        //startActivity(new Intent(MainActivity.this, ConnectingActivity.class));
                }else{
                    askPermission();
                }

            }
        });
    }

    void askPermission(){
        ActivityCompat.requestPermissions(this,permissions,requestCode);
    }

    private boolean isPermissionGranted(){
        for(String permissions : permissions){
            if(ActivityCompat.checkSelfPermission(this,permissions) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return  true;
    }


}