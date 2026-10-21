# Docker, CI và demo Render

## Bố trí hai repository

```sh
git clone https://github.com/daohuynh05/Smart-Management-System-Car-Wash.git backend
git clone https://github.com/daohuynh05/FE-SmartWashCar.git frontend
cd backend
cp .env.example .env
```

Cần đưa các thay đổi Docker/CI trong **cả hai repo** lên GitHub. Compose mặc định tìm frontend ở `../frontend`; nếu dùng tên thư mục khác, sửa `FRONTEND_PATH` trong `.env`.

## Chạy demo bằng Docker Compose

Yêu cầu Docker Engine/Desktop với Compose v2 hỗ trợ `up --wait`.

1. Sửa `POSTGRES_PASSWORD` và `JWT_SECRET` trong `.env`. Tạo JWT secret bằng `openssl rand -base64 32`. Trên PowerShell: `[Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))`.
2. Chạy `docker compose up --build --wait --wait-timeout 300`.
3. Mở http://localhost:3000. Frontend chuyển `/api` và `/ws` vào backend qua nginx; PostgreSQL và backend chỉ mở trong mạng Docker.
4. Xem trạng thái: `docker compose ps`; xem log: `docker compose logs -f backend`.
5. Kiểm tra đăng nhập và phân quyền: `python scripts/smoke_demo.py http://localhost:3000`.

Đổi cổng bằng `FRONTEND_PORT`; đồng thời cập nhật `ALLOWED_ORIGINS` theo URL truy cập thực tế. File `.env` không được commit.

| Vai trò | Email đăng nhập | Số điện thoại giả | Mật khẩu mặc định |
| --- | --- | --- | --- |
| Customer | customer@example.invalid | 0000000001 | CustomerDemo!2026 |
| Admin | admin@example.invalid | 0000000002 | AdminDemo!2026 |

Seeder dùng BCrypt, chỉ chạy khi bật profile `demo`, tạo thêm xe `DEMO-0001`, dịch vụ mẫu và cấu hình mặc định. Chạy lại không nhân đôi tài khoản/xe hoặc ghi đè mật khẩu đã đổi. `DEMO_CUSTOMER_PASSWORD` và `DEMO_ADMIN_PASSWORD` chỉ có tác dụng khi tạo mới tài khoản.

Dừng: `docker compose down` (giữ dữ liệu demo). **Xóa toàn bộ dữ liệu demo và tạo lại**: `docker compose down --volumes`, sau đó chạy lại lệnh `up`. Chỉ thực hiện trong đúng project `autowash-demo`.

## CI khi mở/cập nhật pull request

- Backend: Java 21, PostgreSQL 16 tạm thời, `bash mvnw -B -ntp verify`, build Docker image và lưu báo cáo Surefire.
- Backend `compose-smoke`: dựng đủ ba dịch vụ, kiểm tra đăng nhập Customer/Admin, profile và cấm Customer truy cập dashboard Admin qua nginx.
- Frontend: Node 22, `npm ci`, `npm test`, `npm run build` (bao gồm TypeScript), build Docker image.
- Chạy trên `pull_request`, push `main` và `workflow_dispatch`; chỉ cần quyền đọc repo, không cần secret production.
- `compose-smoke` lấy frontend từ nhánh `main` của repo frontend. **Merge phần frontend trước**, rồi chạy lại CI backend; nếu thử nhánh khác, sửa `ref` của bước checkout frontend.
- Workflow Azure cũ chỉ chạy khi đặt repository variable `ENABLE_AZURE_DEPLOY=true`, URL API lấy từ variable `VITE_API_BASE_URL`. CI mới luôn hoạt động độc lập với Azure.
- Có thể bật branch protection yêu cầu các job trên trước khi merge. Workflow chưa chạy trên GitHub cho đến khi các file được push.

