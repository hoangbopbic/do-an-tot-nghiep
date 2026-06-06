package com.example.app_tn;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {

    String correctPin = "123456";
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        EditText edtPin = findViewById(R.id.edtPin);
        Button btn = findViewById(R.id.btnUnlock);

        // 1. Cấu hình xác thực sinh trắc học
        setupBiometric();

        // 2. Tự động hiện nhận diện khuôn mặt khi mở màn hình
        biometricPrompt.authenticate(promptInfo);

        // 3. Xử lý nút bấm PIN (Giữ nguyên logic cũ)
        btn.setOnClickListener(v -> {
            if (edtPin.getText().toString().equals(correctPin)) {
                navigateToMain();
            } else {
                Toast.makeText(this, "Sai PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBiometric() {
        executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(LockActivity.this,
                executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                // Người dùng hủy hoặc lỗi, để họ nhập PIN
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Xác thực thành công!", Toast.LENGTH_SHORT).show();
                navigateToMain();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Không nhận diện được khuôn mặt", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Xác thực khuôn mặt/Vân tay")
                .setSubtitle("Sử dụng sinh trắc học để mở khóa")
                .setNegativeButtonText("Sử dụng mã PIN") // Nút để chuyển sang nhập PIN thủ công
                .build();
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}