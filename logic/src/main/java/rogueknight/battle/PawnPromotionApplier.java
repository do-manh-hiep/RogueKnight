package rogueknight.battle;

import rogueknight.action.BattleEvent;

import java.util.Objects;
import rogueknight.board.Position;

/**
 * Handles promotion of a Pawn reaching row 0 (rank 1) into a Queen.
 * According to TD specifications and DEC-05/DEC-07:
 * - Retains original id and spawnOrder.
 * - Changes type to QUEEN.
 * - Stats: HP = 2, cycle = 4 (from balance.json).
 * - Schedule: nextActivationTurn = currentTurn + 5 (distinct schedule, does not use +cycle).
 * - currentDirection = null (only Pawns have direction).
 * - Does not attack immediately in the turn of promotion.
 */
final class PawnPromotionApplier {

    public static final int PROMOTED_QUEEN_HP = 2;
    public static final int PROMOTED_QUEEN_CYCLE = 4;
    public static final int PROMOTION_ACTIVATION_OFFSET = 5;

    private PawnPromotionApplier() { }

    /**
     * Checks whether the Pawn at the given position is eligible for promotion.
     */
    static boolean canPromote(Position position) {
        return position != null && position.row() == 0;
    }

    /**
     * Executes promotion for the Pawn if it reached row 0.
     *
     * @param state       mutable battle state
     * @param pawnId      ID of the Pawn
     * @param currentTurn current global turn
     * @return the PiecePromoted event
     */
    static BattleEvent.PiecePromoted promote(BattleState state, String pawnId, int currentTurn) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");

        PieceState pawn = state.pieceState(pawnId).orElseThrow(
                () -> new IllegalArgumentException("Unknown piece: " + pawnId)
        );

        if (pawn.type() != PieceType.PAWN) {
            throw new IllegalArgumentException("Piece is not a PAWN: " + pawnId);
        }

        Position pos = state.positionOf(pawnId).orElseThrow(
                () -> new IllegalStateException("Piece without position: " + pawnId)
        );

        if (!canPromote(pos)) {
            throw new IllegalStateException("Pawn at " + pos + " is not at row 0");
        }

        PieceState queen = new PieceState(
                pawn.id(),
                PieceType.QUEEN,
                PROMOTED_QUEEN_HP,
                PROMOTED_QUEEN_CYCLE,
                currentTurn + PROMOTION_ACTIVATION_OFFSET,
                pawn.spawnOrder(),
                null
        );

        state.replacePiece(queen);

        return new BattleEvent.PiecePromoted(pawn.id(), PieceType.PAWN, PieceType.QUEEN, pos);
    }
}
