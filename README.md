# AutoWash Pro Backend

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-Starter-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue.svg)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Enabled-red.svg)](https://flywaydb.org/)

**AutoWash Pro** là hệ thống quản lý và đặt lịch rửa xe thông minh chuyên nghiệp (quản lý dịch vụ cho cả Ô tô và Xe máy), tích hợp công nghệ lưu trữ Supabase PostgreSQL, dịch vụ hàng đợi ưu tiên theo hạng thành viên (Loyalty Tiers), tích lũy & khấu trừ điểm trực tiếp khi đặt lịch, quản lý khuyến mãi phân cấp, và hệ thống báo cáo doanh thu trực quan cho quản trị viên.

---

## 🌟 Tính Năng Nổi Bật Hiện Tại

### 1. Loyalty Engine & Phân Hạng Thành Viên
* **Hệ thống 4 hạng thành viên**: `MEMBER` (Cấp 1) $\rightarrow$ `SILVER` (Cấp 2) $\rightarrow$ `GOLD` (Cấp 3) $\rightarrow$ `PLATINUM` (Cấp 4).
* **Tự động thăng hạng theo số lượt rửa xe (`totalVisits` / `visits`)**:
  * `< 10` lượt rửa: Hạng **MEMBER**.
  * `$\ge$ 10` lượt rửa: Lên hạng **SILVER**.
  * `$\ge$ 25` lượt rửa: Lên hạng **GOLD**.
  * `$\ge$ 50` lượt rửa: Lên hạng **PLATINUM**.
  *(Các ngưỡng thăng hạng này được cấu hình động trong Hệ thống).*
* **Tích lũy chi tiêu**: Tích lũy tổng chi tiêu thực tế (`totalSpend`) và số lượt rửa xe thành công (`totalVisits`) tự động khi đơn hàng hoàn thành (`DONE`).
* **Hệ thống trừ điểm FIFO**: Hỗ trợ khấu trừ điểm thưởng trực tiếp khi đặt lịch để giảm trừ tiền mặt (Quy đổi: 1 điểm = 100đ). Hỗ trợ quản lý điểm hết hạn sau 12 tháng theo cơ chế FIFO.

### 2. Hệ Thống Đặt Lịch (Booking System)
* **Cửa sổ đặt lịch thông minh theo hạng**:
  * `MEMBER`: Đặt trước tối đa 7 ngày.
  * `SILVER`: Đặt trước tối đa 10 ngày.
  * `GOLD`: Đặt trước tối đa 12 ngày.
  * `PLATINUM`: Đặt trước tối đa 14 ngày.
* **Hàng đợi ưu tiên**: Điểm ưu tiên (`priorityScore`) được tính toán tự động dựa trên hạng thành viên của khách hàng để tối ưu hóa thứ tự phục vụ tại tiệm.
* **Đa dịch vụ & Gói Combo**: Hỗ trợ đặt một hoặc nhiều dịch vụ đơn lẻ hoặc các gói combo chăm sóc xe chuyên sâu cùng lúc.

### 3. Quản Lý Khuyến Mãi Phân Cấp (Hierarchical Promotions)
* **Khuyến mãi đại trà**: Áp dụng chung cho tất cả các thành viên (MEMBER, SILVER, GOLD, PLATINUM).
* **Khuyến mãi độc quyền**: Cấu hình giới hạn chỉ hiển thị và cho phép áp dụng đối với khách hàng đạt hạng thành viên tối thiểu yêu cầu (Ví dụ: Mã chỉ dành riêng cho hạng SILVER trở lên).

---

## 🛠️ Công Nghệ & Kiến Trúc Kỹ Thuật

* **Ngôn ngữ & Framework**: Java 21 + Spring Boot + Spring Data JPA.
* **Cơ sở dữ liệu**: PostgreSQL (Host trên Supabase Cloud).
* **Quản lý database migration**: Flyway (Quét và chạy tự động trên classpath `db/migration`).
* **Bộ nhớ đệm**: Redis (Hỗ trợ cache phiên làm việc và OTP).
* **Bảo mật**: Spring Security + JWT Stateless Authentication.
* **Gửi thông báo & OTP**: Tích hợp API Resend để gửi Email OTP đăng ký/đăng nhập và thông báo biến động điểm số.
* **Thời gian thực**: WebSocket (STOMP Protocol) để đẩy thông tin cập nhật trạng thái đơn hàng.

---

## 📁 Cấu Trúc Mã Nguồn

```
src/main/java/com/autowash/autowash_pro/
├── config/        Cấu hình Spring Security, CORS, JWT, Redis, WebSocket, Swagger
├── controller/    Các Endpoint REST API (Auth, Customer, Booking, Vehicle, Admin, Service, Promotion)
├── service/       Xử lý nghiệp vụ (BookingService, LoyaltyService, CustomerService, v.v.)
├── repository/    Tương tác cơ sở dữ liệu (Spring Data JPA)
├── entity/        Các thực thể JPA (Customer, Vehicle, Booking, WashHistory, CustomerPoints, Promotion, Services)
├── dto/           Lớp vận chuyển dữ liệu Request/Response (Auth, Booking, Customer, Promotion, Service)
├── enums/         Các kiểu Enum định nghĩa trạng thái và hạng (BookingStatus, Tier, PointType, PromoType)
└── exception/     Quản lý lỗi tập trung (GlobalExceptionHandler)
```

---

## 🗃️ Lịch Sử Migrations Cơ Sở Dữ Liệu (Flyway)

Tọa lạc tại `src/main/resources/db/migration/`:
* `V1__init_schema.sql`: Khởi tạo cấu trúc bảng cơ bản (`customers`, `vehicles`, `bookings`, `customer_points`, `promotions`, v.v.).
* `V2__loyalty_tier_config.sql`: Khởi tạo cấu hình hạng thành viên và nhân tố tích lũy điểm.
* `V3__add_services_and_combos.sql`: Thêm bảng quản lý dịch vụ (`services`, `booking_services`).
* `V4__fix_null_service_values.sql`: Bổ sung ràng buộc dữ liệu dịch vụ.
* `V5__add_promotion_to_bookings.sql`: Liên kết mã khuyến mãi vào thực thể đặt lịch.
* `V6__add_max_discount_to_promotions.sql`: Thêm giới hạn tiền giảm giá tối đa cho mã khuyến mãi.
* `V7__add_points_deduction_to_bookings.sql`: Bổ sung trường điểm khấu trừ và số tiền giảm trừ tương ứng khi đặt lịch.
* `V8__sync_customer_stats.sql`: Tự động điền bù dữ liệu tổng tiền hóa đơn cũ bị NULL và đồng bộ hóa tổng chi tiêu (`total_spend`) cùng số lần rửa (`total_visits`) cho khách hàng cũ.

---

## 🚀 Hướng Dẫn Cài Đặt và Khởi Chạy

### Yêu Cầu Hệ Thống
* Java JDK 21 trở lên.
* Maven 3.x (Đã đi kèm Wrapper `./mvnw`).

### Các Biến Môi Trường Cần Thiết (Thiết lập trong File Cấu Hình hoặc Environment)
```properties
spring.datasource.url=jdbc:postgresql://<HOST_SUPABASE>:6543/postgres?sslmode=require&prepareThreshold=0
spring.datasource.username=<USER_SUPABASE>
spring.datasource.password=<PASSWORD_SUPABASE>
spring.data.redis.host=<REDIS_HOST>
spring.data.redis.port=<REDIS_PORT>
RESEND_API_KEY=<KEY_RESEND>
JWT_SECRET=<YOUR_SECRET_KEY>
```

### Khởi Chạy Ứng Dụng
1. Biên dịch dự án:
   ```bash
   ./mvnw clean compile
   ```
2. Khởi chạy Server:
   ```bash
   ./mvnw spring-boot:run
   ```
   Ứng dụng sẽ khởi chạy mặc định tại cổng `http://localhost:8080`.
3. Xem tài liệu API (Swagger UI): Truy cập `http://localhost:8080/swagger-ui/index.html` sau khi Server khởi chạy thành công.
