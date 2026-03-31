package com.example.myapplication.UI.Login;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.UI.Home.HomeActivity;
import com.example.myapplication.UI.admin.AdminHomeActivity;
import com.example.myapplication.databinding.ActivityLoginBinding;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.model.User;
import com.example.myapplication.util.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private ActivityLoginBinding binding;
    private SessionManager sessionManager;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(getApplicationContext());
        CartManager.getInstance().init(getApplicationContext()); // Ensure initialized

        binding.getRoot().postDelayed(this::checkLoginStatus, 100);

        setupListeners();
        setupGoogleSignIn();
        startEnterAnimations();
    }

    private void startEnterAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        binding.tvBrandName.startAnimation(fadeIn);
        binding.tvWelcomeSubtitle.startAnimation(fadeIn);
        binding.tilEmail.startAnimation(slideUp);
        binding.tilPassword.startAnimation(slideUp);
        binding.btnLogin.startAnimation(slideUp);
        binding.tvForgotPassword.startAnimation(slideUp); // Nút này giờ ở dưới nút Login
        binding.dividerLayout.startAnimation(slideUp);
        binding.btnGoogleSignIn.startAnimation(slideUp);
        binding.tvRegisterPrompt.startAnimation(slideUp);
        binding.tvRegister.startAnimation(slideUp);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    private void checkLoginStatus() {
        if (sessionManager.isLoggedIn() && mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            String role = sessionManager.getUserRole();
            executorService.execute(() -> CartManager.getInstance().updateUserId(userId));
            navigateToCorrectActivity(role);
        }
    }

    private void setupListeners() {
        binding.btnLogin.setOnClickListener(v -> loginUser());
        binding.tvForgotPassword.setOnClickListener(v -> startActivity(new Intent(this, ForgotPasswordActivity.class)));
        binding.tvRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        TextWatcher clearErrorTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    binding.tilEmail.setError(null);
                    binding.tilPassword.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        binding.etEmail.addTextChangedListener(clearErrorTextWatcher);
        binding.etPassword.addTextChangedListener(clearErrorTextWatcher);
    }

    private void loginUser() {
        hideKeyboard();
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        setLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    setLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            prepareNewSession(user.getUid());
                        }
                    } else {
                        handleLoginFailure(task.getException());
                    }
                });
    }

    private boolean validateInput(String email, String password) {
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.err_enter_email));
            binding.etEmail.requestFocus();
            vibrateDevice();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError(getString(R.string.err_invalid_email));
            binding.tilPassword.setError(getString(R.string.err_reenter));
            binding.etEmail.setText("");
            binding.etPassword.setText("");
            binding.etEmail.requestFocus();
            vibrateDevice();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.err_enter_password));
            binding.etPassword.requestFocus();
            vibrateDevice();
            return false;
        }
        return true;
    }

    private void handleLoginFailure(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            // SAI MẬT KHẨU -> Chỉ báo lỗi & xóa ô mật khẩu
            binding.tilPassword.setError(getString(R.string.err_incorrect_password));
            binding.etPassword.setText("");
            binding.etPassword.requestFocus();
        } else if (exception instanceof FirebaseAuthInvalidUserException) {
            // TÀI KHOẢN KHÔNG TỒN TẠI -> Xóa cả 2
            Toast.makeText(LoginActivity.this, getString(R.string.err_account_not_exist), Toast.LENGTH_SHORT).show();
            clearAllFieldsForNewLogin();
        } else {
            // LỖI KHÁC -> Xóa cả 2
            Toast.makeText(LoginActivity.this, getString(R.string.err_login_failed), Toast.LENGTH_SHORT).show();
            clearAllFieldsForNewLogin();
        }
        vibrateDevice();
    }

    private void clearAllFieldsForNewLogin() {
        binding.tilEmail.setError("Thông tin không chính xác");
        binding.tilPassword.setError("Thông tin không chính xác");
        binding.etEmail.setText("");
        binding.etPassword.setText("");
        binding.etEmail.requestFocus();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            if (account != null) {
                                firebaseAuthWithGoogle(account.getIdToken());
                            }
                        } catch (ApiException e) {
                            handleSignInError("Google Sign In Error: " + e.getStatusCode());
                        }
                    } else {
                        setLoading(false);
                    }
                });
    }

    private void signInWithGoogle() {
        setLoading(true);
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleSignInLauncher.launch(mGoogleSignInClient.getSignInIntent());
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            prepareNewSession(user.getUid());
                        }
                    } else {
                        handleSignInError("Firebase Auth Failed");
                    }
                });
    }

    private void prepareNewSession(String userId) {
        CartManager.getInstance().clearCartOnLogout();
        sessionManager.clearSession();
        getUserRoleAndProceed(userId);
    }

    private void getUserRoleAndProceed(String userId) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FirebaseUser authUser = mAuth.getCurrentUser();
                if (authUser == null)
                    return;

                User dbUser = null;
                try {
                    dbUser = snapshot.getValue(User.class);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing user data: " + e.getMessage());
                    // Fallback: treat as if user doesn't exist in DB yet, or create default
                }

                if (dbUser != null) {
                    String role = (dbUser.getRole() != null) ? dbUser.getRole() : SessionManager.ROLE_USER;
                    sessionManager.createLoginSession(userId, role, true);
                    executorService.execute(() -> CartManager.getInstance().updateUserId(userId));
                    navigateToCorrectActivity(role);
                } else {
                    String displayName = authUser.getDisplayName() != null ? authUser.getDisplayName() : "User";
                    String email = authUser.getEmail() != null ? authUser.getEmail() : "";
                    String photoUrl = authUser.getPhotoUrl() != null ? authUser.getPhotoUrl().toString() : "";

                    User newUser = new User(email, "", displayName, "", photoUrl, SessionManager.ROLE_USER);
                    userRef.setValue(newUser).addOnSuccessListener(aVoid -> {
                        sessionManager.createLoginSession(userId, SessionManager.ROLE_USER, true);
                        executorService.execute(() -> CartManager.getInstance().updateUserId(userId));
                        navigateToCorrectActivity(SessionManager.ROLE_USER);
                    }).addOnFailureListener(e -> handleSignInError("Error creating user data"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleSignInError("Database Error: " + error.getMessage());
            }
        });
    }

    private void navigateToCorrectActivity(String role) {
        Intent intent = new Intent(this,
                SessionManager.ROLE_ADMIN.equals(role) ? AdminHomeActivity.class : HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        if (binding == null)
            return;
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnLogin.setEnabled(!isLoading);
        binding.btnGoogleSignIn.setEnabled(!isLoading);
        binding.tvRegister.setEnabled(!isLoading);
    }

    private void handleSignInError(String message) {
        setLoading(false);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null)
            executorService.shutdown();
    }
}