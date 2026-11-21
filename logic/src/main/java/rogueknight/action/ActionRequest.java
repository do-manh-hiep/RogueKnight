package rogueknight.action;

import rogueknight.board.Position;

/** Value-only request sent by UI/AI to BattleController. */
public record ActionRequest(String actorId, ActionKind kind, int targetColumn, int targetRow) {

    public static ActionRequest move(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.MOVE, target.column(), target.row());
    }

    public static ActionRequest move(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.MOVE, targetColumn, targetRow);
    }

    public static ActionRequest sword(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.SWORD, target.column(), target.row());
    }

    public static ActionRequest sword(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.SWORD, targetColumn, targetRow);
    }

    public static ActionRequest capture(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.CAPTURE, target.column(), target.row());
    }

    public static ActionRequest capture(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.CAPTURE, targetColumn, targetRow);
    }

    public static ActionRequest cannonMovement(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.CANNON_MOVEMENT, target.column(), target.row());
    }

    public static ActionRequest cannonMovement(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.CANNON_MOVEMENT, targetColumn, targetRow);
    }

    public static ActionRequest cannonAttack(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.CANNON_ATTACK, target.column(), target.row());
    }

    public static ActionRequest cannonAttack(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.CANNON_ATTACK, targetColumn, targetRow);
    }

    public static ActionRequest diagonalSkewer(String actorId, Position target) {
        return new ActionRequest(actorId, ActionKind.DIAGONAL_SKEWER, target.column(), target.row());
    }

    public static ActionRequest diagonalSkewer(String actorId, int targetColumn, int targetRow) {
        return new ActionRequest(actorId, ActionKind.DIAGONAL_SKEWER, targetColumn, targetRow);
    }

    /** WAIT has no target; sentinel coordinates are ignored by its rule. */
    public static ActionRequest waitAction(String actorId) {
        return new ActionRequest(actorId, ActionKind.WAIT, -1, -1);
    }

    public boolean targetInBounds() {
        return Position.isInBounds(targetColumn, targetRow);
    }

    /** Call only after targetInBounds() is true. */
    public Position target() {
        return new Position(targetColumn, targetRow);
    }
}