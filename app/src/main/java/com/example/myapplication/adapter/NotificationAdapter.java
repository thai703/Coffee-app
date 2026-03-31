package com.example.myapplication.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.AppNotification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private Context context;
    private List<AppNotification> notificationList;

    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);

        void onNotificationAction(View view, AppNotification notification); // For menu
    }

    public NotificationAdapter(Context context, List<AppNotification> notificationList,
            OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    public void updateList(List<AppNotification> newList) {
        this.notificationList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        AppNotification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        // Format Time "X minutes ago"
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS);
        holder.tvTime.setText(timeAgo);

        // Icon Logic
        holder.ivIcon.setImageResource(notification.getIconResId());

        // Color Logic based on Type
        int bgTint = R.color.orange_100; // Default
        int iconTint = R.color.orange_500;

        if ("ORDER".equals(notification.getType())) {
            bgTint = R.color.blue_100;
            iconTint = R.color.blue_500;
        } else if ("PROMO".equals(notification.getType())) {
            bgTint = R.color.orange_100;
            iconTint = R.color.orange_500;
        } else {
            bgTint = R.color.grey_200;
            iconTint = R.color.grey_600;
        }

        // Note: Simple tint implementation. For real circle bg, we might need dynamic
        // drawable tinting
        // For now, let's just stick to the specific icons.
        holder.ivIcon.setColorFilter(ContextCompat.getColor(context, iconTint));

        // Unread Dot
        holder.viewUnreadDot.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

        holder.ivMore.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationAction(v, notification);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon, ivMore;
        TextView tvTitle, tvMessage, tvTime;
        View viewUnreadDot;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            ivMore = itemView.findViewById(R.id.iv_more);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            viewUnreadDot = itemView.findViewById(R.id.view_unread_dot);
        }
    }
}
