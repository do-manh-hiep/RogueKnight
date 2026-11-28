package rogueknight.ai;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import rogueknight.board.Position;
class GenericEnemyPolicyRankingTest {
 private static Position at(String s){return Position.fromNotation(s);}
 private static GenericEnemyPolicy.Candidate candidate(String s,boolean m,boolean safe,boolean t,int d){return new GenericEnemyPolicy.Candidate(at(s),m,safe,t,d);}
    @Test
    void rankingOrderMatchesDec02() {
        GenericEnemyPolicy.Candidate moveAttack = candidate("h8", true, false, false, 7);
        GenericEnemyPolicy.Candidate safeThreatC2 = candidate("c2", false, true, true, 2);
        GenericEnemyPolicy.Candidate safeThreatC5 = candidate("c5", false, true, true, 2);
        GenericEnemyPolicy.Candidate safeThreatFar = candidate("a1", false, true, true, 6);
        GenericEnemyPolicy.Candidate safeOnlyNear = candidate("a2", false, true, false, 1);
        GenericEnemyPolicy.Candidate unsafeThreatNear = candidate("a3", false, false, true, 1);
        List<GenericEnemyPolicy.Candidate> shuffled = new ArrayList<>(List.of(
                unsafeThreatNear, safeOnlyNear, safeThreatFar, safeThreatC5, moveAttack, safeThreatC2));
        Collections.reverse(shuffled);

        shuffled.sort(GenericEnemyPolicy.RANKING);

        assertEquals(List.of(moveAttack, safeThreatC2, safeThreatC5, safeThreatFar, safeOnlyNear, unsafeThreatNear),
                shuffled);
    }


}
