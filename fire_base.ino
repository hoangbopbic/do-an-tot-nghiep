#include <WiFi.h>
#include <Firebase_ESP_Client.h>
#include <ESP32Servo.h>
#include <LiquidCrystal_I2C.h>

// ================= CONFIG =================
const char* ssid = "hehe";
const char* pass = "16072004";

#define API_KEY "AIzaSyXXXXXXXXXXXX"
#define DATABASE_URL "https://do-an2-eaec6-default-rtdb.asia-southeast1.firebasedatabase.app/"

// ================= FIREBASE =================
FirebaseData fbSend;
FirebaseData fbRead;
FirebaseAuth auth;
FirebaseConfig config;

// ================= LCD =================
LiquidCrystal_I2C lcd(0x27, 16, 2);

// ================= PIN =================
#define LED1 2
#define LED2 4
#define RAIN_PIN 26
#define FLAME_PIN 25
#define LIGHT_PIN 32
#define PIR_PIN 27
#define BUZZER 15
#define DOOR_PIN 13
#define RACK_PIN 12

// ================= SERVO =================
Servo doorServo, rackServo;

// ================= STATE =================
volatile int rainValue, flameValue, lightValue;
volatile bool pirState = false;
volatile bool flameAlert = false;
volatile bool isDark = false;

int mode = 0;
int lastMode = -1;
int targetDoor = 0, targetRack = 0;
int doorPos = 0, rackPos = 0;

int fb_mode = 0, fb_led1 = 0, fb_led2 = 0, fb_door = 0, fb_rack = 0;

bool fireChanged = false;

// ================= SETUP =================
void setup() {
  Serial.begin(115200);

  pinMode(LED1, OUTPUT);
  pinMode(LED2, OUTPUT);
  pinMode(BUZZER, OUTPUT);
  pinMode(RAIN_PIN, INPUT);
  pinMode(FLAME_PIN, INPUT);
  pinMode(PIR_PIN, INPUT);

  doorServo.attach(DOOR_PIN);
  rackServo.attach(RACK_PIN);

  lcd.init();
  lcd.backlight();
  lcd.print("Connecting WiFi");

  WiFi.begin(ssid, pass);
  while (WiFi.status() != WL_CONNECTED) delay(300);

  lcd.clear();
  lcd.print("WiFi OK");

  config.api_key = API_KEY;
  config.database_url = DATABASE_URL;
  config.signer.tokens.legacy_token = "YOUR_TOKEN";

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);

  lcd.clear();
  lcd.print("Firebase OK");

  // TASK
  xTaskCreatePinnedToCore(taskSensor, "sensor", 2048, NULL, 1, NULL, 0);
  xTaskCreatePinnedToCore(taskLogic, "logic", 4096, NULL, 1, NULL, 1);
  xTaskCreatePinnedToCore(taskFirebase, "firebase", 8192, NULL, 1, NULL, 0);
  xTaskCreatePinnedToCore(taskLCD, "lcd", 2048, NULL, 1, NULL, 1);
}

// ================= LOOP =================
void loop() {}

// ================= SENSOR =================
void taskSensor(void *pv) {
  unsigned long pirTimer = 0;

  while (1) {
    rainValue = digitalRead(RAIN_PIN);
    flameValue = digitalRead(FLAME_PIN);
    lightValue = analogRead(LIGHT_PIN);

    flameAlert = (flameValue == 0);
    isDark = (lightValue > 2000);

    if (digitalRead(PIR_PIN)) {
      pirState = true;
      pirTimer = millis();
    }
    if (millis() - pirTimer > 2000) pirState = false;

    vTaskDelay(50 / portTICK_PERIOD_MS);
  }
}

