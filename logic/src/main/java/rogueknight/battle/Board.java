package rogueknight.battle;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import rogueknight.board.Position;

/** Package-private board storage owned exclusively by BattleState. */
final class Board {
    private final Set<Position> rocks;
    private final Map<String, Position> positions = new LinkedHashMap<>();

    Board(Set<Position> rocks) {
        this.rocks = Set.copyOf(rocks);
    }

    Set<Position> rocks() { return rocks; }
    boolean isRock(Position position) { return rocks.contains(position); }

    Optional<String> pieceAt(Position position) {
        Objects.requireNonNull(position, "position");
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            if (entry.getValue().equals(position)) return Optional.of(entry.getKey());
        }
        return Optional.empty();
    }

    boolean isEmpty(Position position) {
        Objects.requireNonNull(position);
        return !isRock(position) && pieceAt(position).isEmpty();
    }

    Optional<Position> positionOf(String pieceId) {
        requireId(pieceId);
        return Optional.ofNullable(positions.get(pieceId));
    }

    Set<String> pieceIds() { return Set.copyOf(positions.keySet()); }
    Map<String, Position> positionsCopy() { return Map.copyOf(positions); }

    void place(String pieceId, Position position) {
        requireId(pieceId);
        if (positions.containsKey(pieceId)) {
            throw new IllegalArgumentException("Duplicate piece id: " + pieceId);
        }
        requireFree(position);
        positions.put(pieceId, position);
    }

    void move(String pieceId, Position destination) {
        requireKnown(pieceId);
        requireFree(destination);
        positions.put(pieceId, destination);
    }

    void remove(String pieceId) {
        requireKnown(pieceId);
        positions.remove(pieceId);
    }

    private static void requireId(String pieceId) {
        if (pieceId == null || pieceId.isBlank()) {
            throw new IllegalArgumentException("Piece id must not be blank");
        }
    }

    private void requireKnown(String pieceId) {
        if (!positions.containsKey(pieceId)) {
            throw new IllegalArgumentException("Unknown piece id: " + pieceId);
        }
    }

    private void requireFree(Position position) {
        Objects.requireNonNull(position);
        if (isRock(position)) {
            throw new IllegalArgumentException("Tile is a rock: " + position);
        }
        if (pieceAt(position).isPresent()) {
            throw new IllegalArgumentException("Tile is occupied: " + position);
        }
    }
}
