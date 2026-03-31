package com.example.myapplication.UI.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.R;
import com.example.myapplication.UI.LandingActivity;
import com.example.myapplication.UI.profile.EditProfileActivity;
import com.example.myapplication.databinding.ActivitySettingsBinding;
import com.example.myapplication.manager.LanguageHelper;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        LanguageHelper.loadLocale(this);

        setupToolbar();
        setupActions();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(getString(R.string.title_settings));
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupActions() {
        // Account
        binding.tvEditProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
        });

        binding.tvChangePassword.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        // General
        binding.switchNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? getString(R.string.status_on) : getString(R.string.status_off);
            Toast.makeText(this, getString(R.string.msg_notification_status, status), Toast.LENGTH_SHORT).show();
        });

        binding.tvLanguage.setOnClickListener(v -> {
            showLanguageDialog();
        });

        // Support
        binding.tvHelpCenter.setOnClickListener(v -> {
            startActivity(new Intent(this, com.example.myapplication.UI.support.SupportActivity.class));
        });

        binding.tvPrivacyPolicy.setOnClickListener(v -> {
            showInfoDialog(getString(R.string.dialog_privacy_title),
                    getString(R.string.dialog_privacy_content));
        });

        binding.tvAboutUs.setOnClickListener(v -> {
            showInfoDialog(getString(R.string.dialog_about_title),
                    getString(R.string.dialog_about_content));
        });

        // Logout
        binding.btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, com.example.myapplication.UI.Login.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showInfoDialog(String title, String content) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(getString(R.string.action_close), (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLanguageDialog() {
        String[] languages = { "Tiếng Việt", "English" };
        int checkedItem = LanguageHelper.getSavedLanguage(this).equals("en") ? 1 : 0;

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.title_select_language))
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String langCode = (which == 1) ? "en" : "vi";
                    LanguageHelper.setLocale(this, langCode);
                    dialog.dismiss();

                    // Restart app to apply language changes completely
                    Intent intent = new Intent(this, com.example.myapplication.UI.SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }

    private void showChangePasswordDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        dialog.show();

        com.google.android.material.textfield.TextInputEditText etOldPass = view.findViewById(R.id.et_old_password);
        com.google.android.material.textfield.TextInputEditText etNewPass = view.findViewById(R.id.et_new_password);
        com.google.android.material.textfield.TextInputEditText etConfirmPass = view
                .findViewById(R.id.et_confirm_new_password);
        android.widget.Button btnCancel = view.findViewById(R.id.btn_cancel);
        android.widget.Button btnConfirm = view.findViewById(R.id.btn_confirm);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnConfirm.setOnClickListener(v -> {
            String oldPass = etOldPass.getText().toString().trim();
            String newPass = etNewPass.getText().toString().trim();
            String confirmPass = etConfirmPass.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, getString(R.string.err_fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, getString(R.string.err_password_mismatch), Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, getString(R.string.err_password_short), Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(oldPass, newPass, dialog);
        });
    }

    private void changePassword(String oldPass, String newPass, androidx.appcompat.app.AlertDialog dialog) {
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // 1. Re-authenticate
            com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.getEmail(), oldPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. Update Password
                    user.updatePassword(newPass).addOnCompleteListener(taskUpdate -> {
                        if (taskUpdate.isSuccessful()) {
                            Toast.makeText(this, getString(R.string.status_password_changed), Toast.LENGTH_SHORT)
                                    .show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, getString(R.string.err_password_update), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(this, getString(R.string.err_incorrect_old_password), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
