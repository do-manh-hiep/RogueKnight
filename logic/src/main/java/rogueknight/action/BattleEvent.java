package rogueknight.action;

import rogueknight.battle.BattleResult;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/**
 * Ordered domain events of one accepted submit: the Player Action, then the Enemy queue (TD 4.10).
 * Events describe what already happened; the UI replays them and never re-derives rules from them.
 */
public sealed interface BattleEvent {
    record ActionPerformed(ActionKind kind, String actorId) implements BattleEvent { }
    record PieceMoved(String pieceId, Position from, Position to) implements BattleEvent { }
    record PieceDamaged(String pieceId, int amount, int remainingHp) implements BattleEvent { }
    record PieceRemoved(String pieceId, Position at) implements BattleEvent { }
    /** A Pawn reached row 1 inside its Move; same ID, new type (DEC-07). */
    record PiecePromoted(String pieceId, PieceType fromType, PieceType toType, Position at) implements BattleEvent { }
    /** Emitted exactly once per accepted Player Action that leaves the battle ongoing; never per Enemy. */
    record TurnAdvanced(int oldTurn, int newTurn) implements BattleEvent { }
    /** An Enemy starts its activation, including activations that end in a skip (UI shake cue). */
    record ActorActivated(String actorId, Position at, int turn) implements BattleEvent { }
    /** The battle has just reached a terminal result; emitted at most once per battle. */
    record BattleEnded(BattleResult result) implements BattleEvent { }
}
