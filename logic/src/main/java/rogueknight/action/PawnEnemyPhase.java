package rogueknight.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/**
 * Implementation of EnemyPhase focusing on Pawn activation, movement, attack, and promotion (T-03.03).
 * Execution order:
 * 1. Collect all live enemies with nextActivationTurn <= globalTurn.
 * 2. Order by PieceType rank (PAWN, ROOK, BISHOP, KING, QUEEN), then by spawnOrder ascending.
 * 3. For each Pawn:
 *    - Check for attack on Knight in 3 forward tiles. If yes, attack and end activation.
 *    - If no attack, BFS pathfind 1 step towards row 0.
 *    - Move 1 step and update direction. No attack after move.
 *    - If reaching row 0, promote immediately to Queen (nextActivationTurn = currentTurn + 5).
 *    - If not promoted, advance nextActivationTurn by cycle.
 * 4. Stop immediately if Knight dies or battle ends.
 */
public final class PawnEnemyPhase implements EnemyPhase {

    @Override
    public List<BattleEvent> run(BattleState state) {
        Objects.requireNonNull(state, "state must not be null");

        if (state.result() != BattleResult.ONGOING) {
            return List.of();
        }

        List<BattleEvent> events = new ArrayList<>();
        int currentTurn = state.globalTurn();

        // Build activation queue from snapshot of live pieces
        BattleSnapshot snapshot = state.snapshot();
        List<String> activationQueue = snapshot.pieces().stream()
                .filter(p -> p.type() != PieceType.KNIGHT)
                .filter(p -> p.nextActivationTurn() <= currentTurn)
                .sorted(Comparator.comparingInt((BattleSnapshot.PieceView p) -> typeRank(p.type()))
                        .thenComparingInt(BattleSnapshot.PieceView::spawnOrder))
                .map(BattleSnapshot.PieceView::id)
                .toList();

        for (String enemyId : activationQueue) {
            // Check terminal state before each piece activation
            if (state.result() != BattleResult.ONGOING) {
                break;
            }

            Optional<PieceState> pieceOpt = state.pieceState(enemyId);
            if (pieceOpt.isEmpty() || pieceOpt.get().hp() <= 0) {
                continue; // Piece was eliminated earlier in the phase
            }

            PieceState piece = pieceOpt.get();

            if (piece.type() == PieceType.PAWN) {
                activatePawn(state, enemyId, currentTurn, events);
            } else {
                // Non-pawn enemies (T-03.02 scope): advance schedule so they do not loop indefinitely
                state.replacePiece(piece.withNextActivationTurn(piece.nextActivationTurn() + piece.cycle()));
            }
        }

        return List.copyOf(events);
    }

    private void activatePawn(BattleState state, String pawnId, int currentTurn, List<BattleEvent> events) {
        // 1. Attack check prior to moving
        if (PawnAttackRule.canAttackKnight(state, pawnId)) {
            List<BattleEvent> attackEvents = PawnAttackRule.executeAttack(state, pawnId);
            events.addAll(attackEvents);

            // Pawn remains in place; advance next activation turn by cycle
            Optional<PieceState> updatedPawn = state.pieceState(pawnId);
            if (updatedPawn.isPresent() && updatedPawn.get().hp() > 0) {
                PieceState p = updatedPawn.get();
                state.replacePiece(p.withNextActivationTurn(p.nextActivationTurn() + p.cycle()));
            }
            return;
        }

        // 2. Pathfind and move towards row 0
        Optional<PawnPathfinder.NextMove> nextMoveOpt = PawnPathfinder.findNextMove(state, pawnId);
        if (nextMoveOpt.isPresent()) {
            PawnPathfinder.NextMove nextMove = nextMoveOpt.get();
            Position from = state.positionOf(pawnId).orElseThrow();
            Position to = nextMove.target();

            state.movePiece(pawnId, to);
            events.add(new BattleEvent.PieceMoved(pawnId, from, to));

            PieceState movedPawn = state.pieceState(pawnId).orElseThrow()
                    .withDirection(nextMove.direction());
            state.replacePiece(movedPawn);

            // 3. Promotion check upon reaching row 0
            if (PawnPromotionApplier.canPromote(to)) {
                BattleEvent.PiecePromoted promotedEvt = PawnPromotionApplier.promote(state, pawnId, currentTurn);
                events.add(promotedEvt);
                // Promotion sets nextActivationTurn = currentTurn + 5; do not advance by cycle!
            } else {
                // Regular advance by cycle
                PieceState finalPawn = state.pieceState(pawnId).orElseThrow();
                state.replacePiece(finalPawn.withNextActivationTurn(finalPawn.nextActivationTurn() + finalPawn.cycle()));
            }
        } else {
            // Cannot move (blocked or no path): advance schedule
            PieceState p = state.pieceState(pawnId).orElseThrow();
            state.replacePiece(p.withNextActivationTurn(p.nextActivationTurn() + p.cycle()));
        }
    }

    private static int typeRank(PieceType type) {
        return switch (type) {
            case PAWN -> 1;
            case ROOK -> 2;
            case BISHOP -> 3;
            case KING -> 4;
            case QUEEN -> 5;
            case KNIGHT -> 0;
        };
    }
}
