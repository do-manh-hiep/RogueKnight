package rogueknight.battle;

/*
 * PieceState bất biến; vị trí chỉ do Board giữ.
 * currentDirection chỉ áp dụng cho Pawn.
 */
public record PieceState(
    String id,
    PieceType type,
    int hp,
    int cycle,
    int nextActivationTurn,
    int spawnOrder,
    Direction currentDirection
) {

    public PieceState {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Piece id must not be blank");
        }

        if (type == null) {
            throw new IllegalArgumentException("Piece type must not be null");
        }

        if (hp < 0 || cycle < 0 || nextActivationTurn < 0) {
            throw new IllegalArgumentException(
                "hp, cycle, nextActivationTurn must be >= 0"
            );
        }

        if (type == PieceType.PAWN && currentDirection == null) {
            throw new IllegalArgumentException("Pawn must have currentDirection");
        }

        if (type != PieceType.PAWN && currentDirection != null) {
            throw new IllegalArgumentException("Only Pawn can have currentDirection");
        }
    }

    // Tương thích với code cũ dùng sáu tham số.
    public PieceState(
        String id,
        PieceType type,
        int hp,
        int cycle,
        int nextActivationTurn,
        int spawnOrder
    ) {
        this(
            id, type, hp, cycle, nextActivationTurn, spawnOrder,
            type == PieceType.PAWN ? Direction.SOUTH : null
        );
    }

    public PieceState withHp(int newHp) {
        return new PieceState(
            id, type, Math.max(0, newHp),
            cycle, nextActivationTurn, spawnOrder, currentDirection
        );
    }

    public PieceState withDirection(Direction newDirection) {
        if (type != PieceType.PAWN) {
            throw new IllegalStateException("Only Pawn can change direction");
        }

        return new PieceState(
            id, type, hp,
            cycle, nextActivationTurn, spawnOrder, newDirection
        );
    }

    public PieceState withNextActivationTurn(int newNextActivationTurn) {
        return new PieceState(
            id, type, hp,
            cycle, newNextActivationTurn, spawnOrder, currentDirection
        );
    }
}