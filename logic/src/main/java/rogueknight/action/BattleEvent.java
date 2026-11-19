package rogueknight.action;

import rogueknight.board.Position;

/*
 * BattleEvent.java  |  T-02.01  |  TD: 4.10, DEC-08
 * Sự kiện đã xảy ra trong một submit, theo thứ tự, để UI phát lại.
 * T-02.01 chỉ có PieceMoved; Damage/Remove/Promotion thêm ở task sau.
 */
public sealed interface BattleEvent {

    record PieceMoved(String pieceId, Position from, Position to) implements BattleEvent {}
}
