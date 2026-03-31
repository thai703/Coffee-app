package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.UI.Cart.CartActivity;
import com.example.myapplication.manager.CartManager;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.Product;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_TEXT = 0;
    private static final int TYPE_PRODUCT = 1;

    private List<Message> messageList;
    private String localUserId;
    private Context context;

    public MessageAdapter(List<Message> messageList, String localUserId) {
        this.messageList = messageList;
        this.localUserId = localUserId;
    }

    public MessageAdapter(List<Message> messageList) {
        this.messageList = messageList;
        this.localUserId = "local_user";
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.isProductMessage()) {
            return TYPE_PRODUCT;
        }
        return TYPE_TEXT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        if (viewType == TYPE_PRODUCT) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message_product, parent, false);
            return new ProductMessageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messageList.get(position);

        if (holder instanceof ProductMessageViewHolder) {
            ProductMessageViewHolder productHolder = (ProductMessageViewHolder) holder;
            Product product = message.getProduct();

            if (product != null) {
                productHolder.tvProductName.setText(product.getName());
                productHolder.tvProductPrice.setText(String.format("%,.0f VND", product.getPrice()));

                // Load ảnh ưu tiên resource ID trước, sau đó đến URL
                Object imageSource;
                if (product.getImageResId() != 0) {
                    imageSource = product.getImageResId();
                } else {
                    imageSource = product.getImageUrl();
                }

                Glide.with(context)
                        .load(imageSource)
                        .placeholder(R.drawable.ic_coffee)
                        .error(R.drawable.ic_coffee)
                        .into(productHolder.ivProductImage);

                productHolder.btnAddToCart.setOnClickListener(v -> {
                    addToCart(product);
                });
            }

        } else if (holder instanceof MessageViewHolder) {
            MessageViewHolder textHolder = (MessageViewHolder) holder;
            textHolder.messageTextView.setText(message.getContent());

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textHolder.messageTextView.getLayoutParams();
            if (message.getSenderId() != null && message.getSenderId().equals(localUserId)) {
                params.gravity = android.view.Gravity.END;
                textHolder.messageTextView.setBackgroundResource(R.drawable.bubble_user);
            } else {
                params.gravity = android.view.Gravity.START;
                textHolder.messageTextView.setBackgroundResource(R.drawable.bubble_bot);
            }
            textHolder.messageTextView.setLayoutParams(params);
        }
    }

    private void addToCart(Product product) {
        // Tạo ID cho CartItem, ưu tiên ID thật của sản phẩm
        String productId = (product.getId() != null && !product.getId().isEmpty()) ? product.getId()
                : product.getName();

        CartItem cartItem = new CartItem(
                productId,
                product.getName(),
                product.getPrice(),
                1,
                "M",
                "100%",
                "100%",
                null,
                null);

        // Xử lý ảnh cho CartItem
        if (product.getImageResId() != 0) {
            cartItem.setImageResId(product.getImageResId());
        } else if (product.getImageUrl() != null) {
            cartItem.setImageUrl(product.getImageUrl());
        } else {
            cartItem.setImageResId(R.drawable.ic_coffee); // Ảnh mặc định
        }

        // Set category nếu có
        if (product.getCategoryName() != null) {
            cartItem.setCategory(product.getCategoryName());
        } else if (product.getCategoryId() != null) {
            cartItem.setCategory(product.getCategoryId());
        }

        CartManager.getInstance().addToCart(cartItem);
        Toast.makeText(context, "Đã thêm " + product.getName() + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();

        // Chuyển sang màn hình Giỏ hàng
        Intent intent = new Intent(context, CartActivity.class);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.messageTextView);
        }
    }

    public static class ProductMessageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;
        MaterialButton btnAddToCart;

        public ProductMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.iv_chat_product_image);
            tvProductName = itemView.findViewById(R.id.tv_chat_product_name);
            tvProductPrice = itemView.findViewById(R.id.tv_chat_product_price);
            btnAddToCart = itemView.findViewById(R.id.btn_chat_add_to_cart);
        }
    }
}