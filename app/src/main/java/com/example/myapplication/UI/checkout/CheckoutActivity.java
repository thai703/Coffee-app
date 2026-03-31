package com.example.myapplication.UI.checkout;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.Locale;

import com.example.myapplication.R;
import com.example.myapplication.UI.order.OrderHistoryActivity;
import com.example.myapplication.UI.profile.AddressActivity;
import com.example.myapplication.UI.profile.EditProfileActivity;
import com.example.myapplication.adapter.CheckoutSummaryAdapter;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.model.Address;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Order;
import com.example.myapplication.model.User;
import com.example.myapplication.utils.VNPayHelper;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CheckoutActivity extends AppCompatActivity {

    public static final String EXTRA_CART_ITEMS = "EXTRA_CART_ITEMS";
    public static final String EXTRA_TOTAL_AMOUNT = "EXTRA_TOTAL_AMOUNT";

    private RecyclerView orderSummaryRecyclerView;
    private CheckoutSummaryAdapter adapter;
    private TextView totalAmountTextView, userAddressTextView, subtotalTextView, btnChangeAddress, tvUserName,
            tvUserPhone;
    private RadioGroup paymentMethodRadioGroup;
    private MaterialButton confirmOrderButton;
    private List<CartItem> cartItems;
    private double totalAmount;
    private double subtotal;
    private ProgressBar progressBar;

    private final ActivityResultLauncher<Intent> editProfileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    String newName = data.getStringExtra(EditProfileActivity.EXTRA_NAME);
                    String newPhone = data.getStringExtra(EditProfileActivity.EXTRA_PHONE);
                    String newAddress = data.getStringExtra(EditProfileActivity.EXTRA_ADDRESS);

                    tvUserName.setText(newName);
                    tvUserPhone.setText(newPhone);
                    userAddressTextView.setText(newAddress);

                    updateUserProfileInFirebase(newName, newPhone, newAddress);
                }
            });

    private final ActivityResultLauncher<Intent> selectAddressLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(AddressActivity.EXTRA_SELECTED_ADDRESS)) {
                        Address selectedAddress = (Address) data
                                .getSerializableExtra(AddressActivity.EXTRA_SELECTED_ADDRESS);
                        if (selectedAddress != null) {
                            updateCheckoutAddress(selectedAddress);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (getIntent().getExtras() != null) {
            cartItems = getIntent().getParcelableArrayListExtra(EXTRA_CART_ITEMS);
        } else {
            cartItems = new ArrayList<>();
        }

        initViews();
        loadUserInfo();
        calculatePrices();
        setupRecyclerView();
        displayOrderSummary();
        setupClickListeners();
    }

    private void initViews() {
        orderSummaryRecyclerView = findViewById(R.id.rv_order_summary);
        totalAmountTextView = findViewById(R.id.tv_total_amount);
        subtotalTextView = findViewById(R.id.tv_subtotal);

        userAddressTextView = findViewById(R.id.tv_user_address);
        btnChangeAddress = findViewById(R.id.btn_change_address);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserPhone = findViewById(R.id.tv_user_phone);
        paymentMethodRadioGroup = findViewById(R.id.rg_payment_method);
        confirmOrderButton = findViewById(R.id.btn_confirm_order);
        progressBar = findViewById(R.id.progress_bar_checkout);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            confirmOrderButton.setEnabled(false);
            confirmOrderButton.setText("Đang xử lý...");
        } else {
            progressBar.setVisibility(View.GONE);
            confirmOrderButton.setEnabled(true);
            confirmOrderButton.setText("Xác nhận đơn hàng");
        }
    }

    private void setupClickListeners() {
        btnChangeAddress.setOnClickListener(v -> {
            Intent intent = new Intent(CheckoutActivity.this, AddressActivity.class);
            intent.putExtra("IS_SELECTION_MODE", true);
            selectAddressLauncher.launch(intent);
        });
        confirmOrderButton.setOnClickListener(v -> placeOrder());
    }

    private void updateCheckoutAddress(Address address) {
        tvUserName.setText(address.getName());
        tvUserPhone.setText(address.getPhone());
        userAddressTextView.setText(address.getFullAddress());
    }

    private void calculatePrices() {
        subtotal = 0;
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                if (item != null) {
                    subtotal += item.getProductPrice() * item.getQuantity();
                }
            }
        }
        // Removed shippingFee and discountAmount
        totalAmount = subtotal;
        if (totalAmount < 0)
            totalAmount = 0;
    }

    private void setupRecyclerView() {
        adapter = new CheckoutSummaryAdapter(cartItems);
        orderSummaryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        orderSummaryRecyclerView.setAdapter(adapter);
    }

    private void displayOrderSummary() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        subtotalTextView.setText(currencyFormat.format(subtotal));
        totalAmountTextView.setText(currencyFormat.format(totalAmount));
    }

    private void openEditProfile() {
        Intent intent = new Intent(CheckoutActivity.this, EditProfileActivity.class);
        intent.putExtra(EditProfileActivity.EXTRA_NAME, tvUserName.getText().toString());
        intent.putExtra(EditProfileActivity.EXTRA_PHONE, tvUserPhone.getText().toString());
        intent.putExtra(EditProfileActivity.EXTRA_ADDRESS, userAddressTextView.getText().toString());
        editProfileLauncher.launch(intent);
    }

    private boolean isDeliveryInfoIncomplete() {
        String name = tvUserName.getText().toString();
        String phone = tvUserPhone.getText().toString();
        String address = userAddressTextView.getText().toString();
        if (name == null || phone == null || address == null)
            return true;
        return name.contains("Chưa có") || phone.contains("Chưa có") || address.contains("Chưa có");
    }

    private void placeOrder() {
        if (paymentMethodRadioGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDeliveryInfoIncomplete()) {
            Toast.makeText(this, "Vui lòng cập nhật đầy đủ thông tin giao hàng.", Toast.LENGTH_LONG).show();
            openEditProfile();
            return;
        }

        int selectedPaymentId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        if (selectedPaymentId == R.id.rb_vnpay) {
            // Thanh toán VNPay - chuyển hướng qua trình duyệt
            initiateVNPayPayment();
        } else {
            // COD - Tạo đơn hàng trực tiếp
            runOrderCreationTask();
        }
    }

    private void runOrderCreationTask() {
        showLoading(true);

        final FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để đặt hàng", Toast.LENGTH_SHORT).show();
            showLoading(false);
            return;
        }
        final String userId = currentUser.getUid();
        final String customerName = tvUserName.getText().toString();
        final String phoneNumber = tvUserPhone.getText().toString();
        final String shippingAddress = userAddressTextView.getText().toString();
        final int selectedPaymentId = paymentMethodRadioGroup.getCheckedRadioButtonId();
        final List<CartItem> itemsForOrder = new ArrayList<>(cartItems != null ? cartItems : new ArrayList<>());

        final double finalTotalAmount = totalAmount;

        new Thread(() -> {
            String paymentMethod;
            if (selectedPaymentId == R.id.rb_cash_on_delivery) {
                paymentMethod = "Tiền mặt";
            } else if (selectedPaymentId == R.id.rb_vnpay) {
                paymentMethod = "Ví VNPay";
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(CheckoutActivity.this, "Phương thức thanh toán không hợp lệ", Toast.LENGTH_SHORT)
                            .show();
                    showLoading(false);
                });
                return;
            }

            DatabaseReference userOrdersRef = FirebaseDatabase.getInstance().getReference("orders").child(userId);
            String orderId = userOrdersRef.push().getKey();
            long orderDate = System.currentTimeMillis();

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                itemsForOrder.removeIf(Objects::isNull);
            }

            Order order = new Order(orderId, userId, itemsForOrder, finalTotalAmount, orderDate, customerName,
                    phoneNumber, shippingAddress, paymentMethod, "Pending");

            userOrdersRef.child(orderId).setValue(order).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // --- LƯU LỊCH SỬ ĐƠN HÀNG ĐỂ AI HỌC ---
                    saveOrderHistoryForAI(userId, itemsForOrder);
                    // ----------------------------------------

                    runOnUiThread(() -> {
                        showLoading(false);
                        CartManager.getInstance().clearCart();

                        Toast.makeText(CheckoutActivity.this, getString(R.string.status_order_success),
                                Toast.LENGTH_SHORT).show();

                        // Chuyển sang Order History (KHÔNG hiện Bill với tiền mặt vì chưa thanh toán)
                        Intent intent = new Intent(CheckoutActivity.this, OrderHistoryActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });

                } else {
                    runOnUiThread(() -> showLoading(false));
                    String errorMessage = getString(R.string.err_order_failed);
                    if (task.getException() != null) {
                        errorMessage = "Lỗi: " + task.getException().getMessage();
                        Log.e("CheckoutActivity", "Firebase Database Error", task.getException());
                    }
                    final String finalMsg = errorMessage;
                    runOnUiThread(() -> Toast.makeText(CheckoutActivity.this, finalMsg, Toast.LENGTH_LONG).show());
                }
            });
        }).start();

    }

    private void showReceiptDialog(String orderId, double totalAmount, List<CartItem> items) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_receipt);

        // Full width dialog
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            // Add some margin
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.y = 50; // Top margin
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

        // Set Data
        java.util.Date now = new java.util.Date();
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvTime.setText(timeFormat.format(now));

        tvOrderId.setText(orderId.length() > 6 ? orderId.substring(orderId.length() - 6) : orderId);

        // Payment Method & Customer Info
        // In CheckoutActivity, we know the method (Cash/VNPay). This dialog is
        // typically for Cash?
        // Actually CheckoutActivity calls this for Cash (removed actually, but
        // restoring functionality maybe?
        // OR user said "CheckoutActivity no longer calls showReceiptDialog" in previous
        // summary
        // BUT I'm modifying CheckoutActivity source code.
        // Wait, current CheckoutActivity DOES NOT call showReceiptDialog for COD?
        // It calls it for VNPayReturn? No VNPayReturn is separate.
        // Let's check where showReceiptDialog is called in CheckoutActivity.
        // It was removed for COD flow.
        // If it's dead code in CheckoutActivity, I should still update it to compile
        // OR maybe it is used for something else?
        // Ah, the previous turn I REMOVED the call for COD.
        // But the method definition exists. I will update it just in case or to prevent
        // build errors.
        // I will bind placeholders.

        tvPaymentMethod.setText("Tiền mặt"); // Defaulting since this activity handles Cash
        // Get user info if available
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance()
                .getCurrentUser();
        if (user != null) {
            tvCustomerName.setText(user.getDisplayName() != null ? user.getDisplayName() : "Khách hàng");
            tvCustomerEmail.setText(user.getEmail() != null ? user.getEmail() : "...");
        } else {
            // Fallback to shipping name if we can get it?
            // Just leaving as ... or "Khách vãng lai"
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

        // Close button logic...
        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
            // Navigate to Order History after interaction
            Intent intent = new Intent(CheckoutActivity.this, OrderHistoryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    private boolean saveReceiptAsImage(View view, String orderId) {
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

    private void saveOrderHistoryForAI(String userId, List<CartItem> items) {
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .child("order_history");

        // Tạo chuỗi danh sách các món
        StringBuilder itemsBuilder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            itemsBuilder.append(items.get(i).getProductName());
            if (i < items.size() - 1) {
                itemsBuilder.append(", ");
            }
        }
        String orderContent = itemsBuilder.toString();

        // Lưu vào Firebase theo timestamp
        String timestamp = String.valueOf(System.currentTimeMillis());
        historyRef.child(timestamp).setValue(orderContent);
    }

    private void loadUserInfo() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            tvUserName
                                    .setText(TextUtils.isEmpty(user.getName()) ? getString(R.string.placeholder_no_name)
                                            : user.getName());
                            tvUserPhone.setText(
                                    TextUtils.isEmpty(user.getPhone()) ? getString(R.string.placeholder_no_phone)
                                            : user.getPhone());
                            userAddressTextView.setText(
                                    TextUtils.isEmpty(user.getAddress()) ? getString(R.string.placeholder_no_address)
                                            : user.getAddress());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(CheckoutActivity.this, getString(R.string.err_load_user_info), Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }
    }

    private void initiateVNPayPayment() {
        // Validate thông tin giao hàng
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();
        String customerName = tvUserName.getText().toString();
        String phoneNumber = tvUserPhone.getText().toString();
        String shippingAddress = userAddressTextView.getText().toString();

        // Tạo thông tin đơn hàng
        String orderInfo = "Thanh toan don hang #" + System.currentTimeMillis();
        long totalAmountLong = (long) this.totalAmount;

        // Lưu thông tin đơn hàng vào SharedPreferences để xử lý sau khi thanh toán
        android.content.SharedPreferences prefs = getSharedPreferences("VNPAY_ORDER", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        // Lưu thông tin cơ bản
        editor.putString("userId", userId);
        editor.putString("customerName", customerName);
        editor.putString("phoneNumber", phoneNumber);
        editor.putString("shippingAddress", shippingAddress);
        editor.putFloat("totalAmount", (float) this.totalAmount);
        editor.putLong("orderDate", System.currentTimeMillis());

        // Lưu cart items dưới dạng JSON
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String cartItemsJson = gson.toJson(cartItems);
        editor.putString("cartItems", cartItemsJson);

        editor.apply();

        // Tạo payment URL
        String paymentUrl = VNPayHelper.createPaymentUrl(orderInfo, totalAmountLong, "billpayment");

        if (paymentUrl != null) {
            // Mở trình duyệt để thanh toán
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(paymentUrl));
            startActivity(browserIntent);

            Toast.makeText(this, R.string.vnpay_processing, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Lỗi tạo link thanh toán VNPay", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUserProfileInFirebase(String name, String phone, String address) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);
            updates.put("phone", phone);
            updates.put("address", address);
            userRef.updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Toast
                            .makeText(CheckoutActivity.this, "Đã cập nhật thông tin", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast
                            .makeText(CheckoutActivity.this, "Lỗi cập nhật thông tin", Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
