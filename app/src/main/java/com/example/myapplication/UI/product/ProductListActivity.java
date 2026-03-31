package com.example.myapplication.UI.product;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.myapplication.R;
import com.example.myapplication.UI.Detail.ProductDetailActivity;
import com.example.myapplication.adapter.ProductAdapter;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.manager.FavoritesManager;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Product;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProductListActivity extends AppCompatActivity implements ProductAdapter.OnProductClickListener {

    private RecyclerView recyclerView;
    private ProductAdapter adapter;
    private List<Product> displayedProductList;
    private List<Product> allProductsFromCategory;
    private SearchView searchView;
    private TextView tvToolbarTitle;
    private FavoritesManager favoritesManager;
    private SessionManager sessionManager;

    public static final String EXTRA_PRODUCT_LIST = "EXTRA_PRODUCT_LIST";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        // Initialize managers
        favoritesManager = FavoritesManager.getInstance(this);
        sessionManager = new SessionManager(this);

        initViews();
        setupRecyclerView();

        String categoryId = getIntent().getStringExtra("categoryId");
        String categoryName = getIntent().getStringExtra("categoryName");
        tvToolbarTitle.setText(categoryName != null ? categoryName : "Sản phẩm");

        // Check if a product list was passed from HomeActivity
        if (getIntent().hasExtra(EXTRA_PRODUCT_LIST)) {
            allProductsFromCategory = (List<Product>) getIntent().getSerializableExtra(EXTRA_PRODUCT_LIST);
            filter("");
        } else {
            fetchProducts(categoryId);
        }

        setupSearchView();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_product_list);
        searchView = findViewById(R.id.searchView);
        tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        findViewById(R.id.iv_back_button).setOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        displayedProductList = new ArrayList<>();
        allProductsFromCategory = new ArrayList<>();
        adapter = new ProductAdapter(displayedProductList, this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
    }

    private void fetchProducts(String categoryId) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        // Change to addValueEventListener for realtime updates (ratings, etc.)
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                allProductsFromCategory.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Product product = snapshot.getValue(Product.class);
                    if (product != null) {
                        if (categoryId == null || categoryId.equalsIgnoreCase("all")
                                || categoryId.equalsIgnoreCase(product.getCategoryId())) {
                            product.setId(snapshot.getKey());
                            allProductsFromCategory.add(product);
                        }
                    }
                }
                // Re-apply current filter
                String currentQuery = searchView.getQuery().toString();
                filter(currentQuery);

                // Load saved favorites
                loadFavorites();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ProductListActivity.this, "Lỗi tải sản phẩm", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });
    }

    private void filter(String text) {
        displayedProductList.clear();

        // Null safety check
        if (text == null) {
            text = "";
        }

        if (text.isEmpty()) {
            displayedProductList.addAll(allProductsFromCategory);
        } else {
            for (Product item : allProductsFromCategory) {
                // Add null checks to prevent crash
                if (item != null && item.getName() != null &&
                        item.getName().toLowerCase().contains(text.toLowerCase())) {
                    displayedProductList.add(item);
                }
            }
        }

        // Sort favorites to the top
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            displayedProductList.sort((p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0; // Keep original order for others
            });
        } else {
            java.util.Collections.sort(displayedProductList, (p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0;
            });
        }

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onProductClick(Product product, ImageView sharedImageView) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT, product);
        startActivity(intent);
    }

    @Override
    public void onAddToCartClick(Product product) {
        try {
            CartItem item = new CartItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    1, // quantity
                    null, // size
                    null, // sugar
                    null, // ice
                    product.getCategoryId(), // Use categoryId from Product
                    product.getImageUrl());

            // Check if imageResId is valid and set it if imageUrl is empty or local
            if (product.getImageResId() != 0) {
                item.setImageResId(product.getImageResId());
            }

            CartManager.getInstance().addToCart(item);
            Toast.makeText(this, "Đã thêm " + product.getName() + " vào giỏ hàng", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi thêm vào giỏ hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onFavoriteClick(Product product, boolean isFavorite) {
        // Save to persistent storage
        String userId = sessionManager.getUserId();
        if (userId != null && product.getId() != null) {
            if (isFavorite) {
                favoritesManager.addFavorite(userId, product.getId());
            } else {
                favoritesManager.removeFavorite(userId, product.getId());
            }
        }

        // Update the product in both lists
        for (Product p : allProductsFromCategory) {
            if (p.getId() != null && p.getId().equals(product.getId())) {
                p.setFavorite(isFavorite);
                break;
            }
        }

        for (Product p : displayedProductList) {
            if (p.getId() != null && p.getId().equals(product.getId())) {
                p.setFavorite(isFavorite);
                break;
            }
        }

        // Show appropriate message
        String message = isFavorite
                ? product.getName() + " đã được thêm vào yêu thích"
                : product.getName() + " đã bị xóa khỏi yêu thích";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Delay re-sorting for 350ms so user can see the heart animation play
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            String currentQuery = searchView.getQuery().toString();
            filter(currentQuery);
        }, 350);
    }

    private void loadFavorites() {
        String userId = sessionManager.getUserId();
        if (userId != null) {
            Set<String> favoriteIds = favoritesManager.getAllFavorites(userId);
            for (Product product : allProductsFromCategory) {
                if (product.getId() != null && favoriteIds.contains(product.getId())) {
                    product.setFavorite(true);
                }
            }
            for (Product product : displayedProductList) {
                if (product.getId() != null && favoriteIds.contains(product.getId())) {
                    product.setFavorite(true);
                }
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof androidx.appcompat.widget.SearchView.SearchAutoComplete) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                    v.clearFocus();
                    // Close the SearchView
                    if (searchView != null) {
                        searchView.setQuery("", false);
                        searchView.setIconified(true);
                        searchView.clearFocus();
                    }
                    // Hide keyboard
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(
                            android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
