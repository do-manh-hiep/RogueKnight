# RogueKnight

## 1. Project Overview

RogueKnight là game chiến thuật theo lượt trên máy tính, lấy cảm hứng từ cờ vua, roguelike và Shotgun King.

Người chơi điều khiển Knight, chọn hai trang bị và đối đầu với quân địch trên bàn cờ 8 × 8. Phạm vi gameplay gồm một trận đấu trên Map01.

<details>
<summary>Technology Stack</summary>

| Thành phần | Công nghệ | Phiên bản |
|---|---|---|
| Ngôn ngữ và runtime | Java — Eclipse Temurin JDK | 21 |
| Game framework | libGDX | 1.13.1 |
| Desktop backend | LWJGL3 | >=3.4.3 |
| Khởi tạo dự án | gdx-liftoff | ~ |
| Build | Gradle Wrapper | 9.8.0 |
| Kiểm thử | JUnit Jupiter | 5.10.0 |
| Test runner | JUnit Platform | 1.10.0 |
| Đọc JSON | Gson | 2.13.2 |
| Giao diện | Scene2D.ui | ~ |
| Hoạt ảnh giao diện | Scene2D Actions | ~ |
| Xử lý font | gdx-freetype | ~ |
| Font tiếng Việt | Be Vietnam Pro | ~ |
| Font số và tiêu đề pixel | VT323 | ~ |
| Render sprite | SpriteBatch | ~ |
| Vẽ highlight | ShapeRenderer | ~ |

Quy ước:

- `x.y.z`: phiên bản cố định.
- `>=x.y.z`: phiên bản tối thiểu.
- `~`: không quản lý phiên bản riêng.
- `-`: chưa xác định phiên bản.

Các thành phần thuộc libGDX dùng phiên bản theo framework.

</details>

## 2. Project Structure

Dự án tách gameplay, giao diện và launcher desktop thành ba module, dùng package gốc `rogueknight`.

```text
RogueKnight/
├── logic/                         # Gameplay và dữ liệu, pure Java
│   ├── src/main/java/rogueknight/
│   ├── src/main/resources/
│   ├── src/test/java/rogueknight/
│   ├── src/test/resources/
│   └── build.gradle
├── core/                          # Giao diện, input, render và hoạt ảnh
│   ├── src/main/java/rogueknight/
│   └── build.gradle
├── lwjgl3/                        # Launcher và đóng gói desktop
│   ├── src/main/java/rogueknight/desktop/
│   ├── src/main/resources/
│   └── build.gradle
├── assets/                        # Hình ảnh, âm thanh và font
├── .vscode/settings.json
├── .editorconfig
├── [Gradle config & wrapper files]
└── README.md
```

`[Gradle config & wrapper files]` gồm cấu hình Gradle tại root, `gradle/wrapper/`, `gradlew` và `gradlew.bat`; không phải tên thư mục.

<details open>
<summary>Where to Add Code</summary>

Chọn module theo responsibilities, rồi tạo subsystem ngay dưới package gốc:

```text
Code: <module>/src/main/java/rogueknight/<subsystem>/
Test: <module>/src/test/java/rogueknight/<subsystem>/
```

Code và test cùng subsystem dùng package `rogueknight.<subsystem>`. Chỉ tạo package khi có code; tách package con khi cần.

| Content | Location |
|---|---|
| Dữ liệu gameplay | `logic/src/main/resources/` |
| Dữ liệu test | `logic/src/test/resources/` |
| Hình ảnh, âm thanh, font | `assets/` |

</details>

### Architecture

`core` sử dụng gameplay từ `logic`; `lwjgl3` khởi chạy ứng dụng desktop.

**Module Dependency Direction**

```text
lwjgl3 → core → logic
```

<details open>
<summary>Module Responsibilities</summary>

| Module | Responsibilities | Requires |
|---|---|---|
| `logic` | State, bàn cờ, luật chơi, lượt, trang bị, hành vi quân địch và dữ liệu gameplay | Java, Gson; JUnit cho test |
| `core` | Màn hình, input, render, asset, giao diện và hoạt ảnh | `logic`, libGDX |
| `lwjgl3` | Khởi chạy, cấu hình cửa sổ và đóng gói desktop | `core`, LWJGL3 |

</details>

<details>
<summary>Module Boundaries</summary>

