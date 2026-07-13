# 🚗 AutoWash Pro - Dự án Quản lý Rửa xe thông minh (Loyalty & CRM)

**AutoWash Pro** là đồ án/dự án xây dựng hệ thống quản lý tiệm rửa xe máy thông minh, tập trung vào chăm sóc khách hàng (CRM) và tích điểm thành viên (Loyalty). Hệ thống giúp khách hàng đặt lịch online dễ dàng và giúp admin quản lý hàng đợi, cấu hình thăng hạng và áp dụng khuyến mãi tại quầy.

---

## 🗺️ Sơ Sơ đồ Thiết kế & UML (UML Diagrams)

Để dễ hình dung cách hệ thống vận hành, tụi mình đã vẽ các sơ đồ UML và lưu sẵn ảnh trong thư mục [doc](file:///d:/Java/autowash-pro/doc/):

### 1. Kiến trúc hệ thống (System Structure)
Sơ đồ mô tả kết nối từ Frontend SPA đến Spring Boot Backend và Database PostgreSQL:
![Sơ đồ kiến trúc](doc/SystemStructure.png)

### 2. Sơ đồ Use Case (Use Case Diagram)
Các chức năng mà Khách hàng và Admin có thể thực hiện trên hệ thống:
![Sơ đồ Use Case](doc/Usecase.png)

### 3. Sơ Sơ đồ Lớp (Entity Class Diagram)
Mô tả các thực thể JPA trong database và mối quan hệ giữa chúng:
![Sơ đồ Lớp](doc/ClassDiagram.png)

### 4. Luồng Đặt lịch & Loyalty (Sequence Diagram)
Chi tiết các bước Đặt lịch rửa xe, trừ điểm tích lũy FIFO và Hoàn lại điểm khi khách hủy lịch:
![Sơ đồ Trình tự](doc/BookingSQ.png)

### 5. Sơ đồ Hoạt động (Activity Diagrams)
Mô tả chi tiết luồng xử lý nghiệp vụ chính:
* **Tiến trình Đặt lịch rửa xe (Booking Process):**
  ![Sơ đồ Hoạt động Đặt lịch](doc/Activity_Booking.png)
* **Tiến trình Tích điểm & Thăng hạng (Loyalty Process):**
  ![Sơ đồ Hoạt động Tích điểm](doc/Activity_Loyalty.png)

---

## 📊 Bảng Đối Chiếu Yêu Cầu & Trạng Thái Thực Tế (Requirements & Gap Analysis)

Dưới đây là bảng so sánh nhanh giữa **Yêu cầu ban đầu** và **Kết quả thực tế trong code** để dễ theo dõi và đối chiếu:

| Phân hệ / Tính năng | Yêu cầu ban đầu | Trạng thái thực tế trong Code | File xử lý chính / Dẫn chứng |
| :--- | :--- | :--- | :--- |
| **Tích điểm & Thăng hạng** | • Chi tiêu tích điểm x hệ số hạng.<br>• Đếm lượt rửa để thăng hạng.<br>• Tự động nâng/hạ hạng hàng tháng. | **Xong & Cải tiến:**<br>• Tích điểm khi đơn chuyển sang `DONE` dựa trên hệ số cấu hình động.<br>• Thăng hạng dựa trên tổng số lượt rửa (`totalVisits`).<br>⚠️ **Chưa làm:** Logic tự động hạ hạng định kỳ hàng tháng. | • Tích điểm & Thăng hạng: `earnPoints()` & `checkAndUpdateTier()` trong [LoyaltyService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/LoyaltyService.java#L92-L165)<br>• Cron rà soát hạng: [LoyaltyService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/LoyaltyService.java#L368-L372) |
| **Tiêu điểm & Hết hạn** | • Điểm hết hạn sau 12 tháng theo FIFO.<br>• Đổi điểm lấy quà hoặc giảm giá. | **Xong & Thay đổi:**<br>• Điểm tự hết hạn sau 12 tháng bằng Cron chạy lúc 00:00 hàng ngày.<br>• **Thay đổi:** Bỏ đổi quà hiện vật. Điểm được dùng trực tiếp để trừ vào tiền đặt lịch (tỷ lệ **1 điểm = 100đ**).<br>• **Cải tiến:** Tự động hoàn lại điểm khi hủy lịch. | • Quét hết hạn: `expireOldPoints()` trong [LoyaltyService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/LoyaltyService.java#L330-L362)<br>• Khấu trừ & Hoàn điểm: [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L318-L363) |
| **Đặt lịch theo hạng** | • Giới hạn ngày đặt trước theo rank:<br>- Member: 7 ngày<br>- Silver: 10 ngày<br>- Gold: 12 ngày<br>- Platinum: 14 ngày | **Xong:**<br>• Chặn đặt lịch và báo lỗi nếu khách chọn ngày vượt quá giới hạn ngày của hạng (đọc động từ `TierRule` cấu hình hệ thống). | • Giá trị mặc định: [Tier.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/enums/Tier.java#L6-L13)<br>• Validate slot: `validateSchedulableSlot()` trong [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L691-L726) |
| **Khách hàng & Xe** | • Liên kết số điện thoại + biển số xe.<br>• Xem lịch sử rửa xe và lịch sử điểm. | **Xong & Cải tiến:**<br>• Liên kết `@OneToMany` giữa Khách và Xe.<br>• Giao diện hiển thị lịch sử biến động điểm trực quan.<br>• **Cải tiến:** POS Admin tự động tách thông tin ghi chú để tạo khách mới/xe mới nếu chưa có. | • Class model: [Customer.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/entity/Customer.java#L96-L101)<br>• Tách ghi chú tự tạo khách: [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L147-L215) |
| **Cấu hình & Khuyến mãi** | • Cấu hình quy tắc thăng hạng, tỷ lệ điểm.<br>• Chạy khuyến mãi phân cấp (ví dụ: mã chỉ cho rank Silver+). | **Xong:**<br>• Cấu hình động tỷ lệ tích điểm và thăng hạng trên UI Admin.<br>• Check rank của khách hàng trước khi áp dụng mã khuyến mãi. | • Giao diện Admin: [AdminConfigurationPage.tsx](file:///c:/Users/PC/Downloads/production/fe-smartwashcar/fe-autowashcar/src/features/admin/pages/admin-configuration-page.tsx)<br>• Check rank áp mã: [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L256-L287) |
| **Xếp hàng ưu tiên** | *Không có trong yêu cầu ban đầu.* | **Cải tiến thêm:**<br>• Tự động tính điểm ưu tiên (`priorityScore` từ 10 đến 40) theo hạng thành viên để ưu tiên làm xe trước khi tiệm bị quá tải. | • Gán điểm ưu tiên: [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L375) |
| **Tương tác AI** | • Tích hợp trợ lý AI tương tác khách hàng (tùy chọn). | ⚠️ **Chưa làm:**<br>• Chưa kết nối API AI (như ChatGPT/Gemini). Hiện tại mới chỉ là trang Cẩm nang bài viết (Articles) viết thủ công bằng mã HTML. | • Quản lý bài viết: [ArticleService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/ArticleService.java) & [AdminArticlesPage.tsx](file:///c:/Users/PC/Downloads/production/fe-smartwashcar/fe-autowashcar/src/features/admin/pages/admin-articles-page.tsx) |
| **Thanh toán & Hoàn tiền** | • Không có thanh toán online và hoàn tiền online. | **Xong:**<br>• Hệ thống chỉ hỗ trợ thanh toán tiền mặt tại quầy sau khi rửa xe xong. Trạng thái lịch đặt ban đầu là `PENDING` và cập nhật thủ công. | • Logic checkout tại quầy: `updateStatus(..., BookingStatus.DONE)` trong [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java#L524-L603) |

---

## 🛠️ Công Nghệ Sử Dụng (Tech Stack)

### 🖥️ Backend (autowash-pro)
* **Runtime & Framework:** Java 21 + Spring Boot 3.3.x.
* **Database:** PostgreSQL (Host trên đám mây Supabase Cloud).
* **Database Migration:** Flyway (Tự động chạy script cập nhật schema).
* **Security:** Spring Security + JWT.
* **Realtime:** WebSocket (STOMP Protocol) để đẩy thông báo/trạng thái tức thời lên Client.

### 🎨 Frontend (fe-smartwashcar)
* **Framework:** React 19 + TypeScript + Vite.
* **Styling:** Tailwind CSS v4.
* **State Management:** Redux Toolkit.
* **Icons & Animation:** Lucide-React + Framer Motion.
* **Router:** SPA Navigation (Browser History API).

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Dự Án (Installation & Setup)

### 1. Khởi chạy Backend
**Chuẩn bị:** Cài sẵn Java JDK 21 và Maven (hoặc dùng Maven Wrapper `./mvnw` đi kèm).

**Các bước chạy:**
1. Tạo một DB trống trên PostgreSQL (hoặc dùng Supabase).
2. Điền thông tin kết nối DB vào file `src/main/resources/application-dev.yml`:
   ```yaml
   spring:
     datasource:
       url: jdbc:postgresql://<HOST_DB>:6543/postgres?sslmode=require&prepareThreshold=0
       username: <USERNAME_DB>
       password: <PASSWORD_DB>
   ```
3. Chạy lệnh build:
   ```bash
   ./mvnw clean compile
   ```
4. Khởi chạy dự án:
   ```bash
   ./mvnw spring-boot:run
   ```
   *Backend sẽ chạy tại:* `http://localhost:8080`
5. Xem tài liệu API tự động (Swagger): `http://localhost:8080/swagger-ui/index.html`.

---

### 2. Khởi chạy Frontend
**Chuẩn bị:** Máy cài sẵn Node.js v18 trở lên.

**Các bước chạy:**
1. Mở terminal tại thư mục dự án frontend:
   ```bash
   cd fe-autowashcar
   ```
2. Cài các package phụ thuộc:
   ```bash
   npm install
   ```
3. Kiểm tra file `.env` ngoài thư mục gốc xem URL của Backend đã đúng chưa:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api
   ```
4. Chạy dự án ở chế độ dev:
   ```bash
   npm run dev
   ```
   *Frontend sẽ chạy tại:* `http://localhost:5173`.
5. Đăng nhập & Test:
   - Dùng cổng phát triển `/test` để chuyển đổi nhanh các màn hình giao diện.
   - Tài khoản Admin có thể cấp trực tiếp bằng cách sửa cột `is_admin = true` trong bảng `customers` dưới database.
