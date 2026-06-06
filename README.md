# Hệ thống Điều khiển và Giám sát (Control and Monitoring System)

<p align="center">
  <!-- Thay bằng link ảnh sơ đồ tổng quan hoặc ảnh chụp sản phẩm thực tế của bạn nếu có -->
  <img width="1280" height="721" alt="z7803274611199_d4efd4d4b30f07bb19c2d610544f0a2b" src="https://github.com/user-attachments/assets/6e563405-9a4d-419f-9a5f-3755bb11afd5" />
  <img width="1280" height="721" alt="z7803274583589_848a5aeeb4162268806a1653d9721667" src="https://github.com/user-attachments/assets/2a645b5d-f269-4e38-adfc-3a7b5738bce2" />
  <img width="1280" height="721" alt="z7800343617584_ef0c5234fb4ae136e78efc19f524c69f" src="https://github.com/user-attachments/assets/7fe9acfd-5ebb-4203-b99d-3531f599ed90" />


</p>

Dự án nghiên cứu và xây dựng hệ thống điều khiển, giám sát thiết bị thông qua vi điều khiển ESP32 kết hợp với ứng dụng di động Android và cơ sở dữ liệu đám mây thời gian thực.

---

## 📌 Tính năng hệ thống & Giải pháp Kỹ thuật

* **Giám sát môi trường thời gian thực (Real-time Monitoring):**
  * Tự động phát hiện nguy cơ hỏa hoạn thông qua cụm cảm biến đầu vào.
  * Thu thập dữ liệu liên tục từ cảm biến chuyển động và cảm biến mưa để đưa ra phản hồi trạng thái chính xác về trung tâm.
  * Đồng bộ hóa dữ liệu lên đám mây với độ trễ thấp dưới 1 giây, đảm bảo người dùng nhận được thông tin trạng thái mới nhất.
* **Điều khiển chấp hành & Phản hồi khẩn cấp:**
  * Hỗ trợ điều khiển thiết bị ngoại vi.
  * Tự động kích hoạt cơ cấu chấp hành động cơ SERVO ngay khi các cảm biến đạt ngưỡng cảnh báo nguy hiểm mà không cần can thiệp thủ công.
  * Cho phép người dùng ra lệnh điều khiển trực tiếp hoặc giám sát trạng thái thiết bị từ xa thông qua ứng dụng di động độc lập.

---
## 🤖 Kịch bản Tự động hóa & Logic Hoạt động (Automation Logic)

Hệ thống được thiết kế vận hành dựa trên sự phối hợp chặt chẽ giữa các cảm biến đầu vào (Inputs) và các cơ cấu chấp hành đầu ra (Outputs), xử lý thông qua thuật toán nhúng trên ESP32:

### 1. Bảng cấu hình Logic hệ thống

| Tín hiệu đầu vào (Cảm biến) | Điều kiện môi trường | Cơ cấu chấp hành (Đầu ra) | Trạng thái thiết bị |
| :--- | :--- | :--- | :--- |
| **Cảm biến PIR** + **Quang trở (LDR)** | Có người **VÀ** Trời tối | Đèn cửa (LED/Relay) | **BẬT (Sáng)** |
| **Quang trở (LDR)** | Trời tối | Đèn phòng (LED/Relay) | **BẬT (Sáng)** |
| **Cảm biến mưa** | Phát hiện có mưa | Động cơ SERVO Dàn phơi | **ĐÓNG dàn phơi** |
| **Cảm biến lửa (Flame)** | Phát hiện có hỏa hoạn | Động cơ SERVO Cửa + Còi chip | **MỞ CỬA** đồng thời **CÒI KÊU CẢNH BÁO** |
| **Ứng dụng Android (Qua Firebase)** | Người dùng ra lệnh trên App | Động cơ SERVO Cửa / Dàn phơi | **MỞ/ĐÓNG** theo yêu cầu |

---

### 2. Mô tả thuật toán xử lý (Flowchart Logic)

Quá trình quét và xử lý dữ liệu của vi điều khiển ESP32 được thực hiện liên tục theo chu trình (Vòng lặp `loop()`):

* **Ưu tiên 1 (Cảnh báo khẩn cấp):** Hệ thống liên tục kiểm tra Cảm biến lửa. Nếu phát hiện cháy, ngay lập tức kích hoạt ngắt để **Mở cửa** (đảm bảo lối thoát hiểm) và **Hú còi báo động**, bỏ qua các điều kiện thông thường khác.
* **Ưu tiên 2 (Tự động hóa theo môi trường):** * Kiểm tra Cảm biến mưa -> Nếu mưa, điều khiển Servo đóng dàn phơi lại.
  * Kiểm tra Quang trở -> Nếu trời tối, tự động bật đèn phòng. Nếu đồng thời có người đi qua vùng quét của cảm biến PIR, bật thêm đèn cửa.
