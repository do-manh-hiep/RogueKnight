package rogueknight.action;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/** Read-only validation and legal-target generation for the player's Knight. */
public final class KnightRules {
    private KnightRules() { }

    public static boolean isMoveGeometry(Position from, Position to) {
        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());
        boolean lShape = (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
        boolean adjacent = Math.max(dx, dy) == 1;
        return lShape || adjacent;
    }

    public static boolean isCaptureGeometry(Position from, Position to) {
        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());
        return (dx == 1 && dy == 2) || (dx == 2 && dy == 1);
    }

    public static boolean isSwordGeometry(Position from, Position to) {
        int dx = Math.abs(to.column() - from.column());
        int dy = Math.abs(to.row() - from.row());
        return Math.max(dx, dy) == 1;
    }

    /** Legal target squares for MOVE, SWORD or CAPTURE. WAIT has no target square. */
    public static Set<Position> legalTargets(BattleState state, String actorId, ActionKind kind) {
        if (state == null || kind == null || kind == ActionKind.WAIT) return Set.of();
        Optional<Position> from = knightPosition(state, actorId);
        if (from.isEmpty()) return Set.of();

        Set<Position> targets = new HashSet<>();
        for (int row = 0; row < Position.BOARD_SIZE; row++) {
            for (int column = 0; column < Position.BOARD_SIZE; column++) {
                Position target = new Position(column, row);
                ActionRequest request = new ActionRequest(actorId, kind, column, row);
                if (validate(state, request).isValid()) targets.add(target);
            }
        }
        return Set.copyOf(targets);
    }

    static ValidationOutcome validate(BattleState state, ActionRequest request) {
        if (request == null || request.actorId() == null || request.kind() == null) {
            return ValidationOutcome.invalid(ActionError.INVALID_PAYLOAD);
        }
        Optional<PieceState> actor = state.pieceState(request.actorId());
        if (actor.isEmpty()) return ValidationOutcome.invalid(ActionError.UNKNOWN_ACTOR);
        if (actor.get().type() != PieceType.KNIGHT) {
            return ValidationOutcome.invalid(ActionError.NOT_PLAYER_ACTOR);
        }
        Position from = state.positionOf(request.actorId()).orElseThrow();

        return switch (request.kind()) {
            case MOVE -> validateMove(state, request, from);
            case SWORD -> validateSword(state, request, from);
            case CAPTURE -> validateCapture(state, request, from);
            case WAIT -> ValidationOutcome.valid(ActionPlan.waitPlan(request.actorId(), from));
        };
    }

    private static ValidationOutcome validateMove(BattleState state, ActionRequest request, Position from) {
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);
        Position to = request.target();
        if (!isMoveGeometry(from, to)) return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        Optional<ActionError> occupied = emptyTargetError(state, to);
        if (occupied.isPresent()) return ValidationOutcome.invalid(occupied.get());
        return ValidationOutcome.valid(new ActionPlan(ActionKind.MOVE, request.actorId(), from, to, null));
    }

    private static ValidationOutcome validateSword(BattleState state, ActionRequest request, Position from) {
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);
        Position target = request.target();
        if (!isSwordGeometry(from, target)) return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        if (state.isRock(target)) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);
        Optional<String> targetId = state.pieceAt(target);
        if (targetId.isEmpty()) return ValidationOutcome.invalid(ActionError.NO_TARGET_PIECE);
        PieceState victim = state.pieceState(targetId.get()).orElseThrow();
        if (victim.type() == PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);
        return ValidationOutcome.valid(new ActionPlan(ActionKind.SWORD, request.actorId(), from, target, victim.id()));
    }

    private static ValidationOutcome validateCapture(BattleState state, ActionRequest request, Position from) {
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);
        Position target = request.target();
        if (!isCaptureGeometry(from, target)) return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        if (state.isRock(target)) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);
        Optional<String> targetId = state.pieceAt(target);
        if (targetId.isEmpty()) return ValidationOutcome.invalid(ActionError.NO_TARGET_PIECE);
        PieceState victim = state.pieceState(targetId.get()).orElseThrow();
        if (victim.type() == PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);
        if (victim.hp() > 2) return ValidationOutcome.invalid(ActionError.CAPTURE_TARGET_TOO_STRONG);
        return ValidationOutcome.valid(new ActionPlan(ActionKind.CAPTURE, request.actorId(), from, target, victim.id()));
    }

    private static Optional<ActionError> emptyTargetError(BattleState state, Position target) {
        if (state.isRock(target)) return Optional.of(ActionError.OCCUPIED_BY_ROCK);
        if (state.pieceAt(target).isPresent()) return Optional.of(ActionError.OCCUPIED_BY_PIECE);
        return Optional.empty();
    }

    private static Optional<Position> knightPosition(BattleState state, String actorId) {
        if (actorId == null) return Optional.empty();
        return state.pieceState(actorId)
                .filter(piece -> piece.type() == PieceType.KNIGHT)
                .flatMap(piece -> state.positionOf(actorId));
    }
}
