package rogueknight.action;

import rogueknight.board.Position;

/** Ordered domain events emitted by a successfully resolved Player Action. */
public sealed interface BattleEvent {
    record ActionPerformed(ActionKind kind, String actorId) implements BattleEvent { }
    record PieceMoved(String pieceId, Position from, Position to) implements BattleEvent { }
    record PieceDamaged(String pieceId, int amount, int remainingHp) implements BattleEvent { }
    record PieceRemoved(String pieceId, Position at) implements BattleEvent { }
}
