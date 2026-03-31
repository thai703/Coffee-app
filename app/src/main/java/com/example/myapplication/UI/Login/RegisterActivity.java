package com.example.myapplication.UI.Login;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.UI.Home.HomeActivity;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.databinding.ActivityRegisterBinding;
import com.example.myapplication.model.User;
import com.example.myapplication.util.SessionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        setupListeners();
    }

    private void setupListeners() {
        binding.btnRegisterUser.setOnClickListener(v -> registerUser());
        binding.btnBack.setOnClickListener(v -> {
            if (binding.progressBar.getVisibility() == View.GONE) {
                finish();
            }
        });
        binding.tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etRegisterEmail.getText().toString().trim();
        String password = binding.etRegisterPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        setLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Chain the operations: 1. Update Auth Profile -> 2. Write to DB -> 3. Navigate
                            updateAuthProfileAndProceed(firebaseUser, fullName, email);
                        }
                    } else {
                        setLoading(false);
                        Toast.makeText(RegisterActivity.this,
                                getString(R.string.status_register_failed) + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void updateAuthProfileAndProceed(FirebaseUser firebaseUser, String fullName, String email) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        firebaseUser.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        // Log the error, but proceed anyway. The login flow will sync the name.
                        Log.w(TAG, "Failed to update user profile in Auth.", task.getException());
                    }
                    // Proceed to write to database regardless of profile update success
                    writeUserToDatabaseAndProceed(firebaseUser, fullName, email);
                });
    }

    private void writeUserToDatabaseAndProceed(FirebaseUser firebaseUser, String fullName, String email) {
        String userId = firebaseUser.getUid();
        User user = new User(email, "", fullName, "", "", SessionManager.ROLE_USER);

        mDatabase.child("users").child(userId).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(RegisterActivity.this, getString(R.string.status_register_success), Toast.LENGTH_SHORT)
                        .show();
                navigateToHome();
            } else {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, getString(R.string.status_db_error),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        if (TextUtils.isEmpty(fullName)) {
            binding.etFullName.setError(getString(R.string.err_empty_fullname));
            binding.etFullName.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etRegisterEmail.setError(getString(R.string.err_empty_email));
            binding.etRegisterEmail.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etRegisterEmail.setError(getString(R.string.err_invalid_email_format));
            binding.etRegisterEmail.requestFocus();
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            binding.etRegisterPassword.setError(getString(R.string.err_empty_password));
            binding.etRegisterPassword.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            binding.etRegisterPassword.setError(getString(R.string.err_password_short));
            binding.etRegisterPassword.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError(getString(R.string.err_password_mismatch));
            binding.etConfirmPassword.requestFocus();
            return false;
        }
        return true;
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnRegisterUser.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegisterUser.setEnabled(true);
        }
    }

    private void navigateToHome() {
        Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
