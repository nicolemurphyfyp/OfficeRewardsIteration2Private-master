package com.example.officerewards.Activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.officerewards.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.jetbrains.annotations.NotNull;

//Code adapted from https://github.com/bikashthapa01/firebase-authentication-android/blob/master/app/src/main/java/net/smallacademy/authenticatorapp/Login.java

public class LoginActivity extends AppCompatActivity {

    //Declare buttons, edittexts, textviews and firebase objects
    Button btnSignin, btnRegister;
    EditText etEmail, etPassword;
    FirebaseAuth auth;
    FirebaseFirestore fStore;
    String userID, userType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Assign buttons, edittexts, textviews and firebase objects
        auth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        btnSignin = findViewById(R.id.btnSignin);
        btnRegister = findViewById(R.id.btnRegisterActivity);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLogInPassword);

        //If there is a user already logged in, they can bypass login
        if(auth.getCurrentUser() != null) {
            userID = auth.getCurrentUser().getUid();
            //get the record for the user on firebase
            DocumentReference documentReference = fStore.collection("Users").document(userID);
            documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();

                        //This determines whether the user is sent to the regular section or the admin section, by checking the type field for that user in firebase
                        userType = String.valueOf(document.getData().get("type"));
                        checkUserType(userType);
                    }
                    else {
                        Toast.makeText(LoginActivity.this, "Nothing retrieved" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public void SignIn(View view) {
        //get what was typed in the two text fields
        final String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        //Validate the values in the textfields
        //adapted from https://stackoverflow.com/a/25156934
        if(TextUtils.isEmpty(email)){
            //showing an error in a text field https://stackoverflow.com/a/18225457
            etEmail.setError("Email is Required.");
            return;
        }
        if(TextUtils.isEmpty(password)){
            etPassword.setError("Password is Required.");
            return;
        }
        if(password.length() < 6){
            etPassword.setError("Password Must be >= 6 Characters");
            return;
        }
        //check if the email matches an email pattern (eg something@example.com), adapted from https://stackoverflow.com/a/15808057
        if (!(Patterns.EMAIL_ADDRESS.matcher(email).matches())) {
            etEmail.setError("Please enter a valid email");
            etEmail.requestFocus();
            return;
        }

        //submits the email and password to firebase auth
        auth.signInWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull @NotNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    //login succesful, we than take the user's id so we can check whether they are a standard user (employee)
                    //or an employer
                    userID = auth.getCurrentUser().getUid();

                    DocumentReference documentReference = fStore.collection("Users").document(userID);
                    documentReference.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull @NotNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();

                                //This determines whether the user is sent to the regular section or the admin section
                                userType = String.valueOf(document.getData().get("type"));
                                checkUserType(userType);
                            }
                            else {
                                Toast.makeText(LoginActivity.this, "Nothing retrieved" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {
                    Toast.makeText(LoginActivity.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    //depending on which type of user, we send them to a different screen
    //admin section is not started yet so it doesnt send you anywhere
    public void checkUserType(String userType) {
        if(userType.equals("user")) {
            Intent intent = new Intent(LoginActivity.this,MapsActivityCurrentPlace.class);
            startActivity(intent);
        }
        else if(userType.equals("admin")) {
            Log.d("TAG", "Admin Sign in");
        }
    }

    //when the user presses the register button, this takes them to the register activity
    //we use an intent to go to a new activity (screen)
    //code adapted from first example here https://stackoverflow.com/a/41389737
    public void goToRegister(View view) {
        Intent intent = new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(intent);
    }
}