Chạy test backend ngoài Docker với database **riêng** `autowash_test`: đặt `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`, `TEST_DATABASE_PASSWORD`, rồi `bash mvnw -B -ntp verify`. Test tích hợp dùng profile `demo,test`; không cập nhật dữ liệu lịch đặt của database cấu hình dev như test cũ.

## Chuẩn bị deploy Render

Blueprint ở `render.yaml` tạo backend Docker, frontend Static Site và PostgreSQL demo riêng tại Singapore. Các gói được đặt `free`; kiểm tra tính khả dụng/giới hạn của gói trong dashboard trước khi tạo: https://render.com/docs/free . Theo giới hạn hiện tại, database miễn phí hết hạn sau 30 ngày; backend ngủ sau 15 phút không có truy cập. Xem nguồn Render ở trên trước khi triển khai.

1. Push/merge thay đổi của cả hai repo lên `main` (hoặc đổi `branch` và `repo` trong Blueprint nếu dùng fork).
2. Tạo tài khoản Render, kết nối GitHub và chọn **New → Blueprint**, dùng repo backend và file `render.yaml`.
3. Khi được hỏi biến cấu hình, nhập URL dự kiến của frontend cho `ALLOWED_ORIGINS` (origin HTTPS, không có `/` cuối) và URL backend + `/api` cho `VITE_API_BASE_URL`. Nếu chưa có URL cuối, điền tạm rồi cập nhật ở bước 4.
4. Sau khi Render cấp URL thật, đặt:
   - Backend `ALLOWED_ORIGINS=https://<frontend-thuc-te>.onrender.com`.
   - Frontend `VITE_API_BASE_URL=https://<backend-thuc-te>.onrender.com/api`.
   - Redeploy backend và **build/deploy lại frontend** vì biến Vite được đưa vào bundle lúc build.
5. Đợi `https://<backend>/actuator/health` trả `UP`; mở frontend, đăng nhập cả hai tài khoản trong bảng. Truy cập thẳng một route SPA rồi reload để kiểm tra rewrite.
6. Smoke test API public: `python scripts/smoke_demo.py https://<backend-thuc-te>.onrender.com`. Đăng nhập trên UI để kiểm tra thêm CORS giữa hai origin.

Blueprint tự tạo JWT secret ngẫu nhiên và lấy thông tin DB bằng `fromDatabase`. Không dùng DB Supabase cũ, không import dump hoặc thông tin khách hàng thật. Email `.invalid` và biển số `DEMO-*` đều là giả. Không cấu hình khóa gửi email thật cho demo.

Auto deploy đang tắt để bạn chủ động đưa demo lên. Có thể bật `autoDeployTrigger: checksPass` sau khi CI của hai repo đã chạy ổn. Tài liệu Blueprint: https://render.com/docs/blueprint-spec . Việc tạo file không đồng nghĩa với đã deploy; URL public chỉ có sau khi Render build và health check thành công.

## Lưu ý với database cũ

Cấu hình dev đã chuyển sang biến môi trường; thông tin DB trước đây được ghi trong repository cần được thay/thu hồi ở nhà cung cấp. Không sử dụng nó cho demo hay test.

V8 cũ có ba dòng chú thích `//` không hợp lệ với PostgreSQL. Đã sửa thành `--` để migration trên DB trống chạy được. Database từng apply V8 có thể báo checksum khác; không chạy `repair` tự động hoặc trỏ cấu hình demo/test vào database đó. Demo dùng database mới.

Dự án hiện vẫn cần Hibernate `ddl-auto: update` vì Flyway chưa bao phủ toàn bộ entity; cấu hình này chỉ phù hợp demo. Kế hoạch production cần migration bổ sung và đổi sang `validate`.

Spring Boot 4 tách auto-configuration Flyway thành starter riêng. `pom.xml` dùng `spring-boot-starter-flyway` để migration thực sự được thực thi trước JPA: https://docs.spring.io/spring-boot/how-to/data-initialization.html .
