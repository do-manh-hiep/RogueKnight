# Logic audit — 27/11/2026

- **Repository:** do-manh-hiep/RogueKnight
- **Branch audited:** refactor/architecture-cleanup-7119
- **Audit baseline:** 28e3f4348010566604bff6e0efff629d6cf1c27c
- **Scope:** source và automated tests của module logic; các điểm tích hợp trực tiếp với core/Main được đọc để xác định error boundary theo DEC-12.
- **TD:** RogueKnight TD v0.1 được giữ nguyên. Tài liệu này là audit note bổ sung, không thay thế/cập nhật TD.
- **Method:** static source/test review trên branch; không thực thi code trong môi trường audit này.

## 1. Kết luận điều hành

**Logic audit source-level: hoàn tất vòng rà soát hiện tại; chưa đủ điều kiện tuyên bố implementation đạt DEC-04/DEC-12.**

Các nhóm gameplay chính có phân lớp và test khá đầy đủ: Board/BattleState, Player action validation/resolution, Enemy rules/AI/turn runner, equipment actions, promotion/scheduler và terminal evaluation. Không phát hiện trong các source đã rà soát một lỗi gameplay cụ thể đủ căn cứ để đề xuất thay đổi luật hoặc AI policy.

Hai quyết định đã Approved vẫn còn nợ triển khai ở boundary:
- DEC-04: aggregate có ownership đúng về mặt mô hình, nhưng public mutable API và EnemyPhase boundary vẫn cho phép caller bên ngoài controller mutate BattleState.
- DEC-12: logic runner fail-fast với một AI Act invalid, nhưng fail-stop chưa được hoàn tất ở application boundary; lỗi hiện có thể thoát khỏi luồng input/render mà không được chuyển thành runtime-failure state/UI. Ngoài ra null decision hiện bị xem như Skip.

Vì vậy, audit logic có thể được đóng như một **review/report phase**, nhưng không thể đánh dấu DEC-04/DEC-12 implementation là Done. Các gap được chuyển thành implementation tasks riêng, không âm thầm sửa code trong audit này.

## 2. Findings

### AUD-01 — Public mutable state API chưa được cô lập (P0, DEC-04)

**Evidence**
- `BattleState` là aggregate và giữ Board, PieceState map, Inventory, turn, phase, result.
- Tuy nhiên constructor và các mutation method `addPiece`, `movePiece`, `removePiece`, `replacePiece`, `advanceTurn`, `setPhase`, `finish`, `consumeEquipment` đều public.
- `Board` cũng là public class với constructor và mutation method `place`, `move`, `remove` public.
- `BattleController` có public constructor nhận `BattleState`; public `EnemyPhase.run(BattleState)` chuyển mutable aggregate cho implementation được inject.
- `EnemyTurnRunner.run(BattleState)` là public theo interface; test stub của EnemyPhase cũng mutate trực tiếp state.
- `BattleFactory.createMap01Controller(...)` đã tránh trả aggregate ra ngoài, nhưng các constructor/API công khai trên vẫn là đường bypass.

**Đánh giá**
- Ownership được mô tả và tập trung về một aggregate, nhưng chưa được enforce bằng API boundary.
- Đây là vấn đề kiến trúc/encapsulation theo DEC-04, không phải bằng chứng state hiện đang lệch trong gameplay bình thường.
- Không nên chỉ đổi visibility theo từng method trước khi xác định package/module cần quyền nội bộ; Factory, Resolver, EnemyTurnRunner và test fixtures hiện có package boundary khác nhau.

**Hướng xử lý**
- Audit call sites và chia quyền: factory/bootstrap, controller, resolver/runner, rules/AI, core/UI, tests.
- Thiết kế internal mutation capability/command port hoặc tái tổ chức package phù hợp; caller UI/AI không nhận mutable aggregate.
- Giữ ActionPlan và ActionResolver nội bộ; không tạo đường gọi resolver bỏ qua validation/controller.

**Test cần bổ sung**
- Compile/API boundary test hoặc module/package boundary test chứng minh core và AI không thể gọi mutation API.
- Contract test chứng minh chỉ controller/application command flow có thể áp dụng action.
- Giữ các test invariant/snapshot/invalid request hiện tại.

### AUD-02 — DEC-12 chưa có application-level fail-stop boundary (P1, DEC-12)

