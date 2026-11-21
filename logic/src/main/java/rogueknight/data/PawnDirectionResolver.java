package rogueknight.data;

import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class PawnDirectionResolver {

    public Direction initialDirection(
        BattleSnapshot snapshot,
        String pawnId
    ) {
        Objects.requireNonNull(snapshot, "snapshot");

        BattleSnapshot.PieceView pawn =
            snapshot.pieceById(pawnId).orElseThrow(
                () -> new IllegalArgumentException("Unknown Pawn: " + pawnId)
            );

        if (pawn.type() != PieceType.PAWN) {
            throw new IllegalArgumentException("Piece must be PAWN: " + pawnId);
        }

        Position start = pawn.position();

        // Pawn ở hàng phong cấp không còn bước đường đi để chọn.
        if (start.row() == 0) {
            return Direction.SOUTH;
        }

        Set<Position> blocked = new HashSet<>(snapshot.rocks());

        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            // Theo TD: bỏ qua Knight khi tính đường.
            if (!piece.id().equals(pawnId)
                && piece.type() != PieceType.KNIGHT) {
                blocked.add(piece.position());
            }
        }

        // BFS ngược từ tất cả ô đích còn trống trên row = 0.
        Map<Position, Integer> distances = new HashMap<>();
        Queue<Position> queue = new ArrayDeque<>();

        for (int column = 0; column < Position.BOARD_SIZE; column++) {
            Position goal = new Position(column, 0);

            if (!blocked.contains(goal)) {
                distances.put(goal, 0);
                queue.add(goal);
            }
        }

        while (!queue.isEmpty()) {
            Position current = queue.remove();
            int nextDistance = distances.get(current) + 1;

            for (int dc = -1; dc <= 1; dc++) {
                for (int dr = -1; dr <= 1; dr++) {
                    if (dc == 0 && dr == 0) {
                        continue;
                    }

                    int column = current.column() + dc;
                    int row = current.row() + dr;

                    if (!isInside(column, row)) {
                        continue;
                    }

                    Position next = new Position(column, row);

                    if (blocked.contains(next) || distances.containsKey(next)) {
                        continue;
                    }

                    distances.put(next, nextDistance);
                    queue.add(next);
                }
            }
        }

        Integer startDistance = distances.get(start);

        if (startDistance == null) {
            return Direction.SOUTH;
        }

        // Trong các bước thuộc đường ngắn nhất:
        // ưu tiên SOUTH, sau đó column rồi row tăng dần.
        Position straightSouth = new Position(
            start.column(),
            start.row() - 1
        );

        if (isShortestStep(straightSouth, startDistance, distances)) {
            return Direction.SOUTH;
        }

        for (int dc = -1; dc <= 1; dc++) {
            for (int dr = -1; dr <= 1; dr++) {
                if (dc == 0 && dr == 0) {
                    continue;
                }

                int column = start.column() + dc;
                int row = start.row() + dr;

                if (!isInside(column, row)) {
                    continue;
                }

                Position next = new Position(column, row);

                if (isShortestStep(next, startDistance, distances)) {
                    return directionOf(dc, dr);
                }
            }
        }

        return Direction.SOUTH;
    }

    private boolean isShortestStep(
        Position position,
        int startDistance,
        Map<Position, Integer> distances
    ) {
        Integer distance = distances.get(position);

        return distance != null && distance == startDistance - 1;
    }

    private boolean isInside(int column, int row) {
        return column >= 0 && column < Position.BOARD_SIZE
            && row >= 0 && row < Position.BOARD_SIZE;
    }

    private Direction directionOf(int dc, int dr) {
        for (Direction direction : Direction.values()) {
            if (direction.deltaColumn() == dc
                && direction.deltaRow() == dr) {
                return direction;
            }
        }

        throw new IllegalArgumentException(
            "Invalid direction: " + dc + "," + dr
        );
    }
}