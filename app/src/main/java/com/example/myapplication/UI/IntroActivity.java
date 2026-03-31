package com.example.myapplication.UI;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.UI.Login.LoginActivity;

import com.example.myapplication.manager.LanguageHelper;

public class IntroActivity extends AppCompatActivity {

    private ImageView imageView;
    private TextView tvWelcome, tvSlogan;
    private Button btnExplore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LanguageHelper.loadLocale(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // Ánh xạ View
        imageView = findViewById(R.id.imageView);
        tvWelcome = findViewById(R.id.tv_welcome);
        tvSlogan = findViewById(R.id.tv_slogan);
        btnExplore = findViewById(R.id.btn_explore);

        // 1. Hiệu ứng Ken Burns cho background (Zoom chậm)
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(imageView, "scaleX", 1.0f, 1.2f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(imageView, "scaleY", 1.0f, 1.2f);
        scaleX.setDuration(10000); // 10 giây
        scaleY.setDuration(10000);
        AnimatorSet scaleSet = new AnimatorSet();
        scaleSet.playTogether(scaleX, scaleY);
        scaleSet.start();

        // 2. Staggered Animation cho Text và Button (Hiện dần + Trượt lên)
        animateView(tvWelcome, 300); // Hiện nhanh hơn
        animateView(tvSlogan, 500); // Hiện nhanh hơn
        animateView(btnExplore, 700); // Hiện nhanh hơn

        // Sự kiện click
        btnExplore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(IntroActivity.this, LoginActivity.class);
                startActivity(intent);
                // Sử dụng transition fade_in và fade_out hệ thống cho mượt
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
    }

    // Hàm tạo hiệu ứng Fade In + Slide Up
    private void animateView(View view, long delay) {
        view.setTranslationY(100f); // Đẩy xuống 100 đơn vị
        view.setAlpha(0f); // Ẩn hoàn toàn

        view.animate()
                .translationY(0f) // Trượt về vị trí cũ
                .alpha(1f) // Hiện rõ dần
                .setDuration(600) // Thời gian hiệu ứng ngắn lại chút
                .setStartDelay(delay) // Thời gian chờ
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
    }
}
