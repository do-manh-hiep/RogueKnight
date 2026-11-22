package rogueknight.battle;

import java.util.List;
import java.util.Optional;
import rogueknight.board.Position;

/*
 * StateView.java  |  T-03.01/T-03.02  |  TD: 4.1, 4.5 (StateView), 6.1
 *
 * Giao diện chỉ đọc của một trận đang sống, dùng cho Rules/AI/Scheduler/Evaluator.
 * - BattleState cài đặt trực tiếp, không sao chép.
 * - AI giả định "nếu Enemy ở ô X" qua HypotheticalView bọc StateView, không move quân thật.
 * - UI không dùng StateView; UI nhận BattleSnapshot bất biến.
 */
public interface StateView {

    int globalTurn();

    Optional<PieceState> pieceState(String id);

    Optional<Position> positionOf(String id);

    Optional<String> pieceAt(Position position);

    boolean isRock(Position position);

    /** Ô không có Rock và không có quân. */
    default boolean isEmpty(Position position) {
        return !isRock(position) && pieceAt(position).isEmpty();
    }

    /**
     * Các quân còn sống theo thứ tự thêm vào trận.
     * Caller cần thứ tự theo luật (queue, tie-break) phải tự sắp xếp, không dựa vào thứ tự này.
     */
    List<PieceState> livePieces();
}
