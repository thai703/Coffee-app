package com.example.myapplication.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderDetailAdapter extends RecyclerView.Adapter<OrderDetailAdapter.OrderDetailViewHolder> {

    private final Context context;
    private final List<CartItem> orderItemList;
    private final boolean isOrderCompleted;
    private final OnRateClickListener rateListener;

    public interface OnRateClickListener {
        void onRateClick(String productId);
    }

    public OrderDetailAdapter(Context context, List<CartItem> orderItemList, boolean isOrderCompleted,
            OnRateClickListener rateListener) {
        this.context = context;
        this.orderItemList = orderItemList;
        this.isOrderCompleted = isOrderCompleted;
        this.rateListener = rateListener;
    }

    @NonNull
    @Override
    public OrderDetailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_detail_product, parent, false);
        return new OrderDetailViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderDetailViewHolder holder, int position) {
        CartItem item = orderItemList.get(position);
        if (item == null)
            return;

        holder.bind(item, context, isOrderCompleted, rateListener);

        // Improved image loading logic
        Object imageSource = null;
        int placeholderResId = R.drawable.ic_coffee_placeholder;

        if (item.getCategory() != null) {
            String catLower = item.getCategory().toLowerCase();
            if (catLower.contains("cà phê") || catLower.contains("coffee")) {
                placeholderResId = R.drawable.ic_category_coffee;
            } else if (catLower.contains("trà") || catLower.contains("tea")) {
                placeholderResId = R.drawable.ic_category_tea;
            } else if (catLower.contains("sinh tố") || catLower.contains("smoothie")) {
                placeholderResId = R.drawable.ic_category_smoothie;
            } else if (catLower.contains("bánh") || catLower.contains("pastry") || catLower.contains("cake")) {
                placeholderResId = R.drawable.ic_category_pastry;
            }
        }

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            imageSource = imageUrl;
        } else if (item.getImageResId() != 0) {
            imageSource = item.getImageResId();
        }

        if (imageSource != null) {
            Glide.with(context)
                    .load(imageSource)
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .into(holder.productImage);
        } else {
            holder.productImage.setImageResource(placeholderResId);
        }
    }

    @Override
    public int getItemCount() {
        return orderItemList != null ? orderItemList.size() : 0;
    }

    public static class OrderDetailViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productName, productPrice, quantity, productOptions;
        Button btnRateProduct;

        public OrderDetailViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.iv_product_image);
            productName = itemView.findViewById(R.id.tv_product_name);
            productPrice = itemView.findViewById(R.id.tv_product_price);
            quantity = itemView.findViewById(R.id.tv_quantity);
            productOptions = itemView.findViewById(R.id.tv_product_options);
            btnRateProduct = itemView.findViewById(R.id.btn_rate_product);
        }

        public void bind(CartItem item, Context context, boolean isOrderCompleted, OnRateClickListener rateListener) {
            productName.setText(item.getProductName());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            productPrice.setText(formatter.format(item.getProductPrice()));
            quantity.setText(context.getString(R.string.quantity_display, item.getQuantity()));

            // Set product options (Size, Sugar, Ice)
            StringBuilder options = new StringBuilder();
            if (item.getSize() != null)
                options.append("Size: ").append(item.getSize());
            if (item.getSugar() != null) {
                if (options.length() > 0)
                    options.append(", ");
                options.append(item.getSugar());
            }
            if (item.getIce() != null) {
                if (options.length() > 0)
                    options.append(", ");
                options.append(item.getIce());
            }
            productOptions.setText(options.toString());
            productOptions.setVisibility(options.length() > 0 ? View.VISIBLE : View.GONE);

            if (isOrderCompleted && item.getProductId() != null) {
                btnRateProduct.setVisibility(View.VISIBLE);
                btnRateProduct.setOnClickListener(v -> {
                    if (rateListener != null) {
                        rateListener.onRateClick(item.getProductId());
                    }
                });
            } else {
                btnRateProduct.setVisibility(View.GONE);
            }
        }
    }
}
