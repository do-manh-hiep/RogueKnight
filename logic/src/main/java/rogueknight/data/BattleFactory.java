package rogueknight.data;

import rogueknight.battle.BattleController;
import rogueknight.action.EnemyPolicy;
import rogueknight.battle.BattlePhase;
import rogueknight.battle.BattleResult;
import rogueknight.battle.BattleSnapshot;
import rogueknight.battle.Direction;
import rogueknight.battle.EquipmentType;
import rogueknight.battle.Inventory;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PieceData;
import rogueknight.data.dto.PositionData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Public bootstrap facade. Only immutable setup data crosses into the battle package. */
public final class BattleFactory {
    public BattleController createMap01Controller(EquipmentType first, EquipmentType second, EnemyPolicy enemyPolicy)
            throws IOException {
        return BattleController.fromSnapshot(createMap01Setup(first, second), enemyPolicy);
    }

    private BattleSnapshot createMap01Setup(EquipmentType first, EquipmentType second) throws IOException {
        MapData map = new MapLoader().loadMap01();
        PieceCatalog catalog = new PieceCatalog(new BalanceLoader().load());
        Inventory inventory = Inventory.initial(first, second);
        Set<Position> rocks = new HashSet<>();
        for (PositionData rock : map.rocks()) rocks.add(new Position(rock.column(), rock.row()));

        List<BattleSnapshot.PieceView> pieces = new ArrayList<>();
        for (PieceData piece : map.pieces()) {
            PieceType type = PieceType.valueOf(piece.type());
            PieceSpec spec = catalog.get(piece.type());
            int cycle = type == PieceType.KNIGHT ? 0 : spec.cycle();
            int nextActivationTurn = type == PieceType.KNIGHT ? 0 : cycle;
            pieces.add(new BattleSnapshot.PieceView(piece.id(), type, spec.hp(), cycle,
                nextActivationTurn, piece.spawnOrder(), new Position(piece.column(), piece.row()),
                type == PieceType.PAWN ? Direction.SOUTH : null));
        }

        BattleSnapshot initial = new BattleSnapshot(1, BattlePhase.PLAYER_SELECT, BattleResult.ONGOING,
            rocks, pieces, inventory);
        PawnDirectionResolver resolver = new PawnDirectionResolver();
        List<BattleSnapshot.PieceView> oriented = initial.pieces().stream().map(piece -> {
            if (piece.type() != PieceType.PAWN) return piece;
            Direction direction = resolver.initialDirection(initial, piece.id());
            return new BattleSnapshot.PieceView(piece.id(), piece.type(), piece.hp(), piece.cycle(),
                piece.nextActivationTurn(), piece.spawnOrder(), piece.position(), direction);
        }).toList();
        return new BattleSnapshot(initial.globalTurn(), initial.phase(), initial.result(),
            initial.rocks(), oriented, initial.inventory());
    }
}