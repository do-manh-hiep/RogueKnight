package rogueknight.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.StateView;
import rogueknight.board.Position;

/** Shared reverse-BFS path search used for Pawn setup direction and subsequent movement. */
public final class PawnPathfinder {
    private static final Comparator<Position> BOARD_ORDER =
            Comparator.comparingInt(Position::column).thenComparingInt(Position::row);

    private PawnPathfinder() { }

    /** Finds the initial facing direction. Initial planning ignores the Knight as an obstacle. */
    public static Direction initialDirection(BattleSnapshot snapshot, String pawnId) {
        Objects.requireNonNull(snapshot, "snapshot");
        BattleSnapshot.PieceView pawn = snapshot.pieceById(pawnId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown Pawn: " + pawnId));
        if (pawn.type() != PieceType.PAWN) {
            throw new IllegalArgumentException("Piece must be PAWN: " + pawnId);
        }
        Position start = pawn.position();
        if (start.row() == 0) return Direction.SOUTH;

        Set<Position> blocked = new HashSet<>(snapshot.rocks());
        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            if (!piece.id().equals(pawnId) && piece.type() != PieceType.KNIGHT) {
                blocked.add(piece.position());
            }
        }
        return shortestDirection(start, distances(blocked));
    }

    /** Finds the next legal single-step move; the first step cannot land on the Knight. */
    public static Optional<Position> findNextMove(StateView state, String pawnId) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");
        Optional<PieceState> pawn = state.pieceState(pawnId);
        Optional<Position> start = state.positionOf(pawnId);
        if (pawn.isEmpty() || pawn.get().type() != PieceType.PAWN
                || start.isEmpty() || start.get().row() == 0) return Optional.empty();

        Map<Position, Integer> distance = distances(blockedSquares(state, pawnId));
        List<Position> candidates = neighbors(start.get()).stream()
                .filter(state::isEmpty).filter(distance::containsKey).toList();
        if (candidates.isEmpty()) return Optional.empty();

        int shortest = candidates.stream().mapToInt(distance::get).min().orElseThrow();
        return candidates.stream()
                .filter(position -> distance.get(position) == shortest)
                .min(Comparator.comparing((Position position) ->
                                !(position.column() == start.get().column()
                                        && position.row() == start.get().row() - 1))
                        .thenComparing(BOARD_ORDER));
    }

    private static Direction shortestDirection(Position start, Map<Position, Integer> distance) {
        Integer startDistance = distance.get(start);
        if (startDistance == null) return Direction.SOUTH;
        Position south = new Position(start.column(), start.row() - 1);
        if (isShortestStep(south, startDistance, distance)) return Direction.SOUTH;
        for (Position next : neighbors(start)) {
            if (isShortestStep(next, startDistance, distance)) {
                return Direction.fromDelta(next.column() - start.column(), next.row() - start.row());
            }
        }
        return Direction.SOUTH;
    }

    private static boolean isShortestStep(Position position, int startDistance,
                                          Map<Position, Integer> distance) {
        return distance.getOrDefault(position, -1) == startDistance - 1;
    }

    private static Set<Position> blockedSquares(StateView state, String pawnId) {
        Set<Position> blocked = new HashSet<>();
        for (int column = 0; column < Position.BOARD_SIZE; column++) {
            for (int row = 0; row < Position.BOARD_SIZE; row++) {
                Position position = new Position(column, row);
                if (state.isRock(position)) {
                    blocked.add(position);
                    continue;
                }
                state.pieceAt(position).ifPresent(id -> {
                    if (!id.equals(pawnId)
                            && state.pieceState(id).map(piece -> piece.type() != PieceType.KNIGHT).orElse(true)) {
                        blocked.add(position);
                    }
                });
            }
        }
        return blocked;
    }

    private static Map<Position, Integer> distances(Set<Position> blocked) {
        Map<Position, Integer> distance = new HashMap<>();
        Queue<Position> queue = new ArrayDeque<>();
        for (int column = 0; column < Position.BOARD_SIZE; column++) {
            Position goal = new Position(column, 0);
            if (!blocked.contains(goal)) {
                distance.put(goal, 0);
                queue.add(goal);
            }
        }
        while (!queue.isEmpty()) {
            Position current = queue.remove();
            int nextDistance = distance.get(current) + 1;
            for (Position next : neighbors(current)) {
                if (!blocked.contains(next) && !distance.containsKey(next)) {
                    distance.put(next, nextDistance);
                    queue.add(next);
                }
            }
        }
        return distance;
    }

    private static List<Position> neighbors(Position position) {
        List<Position> neighbors = new ArrayList<>(8);
        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                if ((dc != 0 || dr != 0)
                        && Position.isInBounds(position.column() + dc, position.row() + dr)) {
                    neighbors.add(new Position(position.column() + dc, position.row() + dr));
                }
            }
        }
        return neighbors;
    }
}
