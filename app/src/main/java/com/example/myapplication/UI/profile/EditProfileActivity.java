package com.example.myapplication.UI.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityProfileEditBinding;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.example.myapplication.manager.LanguageHelper;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    // Constants for Intent extras
    public static final String EXTRA_NAME = "extra_name";
    public static final String EXTRA_PHONE = "extra_phone";
    public static final String EXTRA_ADDRESS = "extra_address";

    private ActivityProfileEditBinding binding;

    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private FirebaseUser currentUser;
    private Uri imageUri;
    private ActivityResultLauncher<String> galleryLauncher;
    private boolean isAvatarDeleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        binding = ActivityProfileEditBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.err_login_required), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        initGalleryLauncher();

        // Check if we received data to pre-fill (optional, from CheckoutActivity)
        if (getIntent() != null) {
            if (getIntent().hasExtra(EXTRA_NAME))
                binding.etFullName.setText(getIntent().getStringExtra(EXTRA_NAME));
            if (getIntent().hasExtra(EXTRA_PHONE))
                binding.etPhone.setText(getIntent().getStringExtra(EXTRA_PHONE));
            if (getIntent().hasExtra(EXTRA_ADDRESS))
                binding.etAddress.setText(getIntent().getStringExtra(EXTRA_ADDRESS));
        }

        setupToolbar();
        setupListeners();
        loadCurrentUserInfo();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        binding.btnSaveProfile.setOnClickListener(v -> saveUserProfile());
        binding.fabEditAvatar.setOnClickListener(v -> galleryLauncher.launch("image/*"));
        binding.fabDeleteAvatar.setOnClickListener(v -> {
            binding.ivAvatar.setImageResource(R.drawable.ic_person);
            imageUri = null;
            isAvatarDeleted = true;
            binding.fabDeleteAvatar.setVisibility(View.GONE);
        });
    }

    private void initGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        imageUri = uri;
                        isAvatarDeleted = false;
                        Glide.with(this)
                                .load(imageUri)
                                .circleCrop()
                                .into(binding.ivAvatar);
                        binding.fabDeleteAvatar.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void loadCurrentUserInfo() {
        binding.etEmail.setText(currentUser.getEmail());

        setLoading(true);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Avatar Load
                        if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                            Glide.with(EditProfileActivity.this)
                                    .load(user.getImageUrl())
                                    .placeholder(R.drawable.ic_person)
                                    .circleCrop()
                                    .into(binding.ivAvatar);
                            binding.fabDeleteAvatar.setVisibility(View.VISIBLE);
                        } else {
                            binding.fabDeleteAvatar.setVisibility(View.GONE);
                        }

                        // Only set if empty (to avoid overwriting pre-filled data from intent if any)
                        if (binding.etFullName.getText().toString().isEmpty())
                            binding.etFullName.setText(user.getName());
                        binding.etRole.setText(user.getRole());
                        if (binding.etPhone.getText().toString().isEmpty())
                            binding.etPhone.setText(user.getPhone());
                        if (binding.etAddress.getText().toString().isEmpty())
                            binding.etAddress.setText(user.getAddress());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, getString(R.string.err_load_current_info), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void saveUserProfile() {
        String newName = binding.etFullName.getText().toString().trim();
        String newPhone = binding.etPhone.getText().toString().trim();
        String newAddress = binding.etAddress.getText().toString().trim();

        if (newName.isEmpty()) {
            binding.etFullName.setError(getString(R.string.err_name_required));
            binding.etFullName.requestFocus();
            return;
        }

        setLoading(true);

        if (imageUri != null) {
            uploadImageAndSave(newName, newPhone, newAddress);
        } else if (isAvatarDeleted) {
            performUpdate(newName, newPhone, newAddress, ""); // Empty string to effectively remove it or handle in
                                                              // performUpdate
        } else {
            performUpdate(newName, newPhone, newAddress, null);
        }
    }

    private void uploadImageAndSave(String name, String phone, String address) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference("avatars/" + currentUser.getUid());
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    performUpdate(name, phone, address, uri.toString());
                }))
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, getString(R.string.err_upload_image_prefix, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void performUpdate(String name, String phone, String address, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        if (imageUrl != null) {
            updates.put("imageUrl", imageUrl);
        }

        userRef.updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, getString(R.string.status_update_success),
                            Toast.LENGTH_SHORT).show();

                    Intent resultIntent = new Intent();
                    resultIntent.putExtra(EXTRA_NAME, name);
                    resultIntent.putExtra(EXTRA_PHONE, phone);
                    resultIntent.putExtra(EXTRA_ADDRESS, address);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this,
                            getString(R.string.err_update_profile_prefix, e.getMessage()), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSaveProfile.setEnabled(!isLoading);
        binding.fabEditAvatar.setEnabled(!isLoading);
        binding.fabDeleteAvatar.setEnabled(!isLoading);
    }
}
