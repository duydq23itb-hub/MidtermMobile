# Ứng Dụng ECOMER - Quản Lý Sản Phẩm (Midterm Mobile)

Ứng dụng Android xây dựng bằng **Kotlin & Jetpack Compose**, kết nối trực tiếp với **Firebase (Authentication, Cloud Firestore, Cloud Storage)** đáp ứng đầy đủ yêu cầu bài kiểm tra:

---

## 1. Các Tính Năng Đã Triển Khai

### 1.1. Phân hệ Quản trị viên (Admin) - Đầy đủ CRUD (8.5 Điểm)
- **Giao diện chuẩn theo ảnh mẫu bài thi:**
  - Tiêu đề: **Dữ liệu sản phẩm**
  - Ô nhập: **Tên sản phẩm** (ví dụ: `Ap phong nam`)
  - Ô nhập: **Loại sản phẩm** (ví dụ: `Thoi trang nu`)
  - Ô nhập: **Giá** (ví dụ: `300000`, bàn phím số)
  - Chọn hình ảnh dạng tệp kèm biểu tượng thư mục
  - Nút bấm chính: **THÊM SẢN PHẨM** (chuyển thành **CẬP NHẬT SẢN PHẨM** + nút **HỦY** khi ở chế độ sửa)
  - Tiêu đề danh sách: **Danh sách sản phẩm:**
  - Danh sách từng thẻ sản phẩm hiển thị:
    - Ảnh thumbnail sản phẩm (bên trái)
    - Thông tin gồm: `Tên sp: ...`, `Giá sp: ...`, `Loại sp: ...`
    - Cột nút thao tác (bên phải):
      - Nút **Sửa** (Biểu tượng bút chì viền vàng)
      - Nút **Xóa** (Biểu tượng thùng rác viền đỏ, có hộp thoại xác nhận)

### 1.2. Trường Image & Lưu trữ hình ảnh (0.5 Điểm)
- Bổ sung trường `image` lưu hình ảnh.
- Hỗ trợ chọn ảnh trực tiếp từ bộ nhớ máy (`GetContent`).
- Cơ chế tải lên thông minh:
  - Tải lên **Firebase Storage** (`products/img_...jpg`) và lấy `downloadUrl`.
  - Có cơ chế dự phòng tự động chuyển sang nén Base64 nếu Firebase Storage của project chưa bật rules/bucket, đảm bảo ảnh luôn hiển thị ổn định bằng **Coil**.
- **Không tự ý thêm bất kỳ trường nào khác** vào Firestore document (chỉ lưu 4 trường: `tenSanPham`, `loaiSanPham`, `gia`, `image`).

### 1.3. Xác thực & Phân quyền User (1.0 Điểm)
- Xác thực bằng **Firebase Authentication (Email & Password)**.
- Màn hình Đăng nhập & Đăng ký:
  - Chọn vai trò khi đăng ký: **Admin** hoặc **User (Khách hàng)**.
  - Tích hợp 2 nút **Đăng nhập nhanh Admin** và **Đăng nhập nhanh User** giúp giảng viên chấm bài hoặc quay video demo ngay lập tức mà không cần gõ phím.
- **Phân quyền người dùng (Role-Based Access Control):**
  - **Admin**: Có đầy đủ form nhập liệu và quyền Thêm, Sửa, Xóa sản phẩm.
  - **User**: Xem danh sách sản phẩm, tìm kiếm sản phẩm. Ẩn form nhập và các nút Sửa/Xóa của Admin.
  - Nút **Đăng xuất** an toàn trên thanh tiêu đề.

### 1.4. Tính năng Xuất CSDL JSON
- Hỗ trợ xuất file `csdl_san_pham.json` theo đúng cấu trúc Firestore để nộp bài kiểm tra.
- Trên giao diện Admin có nút **Xuất JSON** xem trực tiếp và nút **Sao chép** vào clipboard.

---

## 2. Tài Khoản Thử Nghiệm Nhanh

| Vai trò | Email | Mật khẩu | Quyền hạn |
|---|---|---|---|
| **Admin** | `admin@ecomer.com` | `admin123` | Toàn quyền Thêm, Sửa, Xóa, Xem |
| **User** | `user@ecomer.com` | `user123` | Chỉ xem và tìm kiếm sản phẩm |

*(Hoặc bấm trực tiếp vào nút "Vào Admin" / "Vào User" trên màn hình đăng nhập).*

---

## 3. Cấu Trúc Firestore Document (`products`)
Tuân thủ nghiêm ngặt quy định không thêm trường dư thừa:
```json
{
  "tenSanPham": "Ap phong nam",
  "loaiSanPham": "Thoi trang nu",
  "gia": 300000,
  "image": "https://..."
}
```

---

## 4. Hướng Dẫn Biên Dịch & Chạy App
1. Mở dự án bằng **Android Studio**.
2. File cấu hình `app/google-services.json` đã được cài đặt sẵn.
3. Chọn thiết bị (Emulator hoặc điện thoại thật) và nhấn **Run** (`Shift + F10`).
