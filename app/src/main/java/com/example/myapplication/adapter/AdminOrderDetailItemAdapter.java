package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CartItem;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminOrderDetailItemAdapter extends RecyclerView.Adapter<AdminOrderDetailItemAdapter.ViewHolder> {

    private List<CartItem> cartItems;

    public AdminOrderDetailItemAdapter(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_detail_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView ivProductImage;
        private TextView tvProductName, tvProductOptions, tvQuantity, tvProductPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_product_image);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvProductOptions = itemView.findViewById(R.id.tv_product_options);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvProductPrice = itemView.findViewById(R.id.tv_product_price);
        }

        public void bind(CartItem item) {
            tvProductName.setText(item.getProductName());

            // Construct options text if fields are available
            StringBuilder options = new StringBuilder();
            if (item.getProductSize() != null)
                options.append(item.getProductSize());
            if (item.getProductSugar() != null)
                options.append(", ").append(item.getProductSugar());
            if (item.getProductIce() != null)
                options.append(", ").append(item.getProductIce());

            tvProductOptions.setText(options.toString());
            tvQuantity.setText("Số lượng: " + item.getQuantity());

            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvProductPrice.setText(formatter.format(item.getProductPrice() * item.getQuantity()));

            Glide.with(itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.coffee_image)
                    .into(ivProductImage);
        }
    }
}
