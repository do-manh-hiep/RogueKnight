# RogueKnight - Project Skeleton

Dự án game RogueKnight (BTL OOP).

## 1. Yêu cầu môi trường
* **Java:** JDK 21 (Kiểm tra bằng lệnh `java -version`)
* **Build tool:** Gradle Wrapper (đã tích hợp sẵn `gradlew`, không cần cài Gradle riêng)

## 2. Các lệnh Command Line chính
* **Biên dịch toàn bộ dự án:**
  ```bash
  gradlew build

Chạy bài Unit Test (Logic thuần Java):

Bash
gradlew test

Khởi chạy game trên Desktop:

Bash
gradlew lwjgl3:run

## 3. Cấu trúc thư mục & Quy tắc ranh giới (Boundary)
* `core/src/main/java/.../logic`: Chứa toàn bộ luật chơi (`GameState`, `TurnCoordinator`, `Board`, `Piece`...)[cite: 5]. **Quy tắc bắt buộc:** Tầng này là Java thuần (Pure Java), tuyệt đối KHÔNG import thư viện `com.badlogicgames.gdx.*`[cite: 4].
* `core/src/main/java/.../presentation`: Chứa màn hình hiển thị, giao diện UI, xử lý input[cite: 5]. Được phép dùng thư viện libGDX[cite: 4].
* `core/src/test/java/.../logic`: Chứa bài kiểm thử unit test JUnit 5 cho phần logic (chạy độc lập từ dòng lệnh, không mở cửa sổ game)[cite: 4, 6].
* `lwjgl3/`: Chứa mã nguồn khởi chạy desktop launcher và cấu hình cửa sổ[cite: 5].
* `assets/`: Chứa tài nguyên game (`maps/`, `sprites/`, `ui/`, `fonts/`, `sfx/`, `music/`)[cite: 5]. Khai báo bản quyền tại `CREDITS.md` và thư mục `licenses/`[cite: 5].

## 4. Quy trình thêm tính năng mới cho thành viên
1. Viết luật chơi hoặc thực thể mới trong package `logic`[cite: 8].
2. Viết bài test tương ứng trong `src/test/java/.../logic` và chạy lệnh `gradlew test` để đảm bảo logic chạy đúng[cite: 8].
3. Kết nối hiển thị từ tầng `presentation` thông qua hợp đồng dữ liệu trả về[cite: 7].

