package com.example.myapplication.UI.order;

import android.os.Bundle;
import android.view.View;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.myapplication.databinding.ActivityUserOrderDetailBinding;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.adapter.OrderDetailAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserOrderDetailActivity extends AppCompatActivity {
    private ActivityUserOrderDetailBinding binding;
    private Order order;
    private OrderDetailAdapter adapter;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        binding = ActivityUserOrderDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();

        // Setup toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize RecyclerView
        binding.rvOrderItems.setLayoutManager(new LinearLayoutManager(this));

        // Get order from intent
        if (getIntent().hasExtra("order")) {
            order = (Order) getIntent().getSerializableExtra("order");
            displayOrderDetails();
            setupActionButton();
        } else {
            Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private String formatDate(Date date) {
        if (date == null)
            return "N/A";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return sdf.format(date);
    }

    private void displayOrderDetails() {
        if (order != null) {
            // Display customer info with formatted labels
            if (order.getCustomerName() != null) {
                binding.tvCustomerName.setText(getString(R.string.label_name) + order.getCustomerName());
            }
            if (order.getPhoneNumber() != null) {
                binding.tvPhoneNumber.setText(getString(R.string.label_phone) + order.getPhoneNumber());
            }
            if (order.getShippingAddress() != null) {
                binding.tvAddress.setText(getString(R.string.label_address) + order.getShippingAddress());
            }

            // Display order info with prefix
            binding.tvOrderId.setText(getString(R.string.order_id_prefix) + order.getOrderId());

            if (order.getTimestamp() != 0) {
                binding.tvOrderDate.setText(
                        getString(R.string.label_order_date) + "\n" + formatDate(new Date(order.getTimestamp())));
            }

            // Translate Status
            String originalStatus = order.getStatus();
            String displayStatus = originalStatus;
            int backgroundResId = R.drawable.bg_status_completed; // Default

            if (originalStatus != null) {
                if ("Pending".equalsIgnoreCase(originalStatus) || "Confirmed".equalsIgnoreCase(originalStatus)
                        || "Đang xử lý".equalsIgnoreCase(originalStatus)) {
                    displayStatus = getString(R.string.status_pending);
                    backgroundResId = R.drawable.bg_status_pending;
                } else if ("Shipping".equalsIgnoreCase(originalStatus)
                        || "Đang giao".equalsIgnoreCase(originalStatus)) {
                    displayStatus = getString(R.string.status_shipping_icon).replace("🛵 ", ""); // Reuse string but
                                                                                                 // remove icon if
                                                                                                 // needed or keep it
                    backgroundResId = R.drawable.bg_status_processing;
                } else if ("Completed".equalsIgnoreCase(originalStatus)
                        || "Hoàn thành".equalsIgnoreCase(originalStatus)) {
                    displayStatus = getString(R.string.status_completed);
                    backgroundResId = R.drawable.bg_status_completed;
                } else if ("Cancelled".equalsIgnoreCase(originalStatus)
                        || "Đã hủy".equalsIgnoreCase(originalStatus)) {
                    displayStatus = getString(R.string.status_cancelled);
                    backgroundResId = R.drawable.bg_status_cancelled;
                }
            }
            binding.tvOrderStatus.setText(displayStatus);
            binding.tvOrderStatus.setBackgroundResource(backgroundResId);

            // Display products
            if (order.getCartItems() != null && !order.getCartItems().isEmpty()) {
                boolean isCompleted = "Hoàn thành".equalsIgnoreCase(order.getStatus())
                        || "Completed".equalsIgnoreCase(order.getStatus());
                adapter = new OrderDetailAdapter(this, order.getCartItems(), isCompleted, productId -> {
                    Intent intent = new Intent(UserOrderDetailActivity.this,
                            com.example.myapplication.UI.Rating.AddRatingActivity.class);
                    intent.putExtra("PRODUCT_ID", productId);
                    intent.putExtra("ORDER_ID", order.getOrderId());
                    startActivity(intent);
                });
                binding.rvOrderItems.setAdapter(adapter);
            }

            // Display payment summary
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            double subtotal = order.getTotalAmount() - order.getShippingFee();
            binding.tvSubtotal.setText(formatter.format(subtotal));
            binding.tvShippingFee.setText(formatter.format(order.getShippingFee()));
            binding.tvTotalAmount.setText(formatter.format(order.getTotalAmount()));
        }
    }

    private void setupActionButton() {
        if (order == null)
            return;

        String status = order.getStatus();

        // Pending: Show ONLY Cancel + Confirm
        if ("Đang xử lý".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
            // Button Hủy
            binding.btnSecondaryAction.setVisibility(View.VISIBLE);
            binding.btnSecondaryAction.setText(R.string.cancel_order);
            binding.btnSecondaryAction.setOnClickListener(v -> cancelOrder());

            // Button Xác nhận
            binding.btnAction.setVisibility(View.VISIBLE);
            binding.btnAction.setText(R.string.confirm_order);
            binding.btnAction.setOnClickListener(v -> confirmCompleted());

        } else if ("Đang giao".equalsIgnoreCase(status) || "Shipping".equalsIgnoreCase(status)) {
            binding.btnSecondaryAction.setVisibility(View.GONE);

            binding.btnAction.setText(R.string.action_confirm_received);
            binding.btnAction.setVisibility(View.VISIBLE);
            binding.btnAction.setOnClickListener(v -> confirmCompleted());

        } else if ("Hoàn thành".equalsIgnoreCase(status) || "Completed".equalsIgnoreCase(status)) {
            binding.btnSecondaryAction.setVisibility(View.GONE);

            binding.btnAction.setText(R.string.action_reorder);
            binding.btnAction.setVisibility(View.VISIBLE);
            binding.btnAction.setOnClickListener(v -> reorderOrder());

        } else if ("Đã hủy".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
            binding.btnSecondaryAction.setVisibility(View.GONE);

            binding.btnAction.setText(R.string.action_reorder);
            binding.btnAction.setVisibility(View.VISIBLE);
            binding.btnAction.setOnClickListener(v -> reorderOrder());

        } else {
            binding.btnSecondaryAction.setVisibility(View.GONE);
            binding.btnAction.setVisibility(View.GONE);
        }
    }

    private void reorderOrder() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.msg_login_reorder, Toast.LENGTH_SHORT).show();
            return;
        }

        if (order.getCartItems() == null || order.getCartItems().isEmpty()) {
            Toast.makeText(this, R.string.msg_no_item_reorder, Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference cartRef = FirebaseDatabase.getInstance().getReference("carts").child(currentUser.getUid());

        for (CartItem item : order.getCartItems()) {
            String cartItemId = cartRef.push().getKey();
            if (cartItemId != null) {
                cartRef.child(cartItemId).setValue(item);
            }
            // Update local CartManager so CartActivity sees it immediately
            com.example.myapplication.manager.CartManager.getInstance().addToCart(item);
        }

        Toast.makeText(this, R.string.msg_reorder_added, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, com.example.myapplication.UI.Cart.CartActivity.class);
        startActivity(intent);
        finish();
    }

    private void cancelOrder() {
        if (order == null || order.getUserId() == null)
            return;

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(order.getUserId())
                .child(order.getOrderId());

        orderRef.child("status").setValue("Đã hủy")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.msg_cancel_success, Toast.LENGTH_SHORT).show();
                    order.setStatus("Đã hủy");
                    setupActionButton(); // Refresh buttons
                    displayOrderDetails(); // Refresh status text
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmCompleted() {
        if (order == null || order.getUserId() == null)
            return;

        DatabaseReference orderRef = FirebaseDatabase.getInstance()
                .getReference("orders")
                .child(order.getUserId())
                .child(order.getOrderId());

        orderRef.child("status").setValue("Completed")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, R.string.msg_confirm_success, Toast.LENGTH_SHORT).show();
                    order.setStatus("Completed");

                    // Hiển thị Bill sau khi xác nhận hoàn thành
                    showReceiptDialog(order.getOrderId(), order.getTotalAmount(), order.getCartItems());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showReceiptDialog(String orderId, double totalAmount, java.util.List<CartItem> items) {
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
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(timeFormat.format(now));

        tvOrderId.setText(orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId);

        // Populate extra info from 'order' object
        if (order != null) {
            tvPaymentMethod.setText(order.getPaymentMethod() != null ? order.getPaymentMethod() : "Tiền mặt");
            tvCustomerName.setText(order.getCustomerName() != null ? order.getCustomerName() : "...");
            // Email might not be in Order, check User?
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                tvCustomerEmail.setText(user.getEmail());
                if (order.getCustomerName() == null)
                    tvCustomerName.setText(user.getDisplayName());
            } else {
                tvCustomerEmail.setText("...");
            }
        } else {
            tvPaymentMethod.setText("...");
            tvCustomerName.setText("...");
            tvCustomerEmail.setText("...");
        }

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvTotal.setText(currencyFormat.format(totalAmount));

        for (CartItem item : items) {
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

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            setupActionButton();
            displayOrderDetails();
            finish();
        });

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
}