* **Ưu tiên 3 (Điều khiển thủ công qua App):** Khi nhận được tín hiệu thay đổi trạng thái từ Firebase Realtime Database (do người dùng bấm nút trên App Android), ESP32 sẽ điều khiển Servo quay góc tương ứng để Đóng/Mở cửa hoặc Dàn phơi theo ý muốn.
## 🧠 Kiến trúc Phần mềm Nhúng & Đa nhiệm (FreeRTOS)

Dự án sử dụng hệ điều hành thời gian thực **FreeRTOS** tích hợp sẵn trên ESP32 để chia nhỏ hệ thống thành 4 tác vụ (`Tasks`) độc lập, phân bổ chạy song song trên cả 2 nhân (`Dual-Core`) của vi điều khiển nhằm tối ưu hóa hiệu năng và tránh hiện tượng nghẽn dòng dữ liệu:

* **Nhân 0 (Core 0) - Xử lý truyền thông và Đọc dữ liệu thô:**
    * `taskSensor` (Độ ưu tiên: 1): Quét dữ liệu liên tục từ các cảm biến (PIR, Flame, Rain, LDR) mỗi 50ms, áp dụng thuật toán chống nhiễu tín hiệu.
    * `taskFirebase` (Độ ưu tiên: 1): Đồng bộ hai chiều với Firebase Realtime Database mỗi 500ms (Đẩy trạng thái cảm biến lên cloud và đọc lệnh điều khiển từ App về).
* **Nhân 1 (Core 1) - Xử lý Logic chấp hành và Giao diện UI:**
    * `taskLogic` (Độ ưu tiên: 1): Xử lý bộ lọc chế độ (Auto/Manual), tính toán góc quét động cơ mượt mà (**Smooth Servo**) để bảo vệ nhông, kích hoạt còi báo động khẩn cấp mỗi 50ms.
    * `taskLCD` (Độ ưu tiên: 1): Cập nhật trạng thái trực quan và phân cấp độ ánh sáng hiển thị lên màn hình LCD 16x2 I2C.

---

## 📌 Sơ đồ chân kết nối cấu hình (Pinout Mapping)

Hệ thống mạch nguyên lý được thiết kế trên phần mềm **Altium Designer** và kết nối phần cứng dựa theo sơ đồ chân cấu hình thực tế của ESP32 như sau:

| Linh kiện ngoại vi | Chân kết nối ESP32 (Pin) | Chế độ (Direction) | Chức năng trong hệ thống |
| :--- | :--- | :--- | :--- |
| **Cảm biến Lửa (Flame)** | `GPIO 25` | INPUT (Digital) | Phát hiện bức xạ lửa (Mức Thấp `0` khi có cháy) |
| **Cảm biến Mưa (Rain)** | `GPIO 26` | INPUT (Digital) | Phát hiện trạng thái mưa môi trường |
| **Cảm biến Người (PIR)** | `GPIO 27` | INPUT (Digital) | Phát hiện chuyển động hồng ngoại của con người |
| **Quang trở (LDR)** | `GPIO 32` | INPUT (Analog) | Đọc giá trị cường độ ánh sáng môi trường |
| **Servo Cửa (Door)** | `GPIO 13` | OUTPUT (PWM) | Điều khiển góc mở cửa thoát hiểm/ra vào |
| **Servo Dàn phơi (Rack)** | `GPIO 12` | OUTPUT (PWM) | Điều khiển đóng/mở cơ cấu dàn phơi thông minh |
| **Đèn Cửa (LED 1)** | `GPIO 2` | OUTPUT (Digital) | Đèn chiếu sáng lối đi khi trời tối có người |
| **Đèn Phòng (LED 2)** | `GPIO 4` | OUTPUT (Digital) | Đèn sinh hoạt tự động bật khi trời tối |
| **Còi chip (Buzzer)** | `GPIO 15` | OUTPUT (Digital) | Hú còi báo động tần số cao khi xảy ra hỏa hoạn |
| **Màn hình LCD 16x2** | `SDA (SDA), SCL (SCL)` | I2C Bus | Hiển thị thông số giám sát và chế độ |

---
## 📱 Giao diện Ứng dụng Android

