# Báo Cáo Tổng Hợp Thay Đổi - Khấu Trừ Đặt Lịch Bằng Điểm Tích Lũy

Báo cáo này tổng hợp tất cả các thay đổi từ Backend đến Frontend để thực hiện cơ chế mới: Điểm tích lũy được sử dụng như một dạng "tiền tệ" để khấu trừ trực tiếp vào hóa đơn đặt lịch với tỷ lệ quy đổi **1 điểm = 100 VNĐ**, và gỡ bỏ hoàn toàn phân hệ đổi quà (rewards) cũ.

---

## 1. Phía Backend (autowash-pro)

### 📂 Cấu trúc Cơ sở dữ liệu
- **[NEW]** [V7__add_points_deduction_to_bookings.sql](file:///d:/Java/autowash-pro/src/main/resources/db/migration/V7__add_points_deduction_to_bookings.sql)
  - Thêm cột `used_points` (lưu số điểm sử dụng) và `points_discount_amount` (lưu số tiền được khấu trừ) vào bảng `bookings`.

### 📂 Thực thể & Đối tượng truyền nhận (DTO)
- **[MODIFY]** [Booking.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/entity/Booking.java)
  - Khai báo thêm thuộc tính `usedPoints` và `pointsDiscountAmount`.
- **[MODIFY]** [CreateBookingRequest.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/dto/request/booking/CreateBookingRequest.java)
  - Nhận thêm biến `usedPoints` gửi lên từ Client.
- **[MODIFY]** [BookingResponse.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/dto/response/booking/BookingResponse.java)
  - Thêm `usedPoints` và `pointsDiscountAmount` vào Response gửi trả về Client và thực hiện builder mapping tương ứng từ thực thể `Booking`.

### 📂 Nghiệp vụ Logic
- **[MODIFY]** [BookingService.java](file:///d:/Java/autowash-pro/src/main/java/com/autowash/autowash_pro/service/BookingService.java)
  - **Khi tạo Booking (`createBooking`)**:
    - Kiểm tra số dư điểm thực tế của khách hàng.
    - Áp dụng tỷ lệ quy đổi: `pointsDiscountAmount = usedPoints * 100`.
    - Khấu trừ tối đa bằng số tiền hóa đơn còn lại sau khi đã áp dụng mã khuyến mãi (Promo Code).
    - Gọi hàm trừ điểm theo cơ chế **FIFO** (`deductPointsFifo`) và lưu log loại `REDEEM` vào lịch sử giao dịch điểm.
  - **Khi hủy Booking (`cancelBooking`)**:
    - Nếu Booking có sử dụng điểm, tự động **hoàn trả điểm** đã khấu trừ về tài khoản khách hàng và ghi nhận lịch sử cộng điểm hoàn.

---

## 2. Phía Client (fe-smartwashcar)

### 📂 Gọi Dịch Vụ API
- **[MODIFY]** [booking-service.ts](file:///c:/Users/PC/Downloads/production/fe-smartwashcar/fe-autowashcar/src/features/booking/services/booking-service.ts)
  - Cập nhật interface `CreateBookingRequest` để đính kèm thêm biến `usedPoints?: number`.

### 📂 Màn hình Điểm Thưởng (Loyalty Page)
- **[MODIFY]** [loyalty-page.tsx](file:///c:/Users/PC/Downloads/production/fe-smartwashcar/fe-autowashcar/src/features/client/pages/loyalty-page.tsx)
  - Sửa lại toàn bộ hiển thị font tiếng Việt lỗi.
  - Loại bỏ hoàn toàn khối giao diện hiển thị 4 món quà quy đổi cứng cũ cùng các hàm logic liên quan (`handleRedeem`, `rewardsList`). Trang này giờ chỉ tập trung hiển thị Hạng thẻ, tiến độ và lịch sử giao dịch.

### 📂 Màn hình Đặt Lịch (Booking Page)
- **[MODIFY]** [booking-page.tsx](file:///c:/Users/PC/Downloads/production/fe-smartwashcar/fe-autowashcar/src/features/booking/pages/booking-page.tsx)
  - Thêm checkbox **"Sử dụng điểm tích lũy"** ngay tại phân vùng Tổng hợp thanh toán.
  - Lấy số dư điểm của khách hàng từ Redux store và tính toán giảm trừ thời gian thực (`Tiền giảm = Số điểm × 100đ`).
  - Gửi tham số `usedPoints` lên Backend khi bấm Xác nhận đặt lịch và tự động reset trạng thái dùng điểm khi thành công.
  - Cập nhật Dialog đặt lịch thành công hiển thị rõ ràng số tiền thực trả sau khi đã khấu trừ khuyến mãi và điểm.
