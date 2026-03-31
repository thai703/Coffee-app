package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.example.myapplication.R;
import com.example.myapplication.model.ProductGrid;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductGridAdapter extends RecyclerView.Adapter<ProductGridAdapter.ProductGridViewHolder> {

    private List<ProductGrid> productList;
    private final OnProductGridClickListener listener;

    public interface OnProductGridClickListener {
        void onProductClick(ProductGrid product);

        void onAddToCartClick(ProductGrid product, View view);

        void onFavoriteClick(ProductGrid product, int position);
    }

    public ProductGridAdapter(List<ProductGrid> productList, OnProductGridClickListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_grid, parent, false);
        return new ProductGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductGridViewHolder holder, int position) {
        ProductGrid product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList != null ? productList.size() : 0;
    }

    public void setProducts(List<ProductGrid> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    public static class ProductGridViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        ImageView favoriteButton;
        TextView productName;
        TextView productPrice;
        ImageButton addToCartButton;

        public ProductGridViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            favoriteButton = itemView.findViewById(R.id.iv_favorite);
            productName = itemView.findViewById(R.id.product_name);
            productPrice = itemView.findViewById(R.id.product_price);
            addToCartButton = itemView.findViewById(R.id.add_to_cart_button);
        }

        public void bind(final ProductGrid product, final OnProductGridClickListener listener) {
            productName.setText(product.getName());

            // Format price to currency
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(currencyFormat.format(product.getPrice()));

            favoriteButton.setSelected(product.isFavorite());

            // Handle Image Loading (Resource ID vs URL)
            if (product.getImageResId() != 0) {
                Glide.with(itemView.getContext())
                        .load(product.getImageResId())
                        .transform(new CenterCrop(), new RoundedCorners(16))
                        .placeholder(R.drawable.product_image_placeholder)
                        .error(R.drawable.product_image_error)
                        .into(productImage);
            } else if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
                String imageUrl = product.getImageUrl();
                if (imageUrl.startsWith("http")) {
                    Glide.with(itemView.getContext())
                            .load(imageUrl)
                            .transform(new CenterCrop(), new RoundedCorners(16))
                            .placeholder(R.drawable.product_image_placeholder)
                            .error(R.drawable.product_image_error)
                            .into(productImage);
                } else {
                    // Handle drawable name string if necessary
                    int resId = itemView.getContext().getResources().getIdentifier(imageUrl, "drawable",
                            itemView.getContext().getPackageName());
                    if (resId != 0) {
                        productImage.setImageResource(resId);
                    } else {
                        productImage.setImageResource(R.drawable.product_image_placeholder);
                    }
                }
            } else {
                productImage.setImageResource(R.drawable.product_image_placeholder);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onProductClick(product);
                    }
                }
            });

            addToCartButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onAddToCartClick(product, v);
                    }
                }
            });

            favoriteButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        // Animation
                        android.view.animation.Animation pop = android.view.animation.AnimationUtils
                                .loadAnimation(itemView.getContext(), R.anim.heart_pop);
                        favoriteButton.startAnimation(pop);

                        listener.onFavoriteClick(product, position);
                    }
                }
            });
        }
    }
}
