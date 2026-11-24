package rogueknight.core.input;

import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.action.BattleController;
import rogueknight.action.BattleEvent;
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
import java.util.Optional;
import java.util.Set;

public final class BattleControllerAdapter implements BattleControllerPort {

    private final BattleController controller;
    // A Knight at 0 HP is removed from the Board (Defeat); the view keeps showing its last square.
    private BoardPosition lastKnownKnightPosition;

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
        if (!controller.canWait("knight")) return List.of();
        modes.add(ActionMode.WAIT);
        modes.add(ActionMode.MOVE);

        for (EquipmentState item : controller.snapshot().inventory().equipment()) {
            if (item.charges() > 0) {
                modes.add(toMode(item.type()));
            }
        }

        modes.sort(Comparator.comparingInt(ActionMode::key));
        return List.copyOf(modes);
    }

    @Override
    public Set<BoardPosition> legalTargets(String actorId, ActionMode mode) {
        Set<BoardPosition> positions = new HashSet<>();

        if (mode == ActionMode.MOVE) {
            for (Position position : controller.legalMoves(actorId)) {
                positions.add(toBoardPosition(position));
            }
            for (Position position : controller.legalTargets(actorId, ActionKind.SWORD)) {
                positions.add(toBoardPosition(position));
            }
            return Set.copyOf(positions);
        }
        if (mode == ActionMode.WAIT) return Set.of();
        for (Position position : controller.legalTargets(actorId, toKind(mode))) {
            positions.add(toBoardPosition(position));
        }
        return Set.copyOf(positions);
    }

    @Override
    public MoveResult submitAction(String actorId, ActionMode mode, BoardPosition target) {
        // Chụp dữ liệu UI trước khi gửi yêu cầu.
        BattleViewSnapshot before = snapshot();

        if (mode == ActionMode.WAIT) return submitWait(actorId);
        if (target == null || mode == null) {
            return MoveResult.rejected("OUT_OF_BOUNDS", before);
        }

        // Gửi tọa độ thô để logic kiểm tra cả trường hợp ngoài biên.
        ActionKind kind = toKind(mode);
        // Default [1] is contextual: clicking an occupied enemy square means Sword Attack.
        if (mode == ActionMode.MOVE && before.enemyPieces().containsKey(target)) {
            kind = ActionKind.SWORD;
        }
        ActionRequest request = new ActionRequest(
            actorId,
            kind,
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

        // The Knight may move and then die in the same submit: its last square comes from the events.
        for (BattleEvent event : result.events()) {
            if (event instanceof BattleEvent.PieceRemoved removed && removed.pieceId().equals("knight")) {
                lastKnownKnightPosition = toBoardPosition(removed.at());
            }
        }
        BattleViewSnapshot after = toViewSnapshot(result.after());

        return MoveResult.success(before, after);
    }

    @Override
    public MoveResult submitWait(String actorId) {
        BattleViewSnapshot before = snapshot();
        ActionResult result = controller.submit(ActionRequest.waitAction(actorId));
        if (!result.accepted()) {
            return MoveResult.rejected(result.error().toString(), before);
        }
        for (BattleEvent event : result.events()) {
            if (event instanceof BattleEvent.PieceRemoved removed && removed.pieceId().equals("knight")) {
                lastKnownKnightPosition = toBoardPosition(removed.at());
            }
        }
        return MoveResult.success(before, toViewSnapshot(result.after()));
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
            case WAIT -> ActionKind.WAIT;
            case MOVE -> ActionKind.MOVE;
            case CANNON_MOVEMENT -> ActionKind.CANNON_MOVEMENT;
            case CANNON_ATTACK -> ActionKind.CANNON_ATTACK;
            case DIAGONAL_SKEWER -> ActionKind.DIAGONAL_SKEWER;
        };
    }

    private BattleViewSnapshot toViewSnapshot(BattleSnapshot snapshot) {
        Optional<BattleSnapshot.PieceView> knight = snapshot.pieceById("knight");
        BoardPosition knightPosition;
        int knightHp;
        Set<BoardPosition> moves;

        if (knight.isPresent()) {
            knightPosition = toBoardPosition(knight.get().position());
            lastKnownKnightPosition = knightPosition;
            knightHp = knight.get().hp();
            moves = legalMoves(knight.get().id());
        } else if (lastKnownKnightPosition != null) {
            knightPosition = lastKnownKnightPosition;
            knightHp = 0;
            moves = Set.of();
        } else {
            throw new IllegalStateException("UI di chuyển hiện tại cần quân có ID knight");
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
