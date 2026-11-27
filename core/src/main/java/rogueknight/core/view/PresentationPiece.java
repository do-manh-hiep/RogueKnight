package rogueknight.core.view;

import java.util.Objects;
import rogueknight.battle.PieceType;

/** Stable-identity presentation data for one live piece. */
public record PresentationPiece(String id, PieceType type, int hp, BoardPosition position) {
    public PresentationPiece {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("piece id must not be blank");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(position, "position");
        if (hp <= 0) throw new IllegalArgumentException("presentation pieces must have positive HP");
    }

    public PresentationPiece at(BoardPosition newPosition) {
        return new PresentationPiece(id, type, hp, newPosition);
    }

    public PresentationPiece withHp(int newHp) {
        if (newHp <= 0) throw new IllegalArgumentException("live piece HP must remain positive");
        return new PresentationPiece(id, type, newHp, position);
    }

    public PresentationPiece withType(PieceType newType) {
        return new PresentationPiece(id, newType, hp, position);
    }
}
