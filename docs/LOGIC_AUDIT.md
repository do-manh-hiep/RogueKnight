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

## 5. Giới hạn bằng chứng và verification

- Source-level audit và requirement mapping được thực hiện bằng static review; matrix ghi nhận implementation/test presence, không tự suy diễn test pass từ việc test tồn tại.
- Người dùng xác nhận đã chạy trên branch head `647667c585e7c8f12fd35668f0d488b6ab894a7f`: `:logic:test --rerun-tasks` với `LOGIC_TEST_EXIT_CODE=0` và `build --rerun-tasks` với `BUILD_EXIT_CODE=0`.
- Kết quả trên xác nhận logic test task và full Gradle build pass tại đúng revision nêu trên; không thay thế manual desktop playtest.
- Không thực hiện manual desktop playtest.
- BR1–BR4 logic được rà soát source/test ở mức coverage; không thay thế full GDD acceptance, UI lifecycle, performance hoặc packaging audit.

## 6. Trạng thái tiến trình và gate tiếp theo

| Phase | Status | Exit condition |
|---|---|---|
| DEC-04 / DEC-12 decision | Approved, recorded in docs/decisions | Đã có xác nhận người dùng và decision records |
| Logic audit source/test | Complete — static review | Findings và coverage matrix đã ghi ở tài liệu này |
| Local verification | **Passed** | Người dùng xác nhận `:logic:test --rerun-tasks` và `build --rerun-tasks` đều exit code 0 trên head `647667c585e7c8f12fd35668f0d488b6ab894a7f` |
| Audit PR | Existing PR #17 — Draft, updated | PR #17 đã tồn tại cho toàn bộ architecture-cleanup branch; phần audit/DEC được thêm vào cùng PR. Không tạo PR trùng. PR vẫn Draft; local logic tests và full build đã pass trên head được xác nhận, desktop playtest chưa được xác nhận; xử lý branch divergence trước khi merge |
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
- Local logic tests và full Gradle build đã được xác nhận pass trên branch head `647667c585e7c8f12fd35668f0d488b6ab894a7f`; desktop playtest chưa được xác nhận. PR vẫn Draft; không tự merge.

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


### 9.1 Requirement → Code → Test → Status