**Evidence**
- `EnemyTurnRunner.resolveEnemyAction` ném `IllegalStateException` khi `EnemyRules.validate` từ chối request do AI sinh ra; message có request, error và global turn.
- Exception không được chuyển thành một runtime-failure result/state riêng ở `BattleController`.
- `BattleController.submit` gọi `enemyPhase.run(state)` trực tiếp; nếu runner ném, submit không trả `ActionResult`. Player action và Turn+1 đã commit trước khi Enemy phase bắt đầu; phase có thể còn ở `ENEMY_RESOLVE`.
- `Main.render` gọi input handler rồi playback; không có catch/error route cho runtime AI failure. `Main.create` chỉ catch `IOException | IllegalArgumentException` quanh bootstrap.
- Chưa thấy flow khóa input, dừng queue, hiển thị runtime failure, hoặc điều hướng về menu/restart khi lỗi AI xảy ra.

**Đánh giá**
- Fail-fast ở tầng logic có thật; fail-stop end-to-end chưa có bằng chứng triển khai.
- Theo DEC-12 đã chốt, action hợp lệ trước lỗi được giữ nguyên, request invalid không apply, trận phải dừng và không được coi là Victory/Defeat.
- Không được nuốt exception hoặc để một trận ở trạng thái ENEMY_RESOLVE có vẻ còn chơi tiếp.

**Hướng xử lý**
- Thêm error boundary ở application/controller integration để nhận diện AI invalid-request failure, giữ lại context và chuyển game flow sang runtime-failure state.
- Dừng enemy queue và playback/input; hiển thị lỗi đủ thông tin; cho phép menu/restart.
- Không rollback action đã commit trước lỗi; không thêm Victory/Defeat giả.

**Test cần bổ sung**
- Invalid request ở activation đầu: request không apply, queue dừng.
- Invalid request sau ít nhất một enemy action hợp lệ: action trước được giữ, request lỗi không apply, không có activation tiếp theo.
- Integration test xác nhận runtime failure không bị phân loại Victory/Defeat, input/playback dừng và có đường thoát.
- Kiểm tra exception context có enemy ID, turn, phase, request và error code.

### AUD-03 — Null EnemyDecision đang bị hiểu thành Skip (P1, DEC-12)

**Evidence**
- Trong `EnemyTurnRunner.activate`, điều kiện `if (!(decision instanceof EnemyDecision.Act act)) return false` khiến mọi giá trị không phải Act đều kết thúc activation như Skip.
- Nếu policy trả null do bug, null cũng đi vào nhánh này; schedule vẫn được hoàn tất như Skip bình thường.

**Đánh giá**
- DEC-12 hiện nói AI sinh request invalid là bug; null decision không phải ActionRequest invalid theo nghĩa hẹp, nhưng là kết quả policy hỏng và không nên bị âm thầm coi là quyết định Skip.
- Skip hợp lệ cần là một giá trị domain rõ ràng, không phải null hoặc object không được nhận diện.

**Hướng xử lý**
- Phân nhánh tường minh Act và Skip; null/decision không hợp lệ phải fail-fast theo cùng error boundary.
- Không thay đổi hành vi Skip hợp lệ.

**Test cần bổ sung**
- Policy trả null phải tạo runtime failure, không tăng lịch như Skip và không chạy activation tiếp theo.
- Skip tường minh vẫn hoàn tất lịch theo rule hiện tại.

### AUD-04 — Config validation có, nhưng setup error boundary chưa đồng nhất (P1, DEC-12)

**Evidence**
- `BalanceLoader` và `MapLoader` parse JSON bằng Gson rồi gọi validator.
- `BalanceValidator` kiểm tra null/missing piece stats và range của các field bắt buộc.
- `MapValidator` kiểm tra kích thước 8x8, danh sách rocks/pieces, bounds, overlap, ID/type, Knight/King và spawnOrder.
- `Main.create` bắt `IOException | IllegalArgumentException` và bọc thành `IllegalStateException`.
- Lỗi parse cú pháp Gson thuộc nhóm runtime parse exception, không thuộc hai loại được catch ở Main; chưa có setup-error presentation/logging flow thống nhất.

**Đánh giá**
- Invalid data theo validator bị từ chối trước khi Battle được khởi tạo; phần này phù hợp nguyên tắc fail-before-battle.
- Nhưng trải nghiệm và chẩn đoán setup failure chưa được bao phủ đồng nhất; malformed JSON có thể thoát khỏi catch hiện tại.

