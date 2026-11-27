package rogueknight.data;

import rogueknight.action.BattleController;
import rogueknight.action.EnemyPhase;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceState;
import rogueknight.battle.PieceType;
import rogueknight.battle.BattleSnapshot;
import rogueknight.board.Position;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PieceData;
import rogueknight.data.dto.PositionData;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public final class BattleFactory {

    /** Creates a playable battle without exposing its mutable aggregate to callers. */
    public BattleController createMap01Controller(
            EquipmentType first,
            EquipmentType second,
            EnemyPhase enemyPhase
    ) throws IOException {
        return new BattleController(createMap01State(first, second), enemyPhase);
    }

    /** Package-private aggregate factory for logic fixtures and factory internals. */
    BattleState createMap01State(
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

        // Chỉ tính hướng sau khi đã đặt đầy đủ đá và quân.
        // Hai Pawn cùng được tính từ một snapshot ban đầu.
        BattleSnapshot initialSnapshot = battle.snapshot();
        PawnDirectionResolver directionResolver = new PawnDirectionResolver();

        for (BattleSnapshot.PieceView piece : initialSnapshot.pieces()) {
            if (piece.type() == PieceType.PAWN) {
                var direction = directionResolver.initialDirection(
                    initialSnapshot,
                    piece.id()
                );

                PieceState pawn = battle.pieceState(piece.id()).orElseThrow();

                battle.replacePiece(pawn.withDirection(direction));
            }
        }

        return battle;
    }
}