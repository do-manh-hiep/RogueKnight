package rogueknight.data.dto;

/** Piece placement as read from a map JSON file. */
public record PieceData(
        String id,
        String type,
        Integer column,
        Integer row,
        Integer spawnOrder) { }
