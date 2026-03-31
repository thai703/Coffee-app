package com.example.myapplication.UI.support;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.BuildConfig;
import com.example.myapplication.R;
import com.example.myapplication.adapter.MessageAdapter;
import com.example.myapplication.api.GeminiApiService;
import com.example.myapplication.model.Message;
import com.example.myapplication.model.Product;
import com.example.myapplication.model.gemini.GeminiRequest;
import com.example.myapplication.model.gemini.GeminiResponse;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatbotActivity extends AppCompatActivity {

    private static final String KEY_MESSAGES = "key_messages";
    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ArrayList<Message> messageList;
    private MessageAdapter messageAdapter;

    private GeminiApiService geminiService;

    // Biến lưu trữ menu và lịch sử để gửi cho AI
    private String menuContext = "";
    private String userHistoryContext = "";
    private boolean isFirstMessageToSend = true;

    // Danh sách toàn bộ sản phẩm để tra cứu khi bot gợi ý
    private List<Product> allProducts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatbot);

        // Toolbar setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendButton);

        if (savedInstanceState != null && savedInstanceState.getSerializable(KEY_MESSAGES) != null) {
            // noinspection unchecked
            messageList = (ArrayList<Message>) savedInstanceState.getSerializable(KEY_MESSAGES);
        } else {
            messageList = new ArrayList<>();
        }

        String localUserId = "local_user";
        messageAdapter = new MessageAdapter(messageList, localUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(messageAdapter);

        // Init Retrofit
        initRetrofit();

        // Bước 1: Lấy dữ liệu (Menu + Lịch sử) -> Sau đó Init AI greeting
        loadDataAndInitGreeting();

        sendButton.setOnClickListener(v -> {
            String messageText = messageEditText.getText().toString().trim();
            if (!messageText.isEmpty()) {
                addMessage(new Message(messageText, true));
                messageEditText.setText("");
                sendMessageToChatbot(messageText);
            } else {
                Toast.makeText(ChatbotActivity.this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_MESSAGES, messageList);
    }

    private void initRetrofit() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://generativelanguage.googleapis.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        geminiService = retrofit.create(GeminiApiService.class);
    }

    private void loadDataAndInitGreeting() {
        // 1. Lấy Menu trước - Sửa nhánh thành "products"
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        productsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allProducts.clear();
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Product product = data.getValue(Product.class);
                    if (product != null) {
                        allProducts.add(product); // Lưu vào danh sách để tra cứu sau này
                        count++;
                        sb.append("- ").append(product.getName())
                                .append(" (").append(formatPrice(product.getPrice())).append(" VND)");
                        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
                            sb.append(": ").append(product.getDescription());
                        }
                        sb.append("\n");
                    }
                }
                menuContext = sb.toString();
                Log.d("ChatbotActivity", "Loaded Menu: " + count + " items.");

                // 2. Sau khi có menu, lấy tiếp Lịch sử ăn uống (nếu đã đăng nhập)
                fetchUserHistoryAndShowGreeting();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatbotActivity", "Menu error: " + error.getMessage());
                fetchUserHistoryAndShowGreeting(); // Vẫn tiếp tục dù lỗi menu
            }
        });
    }

    private void fetchUserHistoryAndShowGreeting() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showGreeting(); // Khách vãng lai
            return;
        }

        // Thay đổi: Lấy dữ liệu từ node "orders" để có đầy đủ ngày giờ, tiền, món
        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference("orders").child(user.getUid());

        // Lấy 20 đơn gần nhất
        historyRef.limitToLast(20).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    StringBuilder sb = new StringBuilder();
                    // Header cho AI hiểu cấu trúc
                    sb.append("LỊCH SỬ MUA HÀNG CHI TIẾT (Dùng để tính toán chi tiêu):\n");
                    sb.append("Format: [Ngày giờ] | [Món ăn] | [Tổng tiền]\n");

                    List<com.example.myapplication.model.Order> tempOrders = new ArrayList<>();
                    for (DataSnapshot item : snapshot.getChildren()) {
                        try {
                            com.example.myapplication.model.Order order = item
                                    .getValue(com.example.myapplication.model.Order.class);
                            if (order != null) {
                                tempOrders.add(order);
                            }
                        } catch (Exception e) {
                            Log.e("ChatbotActivity", "Error parsing order: " + item.getKey(), e);
                        }
                    }
                    // Reverse để đơn mới nhất lên đầu (hoặc để AI tự xử lý, nhưng mới nhất lên đầu
                    // thường tốt hơn cho ngữ cảnh)
                    // Tuy nhiên prompt thường đọc từ trên xuống, ta cứ liệt kê theo thời gian cũng
                    // được.
                    // Nhưng để AI dễ nắm bắt "vừa mua", ta nên để đơn mới nhất ở cuối danh sách
                    // (tương ứng dòng thời gian)
                    // Firebase trả về cũ -> mới. OK giữ nguyên.

                    for (com.example.myapplication.model.Order order : tempOrders) {
                        String dateStr = order.getFormattedDate();
                        // Nếu formattedDate rỗng (data cũ), dùng timestamp
                        if (dateStr == null || dateStr.isEmpty()) {
                            dateStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    .format(new java.util.Date(order.getOrderDate()));
                        }

                        double total = order.getTotalAmount();

                        // Lấy tên các món
                        StringBuilder itemsStr = new StringBuilder();
                        if (order.getCartItems() != null) {
                            for (com.example.myapplication.model.CartItem cartItem : order.getCartItems()) {
                                if (itemsStr.length() > 0)
                                    itemsStr.append(", ");
                                itemsStr.append(cartItem.getProductName())
                                        .append(" (x").append(cartItem.getQuantity()).append(")");
                            }
                        }

                        sb.append("- [").append(dateStr).append("] | ")
                                .append(itemsStr).append(" | ")
                                .append(formatPrice(total)).append(" VND\n");
                    }

                    userHistoryContext = sb.toString();
                    Log.d("ChatbotActivity", "Loaded detailed history: \n" + userHistoryContext);
                }
                showGreeting();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("ChatbotActivity", "History error: " + error.getMessage());
                showGreeting();
            }
        });
    }

    private String formatPrice(double price) {
        return String.format("%,.0f", price);
    }

    private void showGreeting() {
        if (messageList.isEmpty()) {
            runOnUiThread(() -> {
                String greeting = "Xin chào! Mình là trợ lý ảo Aura Cafe. Mình có thể giúp gì cho bạn?";
                if (!userHistoryContext.isEmpty()) {
                    greeting += " (Mình thấy bạn hay ghé quán, để mình gợi ý món hợp gu bạn nhé!)";
                }
                addMessage(new Message(greeting, false));
            });
        }
    }

    private void addMessage(Message message) {
        messageList.add(message);
        messageAdapter.notifyItemInserted(messageList.size() - 1);
        chatRecyclerView.scrollToPosition(messageList.size() - 1);
    }

    private void sendMessageToChatbot(String userMessage) {
        setUiEnabled(false);
        Message loadingMessage = new Message("...", false);
        addMessage(loadingMessage);
        int botMessageIndex = messageList.size() - 1;

        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.isEmpty()) {
            updateBotMessage(loadingMessage, botMessageIndex, "Lỗi: Chưa cấu hình API Key.");
            setUiEnabled(true);
            return;
        }

        // --- KỸ THUẬT PROMPT KẾT HỢP: MENU + LỊCH SỬ ---
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("BẠN LÀ: Nhân viên Aura Cafe. CHỈ ĐƯỢC PHÉP nói về sản phẩm và dịch vụ của Aura Cafe.\n");
        promptBuilder
                .append("QUY TẮC: Trả lời cực ngắn (1-2 câu). Tuyệt đối không giải thích linh tinh, không triết lý.\n");
        promptBuilder.append(
                "NẾU KHÁCH HỎI MÓN KHÔNG CÓ TRONG MENU: Hãy lịch sự từ chối và gợi ý món tương tự có trong menu của quán.\n");

        if (!menuContext.isEmpty()) {
            promptBuilder.append("DANH SÁCH MÓN CỦA QUÁN (MENU):\n").append(menuContext).append("\n");
        }

        if (!userHistoryContext.isEmpty()) {
            promptBuilder.append("LỊCH SỬ KHÁCH HÀNG: ").append(userHistoryContext).append("\n");
        }

        promptBuilder.append("YÊU CẦU: Thân thiện, ngắn gọn.\n");
        promptBuilder.append(
                "QUAN TRỌNG: Nếu khách hỏi về lịch sử mua hàng, chi tiêu, hãy dùng dữ liệu 'LỊCH SỬ MUA HÀNG' ở trên để tính toán và trả lời chính xác con số.\n");
        promptBuilder.append("CÂU HỎI CỦA KHÁCH: ").append(userMessage);

        String contentToSend = promptBuilder.toString();
        // ------------------------------------------------

        GeminiRequest request = new GeminiRequest(contentToSend);
        Call<GeminiResponse> call = geminiService.generateContent(apiKey, request);

        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String botResponseText = response.body().getText();
                    if (botResponseText == null)
                        botResponseText = "Xin lỗi, mình không hiểu ý bạn.";

                    updateBotMessage(loadingMessage, botMessageIndex, botResponseText);
                    checkForProductInResponse(botResponseText);
                } else {
                    String errorMsg = "Lỗi phản hồi từ AI: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                    }
                    Log.e("ChatbotActivity", errorMsg);
                    updateBotMessage(loadingMessage, botMessageIndex, "Hệ thống đang bận, thử lại sau nhé.");
                }
                setUiEnabled(true);
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e("ChatbotActivity", "Lỗi mạng: " + t.getMessage());
                updateBotMessage(loadingMessage, botMessageIndex, "Lỗi kết nối mạng.");
                setUiEnabled(true);
            }
        });
    }

    private void updateBotMessage(Message message, int index, String newContent) {
        message.setContent(newContent);
        messageAdapter.notifyItemChanged(index);
    }

    private void checkForProductInResponse(String responseText) {
        if (allProducts == null || allProducts.isEmpty())
            return;

        String normalizedResponse = responseText.toLowerCase();
        Product foundProduct = null;

        // Ưu tiên tìm tên chính xác và dài nhất trước để tránh nhầm lẫn
        try {
            for (Product product : allProducts) {
                if (product.getName() == null || product.getName().trim().length() < 2)
                    continue;

                // Check logic đơn giản: nếu trong response có tên món thì show card
                if (normalizedResponse.contains(product.getName().toLowerCase())) {
                    foundProduct = product;
                    break; // Chỉ hiển thị 1 món đầu tiên tìm thấy
                }
            }
        } catch (Exception e) {
            Log.e("ChatbotActivity", "Error checking product: " + e.getMessage());
        }

        if (foundProduct != null) {
            Message productMessage = new Message(foundProduct);
            addMessage(productMessage);
        }
    }

    private void setUiEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
        messageEditText.setEnabled(enabled);
    }
}
