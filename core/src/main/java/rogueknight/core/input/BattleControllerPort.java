package rogueknight.core.input;

import rogueknight.core.view.BattleViewSnapshot;
import rogueknight.core.view.BoardPosition;

import java.util.Set;

/**
 * Controller Port abstraction connecting UI/Input handlers to the game battle state.
 * Allows decoupling UI development (T-02.02) from domain battle controller (T-02.01).
 */
public interface BattleControllerPort {
    /** Returns an immutable view snapshot of current battle state. */
    BattleViewSnapshot snapshot();

    /** Returns all legal target positions for the specified actor's move action. */
    Set<BoardPosition> legalMoves(String actorId);

    /**
     * Submits a move request for the specified actor to the target position.
     * Advances turn on success; leaves state completely unchanged on rejection.
     */
    MoveResult submitMove(String actorId, BoardPosition target);
}