- `logic` không import libGDX hoặc xử lý pixel, tọa độ màn hình và sự kiện chuột/bàn phím.
- Subsystem là package trong module, không phải Gradle module riêng.
- `core` gửi request và hiển thị snapshot, event từ `logic`; không định nghĩa lại luật chơi.
- Giao diện và quân địch dùng chung nguồn luật. Query, validation và bước chọn hành động không sửa state.
- Resolver áp dụng thay đổi state; controller điều phối lượt. Hoạt ảnh chỉ hiển thị kết quả, không quyết định lượt.
- `logic` đọc JSON qua Java classpath với UTF-8 và không phụ thuộc ngược vào `core` hoặc `lwjgl3`.

</details>

## 3. Development & Build

Yêu cầu môi trường và các lệnh chạy game, kiểm thử, đóng gói.

<details open>
<summary>Requirements & Editor Setup</summary>

Cài Eclipse Temurin JDK 21 và Git. Không cần cài Gradle riêng.

Dưới đây chứa lệnh chạy trong terminal PowerShell. Chạy lệnh Gradle tại thư mục gốc repository; chọn lệnh phù hợp với việc cần làm.

```powershell
java -version               # Lệnh Kiểm tra Java
javac -version              # Lệnh Kiểm tra Java compiler
```

| Editor | Thiết lập |
|---|---|
| VS Code | Mở thư mục gốc, dùng JDK 21 |
| IntelliJ IDEA | Mở dự án Gradle, chọn JDK 21 cho project và Gradle JVM |
| Eclipse | Chạy lệnh dưới đây, import bằng Existing Gradle Project và chọn JDK 21 |

```powershell
.\gradlew.bat eclipseProject    # Lệnh Tạo metadata và bộ lọc Eclipse
```

`.editorconfig` quản lý định dạng; editor cần hỗ trợ EditorConfig.

VS Code ẩn file sinh tự động qua `.vscode/settings.json`. Gradle cấu hình bộ lọc cho Eclipse và excluded directories cho IntelliJ; cách hiển thị phụ thuộc chế độ xem của IDE.

</details>

<details open>
<summary>Development</summary>

Chạy tại thư mục gốc repository:

```powershell
.\gradlew.bat --version      # Lệnh Kiểm tra Gradle và JVM
.\gradlew.bat :lwjgl3:run    # Lệnh Chạy desktop
.\gradlew.bat :logic:test    # Lệnh Chạy test logic
.\gradlew.bat test           # Lệnh Chạy toàn bộ test
```

</details>

<details open>
<summary>Build</summary>

```powershell
.\gradlew.bat build          # Lệnh Build và chạy test
.\gradlew.bat :lwjgl3:jar    # Lệnh Tạo JAR desktop
```

Đường dẫn file JAR đầu ra (`<version>` là phiên bản dự án):

```text
lwjgl3/build/libs/RogueKnight-<version>.jar
```

```powershell
$versionLine = Get-Content .\gradle.properties | Where-Object { $_ -match '^\s*projectVersion\s*=' }    # Lệnh đọc dòng projectVersion từ gradle.properties
$projectVersion = ($versionLine -split '=', 2)[1].Trim()    # Lệnh Lấy phiên bản dự án
java -jar ".\lwjgl3\build\libs\RogueKnight-$projectVersion.jar"    # Lệnh Chạy bằng Java 21
```

Tên JAR theo `projectVersion` trong `gradle.properties`.

</details>

## 4. Task Management & Workflow

Cách quản lý task, phát triển, review, tích hợp code và sử dụng AI hỗ trợ.

### 4.1. Task Management

