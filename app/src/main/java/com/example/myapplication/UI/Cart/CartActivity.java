package com.example.myapplication.UI.Cart;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.UI.checkout.CheckoutActivity;
import com.example.myapplication.UI.Home.HomeActivity;
import com.example.myapplication.adapter.CartAdapter;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.model.CartItem;
import com.google.android.material.button.MaterialButton;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {

    private RecyclerView cartRecyclerView;
    private CartAdapter cartAdapter;
    private CartManager cartManager;

    private LinearLayout emptyStateView;
    private MaterialButton continueShoppingButton;
    private MaterialButton checkoutButton;
    private View loadingView;

    private TextView tvSubtotal, tvShippingFee, tvTotal;
    private List<CartItem> currentCartItems = new ArrayList<>();
    private double currentTotal = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        initViews();

        cartManager = CartManager.getInstance();
        setupRecyclerView();
        setupListeners();

        cartManager.getCartItemsLiveData().observe(this, cartItems -> {
            Log.d("CartActivity", "LiveData changed! Item count: " + (cartItems != null ? cartItems.size() : 0));
            currentCartItems = cartItems != null ? cartItems : new ArrayList<>();
            cartAdapter.updateCartItems(currentCartItems);
            updateUiComponents(currentCartItems);
        });

        cartManager.getIsLoadingLiveData().observe(this, isLoading -> {
            if (loadingView != null) {
                loadingView.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (isLoading) {
                cartRecyclerView.setVisibility(View.GONE);
                emptyStateView.setVisibility(View.GONE);
            } else {
                updateUiComponents(currentCartItems);
            }
        });
    }

    private void initViews() {
        cartRecyclerView = findViewById(R.id.recycler_view_cart_items);
        emptyStateView = findViewById(R.id.empty_state_view);
        continueShoppingButton = findViewById(R.id.btn_continue_shopping);
        checkoutButton = findViewById(R.id.btn_checkout);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvShippingFee = findViewById(R.id.tv_shipping_fee);
        tvTotal = findViewById(R.id.tv_total);

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.cart_title));
    }

    private void setupRecyclerView() {
        cartAdapter = new CartAdapter(new ArrayList<>(), new CartAdapter.CartItemListener() {
            @Override
            public void onIncrease(CartItem item) {
                cartManager.addCartItem(item, 1);
            }

            @Override
            public void onDecrease(CartItem item) {
                cartManager.removeCartItem(item);
            }

            @Override
            public void onRemove(CartItem item) {
                cartManager.removeAllOfItem(item);
            }
        });
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        cartRecyclerView.setAdapter(cartAdapter);
    }

    private void setupListeners() {
        continueShoppingButton.setOnClickListener(v -> {
            Intent intent = new Intent(CartActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });

        checkoutButton.setOnClickListener(v -> {
            if (currentCartItems != null && !currentCartItems.isEmpty()) {
                Intent intent = new Intent(CartActivity.this, CheckoutActivity.class);
                intent.putParcelableArrayListExtra(CheckoutActivity.EXTRA_CART_ITEMS,
                        (ArrayList<CartItem>) currentCartItems);
                intent.putExtra(CheckoutActivity.EXTRA_TOTAL_AMOUNT, currentTotal);
                startActivity(intent);
            } else {
                Toast.makeText(this, getString(R.string.empty_cart_title), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUiComponents(List<CartItem> cartList) {
        boolean isEmpty = cartList == null || cartList.isEmpty();

        emptyStateView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        cartRecyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        findViewById(R.id.checkout_summary_card).setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        double subTotal = 0;
        for (CartItem item : cartList) {
            subTotal += item.getProductPrice() * item.getQuantity();
        }

        double shippingFee = 0.0;
        currentTotal = subTotal + shippingFee;

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        tvSubtotal.setText(currencyFormat.format(subTotal));
        tvShippingFee.setText(currencyFormat.format(shippingFee));
        tvTotal.setText(currencyFormat.format(currentTotal));

        checkoutButton.setEnabled(!isEmpty);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            checkoutButton.setBackgroundColor(
                    getResources().getColor(!isEmpty ? R.color.premium_accent : R.color.grey_400, getTheme()));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
