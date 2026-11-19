package rogueknight.action;

import rogueknight.board.Position;

/*
 * ActionRequest.java  |  T-02.01  |  TD: 4.6, DEC-04
 *
 * Yêu cầu từ UI/AI gửi vào BattleController: actor (ID), kind, ô đích.
 * - Chỉ chứa giá trị (ID + tọa độ), không chứa PieceState sống.
 * - Đích lưu dạng hai số nguyên thô để request ngoài biên vẫn tạo được và bị
 *   Controller từ chối bằng OUT_OF_BOUNDS trước mọi mutation (Position tự chặn biên).
 * Từ khóa tìm kiếm: move, targetInBounds, target.
 */
public record ActionRequest(String actorId, ActionKind kind, int targetColumn, int targetRow) {

    public static ActionRequest move(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.MOVE, target.column(), target.row());
    }

    public static ActionRequest move(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.MOVE, targetColumn, targetRow);
    }

    public boolean targetInBounds() {
        return Position.isInBounds(targetColumn, targetRow);
    }

    /** Chỉ gọi sau khi targetInBounds() đúng. */
    public Position target() {
        return new Position(targetColumn, targetRow);
    }
}
