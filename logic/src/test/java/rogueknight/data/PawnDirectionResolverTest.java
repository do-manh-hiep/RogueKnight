package rogueknight.data;

import org.junit.jupiter.api.Test;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.BattleState;
import rogueknight.battle.Direction;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PawnDirectionResolverTest {

    private final PawnDirectionResolver resolver =
        new PawnDirectionResolver();

    @Test
    void findsDiagonalDirectionWithoutChangingBattle() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(4, 2),
            Set.of()
        );

        BattleSnapshot before = battle.snapshot();

        Direction direction = resolver.initialDirection(before, "pawn-1");

        assertEquals(Direction.SOUTH_EAST, direction);
        assertEquals(before, battle.snapshot());
    }

    @Test
    void breaksShortestPathTieByColumnThenRow() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 1),
            Set.of()
        );

        // Có ba đường dài hai bước:
        // qua (2,2), (3,2), hoặc (4,2).
        // Chọn (2,2) vì column nhỏ nhất.
        assertEquals(
            Direction.SOUTH_WEST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void avoidsRocks() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 1),
            Set.of(
                new Position(2, 2),
                new Position(3, 2)
            )
        );

        // Hai bước đầu bên trái và ở giữa bị chặn.
        // Đường ngắn nhất còn lại đi qua (4,2).
        assertEquals(
            Direction.SOUTH_EAST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void avoidsOtherPieces() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 1),
            Set.of(new Position(3, 2))
        );

        battle.addPiece(
            new PieceState(
                "rook-1", PieceType.ROOK, 2, 3, 3, 2
            ),
            new Position(2, 2)
        );

        assertEquals(
            Direction.SOUTH_EAST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void usesSouthWhenNoPathExists() {
        Set<Position> rocks = new HashSet<>();

        // Tường đá chắn toàn bộ row 4.
        for (int column = 0; column < 8; column++) {
            rocks.add(new Position(column, 4));
        }

        BattleState battle = createBattle(
            new Position(3, 6),
            new Position(3, 0),
            rocks
        );

        assertEquals(
            Direction.SOUTH,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );

        // Tính hướng không làm Pawn bước xuống.
        assertEquals(
            new Position(3, 6),
            battle.positionOf("pawn-1").orElseThrow()
        );
    }

    @Test
    void allowsDiagonalWithoutAdditionalCornerRule() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(4, 2),
            Set.of(
                new Position(4, 3),
                new Position(3, 2)
            )
        );

        // Hai ô cạnh bị chặn nhưng ô chéo vẫn được BFS xét.
        assertEquals(
            Direction.SOUTH_EAST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void rejectsUnknownPawnOrWrongPieceType() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 0),
            Set.of()
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.initialDirection(battle.snapshot(), "missing")
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> resolver.initialDirection(battle.snapshot(), "knight")
        );
    }

    private BattleState createBattle(
        Position pawnPosition,
        Position knightPosition,
        Set<Position> rocks
    ) {
        BattleState battle = new BattleState(
            rocks,
            Inventory.initial(
                EquipmentType.CANNON_MOVEMENT,
                EquipmentType.DIAGONAL_SKEWER
            )
        );

        battle.addPiece(
            new PieceState(
                "knight", PieceType.KNIGHT, 5, 0, 0, 0
            ),
            knightPosition
        );

        battle.addPiece(
            new PieceState(
                "pawn-1", PieceType.PAWN, 1, 2, 2, 1
            ),
            pawnPosition
        );

        return battle;
    }
}