package rogueknight.battle;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import rogueknight.board.Position;

/** Detached, deeply immutable value snapshot for UI, tests and later replay integration. */
public record BattleSnapshot(int globalTurn, BattlePhase phase, BattleResult result, Set<Position> rocks, List<PieceView> pieces, Inventory inventory) {
    public BattleSnapshot {
        if (globalTurn < 1) throw new IllegalArgumentException("globalTurn must be >= 1");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(rocks, "rocks");
        Objects.requireNonNull(pieces, "pieces");
        Objects.requireNonNull(inventory, "inventory");
        rocks = Set.copyOf(rocks);
        pieces = pieces.stream().map(piece -> Objects.requireNonNull(piece, "piece"))
                .sorted(Comparator.comparingInt(PieceView::spawnOrder).thenComparing(PieceView::id)).toList();
        Set<String> ids = new HashSet<>();
        Set<Position> occupied = new HashSet<>();
        for (PieceView piece : pieces) {
            if (!ids.add(piece.id())) throw new IllegalArgumentException("Duplicate piece id: " + piece.id());
            if (rocks.contains(piece.position()) || !occupied.add(piece.position()))
                throw new IllegalArgumentException("Conflicting occupancy in snapshot: " + piece.position());
        }
    }
    public Optional<PieceView> pieceById(String id) { return pieces.stream().filter(piece -> piece.id().equals(id)).findFirst(); }
    public Optional<PieceView> pieceAt(Position position) { return pieces.stream().filter(piece -> piece.position().equals(position)).findFirst(); }

        public record PieceView(
        String id,
        PieceType type,
        int hp,
        int cycle,
        int nextActivationTurn,
        int spawnOrder,
        Position position,
        Direction currentDirection
    ) {
        public PieceView {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Piece id must not be blank");
            }

            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(position, "position");

            if (hp <= 0 || cycle < 0 || nextActivationTurn < 0) {
                throw new IllegalArgumentException("Invalid live piece stats");
            }

            if (type == PieceType.PAWN && currentDirection == null) {
                throw new IllegalArgumentException(
                    "Pawn view must have currentDirection"
                );
            }

            if (type != PieceType.PAWN && currentDirection != null) {
                throw new IllegalArgumentException(
                    "Only Pawn view can have currentDirection"
                );
            }
        }

        // Giữ tương thích với code cũ chưa truyền hướng.
        public PieceView(
            String id,
            PieceType type,
            int hp,
            int cycle,
            int nextActivationTurn,
            int spawnOrder,
            Position position
        ) {
            this(
                id,
                type,
                hp,
                cycle,
                nextActivationTurn,
                spawnOrder,
                position,
                type == PieceType.PAWN ? Direction.SOUTH : null
            );
        }
    }
}
