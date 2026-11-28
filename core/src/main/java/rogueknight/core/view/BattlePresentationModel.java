package rogueknight.core.view;

import rogueknight.board.Position;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import rogueknight.action.BattleEvent;
import rogueknight.battle.BattleResult;

/**
 * Mutable presentation-only projection of a battle.
 *
 * <p>The model is bootstrapped from an immutable view snapshot and then advanced
 * exclusively by the ordered domain events. It never writes to the domain state.</p>
 */
public final class BattlePresentationModel {
    private final Map<String, PresentationPiece> piecesById = new LinkedHashMap<>();
    private int globalTurn;
    private BattleResult result;

    private BattlePresentationModel(BattleViewSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        globalTurn = snapshot.globalTurn();
        piecesById.putAll(snapshot.piecesById());
        validateInitialPieces(snapshot);
    }

    public static BattlePresentationModel from(BattleViewSnapshot snapshot) {
        return new BattlePresentationModel(snapshot);
    }

    public int globalTurn() {
        return globalTurn;
    }

    public Map<String, PresentationPiece> piecesById() {
        return Map.copyOf(piecesById);
    }

    public Optional<PresentationPiece> piece(String pieceId) {
        return Optional.ofNullable(piecesById.get(pieceId));
    }

    public Optional<BattleResult> result() {
        return Optional.ofNullable(result);
    }

    /** Applies one event. Event order and identity mismatches are treated as programming errors. */
    public void apply(BattleEvent event) {
        Objects.requireNonNull(event, "event");
        if (result != null) {
            throw new IllegalStateException("Cannot apply event after BattleEnded: " + event);
        }

        if (event instanceof BattleEvent.ActionPerformed || event instanceof BattleEvent.ActorActivated) {
            return; // Cues for the playback layer; no board-state mutation.
        }
        if (event instanceof BattleEvent.TurnAdvanced advanced) {
            if (advanced.oldTurn() != globalTurn || advanced.newTurn() != advanced.oldTurn() + 1) {
                throw new IllegalStateException("Out-of-order TurnAdvanced event: " + advanced);
            }
            globalTurn = advanced.newTurn();
            return;
        }
        if (event instanceof BattleEvent.PieceMoved moved) {
            PresentationPiece piece = requirePiece(moved.pieceId());
            Position from = toPosition(moved.from());
            if (!piece.position().equals(from)) {
                throw new IllegalStateException("PieceMoved source mismatch for " + moved.pieceId());
            }
            piecesById.put(piece.id(), piece.at(toPosition(moved.to())));
            return;
        }
        if (event instanceof BattleEvent.PieceDamaged damaged) {
            PresentationPiece piece = requirePiece(damaged.pieceId());
            if (damaged.amount() <= 0 || damaged.remainingHp() < 0
                    || damaged.remainingHp() >= piece.hp()) {
                throw new IllegalStateException("Invalid PieceDamaged event: " + damaged);
            }
            if (damaged.remainingHp() == 0) {
                // Keep a zero-HP piece until the following PieceRemoved event is consumed.
                piecesById.put(piece.id(), piece.withHp(0));
            } else {
                piecesById.put(piece.id(), piece.withHp(damaged.remainingHp()));
            }
            return;
        }
        if (event instanceof BattleEvent.PieceRemoved removed) {
            PresentationPiece piece = requirePiece(removed.pieceId());
            if (!piece.position().equals(toPosition(removed.at()))) {
                throw new IllegalStateException("PieceRemoved position mismatch for " + removed.pieceId());
            }
            piecesById.remove(piece.id());
            return;
        }
        if (event instanceof BattleEvent.PiecePromoted promoted) {
            PresentationPiece piece = requirePiece(promoted.pieceId());
            if (piece.type() != promoted.fromType()
                    || !piece.position().equals(toPosition(promoted.at()))) {
                throw new IllegalStateException("PiecePromoted state mismatch for " + promoted.pieceId());
            }
            piecesById.put(piece.id(), piece.withType(promoted.toType()));
            return;
        }
        if (event instanceof BattleEvent.BattleEnded ended) {
            if (ended.result() == BattleResult.ONGOING) {
                throw new IllegalStateException("BattleEnded cannot carry ONGOING");
            }
            result = ended.result();
            return;
        }
        throw new IllegalArgumentException("Unsupported battle event: " + event.getClass().getName());
    }

    private PresentationPiece requirePiece(String id) {
        PresentationPiece piece = piecesById.get(id);
        if (piece == null) throw new IllegalStateException("Unknown presentation piece: " + id);
        return piece;
    }

    private void validateInitialPieces(BattleViewSnapshot snapshot) {
        for (Map.Entry<String, PresentationPiece> entry : piecesById.entrySet()) {
            PresentationPiece piece = Objects.requireNonNull(entry.getValue(), "presentation piece");
            if (!entry.getKey().equals(piece.id())) {
                throw new IllegalArgumentException("Presentation map key does not match piece id: " + entry.getKey());
            }
            if (snapshot.rocks().contains(piece.position())) {
                throw new IllegalArgumentException("Piece overlaps rock at " + piece.position());
            }
        }
    }

    private static Position toPosition(rogueknight.board.Position position) {
        return new Position(position.column(), position.row());
    }
}
