# RogueKnight

## 1. Project Overview

RogueKnight là game chiến thuật theo lượt trên máy tính, lấy cảm hứng từ cờ vua, roguelike và Shotgun King.

Người chơi điều khiển Knight, chọn hai trang bị và đối đầu với quân địch trên bàn cờ 8 × 8. Phạm vi gameplay gồm một trận đấu trên Map01.

<details>
<summary>Technology Stack</summary>

| Thành phần | Công nghệ | Phiên bản |
| --- | --- | --- |
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

Dự án tách gameplay, giao diện và launcher desktop thành ba module.

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
├── assets/
├── temp/
├── .vscode/settings.json
├── .editorconfig
├── [Gradle config & wrapper files]
└── README.md
```

`[Gradle config & wrapper files]` gồm cấu hình Gradle tại root, `gradle/wrapper/`, `gradlew` và `gradlew.bat`; không phải tên thư mục.

### Architecture

`core` sử dụng gameplay từ `logic`; `lwjgl3` khởi chạy ứng dụng desktop.

Module Dependency Direction:

```text
lwjgl3 → core → logic
```

<details open>
<summary>Module Responsibilities</summary>

| Module | Responsibilities | Requires |
| --- | --- | --- |
| `logic` | State, bàn cờ, luật chơi, lượt, trang bị, hành vi quân địch và dữ liệu gameplay | Java, Gson; JUnit cho test |
| `core` | Màn hình, input, render, asset, giao diện và hoạt ảnh | `logic`, libGDX |
| `lwjgl3` | Khởi chạy, cấu hình cửa sổ và đóng gói desktop | `core`, LWJGL3 |

</details>

<details>
<summary>Where to Add Code</summary>

Chọn module theo responsibilities, rồi tạo subsystem dưới `<package gốc>` là `rogueknight`.

```text
Code:          <module>/src/main/java/<package_gốc>/<subsystem>/
Test:          <module>/src/test/java/<package_gốc>/<subsystem>/
Gameplay Data: logic/src/main/resources/
Test Data:     logic/src/test/resources/
Hình ảnh, âm thanh, font: assets/
File tạm local: temp/
```

`temp/` chứa log, báo cáo và script thử nghiệm; không commit.

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

Dưới đây chứa lệnh chạy trong terminal PowerShell. Chạy lệnh Gradle tại thư mục gốc repository; chọn lệnh phù hợp với việc cần làm.

<details open>
<summary>Requirements</summary>

Cài Eclipse Temurin JDK 21 và Git. Không cần cài Gradle riêng.

```powershell
java -version               # Lệnh Kiểm tra Java
javac -version              # Lệnh Kiểm tra Java compiler
```

</details>

<details>
<summary>Editor Setup</summary>

| Editor | Thiết lập |
| --- | --- |
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

Theo dõi task trong tài liệu hướng dẫn/triển khai hoặc [GitHub Project](https://github.com/users/do-manh-hiep/projects/5), tùy theo nguồn đang áp dụng. Khi dùng Project, các trạng thái gồm:

```text
Backlog → To Do → In Progress → Done
```

Mỗi task có branch riêng và ghi mục tiêu, người làm, dependency và DoD. Khi dùng GitHub, mỗi task được theo dõi bằng một Issue.

| Cột | Trạng thái |
| --- | --- |
| `Backlog` | Chưa sẵn sàng triển khai |
| `To Do` | Đã rõ phạm vi triển khai |
| `In Progress` | Đang làm hoặc chờ review |
| `Done` | Đạt DoD; PR đã merge nếu có thay đổi repo |

<details>
<summary>Khi phát sinh công việc hoặc lỗi</summary>

- **Tạo sub-issue:** thuộc task hiện tại nhưng cần theo dõi riêng.
- **Nâng sub-issue thành Issue độc lập:** vượt phạm vi task ban đầu; đưa lên Kanban, tạo branch riêng và giữ liên kết với task cũ.
- **Tạo Issue mới:** công việc độc lập ngay từ đầu.

</details>

### 4.2. Git Workflow

| Branch | Mục đích |
| --- | --- |
| `main` | Code ổn định để demo hoặc nộp bài |
| `feature/<tên-task>` | Phát triển tính năng |
| `fix/<tên-lỗi>` | Sửa lỗi |
| `chore/<tên-task>` | Bảo trì, cấu hình, dependency hoặc công cụ |

<details open>
<summary>Pull Request Flow</summary>

1. Tạo branch từ `main` đã cập nhật.
2. Triển khai và kiểm tra trên máy cá nhân.
3. Commit & Push, có thể tham khảo [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/).
4. Mở Pull Request (PR) vào `main`:
   - ghi **thay đổi gì** và **đã kiểm tra thế nào**
   - tham chiếu task; dùng `Closes #<số_issue>` khi PR hoàn tất Issue, `Refs #<số_issue>` khi chỉ liên quan
   - Resolve review comments
   - Nhận approval từ >=1 thành viên khác
   - chọn Squash and Merge
