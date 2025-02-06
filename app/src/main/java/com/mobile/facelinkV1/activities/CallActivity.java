package com.mobile.facelinkV1.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mobile.facelinkV1.R;
import com.mobile.facelinkV1.databinding.ActivityCallBinding;
import com.mobile.facelinkV1.models.InterfaceJava;
import com.mobile.facelinkV1.models.User;

import java.util.UUID;

public class CallActivity extends AppCompatActivity {

    ActivityCallBinding binding;
    String uniqueId = "";
    FirebaseAuth auth;
    String username =  "";
    String friendsUsername = "";

    boolean isPeerConnected = false;
    DatabaseReference firebaseRef;

    boolean isAudio = true;
    boolean isVideo = true;
    String createdBy;

    boolean pageExit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityCallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firebaseRef = FirebaseDatabase.getInstance().getReference().child("users");

        username = getIntent().getStringExtra("username");
        String incoming = getIntent().getStringExtra("incoming");
        createdBy = getIntent().getStringExtra("createdBy");


        friendsUsername = incoming;

        setupWebView();

        binding.micBtn.setOnClickListener(v -> {
            isAudio = !isAudio;
            callJavaScriptFunction("javascript:toggleAudio(\""+isAudio+"\")");

            if (isAudio){
                binding.micBtn.setImageResource(R.drawable.mic_on);
            }else{
                binding.micBtn.setImageResource(R.drawable.mic_off);
            }
        });

        binding.videoBtn.setOnClickListener(v -> {
            isVideo = !isVideo;
            callJavaScriptFunction("javascript:toggleVideo(\""+isVideo+"\")");

            if (isVideo){
                binding.videoBtn.setImageResource(R.drawable.video_on);
            }else{
                binding.videoBtn.setImageResource(R.drawable.video_off);
            }
        });



        binding.endBtn.setOnClickListener(v -> {
            firebaseRef.child(friendsUsername).child("callEnded").setValue(true);
            callJavaScriptFunction("javascript:stopMedia();");
            finish();
        });


        listenForCallEnd();

    }

    private void listenForCallEnd() {
        firebaseRef.child(username).child("callEnded").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                firebaseRef.child(username).child("callEnded").removeValue();
                if (snapshot.exists() && snapshot.getValue(Boolean.class)) {
                    callJavaScriptFunction("javascript:stopMedia();");
                    // Navigate to MainActivity
                    Intent intent = new Intent(CallActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the activity stack
                    startActivity(intent);
                    finish(); // Finish this activity
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    void setupWebView(){
        binding.webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }
        });

        binding.webView.getSettings().setJavaScriptEnabled(true);
        binding.webView.getSettings().setMediaPlaybackRequiresUserGesture(false);

        binding.webView.addJavascriptInterface(new InterfaceJava(this),"Android");

        loadVideoCall();

    }

    public void loadVideoCall(){
        String filepath = "file:///android_asset/call.html";

        binding.webView.loadUrl(filepath);

        binding.webView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                initializePeer();
            }
        });
    }


    void initializePeer(){

        uniqueId = getUniqueId();
        callJavaScriptFunction("javascript:init(\"" + uniqueId + "\")");

        if (createdBy.equalsIgnoreCase(username)){
            if (pageExit)
                return;
            firebaseRef.child(username).child("connId").setValue(uniqueId);
            firebaseRef.child(username).child("isAvailable").setValue(true);

            binding.loadingGroup.setVisibility(View.GONE);
            binding.controls.setVisibility(View.VISIBLE);

            FirebaseDatabase.getInstance().getReference()
                    .child("profiles")
                    .child(friendsUsername)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            User user = snapshot.getValue(User.class);
                            Glide.with(CallActivity.this).load(user.getProfile()).into(binding.profile);
                            binding.name.setText(user.getName());
                            binding.city.setText(user.getCity());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        }else{
            new Handler().postDelayed(() -> {
                friendsUsername = createdBy;
                FirebaseDatabase.getInstance().getReference()
                        .child("profiles")
                        .child(friendsUsername)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                User user = snapshot.getValue(User.class);
                                Glide.with(CallActivity.this).load(user.getProfile()).into(binding.profile);
                                binding.name.setText(user.getName());
                                binding.city.setText(user.getCity());
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                FirebaseDatabase.getInstance().getReference()
                        .child("users")
                        .child(friendsUsername)
                        .child("connId")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.getValue() != null){
                                    sendCallRequest();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
            },2000);
        }
    }

    public void onPeerConnected(){
        isPeerConnected = true;
    }

    void sendCallRequest(){
        if (!isPeerConnected) {
            Toast.makeText(this, "You are not connected. Check your internet", Toast.LENGTH_SHORT).show();
            return;
        }
        listenConnId();
    }

    void listenConnId(){
        firebaseRef.child(friendsUsername).child("connId").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() == null)
                    return;
                binding.loadingGroup.setVisibility(View.GONE);
                binding.controls.setVisibility(View.VISIBLE);
                String connId = snapshot.getValue(String.class);
                callJavaScriptFunction("javascript:startCall(\""+connId+"\")");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    void callJavaScriptFunction(String function){
        binding.webView.post(() -> binding.webView.evaluateJavascript(function,null));
    }

    String getUniqueId(){
        return UUID.randomUUID().toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pageExit = true;
        firebaseRef.child(createdBy).setValue(null);
        finish();
    }
    @Override
    protected void onPause() {
        super.onPause();
        binding.webView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        binding.webView.onResume();
    }


}