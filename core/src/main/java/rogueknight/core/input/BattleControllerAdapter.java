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
import rogueknight.core.view.BoardPosition;
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
    private BoardPosition lastKnownKnightPosition;

    public BattleControllerAdapter(BattleController controller) {
        this.controller = Objects.requireNonNull(controller, "controller must not be null");
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
    public Set<BoardPosition> legalTargets(String actorId, ActionMode mode) {
        if (mode == null || mode == ActionMode.WAIT) return Set.of();

        Set<BoardPosition> positions = new HashSet<>();
        if (mode == ActionMode.MOVE) {
            for (Position position : controller.legalMoves(actorId)) {
                positions.add(toBoardPosition(position));
            }
            for (Position position : controller.legalTargets(actorId, ActionKind.SWORD)) {
                positions.add(toBoardPosition(position));
            }
        } else {
            for (Position position : controller.legalTargets(actorId, toKind(mode))) {
                positions.add(toBoardPosition(position));
            }
        }
        return Set.copyOf(positions);
    }

    @Override
    public UiActionResult submitAction(String actorId, ActionMode mode, BoardPosition target) {
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

        if (!result.accepted()) return UiActionResult.rejected(result.error().toString(), before);

        rememberRemovedKnight(result.events());
        return UiActionResult.success(before, toViewSnapshot(result.after()), result.events());
    }

    @Override
    public UiActionResult submitWait(String actorId) {
        BattleViewSnapshot before = snapshot();
        ActionResult result = controller.submit(ActionRequest.waitAction(actorId));
        if (!result.accepted()) return UiActionResult.rejected(result.error().toString(), before);

        rememberRemovedKnight(result.events());
        return UiActionResult.success(before, toViewSnapshot(result.after()), result.events());
    }

    private void rememberRemovedKnight(List<BattleEvent> events) {
        for (BattleEvent event : events) {
            if (event instanceof BattleEvent.PieceRemoved removed && removed.pieceId().equals("knight")) {
                lastKnownKnightPosition = toBoardPosition(removed.at());
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
        BoardPosition knightPosition;
        Set<BoardPosition> moves;

        if (knight.isPresent()) {
            BattleSnapshot.PieceView piece = knight.get();
            knightPosition = toBoardPosition(piece.position());
            lastKnownKnightPosition = knightPosition;
            moves = legalMoves(piece.id());
        } else if (lastKnownKnightPosition != null) {
            knightPosition = lastKnownKnightPosition;
            moves = Set.of();
        } else {
            throw new IllegalStateException("UI di chuyển hiện tại cần quân có ID knight");
        }

        Set<BoardPosition> rocks = new HashSet<>();
        for (Position rock : snapshot.rocks()) rocks.add(toBoardPosition(rock));

        Map<String, PresentationPiece> piecesById = new HashMap<>();
        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            BoardPosition position = toBoardPosition(piece.position());
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

    private static BoardPosition toBoardPosition(Position position) {
        return new BoardPosition(position.column(), position.row());
    }
}
