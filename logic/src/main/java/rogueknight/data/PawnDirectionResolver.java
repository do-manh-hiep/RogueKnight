package rogueknight.data;

import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.Direction;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.util.ArrayDeque;
import java.util.HashSet;
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

        BattleSnapshot.PieceView knight =
            snapshot.pieceById("knight").orElseThrow(
                () -> new IllegalArgumentException("Missing knight")
            );

        if (knight.type() != PieceType.KNIGHT) {
            throw new IllegalArgumentException("Piece knight must be KNIGHT");
        }

        Position start = pawn.position();
        Position target = knight.position();

        // Đá và các quân còn lại là vật cản.
        // Cho phép BFS tìm tới ô Knight, nhưng không di chuyển Pawn vào đó.
        Set<Position> blocked = new HashSet<>(snapshot.rocks());

        for (BattleSnapshot.PieceView piece : snapshot.pieces()) {
            if (!piece.id().equals(pawnId)
                && !piece.id().equals(knight.id())) {
                blocked.add(piece.position());
            }
        }

        Set<Position> visited = new HashSet<>();
        Queue<SearchNode> queue = new ArrayDeque<>();

        visited.add(start);
        queue.add(new SearchNode(start, null));

        while (!queue.isEmpty()) {
            SearchNode current = queue.remove();

            // Duyệt column trước, rồi row, đều tăng dần.
            for (int dc = -1; dc <= 1; dc++) {
                for (int dr = -1; dr <= 1; dr++) {
                    if (dc == 0 && dr == 0) {
                        continue;
                    }

                    int column = current.position().column() + dc;
                    int row = current.position().row() + dr;

                    if (column < 0 || column >= Position.BOARD_SIZE
                        || row < 0 || row >= Position.BOARD_SIZE) {
                        continue;
                    }

                    Position next = new Position(column, row);

                    if (blocked.contains(next) || !visited.add(next)) {
                        continue;
                    }

                    // Ghi nhớ hướng của bước đầu, không phải bước cuối.
                    Direction firstDirection = current.firstDirection();

                    if (firstDirection == null) {
                        firstDirection = directionOf(dc, dr);
                    }

                    if (next.equals(target)) {
                        return firstDirection;
                    }

                    queue.add(new SearchNode(next, firstDirection));
                }
            }
        }

        return Direction.SOUTH;
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

    private record SearchNode(
        Position position,
        Direction firstDirection
    ) {
    }
}