// ================= LOGIC =================
void taskLogic(void *pv) {
  bool lastFire = false;
  unsigned long lastMotion = 0;
  int lastLed1 = -1;
  int lastLed2 = -1;
  while (1) {
    // phát hiện chuyển mode
  if (mode != lastMode) {

    // từ AUTO -> MANUAL
    if (mode == 1) {

      // lưu trạng thái hiện tại vào fb_control
      fb_led1 = digitalRead(LED1);
      fb_led2 = digitalRead(LED2);
      fb_door = (doorPos > 45) ? 1 : 0;
      fb_rack = (rackPos < 45) ? 1 : 0;
      targetDoor = fb_door ? 90 : 0;
      targetRack = fb_rack ? 0 : 90;
    }

    lastMode = mode;
  }
    mode = fb_mode;

    int led1, led2;

    if (mode == 1) {
      led1 = fb_led1;
      led2 = fb_led2;
      targetDoor = fb_door ? 90 : 0;
      targetRack = fb_rack ? 0 : 90;
    } else {
      if (pirState) lastMotion = millis();

      led1 = (isDark && millis() - lastMotion < 3000);
      led2 = isDark;

      targetDoor = flameAlert ? 90 : 0;
      targetRack = rainValue ? 0 : 90;
    }

    if (led1 != lastLed1) {
      digitalWrite(LED1, led1);
      lastLed1 = led1;
    }

    if (led2 != lastLed2) {
      digitalWrite(LED2, led2);
      lastLed2 = led2;
    }

    // FIRE
    if (flameAlert && !lastFire) fireChanged = true;
    if (!flameAlert && lastFire) fireChanged = true;
    lastFire = flameAlert;

    // BUZZER
    if (flameAlert) tone(BUZZER, 1000);
    else noTone(BUZZER);

    // SERVO SMOOTH
    if (doorPos < targetDoor) doorPos += 3;
    else if (doorPos > targetDoor) doorPos -= 3;

    if (rackPos < targetRack) rackPos += 3;
    else if (rackPos > targetRack) rackPos -= 3;

    doorServo.write(constrain(doorPos, 0, 90));
    rackServo.write(constrain(rackPos, 0, 90));

    vTaskDelay(50 / portTICK_PERIOD_MS);
  }
}

// ================= FIREBASE =================
void taskFirebase(void *pv) {
  while (1) {

    if (Firebase.ready()) {

      // SEND SENSOR
      Firebase.RTDB.setBool(&fbSend, "home/sensors/rain", !rainValue);
      Firebase.RTDB.setBool(&fbSend, "home/sensors/flame", flameAlert);
      Firebase.RTDB.setBool(&fbSend, "home/sensors/pir", pirState);
      Firebase.RTDB.setInt(&fbSend, "home/state/door", targetDoor > 0 ? 1 : 0);
      Firebase.RTDB.setInt(&fbSend, "home/state/rack", targetRack == 0 ? 1 : 0);
      Firebase.RTDB.setInt(&fbSend, "home/state/led1", digitalRead(LED1));
      Firebase.RTDB.setInt(&fbSend, "home/state/led2", digitalRead(LED2));
      // FIRE ALERT
      if (fireChanged) {
        Firebase.RTDB.setBool(&fbSend, "home/alert/fire", flameAlert);
        if (flameAlert)
          Firebase.RTDB.setString(&fbSend, "home/alert/msg", "CO CHAY!!!");
        fireChanged = false;
      }

      // READ CONTROL
      if (Firebase.RTDB.getInt(&fbRead, "home/mode")) fb_mode = fbRead.intData();
      if (Firebase.RTDB.getInt(&fbRead, "home/control/led1")) fb_led1 = fbRead.intData();
      if (Firebase.RTDB.getInt(&fbRead, "home/control/led2")) fb_led2 = fbRead.intData();
      if (Firebase.RTDB.getInt(&fbRead, "home/control/door")) fb_door = fbRead.intData();
      if (Firebase.RTDB.getInt(&fbRead, "home/control/rack")) fb_rack = fbRead.intData();
    }

    vTaskDelay(500 / portTICK_PERIOD_MS);
  }
}

// ================= LCD =================
void taskLCD(void *pv) {
  while (1) {
    // lcd.clear();
    // lcd.setCursor(0, 0);
    // lcd.print(!rainValue ? "RAIN: YES" : "RAIN: NO");

    // lcd.setCursor(0, 1);
    // if (flameAlert) lcd.print("!!! FIRE !!!");
    // else lcd.print("SAFE");
    lcd.clear();
    
    // Dòng 1: Hiển thị Rain và Light
    lcd.setCursor(0, 0);
    // R:Y (Rain Yes) hoặc R:N (Rain No)
    lcd.print("R:");
    lcd.print(!rainValue ? "Y" : "N");
    
    // Khoảng cách và hiển thị Light sensor
    lcd.setCursor(6, 0); 
    lcd.print("L:");
    lcd.print(lightValue); // Giá trị từ analogRead(LIGHT_PIN)

    // Dòng 2: Cảnh báo cháy hoặc trạng thái hệ thống
    lcd.setCursor(0, 1);
    if (flameAlert) {
      lcd.print("!!! FIRE !!!");
    } else {
      // Bạn có thể hiện thêm Mode ở đây để dễ quản lý
      lcd.print(mode == 1 ? "MODE: MANUAL" : "MODE: AUTO");
    }
    vTaskDelay(2000 / portTICK_PERIOD_MS);
  }
}