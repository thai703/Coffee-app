package com.example.myapplication.UI.order;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.Admin.AdminOrderDetailActivity;
import com.example.myapplication.R;
import com.example.myapplication.UI.Home.HomeActivity;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.adapter.OrderHistoryAdapter;
import com.example.myapplication.databinding.ActivityOrderHistoryBinding;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Order;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity implements OrderHistoryAdapter.OnItemClickListener {

    private ActivityOrderHistoryBinding binding;
    private OrderHistoryAdapter adapter;
    private List<Order> fullOrderList;
    private DatabaseReference ordersRef;
    private FirebaseAuth mAuth;

    // Listener management to prevent Permission Denied errors on logout
    private DatabaseReference userOrdersRef;
    private ValueEventListener orderListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        binding = ActivityOrderHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarOrderHistory);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbarOrderHistory.setNavigationOnClickListener(v -> finish());

        mAuth = FirebaseAuth.getInstance();
        ordersRef = FirebaseDatabase.getInstance().getReference("orders");

        setupRecyclerView();
        setupTabs();

        binding.btnStartShopping.setOnClickListener(v -> {
            Intent intent = new Intent(OrderHistoryActivity.this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadOrderHistory();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
    }

    private void setupRecyclerView() {
        binding.recyclerViewOrders.setLayoutManager(new LinearLayoutManager(this));
        fullOrderList = new ArrayList<>();
        adapter = new OrderHistoryAdapter(this, new ArrayList<>(), this);
        binding.recyclerViewOrders.setAdapter(adapter);
    }

    private void setupTabs() {
        binding.tabLayoutStatus.removeAllTabs(); // Clear existing tabs
        binding.tabLayoutStatus.addTab(binding.tabLayoutStatus.newTab().setText(getString(R.string.tab_all)));
        binding.tabLayoutStatus.addTab(binding.tabLayoutStatus.newTab().setText(getString(R.string.tab_pending)));
        binding.tabLayoutStatus.addTab(binding.tabLayoutStatus.newTab().setText(getString(R.string.tab_completed)));

        binding.tabLayoutStatus.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab != null && tab.getText() != null) {
                    filterOrdersByStatus(tab.getText().toString());
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
    }

    private void filterOrdersByStatus(String statusTab) {
        List<Order> filteredList = new ArrayList<>();
        if (getString(R.string.tab_all).equalsIgnoreCase(statusTab)) {
            filteredList.addAll(fullOrderList);
        } else {
            for (Order order : fullOrderList) {
                String orderStatus = order.getStatus();
                if (orderStatus == null)
                    continue;

                boolean match = false;
                if (getString(R.string.tab_pending).equalsIgnoreCase(statusTab)) {
                    match = "Pending".equalsIgnoreCase(orderStatus) || "Đang xử lý".equalsIgnoreCase(orderStatus);
                } else if (getString(R.string.tab_completed).equalsIgnoreCase(statusTab)) {
                    match = "Completed".equalsIgnoreCase(orderStatus) || "Hoàn thành".equalsIgnoreCase(orderStatus);
                }

                if (match) {
                    filteredList.add(order);
                }
            }
        }
        adapter.updateList(filteredList);
    }

    private void loadOrderHistory() {
        binding.progressBarOrders.setVisibility(View.VISIBLE);
        binding.layoutEmptyState.setVisibility(View.GONE);
        binding.recyclerViewOrders.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User not logged in, can't load orders
            binding.progressBarOrders.setVisibility(View.GONE);
            binding.layoutEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewOrders.setVisibility(View.GONE);
            return;
        }

        String userId = currentUser.getUid();
        userOrdersRef = ordersRef.child(userId);

        if (orderListener == null) {
            orderListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    fullOrderList.clear();
                    if (snapshot.exists()) {
                        for (DataSnapshot orderSnapshot : snapshot.getChildren()) {
                            try {
                                Order order = orderSnapshot.getValue(Order.class);
                                if (order != null) {
                                    fullOrderList.add(order);
                                }
                            } catch (Exception e) {
                                Log.e("OrderHistory", "Error parsing order: " + orderSnapshot.getKey(), e);
                            }
                        }
                        Collections.reverse(fullOrderList);

                        int selectedTabPosition = binding.tabLayoutStatus.getSelectedTabPosition();
                        if (selectedTabPosition != -1) {
                            TabLayout.Tab tab = binding.tabLayoutStatus.getTabAt(selectedTabPosition);
                            if (tab != null && tab.getText() != null) {
                                filterOrdersByStatus(tab.getText().toString());
                            }
                        } else {
                            filterOrdersByStatus("Tất cả");
                        }

                        binding.recyclerViewOrders.setVisibility(View.VISIBLE);
                        binding.layoutEmptyState.setVisibility(View.GONE);
                    } else {
                        binding.recyclerViewOrders.setVisibility(View.GONE);
                        binding.layoutEmptyState.setVisibility(View.VISIBLE);
                    }
                    binding.progressBarOrders.setVisibility(View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Check if activity is still valid
                    if (!isFinishing() && !isDestroyed()) {
                        binding.progressBarOrders.setVisibility(View.GONE);
                        Log.e("OrderHistory", "Firebase query failed: " + error.getMessage());
                        // Don't show Toast for Permission Denied if it's due to logout
                        if (!error.getMessage().contains("Permission denied")) {
                            Toast.makeText(OrderHistoryActivity.this, "Failed to load data: " + error.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            };
        }

        // Attach listener
        userOrdersRef.addValueEventListener(orderListener);
    }

    private void detachListener() {
        if (userOrdersRef != null && orderListener != null) {
            userOrdersRef.removeEventListener(orderListener);
        }
    }

    @Override
    public void onItemClick(Order order) {
        // Switch to UserOrderDetailActivity for better UI/UX for customers
        Intent intent = new Intent(this, com.example.myapplication.UI.order.UserOrderDetailActivity.class);
        intent.putExtra("order", order);
        startActivity(intent);
    }

    @Override
    public void onCancelOrder(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn hàng")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng này không?")
                .setPositiveButton("Có", (dialog, which) -> cancelOrderFirebase(order))
                .setNegativeButton("Không", null)
                .show();
    }

    private void cancelOrderFirebase(Order order) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            return;

        DatabaseReference orderRef = ordersRef.child(currentUser.getUid()).child(order.getOrderId());
        orderRef.child("status").setValue("Đã hủy")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đơn hàng đã được hủy thành công!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Hủy đơn hàng thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Đã thêm @Override để đảm bảo liên kết đúng với Interface
    @Override
    public void onConfirmCompleted(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hoàn thành")
                .setMessage("Bạn đã nhận được đơn hàng và muốn xác nhận hoàn thành?")
                .setPositiveButton("Xác nhận", (dialog, which) -> updateOrderStatus(order, "Completed"))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null)
            return;

        DatabaseReference orderRef = ordersRef.child(currentUser.getUid()).child(order.getOrderId());
        orderRef.child("status").setValue(newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái đơn hàng!", Toast.LENGTH_SHORT).show();

                    // Hiển thị Bill khi xác nhận hoàn thành
                    if ("Completed".equals(newStatus)) {
                        showReceiptDialog(order.getOrderId(), order.getTotalAmount(), order.getCartItems());
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showReceiptDialog(String orderId, double totalAmount,
            java.util.List<com.example.myapplication.model.CartItem> items) {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_receipt);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50;
        }

        android.widget.TextView tvTime = dialog.findViewById(R.id.tv_receipt_time);
        android.widget.TextView tvOrderId = dialog.findViewById(R.id.tv_receipt_order_id);
        android.widget.TextView tvTotal = dialog.findViewById(R.id.tv_receipt_total);
        android.widget.TextView tvPaymentMethod = dialog.findViewById(R.id.tv_payment_method);
        android.widget.TextView tvCustomerName = dialog.findViewById(R.id.tv_customer_name);
        android.widget.TextView tvCustomerEmail = dialog.findViewById(R.id.tv_customer_email);

        android.widget.LinearLayout llItems = dialog.findViewById(R.id.ll_receipt_items);
        android.widget.Button btnClose = dialog.findViewById(R.id.btn_close_receipt);
        android.widget.Button btnSave = dialog.findViewById(R.id.btn_save_receipt);

        java.util.Date now = new java.util.Date();
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvTime.setText(timeFormat.format(now));

        tvOrderId.setText(orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId);

        // Find Order object from list to get details
        com.example.myapplication.model.Order targetOrder = null;
        if (fullOrderList != null) {
            for (com.example.myapplication.model.Order o : fullOrderList) {
                if (o.getOrderId().equals(orderId)) {
                    targetOrder = o;
                    break;
                }
            }
        }

        if (targetOrder != null) {
            tvPaymentMethod
                    .setText(targetOrder.getPaymentMethod() != null ? targetOrder.getPaymentMethod() : "Tiền mặt");
            tvCustomerName.setText(targetOrder.getCustomerName() != null ? targetOrder.getCustomerName() : "...");
            // Email is likely not in Order, but we can check current user if it matches
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .getCurrentUser();
            if (user != null) {
                tvCustomerEmail.setText(user.getEmail());
                if (targetOrder.getCustomerName() == null)
                    tvCustomerName.setText(user.getDisplayName());
            } else {
                tvCustomerEmail.setText("...");
            }
        } else {
            tvPaymentMethod.setText("...");
            tvCustomerName.setText("...");
            tvCustomerEmail.setText("...");
        }

        java.text.NumberFormat currencyFormat = java.text.NumberFormat
                .getCurrencyInstance(new java.util.Locale("vi", "VN"));
        tvTotal.setText(currencyFormat.format(totalAmount));

        for (com.example.myapplication.model.CartItem item : items) {
            android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(this);
            rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, 8, 0, 8);

            // Name: Weight 2
            android.widget.TextView tvName = new android.widget.TextView(this);
            tvName.setText(item.getProductName());
            tvName.setTextColor(android.graphics.Color.BLACK);
            android.widget.LinearLayout.LayoutParams paramsName = new android.widget.LinearLayout.LayoutParams(0, -2,
                    2.0f);
            tvName.setLayoutParams(paramsName);
            tvName.setGravity(android.view.Gravity.START | android.view.Gravity.CENTER_VERTICAL);

            // Quantity: Weight 0.5
            android.widget.TextView tvQty = new android.widget.TextView(this);
            tvQty.setText(String.valueOf(item.getQuantity()));
            tvQty.setTextColor(android.graphics.Color.BLACK);
            android.widget.LinearLayout.LayoutParams paramsQty = new android.widget.LinearLayout.LayoutParams(0, -2,
                    0.5f);
            tvQty.setLayoutParams(paramsQty);
            tvQty.setGravity(android.view.Gravity.CENTER);

            // Price: Weight 1
            android.widget.TextView tvPrice = new android.widget.TextView(this);
            tvPrice.setText(currencyFormat.format(item.getProductPrice()));
            tvPrice.setTextColor(android.graphics.Color.BLACK);
            android.widget.LinearLayout.LayoutParams paramsPrice = new android.widget.LinearLayout.LayoutParams(0, -2,
                    1.0f);
            tvPrice.setLayoutParams(paramsPrice);
            tvPrice.setGravity(android.view.Gravity.CENTER);

            // Total: Weight 1
            android.widget.TextView tvTotalItem = new android.widget.TextView(this);
            tvTotalItem.setText(currencyFormat.format(item.getProductPrice() * item.getQuantity()));
            tvTotalItem.setTextColor(android.graphics.Color.BLACK);
            android.widget.LinearLayout.LayoutParams paramsTotal = new android.widget.LinearLayout.LayoutParams(0, -2,
                    1.0f);
            tvTotalItem.setLayoutParams(paramsTotal);
            tvTotalItem.setGravity(android.view.Gravity.CENTER);

            rowLayout.addView(tvName);
            rowLayout.addView(tvQty);
            rowLayout.addView(tvPrice);
            rowLayout.addView(tvTotalItem);

            llItems.addView(rowLayout);
        }

        btnSave.setOnClickListener(v -> {
            // Hide buttons to not include them in the image
            btnSave.setVisibility(android.view.View.GONE);
            btnClose.setVisibility(android.view.View.GONE);

            // Use post to ensure layout updates
            dialog.getWindow().getDecorView().post(() -> {
                android.view.View viewToSave = dialog.getWindow().getDecorView();
                if (saveReceiptAsImage(viewToSave, orderId)) {
                    Toast.makeText(this, "Đã lưu hóa đơn vào thư viện!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Lỗi lưu hóa đơn", Toast.LENGTH_SHORT).show();
                }

                // Restore buttons
                btnSave.setVisibility(android.view.View.VISIBLE);
                btnClose.setVisibility(android.view.View.VISIBLE);
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setCancelable(false);
        dialog.show();
    }

    private boolean saveReceiptAsImage(android.view.View view, String orderId) {
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(
                view.getWidth(), view.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        view.draw(canvas);

        String fileName = "Bill_" + orderId + "_" + System.currentTimeMillis() + ".jpg";

        android.content.ContentValues values = new android.content.ContentValues();
        values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/AuraCoffeeBills");
        }

        android.net.Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values);
        if (uri == null)
            return false;

        try (java.io.OutputStream out = getContentResolver().openOutputStream(uri)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, out);
            return true;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onReorder(Order order) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt lại đơn hàng.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (order.getCartItems() == null || order.getCartItems().isEmpty()) {
            Toast.makeText(this, "Đơn hàng này không có sản phẩm để đặt lại.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(currentUser.getUid());

        for (CartItem item : order.getCartItems()) {
            String cartItemId = cartRef.push().getKey();
            if (cartItemId != null) {
                cartRef.child(cartItemId).setValue(item);
            }
            // Update local CartManager
            com.example.myapplication.manager.CartManager.getInstance().addToCart(item);
        }

        Toast.makeText(this, "Đã thêm sản phẩm vào giỏ hàng!", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, com.example.myapplication.UI.Cart.CartActivity.class);
        startActivity(intent);
    }
}
