package com.example.myapplication.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;

import com.bumptech.glide.Glide;
import com.example.myapplication.R;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Order;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.OrderViewHolder> {

    private final Context context;
    private List<Order> orderList;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Order order);

        void onCancelOrder(Order order);

        void onReorder(Order order);

        void onConfirmCompleted(Order order);
    }

    public OrderHistoryAdapter(Context context, List<Order> orderList, OnItemClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order, listener, context, position);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderDate, tvOrderTotal;
        Chip tvOrderStatus;
        ImageView ivProductThumbnail;
        Button btnCancel, btnReorder, btnConfirmCompleted, btnDetail;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvOrderDate = itemView.findViewById(R.id.tvOrderDate);
            tvOrderTotal = itemView.findViewById(R.id.tvOrderTotal);
            tvOrderStatus = itemView.findViewById(R.id.tvOrderStatus);
            ivProductThumbnail = itemView.findViewById(R.id.ivProductThumbnail);
            btnCancel = itemView.findViewById(R.id.btnCancel);
            btnReorder = itemView.findViewById(R.id.btnReorder);
            btnConfirmCompleted = itemView.findViewById(R.id.btnConfirmCompleted);
            btnDetail = itemView.findViewById(R.id.btnDetail);
        }

        public void bind(final Order order, final OnItemClickListener listener, final Context context, int position) {
            if (order == null)
                return;

            // NEW Logic: No index numbering, clean title formatting
            String nameSummary = context.getString(R.string.order_prefix);
            if (order.getCartItems() != null && !order.getCartItems().isEmpty()) {
                StringBuilder sb = new StringBuilder();
                sb.append(order.getCartItems().get(0).getProductName());
                if (order.getCartItems().size() > 1) {
                    sb.append(", +").append(order.getCartItems().size() - 1).append(" món khác");
                }
                nameSummary = sb.toString();
            }
            tvOrderId.setText(nameSummary);

            // Set Date
            tvOrderDate.setText(order.getFormattedDate());

            // Set Total
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            tvOrderTotal.setText(formatter.format(order.getTotalAmount()));

            String statusRaw = order.getStatus();
            String displayStatus = statusRaw;
            int colorCode = Color.GRAY;

            // Reset visibility
            btnCancel.setVisibility(View.GONE);
            btnConfirmCompleted.setVisibility(View.GONE);
            btnReorder.setVisibility(View.GONE);
            btnDetail.setVisibility(View.GONE);

            if (statusRaw != null) {
                switch (statusRaw) {
                    case "Pending":
                    case "Đang xử lý":
                        displayStatus = "Đang xử lý";
                        tvOrderStatus.setChipStrokeColorResource(R.color.status_pending);
                        tvOrderStatus.setTextColor(context.getColor(R.color.status_pending));
                        btnCancel.setText(R.string.cancel_label);
                        btnCancel.setVisibility(View.VISIBLE);
                        btnConfirmCompleted.setText(R.string.confirm_label);
                        btnConfirmCompleted.setVisibility(View.VISIBLE);
                        break;
                    case "Shipping":
                    case "Đang giao":
                        displayStatus = "Đang giao";
                        tvOrderStatus.setChipStrokeColorResource(R.color.status_processing);
                        tvOrderStatus.setTextColor(context.getColor(R.color.status_processing));
                        btnConfirmCompleted.setVisibility(View.VISIBLE);
                        btnDetail.setVisibility(View.VISIBLE);
                        break;
                    case "Completed":
                    case "Hoàn thành":
                        displayStatus = "Đã hoàn thành";
                        tvOrderStatus.setChipStrokeColorResource(R.color.status_completed);
                        tvOrderStatus.setTextColor(context.getColor(R.color.status_completed));
                        btnReorder.setVisibility(View.VISIBLE);
                        btnDetail.setVisibility(View.VISIBLE);
                        break;
                    case "Cancelled":
                    case "Đã hủy":
                        displayStatus = "Đã hủy";
                        tvOrderStatus.setChipStrokeColorResource(R.color.status_cancelled);
                        tvOrderStatus.setTextColor(context.getColor(R.color.status_cancelled));
                        btnReorder.setVisibility(View.VISIBLE);
                        btnDetail.setVisibility(View.VISIBLE);
                        break;
                    default:
                        displayStatus = statusRaw;
                        btnDetail.setVisibility(View.VISIBLE);
                        break;
                }
            }
            tvOrderStatus.setText(displayStatus);

            // --- CẬP NHẬT LOGIC HIỂN THỊ ẢNH ---
            int placeholderResId = R.drawable.ic_coffee_placeholder; // Default

            if (order.getCartItems() != null && !order.getCartItems().isEmpty()) {
                CartItem firstItem = order.getCartItems().get(0);

                // Determine placeholder based on category
                if (firstItem.getCategory() != null) {
                    String catLower = firstItem.getCategory().toLowerCase();
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

                // Logic mới: Kiểm tra cả URL và Resource ID
                Object imageSource = null;
                if (firstItem.getImageUrl() != null && !firstItem.getImageUrl().isEmpty()) {
                    imageSource = firstItem.getImageUrl();
                } else if (firstItem.getImageResId() != 0) {
                    imageSource = firstItem.getImageResId();
                }

                if (imageSource != null) {
                    Glide.with(context)
                            .load(imageSource)
                            .placeholder(placeholderResId)
                            .error(placeholderResId)
                            .into(ivProductThumbnail);
                } else {
                    ivProductThumbnail.setImageResource(placeholderResId);
                }
            } else {
                ivProductThumbnail.setImageResource(placeholderResId);
            }
            // -------------------------------------

            itemView.setOnClickListener(v -> listener.onItemClick(order));
            btnDetail.setOnClickListener(v -> listener.onItemClick(order));
            btnCancel.setOnClickListener(v -> listener.onCancelOrder(order));
            btnReorder.setOnClickListener(v -> listener.onReorder(order));
            btnConfirmCompleted.setOnClickListener(v -> listener.onConfirmCompleted(order));
        }
    }
}
