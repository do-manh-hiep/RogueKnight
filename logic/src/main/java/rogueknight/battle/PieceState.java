package rogueknight.battle;

/*
 * PieceState.java  |  T-01.02  |  TD: 4.1-4.2, 6.1, I04
 *
 * Dữ liệu của một quân: id, type, hp, cycle, nextActivationTurn, spawnOrder.
 * - KHÔNG chứa position: vị trí chỉ do Board giữ (một nguồn duy nhất, I04).
 * - Bất biến (record). Thay đổi HP dùng withHp() rồi BattleState.replacePiece();
 *   hp không âm (I06): withHp clamp tối thiểu 0.
 * - Knight không có lịch kích hoạt: dùng cycle = 0, nextActivationTurn = 0.
 * - currentDirection của Pawn và PieceSpec/balance.json thuộc T-02.03 (DEC-01, DEC-10).
 * Từ khóa tìm kiếm: withHp, spawnOrder, nextActivationTurn.
 */
public record PieceState(
        String id,
        PieceType type,
        int hp,
        int cycle,
        int nextActivationTurn,
        int spawnOrder) {

    public PieceState {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Piece id must not be blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("Piece type must not be null");
        }
        if (hp < 0 || cycle < 0 || nextActivationTurn < 0) {
            throw new IllegalArgumentException("hp, cycle, nextActivationTurn must be >= 0");
        }
    }

    public PieceState withHp(int newHp) {
        return new PieceState(id, type, Math.max(0, newHp), cycle, nextActivationTurn, spawnOrder);
    }
}
