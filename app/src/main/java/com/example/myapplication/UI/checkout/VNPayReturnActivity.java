package com.example.myapplication.UI.checkout;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.example.myapplication.UI.Home.HomeActivity;
import com.example.myapplication.UI.order.OrderHistoryActivity;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.utils.VNPayHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.CartItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VNPayReturnActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        if (uri != null) {
            handleVNPayReturn(uri);
        } else {
            finish();
        }
    }

    private void handleVNPayReturn(Uri uri) {
        // Verify chữ ký
        boolean isValid = VNPayHelper.verifyPaymentResponse(uri);

        if (!isValid) {
            Toast.makeText(this, "Giao dịch không hợp lệ", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Lấy response code
        String responseCode = uri.getQueryParameter("vnp_ResponseCode");
        String txnRef = uri.getQueryParameter("vnp_TxnRef");
        String amount = uri.getQueryParameter("vnp_Amount");

        if ("00".equals(responseCode)) {
            // Thanh toán thành công - Tạo đơn hàng
            saveOrderToFirebase(txnRef);

        } else {
            // Thanh toán thất bại
            String errorMsg = VNPayHelper.getResponseMessage(responseCode);
            Toast.makeText(this, "Thanh toán thất bại: " + errorMsg, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void saveOrderToFirebase(String txnRef) {
        // Lấy thông tin đơn hàng từ SharedPreferences
        SharedPreferences prefs = getSharedPreferences("VNPAY_ORDER", MODE_PRIVATE);

        String userId = prefs.getString("userId", null);
        String customerName = prefs.getString("customerName", "");
        String phoneNumber = prefs.getString("phoneNumber", "");
        String shippingAddress = prefs.getString("shippingAddress", "");
        float totalAmount = prefs.getFloat("totalAmount", 0);
        long orderDate = prefs.getLong("orderDate", System.currentTimeMillis());
        String cartItemsJson = prefs.getString("cartItems", "[]");

        if (userId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin đơn hàng", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Parse cart items từ JSON
        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<CartItem>>() {
        }.getType();
        List<CartItem> orderItems = gson.fromJson(cartItemsJson, listType);

        // Tạo đơn hàng với status = "Completed" (đã thanh toán)
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(userId);
        String orderId = ordersRef.push().getKey();

        Order order = new Order(
                orderId,
                userId,
                orderItems,
                totalAmount,
                orderDate,
                customerName,
                phoneNumber,
                shippingAddress,
                "Ví VNPay",
                "Completed" // Trạng thái "Completed" vì đã thanh toán thành công
        );

        if (orderId != null) {
            ordersRef.child(orderId).setValue(order)
                    .addOnSuccessListener(aVoid -> {
                        // Xóa giỏ hàng
                        // Xóa giỏ hàng
                        CartManager.getInstance().clearCart();

                        // Xóa SharedPreferences
                        prefs.edit().clear().apply();

                        Toast.makeText(this, "Thanh toán thành công! Đơn hàng đã được tạo.", Toast.LENGTH_LONG).show();

                        // Hiển thị hóa đơn
                        showReceiptDialog(order);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi lưu đơn hàng: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    });
        }
    }

    private void showReceiptDialog(Order order) {
        String orderId = order.getOrderId();
        double totalAmount = order.getTotalAmount();
        List<CartItem> items = order.getCartItems();
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(com.example.myapplication.R.layout.dialog_receipt);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50;
        }

        android.widget.TextView tvTime = dialog.findViewById(com.example.myapplication.R.id.tv_receipt_time);
        android.widget.TextView tvOrderId = dialog.findViewById(com.example.myapplication.R.id.tv_receipt_order_id);
        android.widget.TextView tvTotal = dialog.findViewById(com.example.myapplication.R.id.tv_receipt_total);
        android.widget.TextView tvPaymentMethod = dialog.findViewById(com.example.myapplication.R.id.tv_payment_method);
        android.widget.TextView tvCustomerName = dialog.findViewById(com.example.myapplication.R.id.tv_customer_name);
        android.widget.TextView tvCustomerEmail = dialog.findViewById(com.example.myapplication.R.id.tv_customer_email);

        android.widget.LinearLayout llItems = dialog.findViewById(com.example.myapplication.R.id.ll_receipt_items);
        android.widget.Button btnClose = dialog.findViewById(com.example.myapplication.R.id.btn_close_receipt);
        android.widget.Button btnSave = dialog.findViewById(com.example.myapplication.R.id.btn_save_receipt);

        // Set Data
        java.util.Date now = new java.util.Date();
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvTime.setText(timeFormat.format(now));

        tvOrderId.setText(orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId);
        tvPaymentMethod.setText("VNPay");

        String customerName = order.getCustomerName();

        // Try to get user info if connected (for email fallback)
        FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (customerName != null && !customerName.isEmpty()) {
            tvCustomerName.setText(customerName);
            if (user != null) {
                tvCustomerEmail.setText(user.getEmail() != null ? user.getEmail() : "...");
            } else {
                tvCustomerEmail.setText("...");
            }
        } else if (user != null) {
            tvCustomerName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Khách hàng");
            tvCustomerEmail.setText(user.getEmail() != null ? user.getEmail() : "...");
        } else {
            tvCustomerName.setText("Khách vãng lai");
            tvCustomerEmail.setText("...");
        }

        java.text.NumberFormat currencyFormat = java.text.NumberFormat
                .getCurrencyInstance(new java.util.Locale("vi", "VN"));
        tvTotal.setText(currencyFormat.format(totalAmount));

        // Add Items (Table Row Style)
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
            Intent intent = new Intent(this, OrderHistoryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
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

        Uri uri = getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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
