package com.example.myapplication.UI.admin;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.adapter.AdminOrderDetailItemAdapter;
import com.example.myapplication.databinding.ActivityAdminOrderDetailBinding;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.CartItem;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOrderDetailActivity extends AppCompatActivity {

    private ActivityAdminOrderDetailBinding binding;
    private Order order;
    private AdminOrderDetailItemAdapter itemAdapter;
    private DatabaseReference ordersRef;
    private boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        order = (Order) getIntent().getSerializableExtra("order");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        if (order == null || order.getOrderId() == null) {
            Toast.makeText(this, "Không thể tải thông tin đơn hàng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(order.getUserId())
                .child(order.getOrderId());

        setupUI();
        populateOrderDetails();
        setupListeners();
        updateUiForRole();
    }

    private void updateUiForRole() {
        if (isAdmin) {
            binding.adminPanel.setVisibility(View.VISIBLE);
            binding.btnReorder.setVisibility(View.GONE);
            binding.btnCancelOrder.setVisibility(View.GONE);
            binding.btnCompleteOrder.setVisibility(View.GONE);
        } else {
            binding.adminPanel.setVisibility(View.GONE);
            binding.btnReorder.setVisibility(View.VISIBLE);

            String status = order.getStatus();
            if (status == null)
                status = "";

            // Cancel button logic: only "Đang xử lý" or "Pending"
            if ("Đang xử lý".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                binding.btnCancelOrder.setVisibility(View.VISIBLE);
            } else {
                binding.btnCancelOrder.setVisibility(View.GONE);
            }

            // Complete button logic: Show for "Đang giao", "Shipping" AND "Đang xử lý",
            // "Pending"
            // to match the list adapter logic and ensure it shows up during testing.
            if ("Đang giao".equalsIgnoreCase(status) || "Shipping".equalsIgnoreCase(status) ||
                    "Đang xử lý".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                binding.btnCompleteOrder.setVisibility(View.VISIBLE);
            } else {
                binding.btnCompleteOrder.setVisibility(View.GONE);
            }
        }
    }

    private void setupUI() {
        List<String> statusOptions = new ArrayList<>(Arrays.asList("Đang xử lý", "Đang giao", "Hoàn thành", "Đã hủy"));
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                statusOptions);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerStatus.setAdapter(statusAdapter);

        int currentStatusPosition = statusAdapter.getPosition(order.getStatus());
        if (currentStatusPosition >= 0) {
            binding.spinnerStatus.setSelection(currentStatusPosition);
        }

        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));
        itemAdapter = new AdminOrderDetailItemAdapter(
                order.getCartItems() != null ? order.getCartItems() : new ArrayList<>());
        binding.rvOrderItems.setAdapter(itemAdapter);
    }

    private void populateOrderDetails() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM, HH:mm", new Locale("vi", "VN"));
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

        binding.tvOrderId
                .setText("#" + order.getOrderId().substring(0, Math.min(6, order.getOrderId().length())).toUpperCase());
        binding.tvOrderDate.setText(order.getFormattedDate());
        binding.tvCustomerName.setText(order.getCustomerName());
        binding.tvCustomerPhone.setText(order.getPhoneNumber());
        binding.tvCustomerAddress.setText(order.getShippingAddress());
        binding.tvOrderTotal.setText(currencyFormat.format(order.getTotalAmount()));
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnUpdateStatus.setOnClickListener(v -> {
            String newStatus = binding.spinnerStatus.getSelectedItem().toString();
            updateOrderStatus(newStatus);
        });

        binding.btnReorder.setOnClickListener(v -> reorderItems());

        binding.btnCancelOrder.setOnClickListener(v -> showCancelConfirmationDialog());

        binding.btnCompleteOrder.setOnClickListener(v -> showCompleteConfirmationDialog());
    }

    private void showCancelConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setPositiveButton("Có", (dialog, which) -> cancelOrder())
                .setNegativeButton("Không", null)
                .show();
    }

    private void showCompleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận đã nhận hàng")
                .setMessage("Bạn xác nhận đã nhận được hàng và muốn hoàn tất đơn hàng?")
                .setPositiveButton("Đồng ý", (dialog, which) -> completeOrder())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void cancelOrder() {
        setLoading(true);
        ordersRef.child("status").setValue("Đã hủy")
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Đơn hàng đã được hủy thành công!",
                            Toast.LENGTH_SHORT).show();
                    order.setStatus("Đã hủy");
                    updateUiForRole();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Hủy đơn hàng thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void completeOrder() {
        setLoading(true);
        ordersRef.child("status").setValue("Hoàn thành")
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Đơn hàng đã hoàn thành!", Toast.LENGTH_SHORT).show();
                    order.setStatus("Hoàn thành");
                    updateUiForRole();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void reorderItems() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt lại đơn hàng.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (order.getCartItems() == null || order.getCartItems().isEmpty()) {
            Toast.makeText(this, "Đơn hàng này không có sản phẩm để đặt lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(currentUser.getUid());

        for (CartItem item : order.getCartItems()) {
            String cartItemId = cartRef.push().getKey();
            if (cartItemId != null) {
                cartRef.child(cartItemId).setValue(item);
            }
        }

        setLoading(false);
        Toast.makeText(this, "Đã thêm sản phẩm vào giỏ hàng!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateOrderStatus(String newStatus) {
        setLoading(true);
        ordersRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Cập nhật trạng thái thành công!", Toast.LENGTH_SHORT)
                            .show();
                    order.setStatus(newStatus);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminOrderDetailActivity.this, "Cập nhật thất bại: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBarDetail.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnUpdateStatus.setEnabled(!isLoading);
        binding.btnReorder.setEnabled(!isLoading);
        binding.btnCancelOrder.setEnabled(!isLoading);
        binding.btnCompleteOrder.setEnabled(!isLoading);
    }
}
