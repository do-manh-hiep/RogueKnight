# DEC-02, DEC-11 và contract lượt địch (T-03.01, T-03.02)

Chọn bởi người làm task ngày 22/11/2026, theo đề xuất TD v0.1.
Reviewer xác nhận khi merge.

## DEC-02 — AI Enemy generic (Rook, Bishop, King, Queen)

1. Knight đang trong attack range: Attack, hết activation.
2. Không có legal Move: Skip, lịch vẫn tiến.
3. Còn legal Move thì luôn đi. Chọn ô theo thứ tự ưu tiên:
   1. Move→Attack: từ ô đích đánh được Knight ngay.
   2. Safe: Knight không default-Capture được Enemy ở ô đích
      (ô hình L **và** HP Enemy ≤ 2; dùng `KnightRules.canDefaultCapture`).
   3. Threat: từ ô đích, lần activation kế có ít nhất một Move→Attack
      nếu các quân khác đứng yên.
   4. Khoảng cách Chebyshev `max(|dx|,|dy|)` tới Knight nhỏ hơn.
   5. column nhỏ hơn, rồi row nhỏ hơn.
4. Sau Move, runner tự xét Attack trên state mới; không hỏi AI Move lần hai.

Fixture: `GenericEnemyPolicyTest`, `EnemyTurnIntegrationTest`.
Map01, Turn 3 (Knight Wait): Rook h8 → h1, Bishop c8 → a6.

## DEC-11 — Vừa thắng vừa thua

DEFEAT được ưu tiên. Fixture: `ResultEvaluatorTest`.

## DEC-05/07 — Lịch và queue (theo TD)

- `nextActivationTurn` tường minh, ban đầu bằng cycle; xong activation hoặc skip thì cộng cycle.
- Queue: PAWN, ROOK, BISHOP, KING, QUEEN, rồi spawnOrder, rồi id.
- Queue chụp một lần sau Turn+1; ID đã chết bị bỏ qua.
- Type đổi trong activation (phong cấp) thì runner giữ lịch T+5 do Resolver đặt.

## DEC-12 — AI chọn request invalid

`EnemyTurnRunner` ném `IllegalStateException`, không âm thầm skip.

## Thay đổi contract

- `BattleEvent` thêm `TurnAdvanced`, `ActorActivated`, `BattleEnded` (TD 4.10).
- `ActionKind.ATTACK`: đòn đánh thường của Enemy; Player gửi kind này bị `UNSUPPORTED_ACTION`.
- `ActionError.NOT_ENEMY_ACTOR`.
- Knight HP về 0 bị gỡ khỏi Board như mọi quân; không còn Knight = DEFEAT.
- `BattleController(state)` vẫn dùng `EnemyPhase.NONE` cho test luật Player.
  Game dùng `new EnemyTurnRunner(new EnemyAI())`.
- Fixture test phải có King **và** một Enemy non-King nếu cần trận ONGOING.

## Tích hợp T-03.03 (Pawn)

Mọi Enemy, kể cả Pawn, đi cùng một luồng:
`EnemyTurnRunner` → `EnemyAI` → `EnemyRules.validate` → `ActionResolver` → check terminal.

- `EnemyAI`: Pawn dùng `PawnPolicy`, các loại khác dùng `GenericEnemyPolicy`.
- `PawnPolicy`: Knight trong ba ô theo hướng đang lưu thì ATTACK;
  nếu không thì MOVE bước đầu của `PawnPathfinder` (BFS, DEC-01); không có bước thì Skip.
- `EnemyRules`: Pawn Move một ô tám hướng; Attack theo `PawnAttackRule.attackTargets`.
- `ActionResolver`: Pawn Move đổi `currentDirection` theo bước vừa đi;
  tới hàng 1 thì `PawnPromotionApplier` phong Queen (HP 2, cycle 4, T+5) trong cùng Action.
- Runner chỉ xét Attack sau Move cho loại quân lúc bắt đầu activation là generic,
  nên Pawn và Queen vừa phong cấp không đánh tiếp.
- Bỏ `PawnEnemyPhase` (queue và lịch trùng với runner) và `PawnAttackRule.executeAttack`
  (tự gọi `finish`, bỏ qua `ActionPerformed`). Knight được tìm theo type, không theo ID `"knight"`.
- Test cũ được chuyển sang `rogueknight.ai.PawnTurnTest` qua controller thật.

## Còn mở

- DEC-01 fallback khi Pawn **không có đường phong cấp**: repo chưa định nghĩa hình học fallback,
  nên Pawn hiện Skip (lịch vẫn tiến). Cần nhóm chốt trước khi tuyên bố G3.