5. Xóa branch và chuyển task sang `Done`.

</details>

### 4.3. Development & Debugging Flow

Làm từng phần nhỏ, kiểm tra ngay và build toàn dự án trước PR.

| Context | Action |
| --- | --- |
| Bắt đầu task | Đọc nội dung task, tạo branch, chọn nơi đặt code theo mục 2 |
| Sửa logic | Thêm hoặc cập nhật test, chạy `:logic:test` |
| Sửa giao diện/input | Chạy `:lwjgl3:run`, kiểm tra thao tác và hiển thị |
| Sửa build/dependency | Chạy `build`; chạy desktop nếu ảnh hưởng runtime |
| Gặp lỗi | Ghi log và bước tái hiện, xác định nguyên nhân, sửa rồi kiểm tra lại |
| Chuẩn bị mở PR | Chạy `build`, đối chiếu DoD của task và nội dung thay đổi |
| Sửa đóng gói hoặc nạp tài nguyên | Kiểm tra JAR và tài nguyên liên quan theo mục 3 |
| Mở PR | Thực hiện theo mục 4.2 |

```powershell
.\gradlew.bat :logic:test    # Lệnh Kiểm tra logic
.\gradlew.bat :lwjgl3:run    # Lệnh Kiểm tra desktop
.\gradlew.bat build          # Lệnh Kiểm tra toàn dự án
```

PR ghi thay đổi và kết quả kiểm tra. Lỗi hoặc công việc phát sinh được theo dõi theo mục 4.1.

<details>
<summary>Working with AI</summary>

AI hỗ trợ đọc dự án, triển khai task và debug. Coding agent có thể đọc/sửa file và chạy lệnh khi có công cụ và quyền truy cập.

### Project Context

| Source | Content |
| --- | --- |
| README | Cấu trúc, lệnh, conventions và workflow |
| GDD | Yêu cầu và luật chơi |
| TD | Thiết kế, API, state ownership và flow |
| Task | Mục tiêu, phạm vi, tiến độ, dependency và DoD |
| Code, test và Git | Hiện trạng triển khai và thay đổi đang có |

Đọc phần liên quan đến task. Xác định nguồn task từ yêu cầu: tài liệu hướng dẫn/triển khai hoặc GitHub Issues được theo dõi trên Project. Nếu chưa rõ nguồn hoặc thiếu thông tin cần thiết, yêu cầu bổ sung context.

Ưu tiên CLI. Có công cụ và quyền thì thực hiện trực tiếp; nếu thiếu, cung cấp lệnh và đọc kết quả người dùng gửi trước khi tiếp tục. Dùng GitHub CLI (`gh`) cho GitHub và ripgrep (`rg`) để tìm code/file. Thiếu công cụ thì hướng dẫn cài đặt hoặc dùng công cụ tương đương; với `gh`, cần đăng nhập và có quyền phù hợp với task.