<p align="center">
  <!-- Thay thế bằng file ảnh chụp màn hình app Android của bạn trong thư mục images -->
  <img width="436" height="859" alt="z7800230215258_0a88da74b122a1e3e948907bd74f4f1f" src="https://github.com/user-attachments/assets/6aac109c-505a-4f74-816a-132cdc131d71" />
  <img width="436" height="869" alt="z7800230210347_ea3e97afaacdd57590eb8cccf6d7b368" src="https://github.com/user-attachments/assets/8429050c-6f1d-4a23-a1e6-29b9feab5a23" />
  <img width="436" height="859" alt="z7800343617109_f0cb3a74258cfc2660842c0dd077b498" src="https://github.com/user-attachments/assets/5e8139b6-0a0d-420f-b4d4-7ce101f11a1a" />

</p>
<p align="center"><i>Các màn hình chức năng chính trên ứng dụng di động</i></p>

---

## 🛠 Chi tiết Kỹ thuật & Công nghệ Tích hợp

### 1. Khối Phần cứng & Nhúng (Firmware)
* **Vi điều khiển chính:** Sử dụng dòng chip SoC **ESP32** (Kiến trúc Dual-Core, tích hợp Wi-Fi) để xử lý đa nhiệm tác vụ đọc cảm biến và truyền thông dữ liệu.
* **Chi tiết linh kiện tích hợp:**
  * *Cảm biến ngọn lửa (Flame Sensor):* Phát hiện bức xạ hồng ngoại từ lửa để cảnh báo hỏa hoạn.
  * *Cảm biến chuyển động PIR:* Phát hiện sự xâm nhập hoặc dịch chuyển của con người dựa trên tia hồng ngoại.
  * *Cảm biến mưa:* Đo lường độ ẩm bề mặt để đưa ra tín hiệu điều khiển cơ cấu dàn phơi.
  * *Động cơ SERVO & Relay:* Đóng vai trò là các cơ cấu chấp hành vật lý của hệ thống.
* **Kiến trúc phần mềm nhúng:** Sử dụng ngôn ngữ C/C++ tối ưu hóa quá trình đọc dữ liệu đầu vào với kỹ thuật chống nhiễu tín hiệu và quản lý kết nối Wi-Fi thông minh (Auto-reconnect).
* **Thiết kế mạch nguyên lý:** Hệ thống mạch được chuẩn hóa và thiết kế Layout PCB chuyên nghiệp bằng công cụ **Altium Designer**.
  
  <img width="1326" height="669" alt="image" src="https://github.com/user-attachments/assets/717d4de7-8485-4a68-91a4-cba27ffe8a18" />



### 2. Khối Ứng dụng Di động & Hệ quản trị Cơ sở dữ liệu
* **Ứng dụng Android (Mobile App):**
  * Phát triển bằng **Ngôn ngữ Java** trên môi trường **Android Studio**, tuân thủ mô hình thiết kế hướng đối tượng giúp mã nguồn tường minh, dễ bảo trì và mở rộng.
  * Thiết kế giao diện người dùng (UI/UX) trực quan, hiển thị trạng thái thiết bị rõ ràng và tối ưu hóa hiệu năng để hoạt động mượt mà.
* **Cơ sở dữ liệu Đám mây (Database & Cloud):**
  * Tích hợp **Firebase Realtime Database** làm kiến trúc lưu trữ và đồng bộ trung tâm.
  * Sử dụng cơ chế giao tiếp hướng sự kiện (Event-driven) thông qua giao thức WebSockets, giúp dữ liệu thay đổi lập tức cập nhật thời gian thực mà không cần thực hiện các truy vấn lặp đi lặp lại.

---
## 📂 Cấu trúc thư mục dự án
* `/app`: Toàn bộ mã nguồn của ứng dụng Android (Java, Layout XML).
* `/fire_base.ino`: Mã nguồn nạp cho vi điều khiển (ESP32).
* `/images`: Thư mục lưu trữ hình ảnh tài liệu minh họa cấu trúc dự án.


## 🚀 Hướng dẫn cài đặt và chạy dự án

### 1. Cấu hình Vi điều khiển
1. Mở file `fire_base.ino` bằng Arduino IDE.
2. Cài đặt các thư viện cần thiết: `WiFi.h`, `FirebaseESP32.h`.
3. Thay đổi thông tin kết nối phù hợp:
```cpp
   #define WIFI_SSID "Tên_Wifi_Của_Bạn"
   #define WIFI_PASSWORD "Mật_Khẩu_Wifi"
   #define FIREBASE_HOST "ten-project.firebaseio.com"
   #define FIREBASE_AUTH "Ma_Token_Firebase"
