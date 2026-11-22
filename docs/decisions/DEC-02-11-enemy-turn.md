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
- Type đổi trong activation (phong cấp) thì runner giữ lịch T+5 do Pawn đặt.

## DEC-12 — AI chọn request invalid

`EnemyTurnRunner` ném `IllegalStateException`, không âm thầm skip.

## Thay đổi contract

- `BattleEvent` thêm `TurnAdvanced`, `ActorActivated`, `BattleEnded` (TD 4.10).
- `ActionKind.ATTACK`: đòn đánh thường của Enemy; Player gửi kind này bị `UNSUPPORTED_ACTION`.
- `ActionError.NOT_ENEMY_ACTOR`.
- Knight HP về 0 bị gỡ khỏi Board như mọi quân; không còn Knight = DEFEAT.
- `BattleController(state)` vẫn dùng `EnemyPhase.NONE` cho test luật Player.
  Game dùng `new EnemyTurnRunner(new GenericEnemyPolicy())`.
- Fixture test phải có King **và** một Enemy non-King nếu cần trận ONGOING.

## Điểm nối cho T-03.03

Pawn đang dùng `PawnActivation.SKIP`: chỉ phát `ActorActivated`, lịch vẫn tiến.
T-03.03 cắm logic thật qua `new EnemyTurnRunner(policy, pawnActivation)`:

- Không phát `ActorActivated`, không cộng cycle, không tăng Turn, không tự kết thúc trận.
- Phong cấp: đổi type và đặt `nextActivationTurn = T + 5` trong cùng Action.
- Không Attack sau Move.
