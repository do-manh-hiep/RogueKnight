package rogueknight.data.dto;

import java.util.List;

/** JSON map definition; validation is performed before conversion to runtime state. */
public record MapData(
        Integer width,
        Integer height,
        List<PositionData> rocks,
        List<PieceData> pieces) { }
