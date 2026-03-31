package com.example.myapplication.UI.Home;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.example.myapplication.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.myapplication.UI.Cart.CartActivity;
import com.example.myapplication.UI.Detail.ProductDetailActivity;
import com.example.myapplication.UI.support.ChatbotActivity;
import com.example.myapplication.UI.profile.ProfileActivity;
import com.example.myapplication.UI.settings.SettingsActivity;
import com.example.myapplication.UI.support.SupportActivity;
import com.example.myapplication.adapter.CategoryAdapter;
import com.example.myapplication.adapter.ProductGridAdapter;
import com.example.myapplication.adapter.SearchSuggestionAdapter;
import com.example.myapplication.adapter.SliderAdapter;
import com.example.myapplication.manager.LanguageHelper;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.manager.FavoritesManager;
import com.example.myapplication.util.SessionManager;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Category;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.ProductGrid;
import com.example.myapplication.model.Slider;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.relex.circleindicator.CircleIndicator3;

public class HomeActivity extends AppCompatActivity
        implements ProductGridAdapter.OnProductGridClickListener, SliderAdapter.OnSliderClickListener,
        SearchSuggestionAdapter.OnSuggestionClickListener, CategoryAdapter.OnCategoryClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private ViewPager2 bannerViewPager;
    private RecyclerView productRecyclerView;
    private RecyclerView categoryRecyclerView;
    private RecyclerView searchSuggestionsRecyclerView;
    private CircleIndicator3 indicator;
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private SearchView searchView;
    private ShimmerFrameLayout shimmerViewContainer;
    private NestedScrollView mainContentScroll;
    private TextView tvWelcomeMessage;
    private BottomNavigationView bottomNavigationView;
    private FloatingActionButton fabCart;
    private String currentCategoryId = "0";
    private FavoritesManager favoritesManager;
    private SessionManager sessionManager;

    private ImageView ivCart;
    private ImageView ivNotification;
    private TextView tvCartCount;

    private SliderAdapter sliderAdapter;
    private ProductGridAdapter productGridAdapter;
    private SearchSuggestionAdapter searchSuggestionAdapter;
    private com.example.myapplication.adapter.CategoryHomeAdapter categoryAdapter;

    private List<Slider> sliderList;
    private List<ProductGrid> allProductsList;
    private List<String> suggestionList;
    private List<Category> categoryList;

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            if (sliderList != null && !sliderList.isEmpty()
                    && bannerViewPager.getCurrentItem() == sliderList.size() - 1) {
                bannerViewPager.setCurrentItem(0);
            } else {
                bannerViewPager.setCurrentItem(bannerViewPager.getCurrentItem() + 1);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageHelper.loadLocale(this);
        setContentView(R.layout.activity_home);

        // Initialize managers
        favoritesManager = FavoritesManager.getInstance(this);
        sessionManager = new SessionManager(this);

        // Init Cart Manager for persistence
        CartManager.getInstance().init(this);
        // Ensure user ID is set if logged in
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            CartManager.getInstance().updateUserId(currentUser.getUid());
        }

        initViews();
        initEvents();
        setupToolbar();
        setupSwipeToRefresh();
        setupBottomNavigation();
        setupSearch();
        loadAndDisplayData();

        CartManager.getInstance().getCartItemsLiveData().observe(this, cartItems -> {
            int cartCount = cartItems != null ? cartItems.size() : 0;
            updateCartCount(cartCount);
        });
    }

    private void initViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        bannerViewPager = findViewById(R.id.bannerViewPager);
        indicator = findViewById(R.id.banner_indicator);
        tvWelcomeMessage = findViewById(R.id.tv_welcome_message);
        productRecyclerView = findViewById(R.id.recycler_view_products);
        categoryRecyclerView = findViewById(R.id.recycler_view_categories);
        searchView = findViewById(R.id.search_view);
        bottomNavigationView = findViewById(R.id.bottom_navigation_bar);
        fabCart = findViewById(R.id.fab_cart);
        searchSuggestionsRecyclerView = findViewById(R.id.recycler_view_search_suggestions);
        shimmerViewContainer = findViewById(R.id.shimmer_view_container);
        mainContentScroll = findViewById(R.id.main_content_scroll);

        ivCart = findViewById(R.id.iv_cart);
        ivNotification = findViewById(R.id.iv_notification);
        tvCartCount = findViewById(R.id.tv_cart_count);

        productRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        productRecyclerView.setHasFixedSize(true);
        productRecyclerView.setNestedScrollingEnabled(false);

        allProductsList = new ArrayList<>();
        productGridAdapter = new ProductGridAdapter(new ArrayList<>(), this);
        productRecyclerView.setAdapter(productGridAdapter);

        // Setup Category Adapter
        categoryList = new ArrayList<>();
        // Use standard Linear layout for horizontal scroll
        categoryRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this,
                androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));

        categoryAdapter = new com.example.myapplication.adapter.CategoryHomeAdapter(this, categoryList,
                this::onCategoryClick);
        categoryRecyclerView.setAdapter(categoryAdapter);

        fabCart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));

        FloatingActionButton fabChat = findViewById(R.id.fab_chat);
        fabChat.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChatbotActivity.class)));

        FloatingActionButton fabAI = findViewById(R.id.fab_ai_recommend);
        fabAI.setOnClickListener(v -> getAIRecommendation());

        fabAI.postDelayed(() -> {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(fabAI, "rotation", 0f, 15f, -15f, 10f, -10f, 0f);
            rotate.setDuration(1200);
            rotate.setRepeatMode(android.animation.ValueAnimator.RESTART);
            rotate.setRepeatCount(1);
            rotate.start();
        }, 2000);
    }

    private void initEvents() {
        if (ivCart != null) {
            ivCart.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, CartActivity.class)));
        }

        if (ivNotification != null) {
            ivNotification.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this,
                        com.example.myapplication.UI.notification.NotificationActivity.class);
                startActivity(intent);
            });
        }

        viewNotificationBadge = findViewById(R.id.view_notification_badge);
        checkUnreadNotifications();

        TextView tvViewAll = findViewById(R.id.tv_view_all_popular);
        if (tvViewAll != null) {
            tvViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this,
                        com.example.myapplication.UI.product.ProductListActivity.class);
                startActivity(intent);
            });
        }
    }

    private void setupToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void loadAndDisplayData() {
        shimmerViewContainer.startShimmer();
        shimmerViewContainer.setVisibility(View.VISIBLE);
        mainContentScroll.setVisibility(View.GONE);

        setupSlider();
        setupCategoryRecycler();

        // Load data from Firebase instead of Mock
        fetchPopularProductsFromFirebase();

        // Check if we need to seed initial data (only if empty)
        checkAndUploadMockDataIfEmpty();

        swipeRefreshLayout.setRefreshing(false);
    }

    private void fetchPopularProductsFromFirebase() {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        // Use addValueEventListener for realtime updates (ratings, etc.)
        productsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProductsList.clear();
                List<Product> tempProductList = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        product.setId(data.getKey());
                        tempProductList.add(product);
                    }
                }

                // Sort by Sold Count (Descending) - Display most popular first
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    tempProductList.sort((p1, p2) -> Integer.compare(p2.getSoldCount(), p1.getSoldCount()));
                } else {
                    // Fallback for older Android versions
                    java.util.Collections.sort(tempProductList, new java.util.Comparator<Product>() {
                        @Override
                        public int compare(Product p1, Product p2) {
                            return Integer.compare(p2.getSoldCount(), p1.getSoldCount());
                        }
                    });
                }

                // Load ALL products (not just top 10) to support category filtering
                for (Product product : tempProductList) {

                    // Map Product -> ProductGrid
                    ProductGrid item = new ProductGrid(
                            product.getId(),
                            product.getName(),
                            product.getDescription(),
                            product.getPrice(),
                            product.getImageUrl(), // Use URL
                            product.getCategoryId() // Use Category ID
                    );

                    // Ensure categoryId is set for filtering
                    String categoryId = product.getCategoryId();
                    if (categoryId == null || categoryId.isEmpty()) {
                        // Fallback 1: infer from categoryName if categoryId is missing
                        categoryId = getCategoryIdByName(product.getCategoryName());

                        // Fallback 2: if still "0", infer from product Name
                        if ("0".equals(categoryId)) {
                            categoryId = getCategoryIdByName(product.getName());
                        }

                        android.util.Log.d("HomeActivity", "Product " + product.getName() +
                                " missing categoryId, inferred: " + categoryId);
                    }
                    item.setCategoryId(categoryId);

                    android.util.Log.d("HomeActivity", "Loaded product: " + product.getName() +
                            ", categoryId: " + categoryId + ", category: " + getCategoryKeyById(categoryId));

                    // Map other fields if necessary
                    item.setCategory(getCategoryKeyById(categoryId));

                    // Handle legacy or mixed data types for imageResId if needed
                    if (product.getImageResId() != 0) {
                        item.setImageResId(product.getImageResId());
                    }

                    allProductsList.add(item);
                }

                // Load saved favorites and apply to products
                String userId = sessionManager.getUserId();
                if (userId != null) {
                    Set<String> favoriteIds = favoritesManager.getAllFavorites(userId);
                    for (ProductGrid product : allProductsList) {
                        if (product.getId() != null && favoriteIds.contains(product.getId())) {
                            product.setFavorite(true);
                        }
                    }
                }

                android.util.Log.d("HomeActivity", "Total products loaded: " + allProductsList.size());

                // Apply "All" category filter to show top 12 products
                filterProductsByCategory("0");

                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
                mainContentScroll.setVisibility(View.VISIBLE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                shimmerViewContainer.stopShimmer();
                shimmerViewContainer.setVisibility(View.GONE);
                Toast.makeText(HomeActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMockProducts() {
        allProductsList.clear();

        ProductGrid p1 = new ProductGrid("p1", "Cà phê Latte", "Hương vị Ý", 35000, R.drawable.ic_coffee, "Cà phê");
        p1.setCategoryId("1");
        allProductsList.add(p1);

        ProductGrid p2 = new ProductGrid("p2", "Cà phê Đen", "Đậm đà tỉnh táo", 25000, R.drawable.ic_coffee_1,
                "Cà phê");
        p2.setCategoryId("1");
        allProductsList.add(p2);

        ProductGrid p3 = new ProductGrid("p3", "Bạc Xỉu", "Ngọt ngào sữa đặc", 32000, R.drawable.ic_coffee_2, "Cà phê");
        p3.setCategoryId("1");
        allProductsList.add(p3);

        ProductGrid p4 = new ProductGrid("p4", "Cà phê Sữa Đá", "Chuẩn vị Việt", 29000, R.drawable.ic_coffee_3,
                "Cà phê");
        p4.setCategoryId("1");
        allProductsList.add(p4);

        ProductGrid p5 = new ProductGrid("p5", "Trà Sữa Trân Châu", "Dai ngon sần sật", 45000, R.drawable.ic_milktea,
                "Trà");
        p5.setCategoryId("2");
        allProductsList.add(p5);

        ProductGrid p6 = new ProductGrid("p6", "Trà Thảo Mộc", "Thanh lọc cơ thể", 30000, R.drawable.ic_tea_image,
                "Trà");
        p6.setCategoryId("2");
        allProductsList.add(p6);

        ProductGrid p7 = new ProductGrid("p7", "Trà Vải Nhiệt Đới", "Giải nhiệt mùa hè", 39000,
                R.drawable.ic_tea_image_1, "Trà");
        p7.setCategoryId("2");
        allProductsList.add(p7);

        ProductGrid p8 = new ProductGrid("p8", "Sinh tố Dâu Tây", "Vitamin C tự nhiên", 40000, R.drawable.ic_strawberry,
                "Sinh tố");
        p8.setCategoryId("3");
        allProductsList.add(p8);

        ProductGrid p9 = new ProductGrid("p9", "Sinh tố Bơ", "Béo ngậy bổ dưỡng", 42000, R.drawable.ic_placeholder5,
                "Sinh tố");
        p9.setCategoryId("3");
        allProductsList.add(p9);

        ProductGrid p10 = new ProductGrid("p10", "Nước ép Cam", "Tăng sức đề kháng", 35000, R.drawable.ic_juice,
                "Sinh tố");
        p10.setCategoryId("3");
        allProductsList.add(p10);

        ProductGrid p11 = new ProductGrid("p11", "Bánh Kem Dâu", "Ngọt ngào tình yêu", 45000, R.drawable.ic_cake,
                "Bánh ngọt");
        p11.setCategoryId("4");
        allProductsList.add(p11);

        ProductGrid p12 = new ProductGrid("p12", "Bánh Tiramisu", "Vị cafe cacao", 48000, R.drawable.ic_cake_1,
                "Bánh ngọt");
        p12.setCategoryId("4");
        allProductsList.add(p12);

        ProductGrid p13 = new ProductGrid("p13", "Bánh Croissant", "Vỏ giòn tan", 28000, R.drawable.ic_cake_2,
                "Bánh ngọt");
        p13.setCategoryId("4");
        allProductsList.add(p13);

        productGridAdapter.setProducts(allProductsList);
        shimmerViewContainer.stopShimmer();
        shimmerViewContainer.setVisibility(View.GONE);
        mainContentScroll.setVisibility(View.VISIBLE);
    }

    private void checkAndUploadMockDataIfEmpty() {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    loadMockProducts(); // Load to list first
                    // Then upload
                    for (ProductGrid item : allProductsList) {
                        String key = item.getId();
                        if (key != null) {
                            // Map ProductGrid -> Product for Firebase
                            Product product = new Product(
                                    item.getName(),
                                    item.getDescription(),
                                    item.getPrice(),
                                    item.getImageResId());
                            product.setCategoryId(item.getCategoryId());
                            product.setImageUrl(""); // Default empty URL for local resource

                            productsRef.child(key).setValue(product);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadAndDisplayData);
        swipeRefreshLayout.setColorSchemeResources(R.color.brown_500, R.color.brown_700, R.color.orange_500);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home)
                return true;
            if (itemId == R.id.nav_menu) {
                mainContentScroll.smoothScrollTo(0, productRecyclerView.getTop());
                return true;
            }
            if (itemId == R.id.nav_placeholder)
                return false;
            if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }

            if (itemId == R.id.nav_support) {
                startActivity(new Intent(this, SupportActivity.class));
                return true;
            }
            return false;
        });
    }

    private void setupSearch() {
        searchSuggestionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        suggestionList = new ArrayList<>();
        searchSuggestionAdapter = new SearchSuggestionAdapter(suggestionList, this);
        searchSuggestionsRecyclerView.setAdapter(searchSuggestionAdapter);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() > 0) {
                    searchSuggestionsRecyclerView.setVisibility(View.VISIBLE);
                    filterSuggestions(newText);
                } else {
                    searchSuggestionsRecyclerView.setVisibility(View.GONE);
                }
                return true;
            }
        });
    }

    private void filterSuggestions(String query) {
        List<String> filtered = new ArrayList<>();
        for (ProductGrid p : allProductsList) {
            String name = p.getName();
            if (name != null && name.toLowerCase().contains(query.toLowerCase())) {
                boolean exists = false;
                for (String s : filtered) {
                    if (s.equals(name)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    filtered.add(name);
                }
            }
            if (filtered.size() >= 5)
                break;
        }
        searchSuggestionAdapter.updateSuggestions(filtered);
    }

    private void performSearch(String query) {
        searchSuggestionsRecyclerView.setVisibility(View.GONE);
        List<ProductGrid> result = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            // If empty query, restore to current category view
            filterProductsByCategory(currentCategoryId);
            return;
        }

        for (ProductGrid p : allProductsList) {
            if (p != null && p.getName() != null &&
                    p.getName().toLowerCase().contains(query.toLowerCase())) {
                result.add(p);
            }
        }

        // Apply sorting: favorites to top
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result.sort((p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0;
            });
        } else {
            java.util.Collections.sort(result, (p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0;
            });
        }

        productGridAdapter.setProducts(result);

        // Safely scroll to products section
        if (mainContentScroll != null && productRecyclerView != null) {
            mainContentScroll.post(() -> {
                mainContentScroll.smoothScrollTo(0, productRecyclerView.getTop());
            });
        }
    }

    private void setupSlider() {
        sliderList = new ArrayList<>();
        sliderList.add(new Slider(R.drawable.banner_khuyenmai1, "Khuyến mãi 1"));
        sliderList.add(new Slider(R.drawable.banner_khuyenmai2, "Khuyến mãi 2"));
        sliderList.add(new Slider(R.drawable.banner_khuyenmai3, "Khuyến mãi 3"));
        sliderList.add(new Slider(R.drawable.banner_khuyenmai5, "Khuyến mãi 5"));

        sliderAdapter = new SliderAdapter(this, sliderList, this);
        bannerViewPager.setAdapter(sliderAdapter);
        indicator.setViewPager(bannerViewPager);

        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
            }
        });
    }

    private void setupCategoryRecycler() {
        categoryList = new ArrayList<>();
        categoryList.add(new Category("0", getString(R.string.cat_all), "ic_category_all"));
        categoryList.add(new Category("1", getString(R.string.cat_coffee), "ic_category_coffee"));
        categoryList.add(new Category("2", getString(R.string.cat_tea), "ic_category_tea"));
        categoryList.add(new Category("3", getString(R.string.cat_smoothie), "ic_category_smoothie"));
        categoryList.add(new Category("4", getString(R.string.cat_pastry), "ic_category_pastry"));

        if (categoryAdapter != null) {
            categoryAdapter.setCategories(categoryList);
        }
    }

    private String getCategoryKeyById(String id) {
        if (id == null)
            return "Tất cả";
        switch (id) {
            case "1":
                return "Cà phê";
            case "2":
                return "Trà";
            case "3":
                return "Sinh tố";
            case "4":
                return "Bánh ngọt";
            default:
                return "Tất cả";
        }
    }

    private String getCategoryIdByName(String name) {
        if (name == null)
            return "0";

        // Use contains() to handle names like "Trà/Trà sữa", "Sinh tố / Nước ép"
        String lowerName = name.toLowerCase();

        if (lowerName.contains("cà phê") || lowerName.contains("coffee") ||
                lowerName.contains("bạc xỉu") || lowerName.contains("latte") ||
                lowerName.contains("cappuccino") || lowerName.contains("mocha") ||
                lowerName.contains("espresso") || lowerName.contains("americano")) {
            return "1";
        } else if (lowerName.contains("trà") || lowerName.contains("tea") ||
                lowerName.contains("matcha") || lowerName.contains("ô long")) {
            return "2";
        } else if (lowerName.contains("sinh tố") || lowerName.contains("smoothie") ||
                lowerName.contains("nước ép") || lowerName.contains("juice") ||
                lowerName.contains("đá xay")) {
            return "3";
        } else if (lowerName.contains("bánh") || lowerName.contains("pastry") ||
                lowerName.contains("cake") || lowerName.contains("donut")) {
            return "4";
        } else {
            return "0"; // Default to "All" for unknown categories
        }
    }

    private void filterProductsByCategory(String categoryId) {
        this.currentCategoryId = categoryId;
        android.util.Log.d("HomeActivity", "Filtering by categoryId: " + categoryId);
        List<ProductGrid> filteredList = new ArrayList<>();
        // "0" is the ID for "All" / "Tất cả"
        if (categoryId == null || categoryId.equals("0")) {
            // For "All" category: Show top popular products
            filteredList.addAll(allProductsList);
        } else {
            // For specific categories: Show ALL products in that category
            for (ProductGrid p : allProductsList) {
                if (categoryId.equals(p.getCategoryId())) {
                    filteredList.add(p);
                }
            }
        }

        // --- SORTING LOGIC: Favorites first ---
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            filteredList.sort((p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0; // Keep original order (popular) for others
            });
        } else {
            java.util.Collections.sort(filteredList, (p1, p2) -> {
                if (p1.isFavorite() && !p2.isFavorite())
                    return -1;
                if (!p1.isFavorite() && p2.isFavorite())
                    return 1;
                return 0;
            });
        }

        // Limit "All" items to top 12 if needed, but maybe show more if favorites
        // exist?
        // Let's stick to showing top 12 for "All" but allow more if favorited?
        // User said "move to top", so I'll show top 12 for All as before.
        if (categoryId == null || categoryId.equals("0")) {
            int limit = Math.min(filteredList.size(), 12);
            filteredList = new ArrayList<>(filteredList.subList(0, limit));
        }

        productGridAdapter.setProducts(filteredList);
    }

    // --- Implementation of Interface Methods ---

    @Override
    public void onCategoryClick(Category category) {
        filterProductsByCategory(category.getId());
    }

    @Override
    public void onSuggestionClick(String suggestion) {
        searchView.setQuery(suggestion, true);
    }

    @Override
    public void onProductClick(ProductGrid product) {
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT, product);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(ProductGrid product, int position) {
        product.setFavorite(!product.isFavorite());

        // Save to persistent storage
        String userId = sessionManager.getUserId();
        if (userId != null && product.getId() != null) {
            if (product.isFavorite()) {
                favoritesManager.addFavorite(userId, product.getId());
            } else {
                favoritesManager.removeFavorite(userId, product.getId());
            }
        }

        // Update the master list so state persists across category changes
        for (ProductGrid p : allProductsList) {
            if (p.getId().equals(product.getId())) {
                p.setFavorite(product.isFavorite());
                break;
            }
        }

        String msg = product.isFavorite() ? getString(R.string.msg_added_to_favorites, product.getName())
                : "Đã bỏ yêu thích " + product.getName();

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        // Find current category to maintain filter
        // Simple way: re-apply current logic but with sorting
        // We need to track current category
        // Delay re-sorting for 350ms so user can see the heart animation play
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            filterProductsByCategory(currentCategoryId);
        }, 350);
    }

    public void onAddToCartClick(ProductGrid product) {
        try {
            double price = parsePrice(String.valueOf(product.getPrice()));

            CartItem item = new CartItem(
                    product.getId(),
                    product.getName(),
                    price,
                    1, // quantity
                    null, // size
                    null, // sugar
                    null, // ice
                    product.getCategory(),
                    product.getImageUrl());
            item.setImageResId(product.getImageResId());

            CartManager.getInstance().addToCart(item);
            showAddToCartSnackBar(product.getName());

        } catch (NumberFormatException e) {
            Toast.makeText(this, getString(R.string.msg_invalid_price), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onAddToCartClick(ProductGrid product, View view) {
        if (view != null && fabCart != null) {
            runAddToCartAnimation(view, fabCart);
        }
        onAddToCartClick(product); // Gọi lại hàm đã có
    }

    private void runAddToCartAnimation(View startView, View targetView) {
        // Create animation view (copy of item image or icon)
        final ImageView animView = new ImageView(this);
        animView.setImageResource(R.drawable.ic_cart);
        animView.setColorFilter(getColor(R.color.premium_accent));

        int[] startPos = new int[2];
        startView.getLocationOnScreen(startPos);
        int[] targetPos = new int[2];
        targetView.getLocationOnScreen(targetPos);

        // Standardize coordinates relative to Root View
        android.view.ViewGroup root = (android.view.ViewGroup) getWindow().getDecorView();
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(60, 60);
        root.addView(animView, params);

        // Start Position variables
        float startX = startPos[0];
        float startY = startPos[1];
        float targetX = targetPos[0] + (targetView.getWidth() / 2f) - 30; // Center offset
        float targetY = targetPos[1] + (targetView.getHeight() / 2f) - 30;

        // Set initial position
        animView.setX(startX);
        animView.setY(startY);

        // Path for Parabolic Curve (Jump UP then Fly DOWN)
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(startX, startY);

        // Control Point: Horizontal center, but shifted UP (Y - 300) to create "jump"
        float controlX = (startX + targetX) / 2;
        float controlY = Math.min(startY, targetY) - 500; // Jump height (can adjust)

        path.quadTo(controlX, controlY, targetX, targetY);

        // Animator for Path
        android.animation.ObjectAnimator pathAnimator = android.animation.ObjectAnimator.ofFloat(animView, View.X,
                View.Y, path);
        pathAnimator.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

        // Scale & Alpha Animations
        android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(animView, View.SCALE_X, 1f,
                0.5f);
        android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(animView, View.SCALE_Y, 1f,
                0.5f);
        android.animation.ObjectAnimator alpha = android.animation.ObjectAnimator.ofFloat(animView, View.ALPHA, 1f,
                0.5f);

        // Play Together
        android.animation.AnimatorSet animatorSet = new android.animation.AnimatorSet();
        animatorSet.playTogether(pathAnimator, scaleX, scaleY, alpha);
        animatorSet.setDuration(800);
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                root.removeView(animView);
                // Trigger target view bump/pulse effect here if desired
            }
        });
        animatorSet.start();
    }

    @Override
    public void onSliderClick(Slider slider) {
        Toast.makeText(this, slider.getTitle(), Toast.LENGTH_SHORT).show();
    }

    // --- Helper Methods ---

    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return 0.0;
        }
        try {
            String numericString = priceStr.replaceAll("[^\\d.]", "");
            if (numericString.isEmpty()) {
                return 0.0;
            }
            return Double.parseDouble(numericString);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public void showAddToCartSnackBar(String productName) {
        View rootView = findViewById(android.R.id.content);
        String message = getString(R.string.msg_added_to_cart, productName);
        if (rootView != null) {
            Snackbar snackbar = Snackbar.make(rootView, message,
                    Snackbar.LENGTH_SHORT);

            // Anchor to Bottom Navigation to avoid overlapping
            if (bottomNavigationView != null) {
                snackbar.setAnchorView(bottomNavigationView);
            }

            snackbar.show();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCartCount(int count) {
        if (tvCartCount != null) {
            if (count > 0) {
                tvCartCount.setText(String.valueOf(count));
                tvCartCount.setVisibility(View.VISIBLE);
            } else {
                tvCartCount.setVisibility(View.GONE);
            }
        }
    }

    // --- AI Recommendation Logic ---

    private void getAIRecommendation() {
        if (allProductsList == null || allProductsList.isEmpty()) {
            Toast.makeText(this, "Đang tải dữ liệu, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "AI đang suy nghĩ...", Toast.LENGTH_SHORT).show();

        // 1. Pick 3 random products
        List<ProductGrid> candidates = new ArrayList<>();
        List<ProductGrid> source = new ArrayList<>(allProductsList);
        java.util.Collections.shuffle(source);
        for (int i = 0; i < Math.min(3, source.size()); i++) {
            candidates.add(source.get(i));
        }

        // 2. Build Prompt
        StringBuilder prompt = new StringBuilder();
        prompt.append(
                "Đóng vai chuyên gia Barista của Aura Cafe. Hãy chọn ra 1 món đồ uống tốt nhất trong danh sách sau đây cho khách hàng vào thời điểm này: ");
        for (ProductGrid p : candidates) {
            prompt.append(p.getName()).append(", ");
        }
        prompt.append("\n");
        prompt.append(
                "Yêu cầu: Trả về kết quả dưới dạng JSON KHÔNG CÓ MARKDOWN với cấu trúc: {\"product_name\": \"Tên món\", \"reason\": \"Lý do ngắn gọn, hấp dẫn (max 20 từ)\"}.\n");
        prompt.append("Lý do phải phù hợp với thời tiết hoặc tâm trạng tích cực. Chỉ trả về JSON thuần.");

        // 3. Init Retrofit & Call API (Quick setup similar to ChatbotActivity)
        retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
                .build();
        com.example.myapplication.api.GeminiApiService service = retrofit
                .create(com.example.myapplication.api.GeminiApiService.class);

        String apiKey = com.example.myapplication.BuildConfig.GEMINI_API_KEY;
        if (apiKey == null) {
            Toast.makeText(this, "Chưa cấu hình API Key!", Toast.LENGTH_SHORT).show();
            return;
        }

        com.example.myapplication.model.gemini.GeminiRequest request = new com.example.myapplication.model.gemini.GeminiRequest(
                prompt.toString());
        service.generateContent(apiKey, request)
                .enqueue(new retrofit2.Callback<com.example.myapplication.model.gemini.GeminiResponse>() {
                    @Override
                    public void onResponse(retrofit2.Call<com.example.myapplication.model.gemini.GeminiResponse> call,
                            retrofit2.Response<com.example.myapplication.model.gemini.GeminiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String jsonText = response.body().getText();
                            if (jsonText != null) {
                                // Cleanup Markdown code blocks if present
                                jsonText = jsonText.replace("```json", "").replace("```", "").trim();
                                try {
                                    org.json.JSONObject jsonObject = new org.json.JSONObject(jsonText);
                                    String recName = jsonObject.getString("product_name");
                                    String recReason = jsonObject.getString("reason");

                                    // Find the product object
                                    ProductGrid recProduct = null;
                                    for (ProductGrid p : allProductsList) {
                                        if (p.getName().equalsIgnoreCase(recName)) {
                                            recProduct = p;
                                            break;
                                        }
                                    }
                                    // Fallback: Use the first candidate if name match fails (AI sometimes alters
                                    // name slightly)
                                    if (recProduct == null)
                                        recProduct = candidates.get(0);

                                    showRecommendationDialog(recProduct, recReason);

                                } catch (Exception e) {
                                    android.util.Log.e("HomeActivity", "JSON Parse Error: " + e.getMessage());
                                    // Fallback manual parse or fuzzy match
                                    showRecommendationDialog(candidates.get(0),
                                            "Món ngon tuyệt vời cho ngày hôm nay! (AI gợi ý)");
                                }
                            }
                        } else {
                            Toast.makeText(HomeActivity.this, "AI đang bận, thử lại sau!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<com.example.myapplication.model.gemini.GeminiResponse> call,
                            Throwable t) {
                        Toast.makeText(HomeActivity.this, "Lỗi kết nối AI!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRecommendationDialog(ProductGrid product, String reason) {
        if (isFinishing() || product == null)
            return;

        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_ai_recommendation);
        dialog.getWindow()
                .setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.getWindow().setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView ivProduct = dialog.findViewById(R.id.iv_ai_product);
        TextView tvName = dialog.findViewById(R.id.tv_ai_product_name);
        TextView tvPrice = dialog.findViewById(R.id.tv_ai_product_price);
        TextView tvReason = dialog.findViewById(R.id.tv_ai_reason);
        TextView tvClose = dialog.findViewById(R.id.tv_close);
        View btnAdd = dialog.findViewById(R.id.btn_ai_add_to_cart);

        // Bind Data
        tvName.setText(product.getName());
        java.text.NumberFormat format = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN"));
        tvPrice.setText(format.format(product.getPrice()));
        tvReason.setText(reason);

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            com.bumptech.glide.Glide.with(this).load(product.getImageUrl()).into(ivProduct);
        } else {
            ivProduct.setImageResource(product.getImageResId() != 0 ? product.getImageResId() : R.drawable.ic_coffee);
        }

        tvClose.setOnClickListener(v -> dialog.dismiss());
        btnAdd.setOnClickListener(v -> {
            onAddToCartClick(product); // Reuse existing logic
            dialog.dismiss();
        });

        dialog.show();
    }

    private View viewNotificationBadge;

    @Override
    protected void onResume() {
        super.onResume();

        // Fix bottom navigation highlight when returning from other screens
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        checkUnreadNotifications();
    }

    private void checkUnreadNotifications() {
        if (viewNotificationBadge == null)
            return;

        com.example.myapplication.manager.NotificationReadManager readManager = new com.example.myapplication.manager.NotificationReadManager(
                this);

        // 1. Check System/Promo (Static)
        boolean hasUnread = false;
        if (!readManager.isRead("sys_1") && !readManager.isDeleted("sys_1"))
            hasUnread = true;
        if (!readManager.isRead("promo_1") && !readManager.isDeleted("promo_1"))
            hasUnread = true;

        if (hasUnread) {
            viewNotificationBadge.setVisibility(View.VISIBLE);
            return; // Found one, no need to check more
        }

        // 2. Check Orders
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(user.getUid());
            ordersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    boolean unreadFound = false;
                    // Re-check static incase they changed (optimized: just assume false from above)
                    if ((!readManager.isRead("sys_1") && !readManager.isDeleted("sys_1")) ||
                            (!readManager.isRead("promo_1") && !readManager.isDeleted("promo_1"))) {
                        unreadFound = true;
                    } else {
                        for (DataSnapshot data : snapshot.getChildren()) {
                            try {
                                // SAFE PARSING: Wrap in try-catch to prevent crash if data is malformed
                                com.example.myapplication.model.Order order = null;
                                try {
                                    order = data.getValue(com.example.myapplication.model.Order.class);
                                } catch (Exception e) {
                                    // Ignore malformed order nodes
                                    continue;
                                }

                                if (order != null) {
                                    String orderId = order.getOrderId();
                                    // Check if valid status that generates notification
                                    String status = order.getStatus();
                                    boolean isValidStatus = false;
                                    if (status != null) {
                                        switch (status) {
                                            case "Pending":
                                            case "Đang xử lý":
                                            case "Shipping":
                                            case "Đang giao":
                                            case "Completed":
                                            case "Hoàn thành":
                                            case "Cancelled":
                                            case "Đã hủy":
                                                isValidStatus = true;
                                                break;
                                        }
                                    }

                                    if (isValidStatus && orderId != null && !readManager.isRead(orderId)
                                            && !readManager.isDeleted(orderId)) {
                                        unreadFound = true;
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                // General safety catch
                                continue;
                            }
                        }
                    }
                    viewNotificationBadge.setVisibility(unreadFound ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sliderHandler.removeCallbacks(sliderRunnable);
    }

    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (ev.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            android.view.View v = getCurrentFocus();
            if (v instanceof androidx.appcompat.widget.SearchView.SearchAutoComplete) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);

                // Check if touch is on search suggestions RecyclerView
                boolean touchOnSuggestions = false;
                if (searchSuggestionsRecyclerView != null &&
                        searchSuggestionsRecyclerView.getVisibility() == android.view.View.VISIBLE) {
                    android.graphics.Rect suggestionsRect = new android.graphics.Rect();
                    searchSuggestionsRecyclerView.getGlobalVisibleRect(suggestionsRect);
                    if (suggestionsRect.contains((int) ev.getRawX(), (int) ev.getRawY())) {
                        touchOnSuggestions = true;
                    }
                }

                // Only close if touch is outside SearchView AND not on suggestions
                if (!outRect.contains((int) ev.getRawX(), (int) ev.getRawY()) && !touchOnSuggestions) {
                    v.clearFocus();
                    // Close the SearchView
                    if (searchView != null) {
                        searchView.setQuery("", false);
                        searchView.setIconified(true);
                        searchView.clearFocus();
                        // Hide suggestions
                        if (searchSuggestionsRecyclerView != null) {
                            searchSuggestionsRecyclerView.setVisibility(android.view.View.GONE);
                        }
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