| Requirement | Code / responsibility found | TD test mapping | Static audit status |
|---|---|---|---|
| R-T01–T02 — initial Turn and invalid request leaves state unchanged | `BattleFactory`, `BattleController.submit`, `KnightRules`, `ActionResolver` | TC-T01–T02; BattleFactoryTest, BattleControllerTest | **Implemented / test present**. Invalid request returns rejected result before resolver; full state immutability is covered by existing controller/rule tests. Runtime pass not verified. |
| R-T03 — Player Action advances clock once only if battle continues | `BattleController.submit`, `endIfTerminal` | TC-T03; BattleControllerTest | **Implemented / test present**. Terminal result is checked before Turn+1. |
| R-T04–T05 — ordered due-enemy queue; each enemy reads latest state | `TurnScheduler`, `EnemyTurnRunner`, `EnemyRules` | TC-T04–T05; TurnSchedulerTest, EnemyTurnRunnerTest, EnemyTurnIntegrationTest | **Implemented / test present**. Runner builds due IDs in schedule order and reads state for each activation. |
| R-T06–T07 — terminal stops queue; no legal enemy action skips activation | `EnemyTurnRunner`, `BattleController.endIfTerminal` | TC-T06–T07; EnemyTurnRunnerTest, ResultEvaluatorTest | **Implemented / test present** for normal terminal/skip paths. Null decision is a separate gap (AUD-03), not a valid Skip. |
| R-B01 — 8×8 board and coordinate bounds | `Position`, `Board` | TC-B01; PositionTest, BoardTest | **Implemented / test present**. |
| R-B02 — one occupant per tile; capture is atomic at action level | `Board`, `BattleState`, `ActionResolver` | TC-B02; BoardTest, BattleStateTest, KnightMoveTest / equipment integration | **Implemented / test present**. Board rejects occupied/rock destinations; capture removes target before moving actor within resolver action. |
| R-B03–B04 — Rock and Piece blockers; Knight jump exception; line movement stops at first blocker | `Board`, `KnightRules`, `EnemyRules`, equipment rules | TC-B03–B04; BoardTest, KnightMoveTest, EnemyRulesTest, EquipmentActionIntegrationTest | **Implemented / test present** in reviewed paths. |
| R-B05 — Map01 exact baseline layout | `MapLoader`, `MapValidator`, `BattleFactory` | TC-B05; loader/validator/factory tests | **Implemented / test present** for setup data/validation. A separate visual/manual Map01 acceptance is not claimed. |
| R-K01 — Knight starts at HP 5 | `BattleFactory`, balance data | TC-K01; BattleFactoryTest / data tests | **Implemented / test present**. Restart-specific behavior belongs to lifecycle/UI integration and is not fully verified here. |
| R-K02–K03 — L-move and adjacent 8-way Move baseline | `KnightRules`, `ActionResolver` | TC-K02–K03; KnightMoveTest | **Implemented / test present**. Landing validation is distinct from intermediate blockers. |
| R-K04 — Sword Attack damage 1; Knight remains in place | `KnightRules`, `ActionResolver` | TC-K04; KnightMoveTest / action tests | **Implemented / test present**. |
| R-K05 — Capture requires eligible Enemy and HP threshold; Knight lands on target | `KnightRules`, `ActionResolver` | TC-K05; KnightMoveTest / action tests | **Implemented / test present**. |
| R-K06 — Wait is legal; Wait itself does not mutate board/equipment, lifecycle proceeds | `KnightRules`, `BattleController`, `ActionResolver` | TC-K06; BattleControllerTest, EnemyTurnIntegrationTest | **Implemented / test present**. |
| R-E01 — Enemy stats and cycle | `BattleFactory`, balance DTO/catalog, `TurnScheduler` | TC-E01; factory/data/scheduler tests | **Implemented / test present**. |
| R-E02–E03 — Attack if in range; otherwise Move then re-evaluate Attack from new state | `EnemyAI`, `GenericEnemyPolicy`, `EnemyTurnRunner`, `EnemyRules` | TC-E02–E03; GenericEnemyPolicyTest, EnemyTurnRunnerTest, EnemyTurnIntegrationTest | **Implemented / test present**. Runner resolves Move and checks terminal before subsequent Attack. |
| R-E04 — Rook/Bishop/King/Queen movement and attack geometry | `EnemyRules`, geometry table | TC-E04; EnemyRulesTest | **Implemented / test present**. |
| R-E05–E07 — AI priority, determinism, default Knight-only threat/capture evaluation | `GenericEnemyPolicy`, `PawnPolicy`, `EnemyAI`, `HypotheticalView` | TC-E05–E07; GenericEnemyPolicyTest and AI tests | **Implemented / test present** for reviewed deterministic policy and read-only evaluation. Policy acceptance remains subject to DEC-02 definition; no balance/policy change is proposed by this audit. |
| R-P01–P02 — Pawn initial direction and Attack-before-BFS behavior | `PawnPolicy`, `PawnPathfinder`, `PawnAttackRule` | TC-P01–P02; PawnPathfinderTest, PawnTurnTest | **Implemented / test present** for the chosen direction/path implementation. |
| R-P03–P06 — Pawn BFS/path selection, fallback, movement and attack behavior | `PawnPathfinder`, `PawnPolicy`, `EnemyRules` | TC-P03–P06; PawnPathfinderTest, PawnTurnTest | **Partial / decision-sensitive**. Core BFS/policy tests exist; confirm the no-path/fallback and edge semantics against the final approved DEC-01 record before calling every edge accepted. |
| R-P07–P08 — Promotion identity and Queen schedule | `PawnPromotionApplier`, `ActionResolver`, `TurnScheduler` | TC-P07–P08; PawnPromotionApplierTest, TurnSchedulerTest, PawnTurnTest, IT-05 | **Implemented / test present**. Existing tests cover identity retention and promotion schedule; no runtime pass asserted. |
| R-Q01–Q04 — Inventory/loadout, charge, invalid action, zero-charge behavior | `Inventory`, `BattleFactory`, `KnightRules`, `ActionResolver` | TC-Q01–Q04; InventoryTest, EquipmentActionIntegrationTest | **Implemented / test present**. Invalid request does not consume charge; valid equipment action consumes one charge. |
| R-Q05–Q08 — Cannon Movement screen, empty landing choices, Capture terminal enemy, second blocker | equipment rules / `KnightRules`, `ActionResolver` | TC-Q05–Q08; EquipmentActionIntegrationTest | **Implemented / test present** for reviewed screen/landing/capture edge cases. |
| R-Q09 — Cannon Attack requires exactly one screen and terminal Enemy | equipment rules / `KnightRules`, `ActionResolver` | TC-Q09; EquipmentActionIntegrationTest | **Implemented / test present**. |
| R-Q10–Q11 — Diagonal Skewer hits two consecutive tiles, passes Enemy, stops at Rock/edge | equipment rules / `KnightRules`, `ActionResolver` | TC-Q10–Q11; EquipmentActionIntegrationTest | **Implemented / test present** for reviewed multi-target and Rock/edge cases. DEC-03 governs the no-hit direction edge. |
| R-R01–R03 — Victory/Defeat and stop processing immediately | `ResultEvaluator`, `BattleController.endIfTerminal`, `EnemyTurnRunner` | TC-R01–R03; ResultEvaluatorTest, BattleControllerTest, EnemyTurnRunnerTest | **Implemented / test present** for gameplay terminal outcomes. Distinct runtime-failure outcome for DEC-12 is not implemented (AUD-02). |
| R-R04–R06 — Stuck warning, Continue, Accept Defeat | selection/stuck lifecycle and controller contracts | TC-R04–R06; lifecycle/controller tests, IT-06 | **Partial / integration gap**. Logic contracts and tests are present in reviewed test inventory; full UI path/desktop acceptance is not established by this logic audit. |
| R-U01–U03 — Screen flow, hover details, current attack range/cues | core screen/view layer | TC-U01–U03 | **Out of logic scope / not verified**. Requires core/desktop walkthrough; not inferable from logic tests. |
| R-U04–U06 — UI reads legal options and renders action/event cues | `BattleControllerAdapter`, `BoardInputHandler`, `BattlePresentationModel`, `BattlePlayback`, `BoardRenderer` | TC-U04–U06; adapter/input/presentation/playback tests | **Partial / test present**. Adapter/event playback path exists; complete screen/hover/animation acceptance is not claimed. |
| R-TECH01–02 — Pure-Java logic; shared legality between UI/AI/validation | Gradle module boundary, `StateView`, `KnightRules`, `EnemyRules`, `EnemyAI` | TC-X01–X02; logic tests, adapter/rule tests | **Implemented by structure / test evidence present**. Verify with clean CI/build and module dependency checks. |
| R-TECH03 — Single owner for clock/mutation | `BattleController`, `BattleState`, `Board`, `ActionResolver` | TC-X03; controller/state tests | **Partial — AUD-01 (P0)**. Runtime flow centralizes turn progression, but public mutation methods and public constructors/interfaces still permit bypassing the ownership boundary. |
| R-TECH04 — Playback does not control gameplay or accept duplicate input | `BattlePlayback`, `BattlePresentationModel`, `Main`, input handler | TC-X04 / FX-17; playback/input tests | **Implemented / test present** for deterministic event playback and input gating in the reviewed path. Manual desktop timing/responsiveness not verified. |
| R-TECH05 — Invalid config rejected before Battle; actionable setup error | `BalanceLoader`, `MapLoader`, validators, `BattleFactory`, `Main.create` | TC-X05; loader/validator/factory tests | **Partial — AUD-04 (P1)**. Validators reject invalid DTO data, but malformed Gson parse exceptions and the application setup-error route are not unified. |
| R-TECH06 — Task handoff ownership/dependency/acceptance | repository task/PR documentation | REV-X06 | **Not part of runtime logic**; project-process evidence is outside this source audit. |
| R-TECH07 — Adding a piece type does not scatter type switches | `PieceCatalog`, geometry/policy strategy structures | REV-X07 | **Partial / not fully verified**. Some geometry/policy selection is data/strategy-based; no full extensibility proof for every subsystem. |

### 9.2 Verification status — do not conflate test existence with pass

| Verification item | Evidence available | Status |
|---|---|---|
| Source review | Relevant logic and direct Main/core integration source inspected at branch head | **Reviewed statically** |
| Test mapping | TD section 7.2 IDs and corresponding test suites inventoried in audit | **Mapped; tests exist** |
| Test execution on revision `6aa5a008...` | Static mapping revision; later exact-head run reported by user on `647667c585e7c8f12fd35668f0d488b6ab894a7f` | **Passed on later head** (`:logic:test --rerun-tasks`, exit code 0) |
| Full Gradle build on revision `6aa5a008...` | Later exact-head run reported by user on `647667c585e7c8f12fd35668f0d488b6ab894a7f` | **Passed on later head** (`build --rerun-tasks`, exit code 0) |
| Desktop manual playtest | No manual walkthrough evidence | **Not performed** |
| CI on this revision | Previous audit record found no workflow run/status evidence | **Not verified; re-check if CI is configured** |
