package com.example.myapplication.UI.Rating;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.databinding.ActivityAddRatingBinding;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.Rating;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

public class AddRatingActivity extends AppCompatActivity {

    private ActivityAddRatingBinding binding;
    private String productId;
    private String orderId;
    private int currentRating = 0; // Default to 0 (no stars) as per requirement

    private DatabaseReference productRef;
    private DatabaseReference ratingRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddRatingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        productId = getIntent().getStringExtra("PRODUCT_ID");
        orderId = getIntent().getStringExtra("ORDER_ID");

        if (productId == null || orderId == null) {
            Toast.makeText(this, "Thiếu thông tin sản phẩm hoặc đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productRef = FirebaseDatabase.getInstance().getReference("products").child(productId);
        ratingRef = productRef.child("ratings");

        setupToolbar();
        loadProductInfo();
        loadExistingRating();
        setupListeners();
    }

    private void loadExistingRating() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        ratingRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Rating rating = snapshot.getValue(Rating.class);
                if (rating != null) {
                    currentRating = Math.round(rating.getStars());
                    updateStarsUI();
                    binding.etComment.setText(rating.getComment());
                } else {
                    currentRating = 0;
                    updateStarsUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadProductInfo() {
        productRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Product product = snapshot.getValue(Product.class);
                if (product != null) {
                    binding.tvProductName.setText(product.getName());
                    Glide.with(AddRatingActivity.this)
                            .load(product.getImageUrl())
                            .placeholder(R.drawable.coffee_image)
                            .into(binding.ivProductImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void setupListeners() {
        binding.star1.setOnClickListener(v -> {
            currentRating = 1;
            updateStarsUI();
        });
        binding.star2.setOnClickListener(v -> {
            currentRating = 2;
            updateStarsUI();
        });
        binding.star3.setOnClickListener(v -> {
            currentRating = 3;
            updateStarsUI();
        });
        binding.star4.setOnClickListener(v -> {
            currentRating = 4;
            updateStarsUI();
        });
        binding.star5.setOnClickListener(v -> {
            currentRating = 5;
            updateStarsUI();
        });

        binding.btnSubmitRating.setOnClickListener(v -> submitRating());

        // Add listeners for suggestion chips
        for (int i = 0; i < binding.chipGroupSuggestions.getChildCount(); i++) {
            Chip chip = (Chip) binding.chipGroupSuggestions.getChildAt(i);
            chip.setOnClickListener(v -> {
                String currentText = binding.etComment.getText().toString();
                String chipText = chip.getText().toString();
                if (currentText.isEmpty()) {
                    binding.etComment.setText(chipText);
                } else {
                    binding.etComment.setText(currentText + ". " + chipText);
                }
            });
        }
    }

    private void updateStarsUI() {
        binding.star1.setImageResource(
                currentRating >= 1 ? R.drawable.ic_star_filled_gold : R.drawable.ic_star_outline_gold);
        binding.star2.setImageResource(
                currentRating >= 2 ? R.drawable.ic_star_filled_gold : R.drawable.ic_star_outline_gold);
        binding.star3.setImageResource(
                currentRating >= 3 ? R.drawable.ic_star_filled_gold : R.drawable.ic_star_outline_gold);
        binding.star4.setImageResource(
                currentRating >= 4 ? R.drawable.ic_star_filled_gold : R.drawable.ic_star_outline_gold);
        binding.star5.setImageResource(
                currentRating >= 5 ? R.drawable.ic_star_filled_gold : R.drawable.ic_star_outline_gold);
    }

    private void submitRating() {
        float ratingValue = currentRating;
        String comment = binding.etComment.getText().toString().trim();

        if (ratingValue == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng viết bình luận đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        String userId = currentUser.getUid();
        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người dùng ẩn danh";
        long timestamp = System.currentTimeMillis();

        // Use userId as the ratingId to allow only one rating per user per product
        Rating rating = new Rating(userId, userId, userName, ratingValue, comment, timestamp);

        // 1. Load the old rating first to calculate the delta
        ratingRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Rating oldRating = snapshot.getValue(Rating.class);
                float oldStars = (oldRating != null) ? oldRating.getStars() : 0;

                // 2. Save the new rating
                ratingRef.child(userId).setValue(rating).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 3. Update the product's average rating using a transaction on summary node
                        updateProductSummaryAndAverage(oldStars, ratingValue);
                    } else {
                        setLoading(false);
                        Toast.makeText(AddRatingActivity.this, "Gửi đánh giá thất bại", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                setLoading(false);
            }
        });
    }

    private void updateProductSummaryAndAverage(float oldStars, float newStars) {
        DatabaseReference summaryRef = productRef.child("ratingSummary");
        summaryRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData mutableData) {
                java.util.Map<String, Object> summary = (java.util.Map<String, Object>) mutableData.getValue();
                if (summary == null) {
                    summary = new java.util.HashMap<>();
                    summary.put("totalStars", 0.0);
                    summary.put("count", 0L);
                }

                double totalStars = summary.get("totalStars") instanceof Double ? (Double) summary.get("totalStars")
                        : ((Long) summary.get("totalStars")).doubleValue();
                long count = (Long) summary.get("count");

                if (oldStars == 0) {
                    // New rating
                    count++;
                    totalStars += newStars;
                } else {
                    // Update rating
                    totalStars += (newStars - oldStars);
                }

                summary.put("totalStars", totalStars);
                summary.put("count", count);

                mutableData.setValue(summary);
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed,
                    @Nullable DataSnapshot currentData) {
                if (committed && currentData != null) {
                    java.util.Map<String, Object> summary = (java.util.Map<String, Object>) currentData.getValue();
                    double totalStars = summary.get("totalStars") instanceof Double ? (Double) summary.get("totalStars")
                            : ((Long) summary.get("totalStars")).doubleValue();
                    long count = (Long) summary.get("count");
                    double avg = count > 0 ? totalStars / count : 0;

                    // Update main product nodes for easy display in lists
                    productRef.child("rating").setValue(avg);
                    productRef.child("ratingCount").setValue(count).addOnCompleteListener(t -> {
                        setLoading(false);
                        Toast.makeText(AddRatingActivity.this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();

                        // Navigate to HomeActivity
                        android.content.Intent intent = new android.content.Intent(AddRatingActivity.this,
                                com.example.myapplication.UI.Home.HomeActivity.class);
                        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    });
                } else {
                    setLoading(false);
                    Toast.makeText(AddRatingActivity.this, "Lỗi cập nhật số liệu", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnSubmitRating.setEnabled(!isLoading);
    }
}
