package rogueknight.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import rogueknight.battle.BattleState;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

/**
 * BFS pathfinder for Pawn moving towards row 0 (rank 1).
 * Features:
 * - 8-directional adjacency graph.
 * - Rocks and other enemy pieces act as impassable obstacles.
 * - The player's Knight is ignored during global pathfinding, but the first step cannot land on the Knight's tile.
 * - Tie-breaking prefers straight-down step (SOUTH) towards row 0, then lexicographical order (column asc, row asc).
 */
public final class PawnPathfinder {

    public record NextMove(Position target, Direction direction) {
        public NextMove {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(direction, "direction must not be null");
        }
    }

    private PawnPathfinder() { }

    /**
     * Calculates the next optimal single-step move for the specified Pawn to reach row 0.
     *
     * @param state  the current battle state
     * @param pawnId ID of the Pawn
     * @return the chosen next move, or empty if already at row 0 or no valid path exists
     */
    public static Optional<NextMove> findNextMove(BattleState state, String pawnId) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(pawnId, "pawnId must not be null");

        Optional<PieceState> pawnOpt = state.pieceState(pawnId);
        if (pawnOpt.isEmpty() || pawnOpt.get().type() != PieceType.PAWN) {
            return Optional.empty();
        }

        Position start = state.positionOf(pawnId).orElse(null);
        if (start == null || start.row() == 0) {
            return Optional.empty();
        }

        // Multi-source BFS starting from all reachable, unblocked squares at row = 0
        int[][] distance = new int[Position.BOARD_SIZE][Position.BOARD_SIZE];
        for (int c = 0; c < Position.BOARD_SIZE; c++) {
            for (int r = 0; r < Position.BOARD_SIZE; r++) {
                distance[c][r] = -1;
            }
        }

        Queue<Position> queue = new ArrayDeque<>();

        for (int c = 0; c < Position.BOARD_SIZE; c++) {
            Position targetSquare = new Position(c, 0);
            if (!isObstacleForPath(state, pawnId, targetSquare)) {
                distance[c][0] = 0;
                queue.add(targetSquare);
            }
        }

        while (!queue.isEmpty()) {
            Position curr = queue.poll();
            int currDist = distance[curr.column()][curr.row()];

            for (int dc = -1; dc <= 1; dc++) {
                for (int dr = -1; dr <= 1; dr++) {
                    if (dc == 0 && dr == 0) {
                        continue;
                    }
                    int nc = curr.column() + dc;
                    int nr = curr.row() + dr;
                    if (!Position.isInBounds(nc, nr)) {
                        continue;
                    }

                    Position neighbor = new Position(nc, nr);
                    if (distance[nc][nr] != -1) {
                        continue;
                    }
                    if (isObstacleForPath(state, pawnId, neighbor)) {
                        continue;
                    }

                    distance[nc][nr] = currDist + 1;
                    queue.add(neighbor);
                }
            }
        }

        // Inspect 8 neighboring squares from start to pick the best first step
        List<Position> candidates = new ArrayList<>();
        int minDistance = Integer.MAX_VALUE;

        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (dc == 0 && dr == 0) {
                    continue;
                }
                int nc = start.column() + dc;
                int nr = start.row() + dr;
                if (!Position.isInBounds(nc, nr)) {
                    continue;
                }

                Position neighbor = new Position(nc, nr);

                // First step cannot land on rock or any piece (including Knight)
                if (state.isRock(neighbor)) {
                    continue;
                }
                Optional<String> occupant = state.pieceAt(neighbor);
                if (occupant.isPresent()) {
                    continue;
                }

                int dist = distance[nc][nr];
                if (dist == -1) {
                    continue;
                }

                if (dist < minDistance) {
                    minDistance = dist;
                    candidates.clear();
                    candidates.add(neighbor);
                } else if (dist == minDistance) {
                    candidates.add(neighbor);
                }
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Tie-breaking:
        // 1. Prefer straight step down towards row 0 (SOUTH: dc == 0, dr == -1)
        Position straightSouth = candidates.stream()
                .filter(p -> p.column() == start.column() && p.row() == start.row() - 1)
                .findFirst()
                .orElse(null);

        Position chosen;
        if (straightSouth != null) {
            chosen = straightSouth;
        } else {
            // 2. Lexicographical tie-breaking: column ascending, then row ascending
            candidates.sort(Comparator.comparingInt(Position::column).thenComparingInt(Position::row));
            chosen = candidates.get(0);
        }

        Direction dir = Direction.fromDelta(
                chosen.column() - start.column(),
                chosen.row() - start.row()
        );

        return Optional.of(new NextMove(chosen, dir));
    }

    private static boolean isObstacleForPath(BattleState state, String pawnId, Position pos) {
        if (state.isRock(pos)) {
            return true;
        }
        Optional<String> pieceId = state.pieceAt(pos);
        if (pieceId.isEmpty()) {
            return false;
        }
        String id = pieceId.get();
        // Knight is ignored when pathfinding globally
        if ("knight".equals(id)) {
            return false;
        }
        // Pawn's own current square is not an obstacle
        if (pawnId.equals(id)) {
            return false;
        }
        // Other pieces are obstacles
        return true;
    }
}
