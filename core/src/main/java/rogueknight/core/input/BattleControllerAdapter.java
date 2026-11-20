package rogueknight.core.input;

import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.action.BattleController;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BattleControllerAdapter implements BattleControllerPort {

    private final BattleController controller;

    public BattleControllerAdapter(BattleController controller) {
        this.controller = Objects.requireNonNull(
            controller,
            "controller must not be null"
        );
    }

    @Override
    public BattleViewSnapshot snapshot() {
        return toViewSnapshot(controller.snapshot());
    }

    @Override
    public Set<BoardPosition> legalMoves(String actorId) {
        Set<BoardPosition> positions = new HashSet<>();

        for (Position position : controller.legalMoves(actorId)) {
            positions.add(toBoardPosition(position));
        }

        return Set.copyOf(positions);
    }

    @Override
    public MoveResult submitMove(String actorId, BoardPosition target) {
        // Chụp dữ liệu UI trước khi gửi yêu cầu.
        BattleViewSnapshot before = snapshot();

        if (target == null) {
            return MoveResult.rejected("OUT_OF_BOUNDS", before);
        }

        // Gửi tọa độ thô để logic kiểm tra cả trường hợp ngoài biên.
        ActionRequest request = ActionRequest.move(
            actorId,
            target.column(),
            target.row()
        );

        ActionResult result = controller.submit(request);

        if (!result.accepted()) {
            return MoveResult.rejected(
                result.error().toString(),
                before
            );
        }

        BattleViewSnapshot after = toViewSnapshot(result.after());

        return MoveResult.success(before, after);
    }

    private BattleViewSnapshot toViewSnapshot(BattleSnapshot snapshot) {
        BattleSnapshot.PieceView knight =
            snapshot.pieceById("knight").orElseThrow(
                () -> new IllegalStateException(
                    "UI di chuyển hiện tại cần quân có ID knight"
                )
            );

        Set<BoardPosition> rocks = new HashSet<>();

        for (Position rock : snapshot.rocks()) {
            rocks.add(toBoardPosition(rock));
        }

        Map<BoardPosition, String> enemies = new HashMap<>();

        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            if (piece.type() != PieceType.KNIGHT) {
                enemies.put(
                    toBoardPosition(piece.position()),
                    piece.type().name()
                );
            }
        }

        BoardPosition knightPosition = toBoardPosition(knight.position());

        String statusMessage =
            "Turn " + snapshot.globalTurn()
            + " | Knight: " + knightPosition.notation()
            + " | HP: " + knight.hp()
            + " | " + snapshot.phase()
            + " | " + snapshot.result();

        return new BattleViewSnapshot(
            snapshot.globalTurn(),
            knightPosition,
            rocks,
            enemies,
            legalMoves(knight.id()),
            statusMessage,
            false
        );
    }

    private BoardPosition toBoardPosition(Position position) {
        return new BoardPosition(
            position.column(),
            position.row()
        );
    }
}