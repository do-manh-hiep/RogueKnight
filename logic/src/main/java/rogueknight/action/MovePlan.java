package rogueknight.action;

import rogueknight.board.Position;

/*
 * MovePlan.java  |  T-02.01  |  TD: 4.7
 * Kế hoạch nội bộ do KnightRules tạo sau khi validate; Resolver áp dụng ngay.
 * Không công khai ra ngoài package để UI không thể tự dựng plan bỏ qua validation.
 */
record MovePlan(String actorId, Position from, Position to) {
}