Sử dụng [GitHub Projects](https://github.com/users/do-manh-hiep/projects/5) với bốn cột:

```text
Backlog → To Do → In Progress → Done
```

Mỗi task có một Issue và branch riêng. Issue ghi mục tiêu, người làm, điều kiện hoàn thành và dependency nếu có.

| Cột | Trạng thái |
|---|---|
| `Backlog` | Chưa sẵn sàng triển khai |
| `To Do` | Phạm vi rõ, đủ đầu vào, không bị chặn |
| `In Progress` | Đang làm hoặc chờ review |
| `Done` | PR đã merge, đạt điều kiện hoàn thành |

<details>
<summary>Khi phát sinh công việc hoặc lỗi</summary>

- **Tạo sub-issue:** thuộc task hiện tại nhưng cần theo dõi riêng.
- **Tách sub-issue thành Issue độc lập:** vượt phạm vi task ban đầu; đưa lên Kanban, tạo branch riêng và giữ liên kết với task cũ.
- **Tạo Issue mới:** công việc độc lập ngay từ đầu.

</details>

### 4.2. Git Workflow

| Branch | Mục đích |
|---|---|
| `main` | Code ổn định để demo hoặc nộp bài |
| `feature/<tên-task>` | Phát triển tính năng |
| `fix/<tên-lỗi>` | Sửa lỗi |
| `chore/<tên-task>` | Bảo trì, cấu hình, dependency hoặc công cụ |

<details open>
<summary>Pull Request Flow</summary>

1. Tạo branch từ `main` đã cập nhật.
2. Triển khai và kiểm tra trên máy cá nhân.
3. Mở Pull Request (PR) vào `main`, liên kết Issue bằng `Closes #<số_issue>`.
4. Nhận ít nhất một approval từ thành viên khác.
5. Squash and Merge để gộp PR thành một commit trên `main`.
6. Xóa branch và chuyển task sang `Done`.

PR ghi **thay đổi gì** và **đã kiểm tra thế nào**. Thay đổi luật chơi hoặc contract cần review với người phụ trách, cập nhật tài liệu và test liên quan.

Commit mô tả ngắn gọn thay đổi; có thể tham khảo [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).

</details>

### 4.3. Development & Debugging Flow

Làm từng phần nhỏ, kiểm tra ngay và build toàn dự án trước PR.

| Context | Action |
|---|---|
| Bắt đầu task | Đọc Issue, tạo branch, chọn nơi đặt code theo mục 2 |
| Sửa logic | Thêm hoặc cập nhật test, chạy `:logic:test` |
| Sửa giao diện/input | Chạy `:lwjgl3:run`, kiểm tra thao tác và hiển thị |
| Sửa build/dependency | Chạy `build`; chạy desktop nếu ảnh hưởng ứng dụng |
| Gặp lỗi | Ghi log và bước tái hiện, sửa rồi chạy lại kiểm tra |
| Trước PR | Chạy `build`, đối chiếu điều kiện hoàn thành của Issue |
| Sửa đóng gói hoặc nạp tài nguyên | Chạy thêm JAR theo mục 3 |

```powershell
.\gradlew.bat :logic:test    # Lệnh Kiểm tra logic
.\gradlew.bat :lwjgl3:run    # Lệnh Kiểm tra desktop
.\gradlew.bat build          # Lệnh Kiểm tra toàn dự án
```

PR ghi thay đổi và kết quả kiểm tra. Lỗi hoặc công việc phát sinh được theo dõi theo mục 4.1.

<details>
<summary>Working with AI</summary>

AI hỗ trợ tìm hiểu dự án, thực hiện task và xử lý lỗi. Chỉ điền những gì bạn biết.

```text
Tôi đang làm việc với dự án RogueKnight.

Tôi muốn:
[Mô tả điều muốn hiểu hoặc làm được.]

Tôi đã làm:
[Ghi thao tác hoặc lệnh đã chạy; bỏ qua nếu chưa làm gì.]

Thông tin liên quan:
[Dán code, thông báo lỗi, ảnh hoặc tài liệu nếu có.]

Hãy giải thích bằng lời dễ hiểu:
1. Phần này dùng để làm gì và nằm ở đâu trong dự án?
2. Tôi cần làm gì tiếp theo? Ghi rõ từng bước, file cần mở
   và nơi chạy lệnh.
3. Sau mỗi bước, tôi kiểm tra kết quả như thế nào?

Giải thích thuật ngữ mới.
Nếu thiếu thông tin, chỉ rõ cần tìm ở đâu và gửi gì.
Không tự đoán code hiện có hoặc thay đổi luật chơi.
```

Đọc lại thay đổi và kiểm tra kết quả. Nếu chưa hiểu một bước, yêu cầu giải thích trước khi tiếp tục.

</details>

## 5. Testing & Debugging

Các cách kiểm tra luật chơi, giao diện và tìm nguyên nhân lỗi.

<details>
<summary>Automated Tests</summary>

Test trong `logic/src/test/java/` chạy không cần cửa sổ game.

Test gameplay đối chiếu các trường hợp trong Technical Design.

```powershell
.\gradlew.bat :logic:test --rerun-tasks    # Lệnh Chạy lại test logic
```

Đường dẫn báo cáo test:

```text
logic/build/reports/tests/test/index.html
```

`UP-TO-DATE` có thể dùng kết quả cũ. `NO-SOURCE` nghĩa là không có source để chạy task, không phải test đã đạt.

</details>

<details>
<summary>Manual Tests</summary>

Kiểm tra ứng dụng mở và đóng bình thường. Với các tính năng đã triển khai, kiểm tra:

- Bàn cờ, tọa độ, click, hover và highlight.
- Font tiếng Việt, hoạt ảnh, resize và click liên tiếp.
- Chuyển màn hình, cảnh báo mắc kẹt, kết quả và restart.

Test logic không thay thế kiểm tra giao diện.

</details>

<details>
<summary>Debugging</summary>

1. Ghi lệnh, log và bước tái hiện.
2. Thu nhỏ lỗi thành trường hợp đơn giản.
3. Đối chiếu code và luật trong Technical Design.
4. Sửa nguyên nhân, chạy lại trường hợp lỗi và test liên quan.

```powershell
.\gradlew.bat build --stacktrace    # Lệnh Xem stack trace khi build lỗi
```

| Loại lỗi | Điểm kiểm tra |
|---|---|
| Compile | Package, import, chữ ký API |
| Gradle hoặc JVM | JDK, Wrapper, dependency |
| Không tìm thấy test | Vị trí test, import Jupiter, `useJUnitPlatform()` |
| Gameplay | Dữ liệu test, request, state trước/sau, lượt |
| Input hoặc hiển thị | Tọa độ, request, snapshot, event |
| Asset hoặc JSON | Đường dẫn, classpath, UTF-8, dữ liệu |

</details>

## 6. Code Conventions

Quy tắc viết và tổ chức code nhất quán giữa các thành viên.

<details>
<summary>Code Conventions</summary>

### Naming

| Thành phần | Quy ước | Ví dụ |
|---|---|---|
| Class, interface, enum | PascalCase | `BattleController` |
| Method, biến, tham số | camelCase | `currentTurn` |
| Hằng số | UPPER_SNAKE_CASE | `BOARD_SIZE` |
| Package | Chữ thường | `equipment` |
| File Java | Trùng tên type | `BattleController.java` |
| Test class | Hậu tố `Test` | `BattleControllerTest` |
| File tài nguyên | snake_case | `knight_idle.png` |

### Formatting

- Định dạng theo `.editorconfig`.
- Dùng formatter của IDE; import từng class, không dùng wildcard `*`.
- Comment giải thích lý do hoặc luật khó hiểu, tránh lặp lại code.

### Organization

- Tuân thủ module boundary, bảo vệ state, chỉ công khai API cần thiết.
- Query và validation không sửa state; giao diện không định nghĩa lại luật chơi.
- Test độc lập, có kết quả mong đợi rõ ràng.
- Thay đổi behavior hoặc API đi kèm cập nhật thiết kế và test.
- Asset có nguồn, ghi công và giấy phép theo yêu cầu.

</details>

## 7. References & Documents

Tài liệu thiết kế, hướng dẫn triển khai và nguồn tham khảo.

<details open>
<summary>Project Documents</summary>

| Tài liệu | Nội dung |
|---|---|
| `RogueKnight_TD_v0.1_Full.pdf` | Yêu cầu, kiến trúc, state, contract, gameplay và kiểm thử |
| `RogueKnight_HuongDan_TrienKhai_TD_v0.1.pdf` | Triển khai theo task, kiểm thử và xử lý lỗi |
| `[Handoff] RogueKnight_Project_Skeleton_Handoff.pdf` | Skeleton, build, test và module boundary |

Phiên bản công nghệ theo mục Technology Stack; thông tin phiên bản trong tài liệu cũ có thể khác.

</details>

<details>
<summary>Related Documents</summary>

- [GitHub Project](https://github.com/users/do-manh-hiep/projects/5)
- [.editorconfig](.editorconfig): định dạng code.
- [VS Code settings](.vscode/settings.json): cấu hình editor.

Thông tin ghi công và nội dung giấy phép của tài nguyên được tổ chức trong `CREDITS.md` và `licenses/` khi bổ sung asset.

</details>

<details>
<summary>Official Resources</summary>

- [Eclipse Temurin](https://adoptium.net/)
- [libGDX](https://libgdx.com/)
- [LWJGL](https://www.lwjgl.org/)
- [gdx-liftoff](https://github.com/libgdx/gdx-liftoff)
- [Gradle](https://docs.gradle.org/)
- [JUnit 5](https://docs.junit.org/5.10.0/user-guide/)
- [Gson](https://github.com/google/gson)
- [EditorConfig](https://editorconfig.org/)
- [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)

</details>
