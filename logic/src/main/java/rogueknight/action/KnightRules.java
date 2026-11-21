package rogueknight.action;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/** Read-only validation and legal-target generation for the player's Knight. */
public final class KnightRules {
    /** DEC-03 toggles: adjust if team finalizes edge-case rules differently. */
    public static final boolean ALLOW_CANNON_MOVE_WITHOUT_TERMINAL_BLOCKER = true;
    public static final boolean ALLOW_EMPTY_SKEWER = false;

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

    /** Legal target squares for MOVE, SWORD, CAPTURE, or equipped items. WAIT has no target square. */
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
            case CANNON_MOVEMENT -> validateCannonMovement(state, request, from);
            case CANNON_ATTACK -> validateCannonAttack(state, request, from);
            case DIAGONAL_SKEWER -> validateDiagonalSkewer(state, request, from);
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

    private static ValidationOutcome validateCannonMovement(BattleState state, ActionRequest request, Position from) {
        Optional<ActionError> eqErr = checkEquipmentReady(state, EquipmentType.CANNON_MOVEMENT);
        if (eqErr.isPresent()) return ValidationOutcome.invalid(eqErr.get());
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);

        Position target = request.target();
        RayScanResult scan = scanOrthogonalRay(state, from, target);
        if (!scan.orthogonal || scan.obstaclesBeforeTarget != 1) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }
        if (state.isRock(target)) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);

        Optional<String> targetId = state.pieceAt(target);
        if (targetId.isPresent()) {
            PieceState victim = state.pieceState(targetId.get()).orElseThrow();
            if (victim.type() == PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);
            if (victim.hp() > 2) return ValidationOutcome.invalid(ActionError.CAPTURE_TARGET_TOO_STRONG);
            return ValidationOutcome.valid(new ActionPlan(
                ActionKind.CANNON_MOVEMENT,
                request.actorId(),
                from,
                target,
                victim.id(),
                EquipmentType.CANNON_MOVEMENT,
                target,
                List.of(victim.id())
            ));
        }

        if (!ALLOW_CANNON_MOVE_WITHOUT_TERMINAL_BLOCKER && !scan.hasTerminalBlockerBehindTarget) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }
        return ValidationOutcome.valid(new ActionPlan(
            ActionKind.CANNON_MOVEMENT,
            request.actorId(),
            from,
            target,
            null,
            EquipmentType.CANNON_MOVEMENT,
            target,
            List.of()
        ));
    }

    private static ValidationOutcome validateCannonAttack(BattleState state, ActionRequest request, Position from) {
        Optional<ActionError> eqErr = checkEquipmentReady(state, EquipmentType.CANNON_ATTACK);
        if (eqErr.isPresent()) return ValidationOutcome.invalid(eqErr.get());
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);

        Position target = request.target();
        RayScanResult scan = scanOrthogonalRay(state, from, target);
        if (!scan.orthogonal || scan.obstaclesBeforeTarget != 1) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }
        if (state.isRock(target)) return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);
        Optional<String> targetId = state.pieceAt(target);
        if (targetId.isEmpty()) return ValidationOutcome.invalid(ActionError.NO_TARGET_PIECE);

        PieceState victim = state.pieceState(targetId.get()).orElseThrow();
        if (victim.type() == PieceType.KNIGHT) return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);

        return ValidationOutcome.valid(new ActionPlan(
            ActionKind.CANNON_ATTACK,
            request.actorId(),
            from,
            target,
            victim.id(),
            EquipmentType.CANNON_ATTACK,
            from,
            List.of(victim.id())
        ));
    }

    private static ValidationOutcome validateDiagonalSkewer(BattleState state, ActionRequest request, Position from) {
        Optional<ActionError> eqErr = checkEquipmentReady(state, EquipmentType.DIAGONAL_SKEWER);
        if (eqErr.isPresent()) return ValidationOutcome.invalid(eqErr.get());
        if (!request.targetInBounds()) return ValidationOutcome.invalid(ActionError.OUT_OF_BOUNDS);

        Position target = request.target();
        int dx = target.column() - from.column();
        int dy = target.row() - from.row();
        if (Math.abs(dx) != Math.abs(dy) || (Math.abs(dx) != 1 && Math.abs(dx) != 2)) {
            return ValidationOutcome.invalid(ActionError.INVALID_GEOMETRY);
        }

        int stepCol = Integer.signum(dx);
        int stepRow = Integer.signum(dy);
        List<Position> traversed = new ArrayList<>(2);
        List<String> hitIds = new ArrayList<>(2);
        Position landing = from;

        for (int step = 1; step <= 2; step++) {
            int col = from.column() + step * stepCol;
            int row = from.row() + step * stepRow;
            if (!Position.isInBounds(col, row)) break;
            Position cur = new Position(col, row);
            if (state.isRock(cur)) break;

            traversed.add(cur);
            Optional<String> occupantId = state.pieceAt(cur);
            if (occupantId.isPresent()) {
                PieceState victim = state.pieceState(occupantId.get()).orElseThrow();
                if (victim.type() == PieceType.KNIGHT) {
                    return ValidationOutcome.invalid(ActionError.NOT_ENEMY_TARGET);
                }
                hitIds.add(victim.id());
                if (victim.hp() <= 1) {
                    landing = cur;
                }
            } else {
                landing = cur;
            }
        }

        if (!traversed.contains(target)) {
            return ValidationOutcome.invalid(ActionError.OCCUPIED_BY_ROCK);
        }
        if (!ALLOW_EMPTY_SKEWER && hitIds.isEmpty()) {
            return ValidationOutcome.invalid(ActionError.NO_TARGET_PIECE);
        }

        return ValidationOutcome.valid(new ActionPlan(
            ActionKind.DIAGONAL_SKEWER,
            request.actorId(),
            from,
            target,
            hitIds.isEmpty() ? null : hitIds.get(0),
            EquipmentType.DIAGONAL_SKEWER,
            landing,
            hitIds
        ));
    }

    private record RayScanResult(boolean orthogonal, int obstaclesBeforeTarget, boolean hasTerminalBlockerBehindTarget) { }

    private static RayScanResult scanOrthogonalRay(BattleState state, Position from, Position target) {
        int dx = target.column() - from.column();
        int dy = target.row() - from.row();
        if ((dx == 0 && dy == 0) || (dx != 0 && dy != 0)) {
            return new RayScanResult(false, 0, false);
        }
        int stepCol = Integer.signum(dx);
        int stepRow = Integer.signum(dy);
        int obstaclesBefore = 0;

        int col = from.column() + stepCol;
        int row = from.row() + stepRow;
        while (col != target.column() || row != target.row()) {
            Position cur = new Position(col, row);
            if (state.isRock(cur) || state.pieceAt(cur).isPresent()) {
                obstaclesBefore++;
            }
            col += stepCol;
            row += stepRow;
        }

        boolean hasBlockerBehind = false;
        col = target.column() + stepCol;
        row = target.row() + stepRow;
        while (Position.isInBounds(col, row)) {
            Position behind = new Position(col, row);
            if (state.isRock(behind) || state.pieceAt(behind).isPresent()) {
                hasBlockerBehind = true;
                break;
            }
            col += stepCol;
            row += stepRow;
        }
        return new RayScanResult(true, obstaclesBefore, hasBlockerBehind);
    }

    private static Optional<ActionError> checkEquipmentReady(BattleState state, EquipmentType type) {
        if (!state.inventory().contains(type)) {
            return Optional.of(ActionError.EQUIPMENT_NOT_OWNED);
        }
        if (state.inventory().chargesOf(type) <= 0) {
            return Optional.of(ActionError.NO_EQUIPMENT_CHARGES);
        }
        return Optional.empty();
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