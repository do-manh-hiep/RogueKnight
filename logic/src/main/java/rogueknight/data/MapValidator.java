package rogueknight.data;

import rogueknight.battle.PieceType;
import rogueknight.board.Position;
import rogueknight.data.dto.MapData;
import rogueknight.data.dto.PositionData;
import rogueknight.data.dto.PieceData;

import java.util.Set;
import java.util.HashSet;

public class MapValidator {
    public void validate(MapData map){
        if(map == null){
            throw new IllegalArgumentException("MapData is null");
        }
        
        Integer width = map.width();
        Integer height = map.height();

        if(width == null || height == null){
            throw new IllegalArgumentException("width/height is null");
        }

        if(width != Position.BOARD_SIZE || height != Position.BOARD_SIZE){
            throw new IllegalArgumentException("Sai kich thuoc: " + width +"x"+height);
        }
        if(map.rocks() == null || map.pieces() == null){
            throw new IllegalArgumentException("Map phai co danh sach rocks, pieces");
        }

        Set<Position> occupiedCells = new HashSet<>();

        for(int i = 0; i < map.rocks().size(); i++){
            PositionData rock = map.rocks().get(i);
            String location = "rocks[" + i + "]";

            if(rock == null){
                throw new IllegalArgumentException(location + " is null");
            }
            validatePosition(rock.column(), rock.row(), location);
            registerCell(occupiedCells, rock.column(), rock.row(), location);
        }
        
        Set<String> seenPieceIds = new HashSet<>();
        boolean hasKnight = false;
        boolean hasKing = false;

        for(int i = 0; i < map.pieces().size(); i++){
            PieceData piece = map.pieces().get(i);
            String location = "pieces[" + i + "]";

            if(piece == null){
                throw new IllegalArgumentException(location + " is null");
            }
            validatePosition(piece.column(), piece.row(), location);
            registerCell(occupiedCells, piece.column(), piece.row(), location);

            String id = piece.id();
            if(id == null || id.isBlank()){
                throw new IllegalArgumentException(
                    location + " phải có id không rỗng."
                );
            }
            if(!seenPieceIds.add(id)){
                throw new IllegalArgumentException(
                    location +" có id bị trùng: "+ id +"."
                );
            }

            String type = piece.type();

            if (type == null) {
                throw new IllegalArgumentException(
                    location + " có type không hợp lệ: null."
                );
            }
            PieceType pieceType;
            try {
                pieceType = PieceType.valueOf(type);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    location + " có type không hợp lệ: " + type + "."
                );
            }

            if (pieceType == PieceType.KNIGHT && !"knight".equals(id)) {
                throw new IllegalArgumentException(
                    location + " có type KNIGHT nhưng id phải là knight."
                );
            }

            if ("knight".equals(id) && pieceType != PieceType.KNIGHT) {
                throw new IllegalArgumentException(
                    location + " dùng id knight nhưng type phải là KNIGHT."
                );
            }

            if (pieceType == PieceType.KNIGHT) {
                hasKnight = true;
            } else if (pieceType == PieceType.KING) {
                hasKing = true;
            }

            if (piece.spawnOrder() == null) {
                throw new IllegalArgumentException(
                        location + " thiếu spawnOrder."
            );            
            }
        }
        if(!hasKnight){
            throw new IllegalArgumentException("Map thiếu KNIGHT");
        }
        if(!hasKing){
            throw new IllegalArgumentException("Map thiếu KING");
        }
    }    

    private void validatePosition(Integer column, Integer row, String location) {
        if(column == null || row == null){
            throw new IllegalArgumentException(location +" thieu column, row");
        }

        if (!Position.isInBounds(column, row)) {
            throw new IllegalArgumentException(location +" co row hoac column khong nam trong board: "+ column +" "+ row  );
        }
    }

    private void registerCell(
        Set<Position> occupiedCells, int column, int row, String location
    ) {
        Position cell = new Position(column, row);

        if(!occupiedCells.add(cell)){
            throw new IllegalArgumentException(
                location + " chồng ô tại (" + column +", " + row + ")."
            );
        }
    }

}