**Hướng xử lý**
- Chuẩn hóa exception boundary cho load/parse/validate, giữ nguyên nguyên nhân và tên resource/field nếu có.
- Chặn bootstrap Battle khi có lỗi; báo setup error có thể đọc được thay vì tạo state thiếu hoặc làm crash không có ngữ cảnh.

**Test cần bổ sung**
- JSON malformed, JSON literal null, missing required fields, invalid range, invalid map coordinates/overlap.
- Xác nhận factory không trả BattleController khi config invalid.
- Xác nhận application bootstrap chuyển sang setup error path.

### AUD-05 — Comment test equipment đã lỗi thời (P2)

**Evidence**
- `EquipmentActionIntegrationTest` có comment nói chưa có terminal check (T-03.01), trong khi test dùng `BattleController` hiện đã gọi `endIfTerminal` sau action.
- Đây là comment/metadata cũ, không phải lỗi behavior.

**Hướng xử lý**
- Sửa comment trong task cleanup riêng; không gộp với thay đổi luật equipment.

## 3. Coverage matrix — logic

| Area | Source chính | Test hiện có | Audit nhận định |
|---|---|---|---|
| Board/occupancy | Board, Position | BoardTest, PositionTest | Tốt: duplicate ID, occupied/rock, failed move atomicity, immutable copies |
| Aggregate/snapshot | BattleState, PieceState, Inventory, BattleSnapshot | BattleStateTest, PieceStateTest, InventoryTest, PieceDirectionStateTest | Tốt về invariants/snapshot; chưa chứng minh caller không có mutation capability |
| Player request/controller | BattleController, ActionRequest/Result, KnightRules | BattleControllerTest, KnightMoveTest | Tốt: invalid payload/actor/phase/result, state unchanged, turn advancement |
| Resolver/equipment | ActionResolver, ActionPlan, KnightRules | EquipmentActionIntegrationTest | Bao phủ screen/blocker, charge, damage, landing, multi-target; cần chạy lại trên head hiện tại |
| Enemy validation/runner | EnemyRules, EnemyTurnRunner, TurnScheduler, ResultEvaluator | EnemyRulesTest, EnemyTurnRunnerTest, ResultEvaluatorTest | Bao phủ sequencing, skip, schedule, terminal stop; thiếu null decision và end-to-end runtime failure |
| AI policy | EnemyAI, GenericEnemyPolicy, PawnPolicy | GenericEnemyPolicyTest, PawnTurnTest, EnemyTurnIntegrationTest | Có deterministic policy và integration với runner; AI read-only qua StateView |
| Promotion/scheduler | PawnPathfinder, PawnPromotionApplier, TurnScheduler | PawnPathfinderTest, PawnPromotionApplierTest, TurnSchedulerTest, PawnTurnTest | Có test riêng và integration; fallback Pawn không có đường phong cấp còn chưa chốt trong repo |
| Config/data | loaders, validators, DTOs, BattleFactory | loader/validator/DTO/factory tests | Có validation cơ bản; thiếu kiểm chứng thống nhất application setup-error boundary |
| Core adapter boundary | BattleControllerAdapter, BoardInputHandler, Main | core adapter/input tests | UI dùng controller port/snapshot/events; cần runtime error integration cho DEC-12 |

## 4. Các điểm đã ổn định, không đề xuất đổi behavior

- `PieceState` là immutable record; Board giữ vị trí duy nhất.
- `BattleState.snapshot()` tạo detached snapshot và kiểm tra consistency giữa Board và piece map.
- `BattleSnapshot` defensive-copy rocks/pieces; `Inventory` là immutable/copy-based theo test hiện có.
- Player path giữ trình tự validate → plan → resolver; invalid request trả rejected result, không gọi resolver, không phát events.
- `ActionResolver` package-private; `ActionPlan` package-private.
- KnightRules/EnemyRules là query/validation; AI policies nhận StateView và trả EnemyDecision.
- EnemyTurnRunner đọc lại state trước từng activation, bỏ ID đã chết, kiểm tra terminal sau mỗi action và dừng queue khi Battle kết thúc.
- Equipment integration tests có nhiều edge cases thực tế; không có căn cứ để chỉnh balance/rules trong audit này.

## 5. Giới hạn bằng chứng

