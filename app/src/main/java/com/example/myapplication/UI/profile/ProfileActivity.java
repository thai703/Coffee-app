package com.example.myapplication.UI.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.UI.order.OrderHistoryActivity;
import com.example.myapplication.UI.Login.LoginActivity;
import com.example.myapplication.databinding.ActivityProfileBinding;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.UI.settings.SettingsActivity;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        LanguageHelper.loadLocale(this);

        setupToolbar();
        setupListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserProfile();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Main Profile Actions

        binding.btnEditProfile
                .setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, EditProfileActivity.class)));

        // Menu Actions - Chỉ giữ các chức năng người dùng
        binding.btnMyOrders
                .setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, OrderHistoryActivity.class)));

        binding.btnAddressBook
                .setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, AddressActivity.class)));

        binding.btnSettings
                .setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, SettingsActivity.class)));

        binding.btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Tạm ẩn các chức năng chưa phát triển
        binding.btnSupport.setOnClickListener(v -> startActivity(
                new Intent(ProfileActivity.this, com.example.myapplication.UI.support.SupportActivity.class)));

        binding.btnPrivacyPolicy.setOnClickListener(v -> showInfoDialog("Chính sách bảo mật",
                "Chúng tôi cam kết bảo vệ thông tin cá nhân của bạn. Dữ liệu của bạn được sử dụng để xử lý đơn hàng và cải thiện trải nghiệm mua hàng tại Aura Coffee."));

        binding.btnAboutUs.setOnClickListener(v -> showInfoDialog("Về Aura Coffee",
                "Aura Coffee - Thưởng thức hương vị cà phê nguyên chất trong không gian hiện đại.\n\nPhiên bản: 1.0.0\nPhát triển bởi: JaThinh Team"));
    }

    private void showInfoDialog(String title, String content) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void loadUserProfile() {
        setLoading(true);
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        if (firebaseUser == null) {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            redirectToLogin();
            return;
        }

        String userId = firebaseUser.getUid();
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                setLoading(false);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        populateUI(user);
                    }
                } else {
                    Toast.makeText(ProfileActivity.this, "Không tìm thấy thông tin người dùng.", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, "Lỗi tải dữ liệu.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(User user) {
        binding.tvProfileName.setText(user.getName());
        binding.tvProfileEmail.setText(user.getEmail());

        // User requested to hide phone and address in Profile screen
        // if (user.getPhone() != null && !user.getPhone().isEmpty()) {
        // binding.tvProfilePhone.setVisibility(View.VISIBLE);
        // binding.tvProfilePhone.setText(user.getPhone());
        // } else {
        // binding.tvProfilePhone.setVisibility(View.GONE);
        // }

        // if (user.getAddress() != null && !user.getAddress().isEmpty()) {
        // binding.tvProfileAddress.setVisibility(View.VISIBLE);
        // binding.tvProfileAddress.setText(user.getAddress());
        // } else {
        // binding.tvProfileAddress.setVisibility(View.GONE);
        // }

        String imageUrl = user.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_person)
                    .error(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivProfileAvatar);
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.ic_person);
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
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
                Toast.makeText(this, "Vui lòng điền đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu mới không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(oldPass, newPass, dialog);
        });
    }

    private void changePassword(String oldPass, String newPass, AlertDialog dialog) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            setLoading(true);
            // 1. Re-authenticate
            com.google.firebase.auth.AuthCredential credential = com.google.firebase.auth.EmailAuthProvider
                    .getCredential(user.getEmail(), oldPass);

            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // 2. Update Password
                    user.updatePassword(newPass).addOnCompleteListener(taskUpdate -> {
                        setLoading(false);
                        if (taskUpdate.isSuccessful()) {
                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        } else {
                            Toast.makeText(this, "Lỗi cập nhật mật khẩu", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    setLoading(false);
                    Toast.makeText(this, "Mật khẩu cũ không chính xác", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        CartManager.getInstance().updateUserId(null);
        Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        redirectToLogin();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean isLoading) {
        binding.progressBarProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }
}
