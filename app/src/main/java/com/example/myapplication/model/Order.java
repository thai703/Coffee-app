package com.example.myapplication.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Order implements Serializable {
    // Fields for new checkout flow
    private String orderId;
    private String userId;
    private List<CartItem> cartItems;
    private double totalAmount;
    private long orderDate;
    private String shippingAddress;
    private String paymentMethod;
    private String status;
    private String formattedDate;

    // Fields for compatibility with old Admin/History code
    private String customerName;
    private String phoneNumber;
    private long timestamp;
    private double totalPrice;

    public Order() {
    }

    public Order(String orderId, String userId, List<CartItem> cartItems, double totalAmount, long orderDate,
            String customerName, String phoneNumber, String shippingAddress, String paymentMethod, String status) {
        this.orderId = orderId;
        this.userId = userId;
        this.cartItems = cartItems;
        this.totalAmount = totalAmount;
        this.orderDate = orderDate;
        this.customerName = customerName;
        this.phoneNumber = phoneNumber;
        this.shippingAddress = shippingAddress;
        this.paymentMethod = paymentMethod;
        this.status = status;

        this.timestamp = orderDate;
        this.totalPrice = totalAmount;
        // THE FIX: Updated date format to include year
        this.formattedDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"))
                .format(new Date(orderDate));
    }

    // Getters and Setters for new fields
    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(List<CartItem> cartItems) {
        this.cartItems = cartItems;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public long getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(long orderDate) {
        this.orderDate = orderDate;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormattedDate() {
        if (formattedDate == null || formattedDate.isEmpty()) {
            if (orderDate > 0) {
                // THE FIX: Updated date format to include year
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
                return sdf.format(new Date(orderDate));
            }
            return "";
        }
        return formattedDate;
    }

    public void setFormattedDate(String formattedDate) {
        this.formattedDate = formattedDate;
    }

    // Getters and Setters for legacy fields
    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    // Shipping Fee
    private double shippingFee;

    public double getShippingFee() {
        return shippingFee;
    }

    public void setShippingFee(double shippingFee) {
        this.shippingFee = shippingFee;
    }

}
