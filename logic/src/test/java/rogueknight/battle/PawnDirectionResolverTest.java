package rogueknight.battle;

import org.junit.jupiter.api.Test;
import rogueknight.ai.PawnPathfinder;
import rogueknight.board.Position;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PawnDirectionResolverTest {

    @Test
    void targetsPromotionRowInsteadOfKnight() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 7),
            Set.of()
        );

        BattleSnapshot before = battle.snapshot();

        assertEquals(
            Direction.SOUTH,
            PawnPathfinder.initialDirection(before, "pawn-1")
        );

        assertEquals(before, battle.snapshot());
    }

    @Test
    void prefersStraightSouthAmongShortestPaths() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
            Set.of()
        );

        // SOUTH, SOUTH_WEST và SOUTH_EAST đều có thể
        // bắt đầu đường dài ba bước tới row = 0.
        // Ưu tiên SOUTH.
        assertEquals(
            Direction.SOUTH,
            PawnPathfinder.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void avoidsRockAndBreaksTieByColumnThenRow() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
            Set.of(new Position(3, 2))
        );

        // SOUTH bị đá chặn.
        // Hai đường chéo ngắn bằng nhau: chọn bên trái.
        assertEquals(
            Direction.SOUTH_WEST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void avoidsOtherEnemyPieces() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
            Set.of()
        );

        battle.addPiece(
            new PieceState(
                "rook-1", PieceType.ROOK, 2, 3, 3, 2
            ),
            new Position(3, 2)
        );

        assertEquals(
            Direction.SOUTH_WEST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void ignoresKnightWhenCalculatingInitialPath() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(3, 2),
            Set.of()
        );

        BattleSnapshot before = battle.snapshot();

        // Theo TD, bỏ qua Knight khi tính đường ban đầu.
        // Đây chỉ là tính hướng, không Move vào ô Knight.
        assertEquals(
            Direction.SOUTH,
            resolver.initialDirection(before, "pawn-1")
        );

        assertEquals(before, battle.snapshot());
    }

    @Test
    void usesSouthWhenNoPromotionPathExists() {
        Set<Position> rocks = new HashSet<>();

        // Chặn toàn bộ row = 1, ngăn đường xuống row = 0.
        for (int column = 0; column < Position.BOARD_SIZE; column++) {
            rocks.add(new Position(column, 1));
        }

        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
            rocks
        );

        BattleSnapshot before = battle.snapshot();

        assertEquals(
            Direction.SOUTH,
            resolver.initialDirection(before, "pawn-1")
        );

        assertEquals(before, battle.snapshot());
    }

    @Test
    void allowsDiagonalWithoutAdditionalCornerRule() {
        BattleState battle = createBattle(
            new Position(3, 1),
            new Position(7, 7),
            Set.of(
                new Position(3, 0),
                new Position(2, 1)
            )
        );

        // SOUTH bị chặn; ô chéo (2,0) vẫn hợp lệ cho BFS,
        // dù hai ô cạnh góc là đá.
        assertEquals(
            Direction.SOUTH_WEST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void choosesShortestPathBeforeStraightSouthPreference() {
        Set<Position> rocks = new HashSet<>();

        // Row = 1 chỉ còn lối đi tại column = 0.
        for (int column = 1; column < Position.BOARD_SIZE; column++) {
            rocks.add(new Position(column, 1));
        }

        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
            rocks
        );

        // Đi chéo trái tới (2,2) cho đường ngắn hơn.
        // SOUTH không được ưu tiên nếu làm đường dài hơn.
        assertEquals(
            Direction.SOUTH_WEST,
            resolver.initialDirection(battle.snapshot(), "pawn-1")
        );
    }

    @Test
    void rejectsUnknownPawnOrWrongPieceType() {
        BattleState battle = createBattle(
            new Position(3, 3),
            new Position(7, 7),
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