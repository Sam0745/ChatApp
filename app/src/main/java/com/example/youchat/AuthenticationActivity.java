package com.example.youchat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.renderscript.ScriptGroup;
import android.view.View;
import android.widget.Toast;

import com.example.youchat.Model.UserModel;
import com.example.youchat.databinding.ActivityAuthenticationBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AuthenticationActivity extends AppCompatActivity {

        ActivityAuthenticationBinding authXml;
        String name,email,password;
        DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authXml=ActivityAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(authXml.getRoot());

        databaseReference= FirebaseDatabase.getInstance().getReference("users");

        authXml.loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                email = authXml.etEmail.getText().toString();
                password = authXml.etPassword.getText().toString();
                if (validateLoginForm()){
                    Login();
                }

            }
        });

        authXml.signBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                name=authXml.etName.getText().toString();
                email = authXml.etEmail.getText().toString();
                password = authXml.etPassword.getText().toString();

                if (validateSignUpForm()){
                    SignIn();
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser()!=null){
            startActivity(new Intent(AuthenticationActivity.this,MainActivity.class));
            finish();
        }
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    private boolean validateLoginForm(){
        if (email.isEmpty()){
            showToast("Email is required");
            return false;
        }
        if (password.isEmpty()) {
            showToast("Password is required");
            return false;
        }
        return true;

    }

    private boolean validateSignUpForm() {
        if (name.isEmpty()) {
            showToast("Name is required");

            return false;
        }

        if (email.isEmpty()) {
            showToast("Email is required");

            return false;
        }

        if (password.isEmpty()) {
          showToast("Password is required");

            return false;
        }

        return true;
    }


    private void Login() {

        FirebaseAuth
                .getInstance()
                .signInWithEmailAndPassword(email.trim(),password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                            startActivity(new Intent(AuthenticationActivity.this,MainActivity.class));
                            finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Login failed: " + e.getMessage());
            }
        });

    }

    private void SignIn() {
        FirebaseAuth
                .getInstance()
                .createUserWithEmailAndPassword(email.trim(),password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        UserProfileChangeRequest userProfileChangeRequest=new UserProfileChangeRequest.Builder().setDisplayName(name).build();
                        FirebaseUser firebaseUser=FirebaseAuth.getInstance().getCurrentUser();
                        firebaseUser.updateProfile(userProfileChangeRequest);
                        UserModel userModel=new UserModel(FirebaseAuth.getInstance().getUid(),name,email,password);
                        databaseReference.child(FirebaseAuth.getInstance().getUid()).setValue(userModel);
                        startActivity(new Intent(AuthenticationActivity.this,MainActivity.class));
                        finish();
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                showToast("Sign up failed: " + e.getMessage());
            }
        });

    }
}