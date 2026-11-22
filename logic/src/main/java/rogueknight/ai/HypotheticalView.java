package rogueknight.ai;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import rogueknight.battle.PieceState;
import rogueknight.battle.StateView;
import rogueknight.board.Position;

/*
 * HypotheticalView.java  |  T-03.02  |  TD: 4.7 (C-03), 6.1 (HypotheticalView)
 *
 * StateView giả định "nếu quân pieceId đứng ở ô position", mọi thứ khác giữ nguyên.
 * AI đánh giá ô ứng viên trên view này thay vì tạm move quân thật rồi trả lại,
 * nên query của AI không bao giờ mutate state (FX-16).
 */
public final class HypotheticalView implements StateView {

    private final StateView base;
    private final String pieceId;
    private final Position position;
    private final Position originalPosition;

    public HypotheticalView(StateView base, String pieceId, Position position) {
        this.base = Objects.requireNonNull(base, "base");
        this.pieceId = Objects.requireNonNull(pieceId, "pieceId");
        this.position = Objects.requireNonNull(position, "position");
        this.originalPosition = base.positionOf(pieceId).orElseThrow(
                () -> new IllegalArgumentException("Piece is not on the Board: " + pieceId));
        if (!position.equals(originalPosition) && !base.isEmpty(position)) {
            throw new IllegalArgumentException("Hypothetical square is not empty: " + position);
        }
    }

    @Override
    public int globalTurn() {
        return base.globalTurn();
    }

    @Override
    public Optional<PieceState> pieceState(String id) {
        return base.pieceState(id);
    }

    @Override
    public Optional<Position> positionOf(String id) {
        return pieceId.equals(id) ? Optional.of(position) : base.positionOf(id);
    }

    @Override
    public Optional<String> pieceAt(Position square) {
        if (square.equals(position)) return Optional.of(pieceId);
        if (square.equals(originalPosition)) return Optional.empty();
        return base.pieceAt(square);
    }

    @Override
    public boolean isRock(Position square) {
        return base.isRock(square);
    }

    @Override
    public List<PieceState> livePieces() {
        return base.livePieces();
    }
}
