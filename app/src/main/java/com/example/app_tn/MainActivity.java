package com.example.app_tn;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.*;

import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    private Switch swMode, swLedCua, swLedNha, swDoor, swRack;
    private TextView txtRain, txtFlame, txtPIR;
    private Button  btnAllOff;
    private LinearLayout btnVoice;

    // Firebase
    private DatabaseReference db;

    // State
    private boolean isUpdatingUI = false;
    private boolean lastFire = false;
    private boolean isDialogShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initFirebase();
        setupListener();
        setupActions();
    }

    // ================= INIT =================
    @SuppressLint("WrongViewCast")
    private void initView() {
        swMode = findViewById(R.id.swMode);
        swLedCua = findViewById(R.id.swLedCua);
        swLedNha = findViewById(R.id.swLedNha);
        swDoor = findViewById(R.id.swDoor);
        swRack = findViewById(R.id.swRack);

        txtRain = findViewById(R.id.txtRain);
        txtFlame = findViewById(R.id.txtFlame);
        txtPIR = findViewById(R.id.txtPIR);

        btnVoice = findViewById(R.id.btnVoice);
        btnAllOff = findViewById(R.id.btnAllOff);
    }

    private void initFirebase() {
        db = FirebaseDatabase.getInstance().getReference("home");
    }

    // ================= REALTIME =================
    private void setupListener() {

        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snap) {

                isUpdatingUI = true;

                int mode = getInt(snap, "mode");

                boolean rain = getBool(snap, "sensors/rain");
                boolean flame = getBool(snap, "sensors/flame");
                boolean pir = getBool(snap, "sensors/pir");

                int led1 = getInt(snap, "state/led1");
                int led2 = getInt(snap, "state/led2");
                int door = getInt(snap, "state/door");
                int rack = getInt(snap, "state/rack");

                boolean fire = getBool(snap, "alert/fire");

                // ===== UI =====
                swMode.setChecked(mode == 1);
//                swLedCua.setChecked(led1 == 1);
//                swLedNha.setChecked(led2 == 1);
//                swDoor.setChecked(door == 1);
//                swRack.setChecked(rack == 1);

                txtRain.setText(rain ? "🌧️ Mưa" : "☀️ Tạnh");
                txtFlame.setText(flame ? "🔥 CHÁY" : "✅ An toàn");
                txtPIR.setText(pir ? "👤 Có người" : "🚫 Không có");

                txtFlame.setTextColor(flame ? 0xFFFF0000 : 0xFF00AA00);

                enableManual(mode == 1);

                // ===== FIRE ALERT =====
                if (fire && !lastFire && !isDialogShowing) {
                    showFireDialog();
                }

                lastFire = fire;
                isUpdatingUI = false;
                if (mode == 0 || justSwitchedMode) {
                    swLedCua.setChecked(led1 == 1);
                    swLedNha.setChecked(led2 == 1);
                    swDoor.setChecked(door == 1);
                    swRack.setChecked(rack == 1);
                }

                justSwitchedMode = false;
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "❌ Firebase lỗi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= FIRE DIALOG =================
    private void showFireDialog() {

        isDialogShowing = true;

        new AlertDialog.Builder(this)
                .setTitle("🔥 CẢNH BÁO CHÁY")
                .setMessage("Phát hiện cháy trong nhà!")
                .setCancelable(false)
                .setPositiveButton("OK", (d, w) -> {
                    d.dismiss();
                    isDialogShowing = false;
                })
                .show();
    }

    // ================= ACTION =================
    private boolean justSwitchedMode = false;
    private void setupActions() {

        swMode.setOnCheckedChangeListener((b, v) -> {
            if (isUpdatingUI) return;
            justSwitchedMode = true;
            db.child("mode").setValue(v ? 1 : 0);
        });

        swLedCua.setOnCheckedChangeListener((b, v) -> {
            if (isUpdatingUI) return;
            db.child("control/led1").setValue(v ? 1 : 0);
        });

        swLedNha.setOnCheckedChangeListener((b, v) -> {
            if (isUpdatingUI) return;
            db.child("control/led2").setValue(v ? 1 : 0);
        });

        swDoor.setOnCheckedChangeListener((b, v) -> {
            if (isUpdatingUI) return;
            db.child("control/door").setValue(v ? 1 : 0);
        });

        swRack.setOnCheckedChangeListener((b, v) -> {
            if (isUpdatingUI) return;
            db.child("control/rack").setValue(v ? 1 : 0);
        });

        btnAllOff.setOnClickListener(v -> {
            db.child("control").setValue(new Control(0,0,0,0));
            Toast.makeText(this, "Đã tắt toàn bộ", Toast.LENGTH_SHORT).show();
        });

        btnVoice.setOnClickListener(v -> startVoice());
    }

    // ================= MODEL =================
    public static class Control {
        public int led1, led2, door, rack;

        public Control(int l1, int l2, int d, int r) {
            led1 = l1;
            led2 = l2;
            door = d;
            rack = r;
        }
    }

    // ================= ENABLE =================
    private void enableManual(boolean enable) {
        swLedCua.setEnabled(enable);
        swLedNha.setEnabled(enable);
        swDoor.setEnabled(enable);
        swRack.setEnabled(enable);
    }

    // ================= GET DATA =================
    private int getInt(DataSnapshot snap, String path) {
        Integer val = snap.child(path).getValue(Integer.class);
        return val != null ? val : 0;
    }

    private boolean getBool(DataSnapshot snap, String path) {
        Boolean val = snap.child(path).getValue(Boolean.class);
        return val != null && val;
    }

    // ================= VOICE =================
    private void startVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN");
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        if (req == 1 && res == RESULT_OK && data != null) {
            ArrayList<String> rs = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (rs == null || rs.isEmpty()) return;

            String cmd = rs.get(0).toLowerCase(Locale.ROOT);

            if (cmd.contains("tắt hết")) btnAllOff.performClick();
            else if (cmd.contains("mở cửa")) set("door",1);
            else if (cmd.contains("đóng cửa")) set("door",0);
            else if (cmd.contains("mở giàn")) set("rack",1);
            else if (cmd.contains("thu giàn")) set("rack",0);
            else if (cmd.contains("bật đèn cửa")) set("led1",1);
            else if (cmd.contains("tắt đèn cửa")) set("led1",0);
            else if (cmd.contains("bật đèn nhà")) set("led2",1);
            else if (cmd.contains("tắt đèn nhà")) set("led2",0);
        }
    }

    private void set(String key, int val){
        db.child("control").child(key).setValue(val);
    }
}