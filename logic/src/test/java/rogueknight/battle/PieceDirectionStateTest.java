package rogueknight.battle;

import org.junit.jupiter.api.Test;
import rogueknight.board.Position;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PieceDirectionStateTest {

    @Test
    void oldConstructorsRemainUsable() {
        PieceState pawn = new PieceState(
            "pawn-1", PieceType.PAWN, 1, 2, 2, 1
        );
        PieceState knight = new PieceState(
            "knight", PieceType.KNIGHT, 5, 0, 0, 0
        );

        assertEquals(Direction.SOUTH, pawn.currentDirection());
        assertNull(knight.currentDirection());

        BattleSnapshot.PieceView view = new BattleSnapshot.PieceView(
            "pawn-1", PieceType.PAWN, 1, 2, 2, 1,
            new Position(2, 6)
        );

        assertEquals(Direction.SOUTH, view.currentDirection());
    }

    @Test
    void changingHpPreservesDirection() {
        PieceState pawn = new PieceState(
            "pawn-1", PieceType.PAWN, 2, 2, 2, 1,
            Direction.SOUTH_WEST
        );

        PieceState damaged = pawn.withHp(1);

        assertEquals(
            new PieceState(
                "pawn-1", PieceType.PAWN, 1, 2, 2, 1,
                Direction.SOUTH_WEST
            ),
            damaged
        );

        assertEquals(2, pawn.hp());
        assertEquals(Direction.SOUTH_WEST, pawn.currentDirection());
    }

    @Test
    void snapshotPreservesDirectionAndRemainsDetached() {
        BattleState battle = new BattleState(
            Set.of(),
            Inventory.initial(
                EquipmentType.CANNON_MOVEMENT,
                EquipmentType.DIAGONAL_SKEWER
            )
        );

        PieceState pawn = new PieceState(
            "pawn-1", PieceType.PAWN, 1, 2, 2, 1,
            Direction.SOUTH_WEST
        );

        battle.addPiece(pawn, new Position(2, 6));

        BattleSnapshot before = battle.snapshot();

        assertEquals(
            Direction.SOUTH_WEST,
            before.pieceById("pawn-1").orElseThrow().currentDirection()
        );

        PieceState changed = pawn.withDirection(Direction.EAST);
        battle.replacePiece(changed);

        // Đổi hướng không được đổi các dữ liệu khác.
        assertEquals(
            new PieceState(
                "pawn-1", PieceType.PAWN, 1, 2, 2, 1,
                Direction.EAST
            ),
            battle.pieceState("pawn-1").orElseThrow()
        );

        assertEquals(
            Direction.EAST,
            battle.snapshot().pieceById("pawn-1")
                .orElseThrow().currentDirection()
        );

        // Snapshot cũ vẫn giữ hướng cũ.
        assertEquals(
            Direction.SOUTH_WEST,
            before.pieceById("pawn-1").orElseThrow().currentDirection()
        );
        assertEquals(
            new Position(2, 6),
            battle.positionOf("pawn-1").orElseThrow()
        );
    }

    @Test
    void rejectsInvalidDirections() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new PieceState(
                "pawn-1", PieceType.PAWN, 1, 2, 2, 1, null
            )
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new PieceState(
                "knight", PieceType.KNIGHT, 5, 0, 0, 0,
                Direction.SOUTH
            )
        );

        PieceState knight = new PieceState(
            "knight", PieceType.KNIGHT, 5, 0, 0, 0
        );

        assertThrows(
            IllegalStateException.class,
            () -> knight.withDirection(Direction.SOUTH)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new BattleSnapshot.PieceView(
                "pawn-1", PieceType.PAWN, 1, 2, 2, 1,
                new Position(2, 6), null
            )
        );
    }
}