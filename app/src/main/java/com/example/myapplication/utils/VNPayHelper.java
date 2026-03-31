package com.example.myapplication.utils;

import android.net.Uri;
import android.util.Log;
import com.example.myapplication.BuildConfig;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayHelper {
    private static final String TAG = "VNPayHelper";

    /**
     * Tạo URL thanh toán VNPay
     * 
     * @param orderInfo Thông tin đơn hàng
     * @param amount    Số tiền (VND)
     * @param orderType Loại đơn hàng (billpayment, topup, etc.)
     * @return Payment URL để mở trình duyệt
     */
    public static String createPaymentUrl(String orderInfo, long amount, String orderType) {
        try {
            Map<String, String> vnpParams = new HashMap<>();

            vnpParams.put("vnp_Version", "2.1.0");
            vnpParams.put("vnp_Command", "pay");
            vnpParams.put("vnp_TmnCode", BuildConfig.VNPAY_TMN_CODE);
            vnpParams.put("vnp_Amount", String.valueOf(amount * 100)); // VNPay yêu cầu nhân 100
            vnpParams.put("vnp_CurrCode", "VND");

            // Transaction reference (unique)
            String txnRef = "ORDER_" + System.currentTimeMillis();
            vnpParams.put("vnp_TxnRef", txnRef);

            vnpParams.put("vnp_OrderInfo", orderInfo);
            vnpParams.put("vnp_OrderType", orderType);
            vnpParams.put("vnp_Locale", "vn");
            vnpParams.put("vnp_ReturnUrl", BuildConfig.VNPAY_RETURN_URL);
            vnpParams.put("vnp_IpAddr", "127.0.0.1");

            // Thời gian tạo giao dịch
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            String createDate = formatter.format(new Date());
            vnpParams.put("vnp_CreateDate", createDate);

            // Thời gian hết hạn (15 phút)
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MINUTE, 15);
            String expireDate = formatter.format(cal.getTime());
            vnpParams.put("vnp_ExpireDate", expireDate);

            // Sắp xếp params theo thứ tự alphabet
            List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
            Collections.sort(fieldNames);

            // Tạo hash data
            StringBuilder hashData = new StringBuilder();
            StringBuilder query = new StringBuilder();

            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = vnpParams.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    // Build hash data
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    // Build query
                    query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                    query.append('=');
                    query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                    if (itr.hasNext()) {
                        query.append('&');
                        hashData.append('&');
                    }
                }
            }

            String queryUrl = query.toString();
            String vnpSecureHash = hmacSHA512(BuildConfig.VNPAY_HASH_SECRET, hashData.toString());
            queryUrl += "&vnp_SecureHash=" + vnpSecureHash;

            String paymentUrl = BuildConfig.VNPAY_PAYMENT_URL + "?" + queryUrl;
            Log.d(TAG, "Payment URL created successfully");

            return paymentUrl;

        } catch (Exception e) {
            Log.e(TAG, "Error creating payment URL", e);
            return null;
        }
    }

    /**
     * Xác thực response từ VNPay
     * 
     * @param uri Deep link URI nhận được
     * @return true nếu chữ ký hợp lệ
     */
    public static boolean verifyPaymentResponse(Uri uri) {
        try {
            Map<String, String> params = new HashMap<>();

            // Lấy tất cả params từ return URL
            for (String key : uri.getQueryParameterNames()) {
                String value = uri.getQueryParameter(key);
                if (value != null && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")) {
                    params.put(key, value);
                }
            }

            // Sắp xếp và tạo hash
            List<String> fieldNames = new ArrayList<>(params.keySet());
            Collections.sort(fieldNames);

            StringBuilder hashData = new StringBuilder();
            Iterator<String> itr = fieldNames.iterator();
            while (itr.hasNext()) {
                String fieldName = itr.next();
                String fieldValue = params.get(fieldName);
                if ((fieldValue != null) && (fieldValue.length() > 0)) {
                    hashData.append(fieldName);
                    hashData.append('=');
                    hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                    if (itr.hasNext()) {
                        hashData.append('&');
                    }
                }
            }

            String receivedHash = uri.getQueryParameter("vnp_SecureHash");
            String calculatedHash = hmacSHA512(BuildConfig.VNPAY_HASH_SECRET, hashData.toString());

            boolean isValid = calculatedHash.equalsIgnoreCase(receivedHash);
            Log.d(TAG, "Payment verification result: " + isValid);

            return isValid;

        } catch (Exception e) {
            Log.e(TAG, "Error verifying payment response", e);
            return false;
        }
    }

    /**
     * Lấy message lỗi từ response code
     */
    public static String getResponseMessage(String responseCode) {
        switch (responseCode) {
            case "00":
                return "Giao dịch thành công";
            case "07":
                return "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường).";
            case "09":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10":
                return "Giao dịch không thành công do: Khách hàng xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11":
                return "Giao dịch không thành công do: Đã hết hạn chờ thanh toán. Xin quý khách vui lòng thực hiện lại giao dịch.";
            case "12":
                return "Giao dịch không thành công do: Thẻ/Tài khoản của khách hàng bị khóa.";
            case "13":
                return "Giao dịch không thành công do Quý khách nhập sai mật khẩu xác thực giao dịch (OTP).";
            case "24":
                return "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51":
                return "Giao dịch không thành công do: Tài khoản của quý khách không đủ số dư để thực hiện giao dịch.";
            case "65":
                return "Giao dịch không thành công do: Tài khoản của Quý khách đã vượt quá hạn mức giao dịch trong ngày.";
            case "75":
                return "Ngân hàng thanh toán đang bảo trì.";
            case "79":
                return "Giao dịch không thành công do: KH nhập sai mật khẩu thanh toán quá số lần quy định.";
            default:
                return "Giao dịch thất bại";
        }
    }

    /**
     * Tạo chữ ký HMAC SHA512
     */
    private static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating HMAC", e);
            return "";
        }
    }
}