- Đây là static review source và test code tại baseline SHA nêu trên; audit này **không khẳng định test pass**.
- Commit status của baseline không có CI status; Actions API trả 0 workflow runs cho branch tại thời điểm kiểm tra.
- Build local được người dùng xác nhận trước đây ở commit cũ hơn; lần build gần nhất được báo cáo tại a35ff57 có 1 core test failure trong Knight-death fixture. Fixture đã được sửa tại f118f53, nhưng chưa có xác nhận người dùng chạy lại build trên f118f53 hoặc head hiện tại.
- Không thực hiện manual desktop playtest.
- BR1–BR4 logic được rà soát source/test ở mức coverage; không thay thế full GDD acceptance, UI lifecycle, performance hoặc packaging audit.

## 6. Trạng thái tiến trình và gate tiếp theo

| Phase | Status | Exit condition |
|---|---|---|
| DEC-04 / DEC-12 decision | Approved, recorded in docs/decisions | Đã có xác nhận người dùng và decision records |
| Logic audit source/test | Complete — static review | Findings và coverage matrix đã ghi ở tài liệu này |
| Local verification | Pending | Người dùng pull head hiện tại và chạy `./gradlew build --rerun-tasks` (Windows: `.\gradlew.bat build --rerun-tasks`), gửi full result/exit code |
| Audit PR | Existing PR #17 — Draft, updated | PR #17 đã tồn tại cho toàn bộ architecture-cleanup branch; phần audit/DEC được thêm vào cùng PR. Không tạo PR trùng. PR vẫn Draft chờ local verification và xử lý branch divergence |
| DEC-04 implementation | Not started | Tạo task/plan riêng sau API-boundary design |
| DEC-12 implementation | Not started | Tạo task/plan riêng cho fail-stop + setup error boundary |
| Merge | Not done | Chỉ sau local/CI verification và review approval; không tự merge |

### Lệnh xác minh local

Tại thư mục gốc repo trên Windows PowerShell:

```powershell
git pull
.\gradlew.bat build --rerun-tasks
"BUILD_EXIT_CODE=$LASTEXITCODE"
```

Gửi toàn bộ phần summary test và dòng `BUILD_EXIT_CODE`. Nếu pass, có thể mở PR cho audit/decision documentation. Các implementation gap AUD-01..AUD-04 cần task/PR riêng hoặc được đưa vào một scope triển khai được duyệt; không được đánh dấu hoàn tất chỉ vì audit PR đã merge.

## 7. PR handoff hiện tại

- PR: https://github.com/do-manh-hiep/RogueKnight/pull/17 — **Draft**, base `main`, head `refactor/architecture-cleanup-7119`.
- PR body đã được cập nhật với summary, AUD-01..AUD-05, verification checklist và điều kiện chưa merge.
- Compare với `main` tại thời điểm audit: branch ahead 61, behind 1, status `diverged`; cần xử lý commit mới trên main trước khi merge.
- Commit audit note: `26ff5e66fc4b2b907597467fb8d90777665031e9`.
- Chưa có xác nhận build local trên branch head; không được chuyển PR sang Ready for review hoặc merge.

## 8. Nguồn source chính đã kiểm tra

- `logic/src/main/java/rogueknight/battle/BattleState.java`
- `logic/src/main/java/rogueknight/battle/StateView.java`
- `logic/src/main/java/rogueknight/battle/BattleSnapshot.java`
- `logic/src/main/java/rogueknight/board/Board.java`
- `logic/src/main/java/rogueknight/action/BattleController.java`
- `logic/src/main/java/rogueknight/action/ActionResolver.java`
- `logic/src/main/java/rogueknight/action/KnightRules.java`
- `logic/src/main/java/rogueknight/action/EnemyRules.java`
- `logic/src/main/java/rogueknight/action/EnemyTurnRunner.java`
- `logic/src/main/java/rogueknight/ai/EnemyAI.java`
- `logic/src/main/java/rogueknight/ai/GenericEnemyPolicy.java`
- `logic/src/main/java/rogueknight/ai/PawnPolicy.java`
- `logic/src/main/java/rogueknight/data/BattleFactory.java`
- `logic/src/main/java/rogueknight/data/BalanceLoader.java`
- `logic/src/main/java/rogueknight/data/BalanceValidator.java`
- `logic/src/main/java/rogueknight/data/MapLoader.java`
- `logic/src/main/java/rogueknight/data/MapValidator.java`
- `core/src/main/java/rogueknight/Main.java`
- `core/src/main/java/rogueknight/core/input/BattleControllerAdapter.java`
- `core/src/main/java/rogueknight/core/input/BoardInputHandler.java`
- Test suites listed in section 3.
