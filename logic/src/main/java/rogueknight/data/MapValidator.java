package rogueknight.data;

import java.util.HashSet;
import java.util.Set;
import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PieceData;
import rogueknight.data.dto.PositionData;

/** Validates a decoded map before any runtime battle state is created. */
public final class MapValidator {

    public void validate(MapData map) {
        if (map == null) throw new IllegalArgumentException("MapData is null");
        if (map.width() == null || map.height() == null) {
            throw new IllegalArgumentException("width/height is null");
        }
        if (map.width() != Position.BOARD_SIZE || map.height() != Position.BOARD_SIZE) {
            throw new IllegalArgumentException("Sai kich thuoc: " + map.width() + "x" + map.height());
        }
        if (map.rocks() == null || map.pieces() == null) {
            throw new IllegalArgumentException("Map phai co danh sach rocks, pieces");
        }

        Set<Position> occupied = new HashSet<>();
        for (int i = 0; i < map.rocks().size(); i++) {
            PositionData rock = map.rocks().get(i);
            String location = "rocks[" + i + "]";
            if (rock == null) throw new IllegalArgumentException(location + " is null");
            register(occupied, rock.column(), rock.row(), location);
        }

        Set<String> ids = new HashSet<>();
        boolean hasKnight = false;
        boolean hasKing = false;
        for (int i = 0; i < map.pieces().size(); i++) {
            PieceData piece = map.pieces().get(i);
            String location = "pieces[" + i + "]";
            if (piece == null) throw new IllegalArgumentException(location + " is null");

            register(occupied, piece.column(), piece.row(), location);
            validateId(piece.id(), ids, location);
            PieceType type = parseType(piece.type(), location);
            validateKnightIdentity(piece.id(), type, location);
            hasKnight |= type == PieceType.KNIGHT;
            hasKing |= type == PieceType.KING;
            if (piece.spawnOrder() == null) {
                throw new IllegalArgumentException(location + " thiếu spawnOrder.");
            }
        }

        if (!hasKnight) throw new IllegalArgumentException("Map thiếu KNIGHT");
        if (!hasKing) throw new IllegalArgumentException("Map thiếu KING");
    }

    private static void register(Set<Position> occupied, Integer column, Integer row, String location) {
        if (column == null || row == null) {
            throw new IllegalArgumentException(location + " thieu column, row");
        }
        if (!Position.isInBounds(column, row)) {
            throw new IllegalArgumentException(location + " co row hoac column khong nam trong board: "
                    + column + " " + row);
        }
        Position position = new Position(column, row);
        if (!occupied.add(position)) {
            throw new IllegalArgumentException(location + " chồng ô tại (" + column + ", " + row + ").");
        }
    }

    private static void validateId(String id, Set<String> ids, String location) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(location + " phải có id không rỗng.");
        }
        if (!ids.add(id)) {
            throw new IllegalArgumentException(location + " có id bị trùng: " + id + ".");
        }
    }

    private static PieceType parseType(String value, String location) {
        if (value == null) {
            throw new IllegalArgumentException(location + " có type không hợp lệ: null.");
        }
        try {
            return PieceType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(location + " có type không hợp lệ: " + value + ".");
        }
    }

    private static void validateKnightIdentity(String id, PieceType type, String location) {
        if (type == PieceType.KNIGHT && !"knight".equals(id)) {
            throw new IllegalArgumentException(location + " có type KNIGHT nhưng id phải là knight.");
        }
        if ("knight".equals(id) && type != PieceType.KNIGHT) {
            throw new IllegalArgumentException(location + " dùng id knight nhưng type phải là KNIGHT.");
        }
    }
}
