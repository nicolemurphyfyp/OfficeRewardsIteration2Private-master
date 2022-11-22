package com.example.officerewards.Activities;


import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.officerewards.R;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//Code adapted from https://github.com/bikashthapa01/firebase-authentication-android/blob/master/app/src/main/java/net/smallacademy/authenticatorapp/Register.java

public class RegisterActivity extends AppCompatActivity {

    //Declare buttons, edittext, string and firebase objects
    Button btnRegister, btnLogin, btnIntro;
    EditText etEmail, etName, etPassword;
    FirebaseAuth auth;
    FirebaseFirestore fStore;
    String userID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //assign the firebase objects, buttons, and edittexts
        auth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        btnRegister = findViewById(R.id.btnRegisterNew);
        btnLogin = findViewById(R.id.btnLoginActivity);
        etEmail = findViewById(R.id.etRegisterEmail);
        etName = findViewById(R.id.etRegisterUsername);
        etPassword = findViewById(R.id.etRegisterPword);

        //If already signed in, send them to the maps screen
        if(auth.getCurrentUser() != null) {
            userID = auth.getCurrentUser().getUid();
            DocumentReference documentReference = fStore.collection("Users").document(userID);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        Intent intent = new Intent(RegisterActivity.this, MapsActivityCurrentPlace.class);
                        startActivity(intent);
                    } else {
                        Log.d("TAG", "Login Failed from Register Screen");
                    }
                }
            });
        }
    }

    //open the login activity (screen)
    //adapted from first example here https://stackoverflow.com/a/41389737
    public void gotoLogin(View view) {
        Intent intent = new Intent(RegisterActivity.this,LoginActivity.class);
        startActivity(intent);
    }

    //Get the details the user typed in, validate them, and then send them to firebase to register them
    public void Register(View view) {
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        final String fullName = etName.getText().toString();

        //Validate that all details are entered
        if(TextUtils.isEmpty(email)){
            etEmail.setError("Email is Required.");
            return;
        }
        if(TextUtils.isEmpty(password)){
            etPassword.setError("Password is Required.");
            return;
        }
        if(TextUtils.isEmpty(fullName)){
            etName.setError("Name is Required.");
            return;
        }
        if(password.length() < 6){
            etPassword.setError("Password Must be >= 6 Characters");
            return;
        }

        //Creating a user with firebase auth, I also create a user object in the Cloud Firestore so I have a corresponding record there
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                if(task.isSuccessful()) {
                    //we use the ID generated by firebase auth as the ID for the new user's record in firestore as well
                    userID = auth.getCurrentUser().getUid();

                    //adapted from https://firebase.google.com/docs/firestore/manage-data/add-data (Set a Document)
                    DocumentReference documentReference = fStore.collection("Users").document(userID);
                    Map<String,Object> user = new HashMap<>();
                    user.put("Name",fullName);
                    user.put("email",email);
                    user.put("type","user");
                    user.put("id",userID);
                    //Insert new user
                    documentReference.set(user).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("SuccessStore", "onSuccess: user Profile is created for "+ userID);

                            //When the user is successfully created, we also create a record under the Points heading with their ID, and set their points to 0
                            DocumentReference documentRef = fStore.collection("Points").document(userID);
                            Map<String,Object> points = new HashMap<>();
                            points.put("points",0);
                            documentRef.set(points).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.d("PointSuccess", "Point Record Created");
                                }
                            });

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("FailStore", "onFailure: " + e.toString());
                        }
                    });
                    Intent intent = new Intent(RegisterActivity.this,MapsActivityCurrentPlace.class);
                    startActivity(intent);
                }
                else {
                    Toast.makeText(RegisterActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}