package rogueknight.action;

import rogueknight.battle.StateView;

/*
 * EnemyPolicy.java  |  T-03.01/T-03.02  |  TD: 4.7 (C-03), 6.1, 6.6
 *
 * Chọn Action đầu tiên của một activation Enemy generic (Rook/Bishop/King/Queen).
 * - Thuần đọc: không sửa Board, HP, hướng, charge hay lịch.
 * - Cùng state cho cùng quyết định (deterministic).
 * - Không tự quyết post-Move Attack: EnemyTurnRunner tự xét Attack trên state sau Move.
 * Cài đặt thật: rogueknight.ai.GenericEnemyPolicy. Test lifecycle dùng stub để cô lập điều phối.
 */
@FunctionalInterface
public interface EnemyPolicy {

    EnemyDecision choose(StateView view, String enemyId);
}
