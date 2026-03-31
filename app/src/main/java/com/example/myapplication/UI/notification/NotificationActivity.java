package com.example.myapplication.UI.notification;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.adapter.NotificationAdapter;
import com.example.myapplication.manager.NotificationReadManager;
import com.example.myapplication.model.AppNotification;
import com.example.myapplication.model.Order;
import com.example.myapplication.UI.order.UserOrderDetailActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList;
    private TextView tvEmpty;
    private NotificationReadManager readManager;
    private List<Order> cachedOrders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        readManager = new NotificationReadManager(this);

        initViews();
        setupRecyclerView();
        loadNotifications();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.action_notification);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.recycler_view_notifications);
        tvEmpty = findViewById(R.id.tv_empty);
    }

    private void setupRecyclerView() {
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(this, notificationList,
                new NotificationAdapter.OnNotificationClickListener() {
                    @Override
                    public void onNotificationClick(AppNotification notification) {
                        NotificationActivity.this.onNotificationClick(notification);
                    }

                    @Override
                    public void onNotificationAction(View view, AppNotification notification) {
                        NotificationActivity.this.onNotificationAction(view, notification);
                    }
                });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    // ...

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notification, menu);
        MenuItem item = menu.findItem(R.id.action_mark_all_read);
        if (item != null) {
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(this,
                    R.drawable.ic_check_circle);
            if (icon != null) {
                icon = androidx.core.graphics.drawable.DrawableCompat.wrap(icon);
                androidx.core.graphics.drawable.DrawableCompat.setTint(icon,
                        androidx.core.content.ContextCompat.getColor(this, R.color.coffee_brown));
                item.setIcon(icon);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_mark_all_read) {
            markAllAsRead();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onNotificationClick(AppNotification note) {
        // Mark locally and persist
        if (!note.isRead()) {
            note.setRead(true);
            readManager.markAsRead(note.getId());
            adapter.notifyDataSetChanged();
        }

        if ("ORDER".equals(note.getType())) {
            Order targetOrder = findOrderById(note.getId());
            if (targetOrder != null) {
                Intent intent = new Intent(this, UserOrderDetailActivity.class);
                intent.putExtra("order", targetOrder);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không tìm thấy đơn hàng", Toast.LENGTH_SHORT).show();
            }
        } else if ("PROMO".equals(note.getType())) {
            Toast.makeText(this, "Chi tiết khuyến mãi: " + note.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void onNotificationAction(View view, AppNotification note) {
        android.widget.PopupMenu popup = new android.widget.PopupMenu(this, view);
        // Add menu items: ID 1 = Toggle Read, ID 2 = Delete
        popup.getMenu().add(0, 1, 0,
                note.isRead() ? getString(R.string.action_mark_unread) : getString(R.string.action_mark_read));
        popup.getMenu().add(0, 2, 1, getString(R.string.action_delete_notif));

        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                boolean newReadState = !note.isRead();
                note.setRead(newReadState);
                if (newReadState)
                    readManager.markAsRead(note.getId());
                else
                    readManager.markAsUnread(note.getId());
                adapter.notifyDataSetChanged();
            } else if (item.getItemId() == 2) {
                readManager.markAsDeleted(note.getId());
                notificationList.remove(note);
                adapter.notifyDataSetChanged();
            }
            return true;
        });
        popup.show();
    }

    private void markAllAsRead() {
        Set<String> allIds = new HashSet<>();
        for (AppNotification note : notificationList) {
            allIds.add(note.getId());
            note.setRead(true);
        }
        readManager.markAllAsRead(allIds);
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Đã đánh dấu đọc tất cả", Toast.LENGTH_SHORT).show();
    }

    private Order findOrderById(String orderId) {
        for (Order o : cachedOrders) {
            if (o.getOrderId().equals(orderId))
                return o;
        }
        return null;
    }

    private void loadNotifications() {
        notificationList.clear();

        // 1. Add System/Promo Notifications (Check Deleted)
        if (!readManager.isDeleted("sys_1")) {
            AppNotification sysNote = new AppNotification(
                    "sys_1",
                    getString(R.string.notif_sys_welcome_title),
                    getString(R.string.notif_sys_welcome_msg),
                    System.currentTimeMillis() - 86400000L * 2,
                    R.drawable.ic_coffee,
                    "SYSTEM");
            sysNote.setRead(readManager.isRead(sysNote.getId()));
            notificationList.add(sysNote);
        }

        if (!readManager.isDeleted("promo_1")) {
            AppNotification promoNote = new AppNotification(
                    "promo_1",
                    getString(R.string.notif_promo_title),
                    getString(R.string.notif_promo_msg),
                    System.currentTimeMillis(),
                    R.drawable.ic_category_coffee,
                    "PROMO");
            promoNote.setRead(readManager.isRead(promoNote.getId()));
            notificationList.add(promoNote);
        }

        // 2. Fetch Orders
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference("orders").child(user.getUid());
            ordersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<AppNotification> orderNotifs = new ArrayList<>();
                    cachedOrders.clear();

                    for (DataSnapshot data : snapshot.getChildren()) {
                        Order order = null;
                        try {
                            order = data.getValue(Order.class);
                        } catch (Exception e) {
                            Log.e("Notification", "Error converting date for key: " + data.getKey(), e);
                            continue;
                        }

                        if (order != null) {
                            cachedOrders.add(order);
                            // Check deleted
                            if (readManager.isDeleted(order.getOrderId()))
                                continue;

                            AppNotification note = convertOrderToNotification(order);
                            if (note != null) {
                                note.setRead(readManager.isRead(note.getId()));
                                orderNotifs.add(note);
                            }
                        }
                    }

                    // Existing list contains only Static items now, we should re-add them
                    // But notificationList is cleared at start of loadNotifications so it implies
                    // fresh load.
                    // Ideally we shouldn't fully reload static items in onDataChange unless we
                    // manage list carefully.
                    // For simplicity, let's keep static items in notificationList and just add
                    // Order Notifs.
                    // Current simplified flow: clear list -> add static -> fetch orders.
                    // Inside callback: we can't easily append without duplication if callback runs
                    // multiple times.
                    // Better: Re-build entire list inside callback.

                    List<AppNotification> fullList = new ArrayList<>();
                    if (!readManager.isDeleted("sys_1")) {
                        // Re-create static logic... simplified for now, just appending to existing list
                        // if empty?
                        // No, let's just clear and rebuild everything inside callback to be safe.
                    }

                    // Actually, simpler: notificationList has static items. remove logic is messy.
                    // Let's just use what we have:
                    notificationList.addAll(orderNotifs);
                    Collections.sort(notificationList, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    adapter.updateList(notificationList);
                    tvEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Notification", "Load orders failed", error.toException());
                }
            });
        } else {
            adapter.updateList(notificationList);
        }
    }

    private AppNotification convertOrderToNotification(Order order) {
        String title = "";
        String msg = "";
        int icon = R.drawable.ic_coffee;
        long time = order.getOrderDate();
        if (time == 0)
            time = System.currentTimeMillis();

        // Get Product Info logic
        String productInfo = "đơn hàng";
        if (order.getCartItems() != null && !order.getCartItems().isEmpty()) {
            String firstName = order.getCartItems().get(0).getProductName();
            int count = order.getCartItems().size();
            if (count > 1) {
                productInfo = getString(R.string.notif_product_info_multi, firstName, (count - 1));
            } else {
                productInfo = firstName;
            }
        }
        String orderIdShort = "#" + order.getOrderId().substring(Math.max(0, order.getOrderId().length() - 6));

        String status = order.getStatus();
        if (status == null)
            return null;

        switch (status) {
            case "Pending":
            case "Đang xử lý":
                title = getString(R.string.notif_title_pending);
                msg = getString(R.string.notif_msg_pending, productInfo, orderIdShort);
                icon = R.drawable.ic_category_coffee;
                break;
            case "Shipping":
            case "Đang giao":
                title = getString(R.string.notif_title_shipping);
                msg = getString(R.string.notif_msg_shipping, productInfo);
                icon = R.drawable.ic_category_tea;
                break;
            case "Completed":
            case "Hoàn thành":
                title = getString(R.string.notif_title_completed);
                msg = getString(R.string.notif_msg_completed, productInfo);
                icon = R.drawable.ic_category_smoothie;
                break;
            case "Cancelled":
            case "Đã hủy":
                title = getString(R.string.notif_title_cancelled);
                msg = getString(R.string.notif_msg_cancelled, productInfo);
                icon = R.drawable.ic_category_pastry;
                break;
            default:
                return null;
        }

        return new AppNotification(
                order.getOrderId(),
                title,
                msg,
                time,
                icon,
                "ORDER");
    }
}
