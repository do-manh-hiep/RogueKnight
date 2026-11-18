package rogueknight.logic.equipment;

import java.util.Optional;

public interface EquipmentBoardView {
    record EnemyInfo(String id, int hp, boolean isKing) {}

    GridPos getKnightPosition();

    boolean isRockAt(GridPos pos);

    Optional<EnemyInfo> getEnemyAt(GridPos pos);

    default boolean isOccupied(GridPos pos) {
        return isRockAt(pos) || getEnemyAt(pos).isPresent() || pos.equals(getKnightPosition());
    }
}