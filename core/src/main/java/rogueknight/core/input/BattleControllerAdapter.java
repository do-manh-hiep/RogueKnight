package rogueknight.core.input;

import rogueknight.action.ActionKind;
import rogueknight.action.ActionRequest;
import rogueknight.action.ActionResult;
import rogueknight.battle.BattleController;
import rogueknight.action.BattleEvent;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.EquipmentState;
import rogueknight.battle.EquipmentType;
import rogueknight.board.Position;
import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.PresentationPiece;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Adapts the domain battle controller to the UI-facing controller port. */
public final class BattleControllerAdapter implements BattleControllerPort {

    private final BattleController controller;
    // A defeated Knight is removed from the board; the view retains its last square.
    private Position lastKnownKnightPosition;

    public BattleControllerAdapter(BattleController controller) {
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
    }

    @Override
    public BattleViewSnapshot snapshot() {
        return toViewSnapshot(controller.snapshot());
    }

    @Override
    public Set<Position> legalMoves(String actorId) {
        return controller.legalTargets(actorId, ActionKind.MOVE);
    }

    @Override
    public List<ActionMode> availableModes() {
        BattleSnapshot snapshot = controller.snapshot();
        if (!controller.canWait("knight")) return List.of();

        List<ActionMode> modes = new ArrayList<>();
        modes.add(ActionMode.WAIT);
        modes.add(ActionMode.MOVE);
        for (EquipmentState item : snapshot.inventory().equipment()) {
            if (item.charges() > 0) modes.add(toMode(item.type()));
        }
        return List.copyOf(modes);
    }

    @Override
    public Set<Position> legalTargets(String actorId, ActionMode mode) {
        if (mode == null || mode == ActionMode.WAIT) return Set.of();

        Set<Position> positions = new HashSet<>();
        if (mode == ActionMode.MOVE) {
            positions.addAll(legalMoves(actorId));
            for (Position position : controller.legalTargets(actorId, ActionKind.SWORD)) {
                positions.add(position);
            }
        } else {
            for (Position position : controller.legalTargets(actorId, toKind(mode))) {
                positions.add(position);
            }
        }
        return Set.copyOf(positions);
    }

    @Override
    public UiActionResult submitAction(String actorId, ActionMode mode, Position target) {
        BattleViewSnapshot before = snapshot();
        if (mode == null) return UiActionResult.rejected("UNSUPPORTED_ACTION", before);
        if (mode == ActionMode.WAIT) return submitWait(actorId);
        if (target == null) return UiActionResult.rejected("OUT_OF_BOUNDS", before);

        // Default Move/Attack mode interprets an occupied enemy square as a Sword Attack.
        ActionKind kind = mode == ActionMode.MOVE && before.hasEnemyAt(target)
            ? ActionKind.SWORD
            : toKind(mode);
        ActionResult result = controller.submit(
            new ActionRequest(actorId, kind, target.column(), target.row())
        );

        return toUiResult(result, before);
    }

    @Override
    public UiActionResult submitWait(String actorId) {
        BattleViewSnapshot before = snapshot();
        ActionResult result = controller.submit(ActionRequest.waitAction(actorId));
        return toUiResult(result, before);
    }

    private UiActionResult toUiResult(ActionResult result, BattleViewSnapshot before) {
        if (!result.accepted()) return UiActionResult.rejected(result.error().toString(), before);
        rememberRemovedKnight(result.events());
        return UiActionResult.success(before, toViewSnapshot(result.after()), result.events());
    }

    private void rememberRemovedKnight(List<BattleEvent> events) {
        for (BattleEvent event : events) {
            if (event instanceof BattleEvent.PieceRemoved removed && removed.pieceId().equals("knight")) {
                lastKnownKnightPosition = removed.at();
            }
        }
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
        Position knightPosition;
        Set<Position> moves;

        if (knight.isPresent()) {
            BattleSnapshot.PieceView piece = knight.get();
            knightPosition = piece.position();
            lastKnownKnightPosition = knightPosition;
            moves = legalMoves(piece.id());
        } else if (lastKnownKnightPosition != null) {
            knightPosition = lastKnownKnightPosition;
            moves = Set.of();
        } else {
            throw new IllegalStateException("UI di chuyển hiện tại cần quân có ID knight");
        }

        Set<Position> rocks = new HashSet<>();
        for (Position rock : snapshot.rocks()) rocks.add(rock);

        Map<String, PresentationPiece> piecesById = new HashMap<>();
        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            Position position = piece.position();
            piecesById.put(piece.id(), new PresentationPiece(
                piece.id(), piece.type(), piece.hp(), position
            ));
        }

        return new BattleViewSnapshot(
            snapshot.globalTurn(),
            knightPosition,
            rocks,
            moves,
            piecesById
        );
    }

}
