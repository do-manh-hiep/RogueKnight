package rogueknight.core.input;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List; import java.util.Set;
import org.junit.jupiter.api.Test;
import rogueknight.battle.BattleController; import rogueknight.ai.EnemyAI;
import rogueknight.battle.BattleSnapshot; import rogueknight.battle.BattlePhase; import rogueknight.battle.BattleResult;
import rogueknight.battle.EquipmentType; import rogueknight.battle.Inventory; import rogueknight.battle.PieceType;
import rogueknight.board.Position; import rogueknight.core.view.BoardPosition;
class BattleControllerAdapterEnemyTurnTest {
 @Test void knightKilledByEnemyTurnShowsDefeatWithoutCrashing(){
  BattleSnapshot setup=new BattleSnapshot(1,BattlePhase.PLAYER_SELECT,BattleResult.ONGOING,Set.of(),List.of(
   new BattleSnapshot.PieceView("knight",PieceType.KNIGHT,1,0,0,0,Position.fromNotation("d4")),
   new BattleSnapshot.PieceView("rook-1",PieceType.ROOK,2,3,2,1,Position.fromNotation("d6")),
   new BattleSnapshot.PieceView("king-1",PieceType.KING,5,5,5,2,Position.fromNotation("h8"))
  ),Inventory.initial(EquipmentType.CANNON_MOVEMENT,EquipmentType.CANNON_ATTACK));
  BattleControllerAdapter adapter=new BattleControllerAdapter(BattleController.fromSnapshot(setup,new EnemyAI()));
  UiActionResult result=adapter.submitAction("knight",ActionMode.MOVE,BoardPosition.fromNotation("d3"));
  assertTrue(result.accepted()); assertEquals(BoardPosition.fromNotation("d3"),result.after().knightPosition());
  assertFalse(result.after().piecesById().containsKey("knight"));
  assertTrue(result.events().stream().anyMatch(event -> event instanceof rogueknight.action.BattleEvent.BattleEnded ended && ended.result() == BattleResult.DEFEAT));
  assertTrue(result.after().legalMoves().isEmpty()); assertDoesNotThrow(adapter::snapshot);
 }
}
