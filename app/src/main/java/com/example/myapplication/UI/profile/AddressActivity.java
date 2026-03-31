package com.example.myapplication.UI.profile;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AddressAdapter;
import com.example.myapplication.databinding.ActivityAddressBinding;
import com.example.myapplication.model.Address;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AddressActivity extends AppCompatActivity implements AddressAdapter.OnAddressClickListener {

    private ActivityAddressBinding binding;
    private AddressAdapter adapter;
    private List<Address> addressList;
    private DatabaseReference addressesRef;

    private boolean isSelectionMode = false;
    public static final String EXTRA_SELECTED_ADDRESS = "EXTRA_SELECTED_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddressBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check selection mode
        if (getIntent().hasExtra("IS_SELECTION_MODE")) {
            isSelectionMode = getIntent().getBooleanExtra("IS_SELECTION_MODE", false);
            if (isSelectionMode) {
                // Change title optionally
                binding.toolbar.setTitle("Chọn địa chỉ");
            }
        }

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        // Navigation click listener moved to setupListeners

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();
        addressesRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("addresses");

        setupRecyclerView();
        setupListeners();
        loadAddresses();
    }

    private void setupRecyclerView() {
        binding.rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        addressList = new ArrayList<>();
        adapter = new AddressAdapter(addressList, this);
        binding.rvAddresses.setAdapter(adapter);
    }

    private void setupListeners() {
        binding.fabAddAddress.setOnClickListener(v -> showAddAddressDialog());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadAddresses() {
        addressesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot addressSnapshot : snapshot.getChildren()) {
                        Address address = addressSnapshot.getValue(Address.class);
                        if (address != null) {
                            addressList.add(address);
                        }
                    }
                }
                adapter.updateList(addressList);
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddressActivity.this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void updateEmptyState() {
        if (addressList.isEmpty()) {
            binding.layoutEmptyAddress.setVisibility(View.VISIBLE);
            binding.rvAddresses.setVisibility(View.GONE);
        } else {
            binding.layoutEmptyAddress.setVisibility(View.GONE);
            binding.rvAddresses.setVisibility(View.VISIBLE);
        }
    }

    private void showAddAddressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thêm địa chỉ mới");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_address, null);
        final EditText etName = view.findViewById(R.id.etAddressName);
        final EditText etPhone = view.findViewById(R.id.etAddressPhone);
        final EditText etFullAddress = view.findViewById(R.id.etAddressFull);

        builder.setView(view);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            // Will be overridden
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String fullAddress = etFullAddress.getText().toString().trim();

            if (TextUtils.isEmpty(name)) {
                etName.setError("Nhập tên gợi nhớ");
                return;
            }
            if (TextUtils.isEmpty(phone)) {
                etPhone.setError("Nhập số điện thoại");
                return;
            }
            if (TextUtils.isEmpty(fullAddress)) {
                etFullAddress.setError("Nhập địa chỉ đầy đủ");
                return;
            }

            addNewAddress(name, phone, fullAddress);
            dialog.dismiss();
        });
    }

    private void addNewAddress(String name, String phone, String fullAddress) {
        String addressId = addressesRef.push().getKey();
        if (addressId != null) {
            Address newAddress = new Address(addressId, name, phone, fullAddress, false);
            addressesRef.child(addressId).setValue(newAddress)
                    .addOnSuccessListener(
                            aVoid -> Toast.makeText(AddressActivity.this, "Đã thêm địa chỉ", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast
                            .makeText(AddressActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onDeleteClick(Address address) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa địa chỉ")
                .setMessage("Bạn có chắc chắn muốn xóa địa chỉ này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    addressesRef.child(address.getId()).removeValue()
                            .addOnSuccessListener(aVoid -> Toast
                                    .makeText(AddressActivity.this, "Đã xóa địa chỉ", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onAddressClick(Address address) {
        if (isSelectionMode) {
            Intent intent = new Intent();
            intent.putExtra(EXTRA_SELECTED_ADDRESS, address);
            setResult(RESULT_OK, intent);
            finish();
        }
    }
}
