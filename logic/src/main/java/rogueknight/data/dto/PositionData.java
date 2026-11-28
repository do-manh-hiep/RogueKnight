package rogueknight.data.dto;

/** Coordinate data read from JSON and validated before conversion to a runtime Position. */
public record PositionData(Integer column, Integer row) { }
