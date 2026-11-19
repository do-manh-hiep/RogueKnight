package rogueknight.board;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/*
 * Board.java  |  T-01.02  |  TD: 2.2, 4.1-4.3, R-B01..R-B02, I01, I04, TC-B01..TC-B02
 *
 * Bàn cờ 8x8: Rock cố định và vị trí các quân theo pieceId.
 * - Board là nguồn vị trí duy nhất (Map pieceId -> Position). Không có cache ô -> quân;
 *   pieceAt() quét map để không thể lệch với positions (I04).
 * - Board tự bảo vệ occupancy: mỗi ô tối đa một quân hoặc một Rock (I01).
 *   place/move vào ô Rock, ô có quân (kể cả ô hiện tại của chính quân đó) bị từ chối.
 * - Rock nạp lúc tạo, bất biến suốt trận (sao chép phòng thủ).
 * - Board không biết HP, lượt, damage hay loại di chuyển; chỉ biết biên, occupancy.
 * - Lỗi gọi sai (trùng ID, ô bị chiếm, ID lạ) ném IllegalArgumentException trước khi
 *   sửa dữ liệu. Mã lỗi nghiệp vụ (OCCUPIED, ...) thuộc Rules ở T-02.01.
 * - place/move/remove public để BattleState gọi; hạn chế truy cập ra ngoài logic
 *   theo TD 6.3 (facade/visibility) xử lý khi chốt DEC-04.
 * Từ khóa tìm kiếm: place, move, remove, pieceAt, positionOf, isEmpty, requireFree.
 */
public final class Board {

    private final Set<Position> rocks;
    private final Map<String, Position> positions = new LinkedHashMap<>();

    public Board(Set<Position> rocks) {
        this.rocks = Set.copyOf(rocks);
    }

    public Set<Position> rocks() {
        return rocks;
    }

    public boolean isRock(Position position) {
        return rocks.contains(position);
    }

    public Optional<String> pieceAt(Position position) {
        Objects.requireNonNull(position, "position");
        for (Map.Entry<String, Position> entry : positions.entrySet()) {
            if (entry.getValue().equals(position)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public boolean isEmpty(Position position) {
        Objects.requireNonNull(position);
        return !isRock(position) && pieceAt(position).isEmpty();
    }

    public Optional<Position> positionOf(String pieceId) {
        requireId(pieceId);
        return Optional.ofNullable(positions.get(pieceId));
    }

    public Set<String> pieceIds() {
        return Set.copyOf(positions.keySet());
    }

    public Map<String, Position> positionsCopy() {
        return Map.copyOf(positions);
    }

    public void place(String pieceId, Position position) {
        requireId(pieceId);
        if (positions.containsKey(pieceId)) {
            throw new IllegalArgumentException("Duplicate piece id: " + pieceId);
        }
        requireFree(position);
        positions.put(pieceId, position);
    }

    public void move(String pieceId, Position destination) {
        requireKnown(pieceId);
        requireFree(destination);
        positions.put(pieceId, destination);
    }

    public void remove(String pieceId) {
        requireKnown(pieceId);
        positions.remove(pieceId);
    }

    private static void requireId(String pieceId) {
        if (pieceId == null || pieceId.isBlank()) {
            throw new IllegalArgumentException("Piece id must not be blank");
        }
    }

    private void requireKnown(String pieceId) {
        if (!positions.containsKey(pieceId)) {
            throw new IllegalArgumentException("Unknown piece id: " + pieceId);
        }
    }

    private void requireFree(Position position) {
        Objects.requireNonNull(position);
        if (isRock(position)) {
            throw new IllegalArgumentException("Tile is a rock: " + position);
        }
        if (pieceAt(position).isPresent()) {
            throw new IllegalArgumentException("Tile is occupied: " + position);
        }
    }
}
