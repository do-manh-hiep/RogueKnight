package rogueknight.data;

import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PieceData;
import rogueknight.data.dto.PositionData;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class BattleFactory {

    public BattleState createMap01(
            EquipmentType first,
            EquipmentType second
    ) throws IOException {
        MapData map = new MapLoader().loadMap01();
        PieceCatalog catalog =
                new PieceCatalog(new BalanceLoader().load());

        Inventory inventory = Inventory.initial(first, second);

        Set<Position> rocks = new HashSet<>();
        for (PositionData rock : map.rocks()) {
            rocks.add(new Position(rock.column(), rock.row()));
        }

        BattleState battle = new BattleState(rocks, inventory);

        for (PieceData piece : map.pieces()) {
            PieceType type = PieceType.valueOf(piece.type());
            PieceSpec spec = catalog.get(piece.type());

            int cycle;
            int nextActivationTurn;

            if (type == PieceType.KNIGHT) {
                cycle = 0;
                nextActivationTurn = 0;
            } else {
                cycle = spec.cycle();
                nextActivationTurn = cycle;
            }

            PieceState state = new PieceState(
                    piece.id(),
                    type,
                    spec.hp(),
                    cycle,
                    nextActivationTurn,
                    piece.spawnOrder()
            );

            Position position =
                    new Position(piece.column(), piece.row());

            battle.addPiece(state, position);
        }

        return battle;
    }
}