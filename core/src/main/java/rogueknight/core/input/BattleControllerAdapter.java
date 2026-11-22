package rogueknight.core.input;

import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.action.BattleController;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.EquipmentState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class BattleControllerAdapter implements BattleControllerPort {

    private final BattleController controller;
    private BoardPosition lastKnownKnightPosition = new BoardPosition(3, 0);

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
        return submitAction(actorId, ActionMode.MOVE, target);
    }

    @Override
    public List<ActionMode> availableModes() {
        List<ActionMode> modes = new ArrayList<>();
        modes.add(ActionMode.MOVE);

        for (EquipmentState item : controller.snapshot().inventory().equipment()) {
            if (item.charges() > 0) {
                modes.add(toMode(item.type()));
            }
        }

        modes.sort(Comparator.comparingInt(ActionMode::ordinal));
        return List.copyOf(modes);
    }

    @Override
    public Set<BoardPosition> legalTargets(String actorId, ActionMode mode) {
        Set<BoardPosition> positions = new HashSet<>();

        for (Position position : controller.legalTargets(actorId, toKind(mode))) {
            positions.add(toBoardPosition(position));
        }

        return Set.copyOf(positions);
    }

    @Override
    public MoveResult submitAction(String actorId, ActionMode mode, BoardPosition target) {
        // Chụp dữ liệu UI trước khi gửi yêu cầu.
        BattleViewSnapshot before = snapshot();

        if (target == null || mode == null) {
            return MoveResult.rejected("OUT_OF_BOUNDS", before);
        }

        // Gửi tọa độ thô để logic kiểm tra cả trường hợp ngoài biên.
        ActionRequest request = new ActionRequest(
            actorId,
            toKind(mode),
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

    private static ActionMode toMode(EquipmentType type) {
        return switch (type) {
            case CANNON_MOVEMENT -> ActionMode.CANNON_MOVEMENT;
            case CANNON_ATTACK -> ActionMode.CANNON_ATTACK;
            case DIAGONAL_SKEWER -> ActionMode.DIAGONAL_SKEWER;
        };
    }

    private static ActionKind toKind(ActionMode mode) {
        return switch (mode) {
            case MOVE -> ActionKind.MOVE;
            case CANNON_MOVEMENT -> ActionKind.CANNON_MOVEMENT;
            case CANNON_ATTACK -> ActionKind.CANNON_ATTACK;
            case DIAGONAL_SKEWER -> ActionKind.DIAGONAL_SKEWER;
        };
    }

    private BattleViewSnapshot toViewSnapshot(BattleSnapshot snapshot) {
        java.util.Optional<BattleSnapshot.PieceView> knightOpt = snapshot.pieceById("knight");
        BoardPosition knightPosition;
        int knightHp;
        Set<BoardPosition> moves;

        if (knightOpt.isPresent()) {
            knightPosition = toBoardPosition(knightOpt.get().position());
            lastKnownKnightPosition = knightPosition;
            knightHp = knightOpt.get().hp();
            moves = legalMoves(knightOpt.get().id());
        } else {
            knightPosition = lastKnownKnightPosition;
            knightHp = 0;
            moves = Set.of();
        }

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

        String statusMessage =
            "Turn " + snapshot.globalTurn()
            + " | Knight: " + (knightHp > 0 ? knightPosition.notation() : "DEAD")
            + " | HP: " + knightHp
            + " | " + snapshot.phase()
            + " | " + snapshot.result();

        return new BattleViewSnapshot(
            snapshot.globalTurn(),
            knightPosition,
            rocks,
            enemies,
            moves,
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