Đặt log, báo cáo và script tạm trong `temp/`; không commit. Chuyển nội dung cần giữ sang vị trí phù hợp trước khi dọn file tạm.

Với GitHub GraphQL trên PowerShell, gửi query và variables qua file JSON trong `temp/` bằng `gh api graphql --input <file>`.

Sau khi cập nhật Issue/Project, đọc lại dữ liệu để xác minh.

### Agent Workflow

| Step | Action |
| --- | --- |
| Đọc task | Xác định phạm vi, dependency và DoD; đọc tài liệu liên quan |
| Kiểm tra hiện trạng | Kiểm tra môi trường, branch, thay đổi chưa commit và code/test |
| Chuẩn bị branch | Dùng branch task hoặc tạo từ `main` đã cập nhật; giữ thay đổi đang có |
| Develop & Debug | Làm từng phần nhỏ; ghi log và bước tái hiện khi lỗi, sửa rồi kiểm tra lại |
| Validate | Kiểm tra theo mục 4.3 |
| Commit & Push | Review diff, stage đúng file, commit và push |
| Mở PR | Theo mục 4.2 |
| Resolve review comments | Đối chiếu từng nhận xét với code; sửa hoặc giải thích, kiểm tra lại và cập nhật PR |
| Merge & Update Progress | Khi được giao và đủ điều kiện mục 4.2, Squash and Merge; xác minh rồi cập nhật tiến độ |

Thực hiện đến bước được giao. Cập nhật nguồn tiến độ đang áp dụng khi bắt đầu, bị chặn, chờ review và hoàn thành.

Công việc vượt phạm vi xử lý theo mục 4.1. Nếu đầu vào thiếu hoặc mâu thuẫn, tiếp tục phần đã rõ và nêu phần cần xác nhận.

Không tự đổi luật, API dùng chung hoặc quyền sửa state để làm test pass. Thay đổi cần thiết phải thống nhất với người phụ trách.

Không đoán nội dung hoặc báo hoàn thành thao tác chưa thực hiện. Khi bị chặn, ghi rõ nguyên nhân và đầu vào/quyền truy cập cần bổ sung.

### Communication

Viết ngắn, dùng lời dễ hiểu và thuật ngữ nhất quán với README. Nêu kết quả trước, sau đó là việc cần làm và cách kiểm tra. Giải thích thuật ngữ mới; tránh lặp hướng dẫn hoặc thêm chi tiết không giúp thực hiện task.

Khi cung cấp lệnh, ghi rõ terminal, nơi chạy và kết quả mong đợi. Phân biệt lệnh, đường dẫn và ví dụ. Hướng dẫn dùng mẫu tổng quát; chỉ ghi tên file, phiên bản hoặc hiện trạng cụ thể khi cần.

### Task Prompt

```text
Task:
[Task ID/tên task và nguồn tài liệu, hoặc liên kết Issue.]

Mục tiêu:
[Kết quả cần đạt.]

Thực hiện đến:
[Code + kiểm tra / commit + push / mở PR /
resolve review comments / merge.]

Thông tin thêm:
[Phạm vi, việc đã làm, log/lỗi nếu có.]

Đọc README và thực hiện theo Working with AI.

Báo kết quả:
- Thay đổi.
- Kiểm tra và kết quả.
- Phần còn lại/bị chặn.
- Trạng thái commit, push và PR.
- Nơi đã cập nhật tiến độ.
```

Kiểm tra diff và kết quả trước khi review/merge.

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
| --- | --- |
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
| --- | --- | --- |
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
| --- | --- |
| `RogueKnight_TD_v0.1_Full.pdf` | Yêu cầu, kiến trúc, state, contract, gameplay và kiểm thử |
| - | - |

</details>

<details>
<summary>Related Documents</summary>

- [GitHub Project](https://github.com/users/do-manh-hiep/projects/5)
- [CREDITS.md](CREDITS.md)
- [licenses/](licenses/)

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
