package com.example.myapplication.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.UI.admin.AdminOrderDetailActivity;
import com.example.myapplication.model.CartItem;
import com.example.myapplication.model.Order;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder> {

    private Context context;
    private List<Order> orderList;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onConfirmOrder(Order order);

        void onCancelOrder(Order order);
    }

    public AdminOrderAdapter(Context context, List<Order> orderList, OnOrderActionListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AdminOrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new AdminOrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminOrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        if (order == null)
            return;

        holder.tvOrderId.setText("Mã đơn: #" + (order.getOrderId() != null && order.getOrderId().length() > 6
                ? order.getOrderId().substring(order.getOrderId().length() - 6)
                : order.getOrderId()));
        holder.tvOrderStatus.setText(order.getStatus());
        holder.tvOrderDate.setText("Ngày đặt: " + order.getFormattedDate());
        holder.tvCustomerName.setText("Khách: " + order.getCustomerName());
        holder.tvCustomerPhone.setText("Số điện thoại: " + order.getPhoneNumber());
        holder.tvCustomerAddress.setText("Địa chỉ: " + order.getShippingAddress());

        // --- HIỂN THỊ CHI TIẾT MÓN ĂN ---
        StringBuilder itemsBuilder = new StringBuilder();
        if (order.getCartItems() != null) {
            for (CartItem item : order.getCartItems()) {
                itemsBuilder.append("- ").append(item.getQuantity()).append("x ")
                        .append(item.getProductName() != null ? item.getProductName() : item.getName())
                        .append("\n");
            }
        }
        holder.tvOrderItems.setText(itemsBuilder.toString().trim());
        // ----------------------------------

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.tvOrderTotal.setText("Tổng tiền: " + currencyFormat.format(order.getTotalAmount()));

        // Status coloring
        if ("Pending".equalsIgnoreCase(order.getStatus())) {
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            holder.layoutActionButtons.setVisibility(View.VISIBLE);
        } else if ("Confirmed".equalsIgnoreCase(order.getStatus())) {
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.layoutActionButtons.setVisibility(View.GONE);
        } else if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.layoutActionButtons.setVisibility(View.GONE);
        } else {
            holder.tvOrderStatus.setTextColor(ContextCompat.getColor(context, android.R.color.black));
            holder.layoutActionButtons.setVisibility(View.GONE);
        }

        holder.btnConfirm.setOnClickListener(v -> {
            if (listener != null)
                listener.onConfirmOrder(order);
        });

        holder.btnCancel.setOnClickListener(v -> {
            if (listener != null)
                listener.onCancelOrder(order);
        });

        // Click vào item để xem chi tiết
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, AdminOrderDetailActivity.class);
            intent.putExtra("order", order);
            intent.putExtra("isAdmin", true);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orderList != null ? orderList.size() : 0;
    }

    public static class AdminOrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvOrderStatus, tvOrderDate, tvCustomerName, tvCustomerPhone, tvCustomerAddress,
                tvOrderTotal;
        TextView tvOrderItems; // Mới thêm
        Button btnConfirm, btnCancel;
        View layoutActionButtons;

        public AdminOrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderStatus = itemView.findViewById(R.id.tv_order_status);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvCustomerPhone = itemView.findViewById(R.id.tv_customer_phone);
            tvCustomerAddress = itemView.findViewById(R.id.tv_customer_address);
            tvOrderItems = itemView.findViewById(R.id.tv_order_items); // Ánh xạ View mới
            tvOrderTotal = itemView.findViewById(R.id.tv_order_total);
            btnConfirm = itemView.findViewById(R.id.btn_confirm);
            btnCancel = itemView.findViewById(R.id.btn_cancel);
            layoutActionButtons = itemView.findViewById(R.id.layout_action_buttons);
        }
    }
}
