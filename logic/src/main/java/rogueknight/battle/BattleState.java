package rogueknight.battle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import rogueknight.battle.BattleSnapshot.PieceView;
import rogueknight.board.Board;
import rogueknight.board.Position;

/** Mutable aggregate for one battle; owns Board, PieceState map and Inventory. */
public final class BattleState {
    private static final int INITIAL_TURN = 1;
    private final Board board;
    private final Map<String, PieceState> pieces = new LinkedHashMap<>();
    private Inventory inventory;
    private int globalTurn = INITIAL_TURN;
    private BattlePhase phase = BattlePhase.PLAYER_SELECT;
    private BattleResult result = BattleResult.ONGOING;

    public BattleState(Set<Position> rocks, Inventory inventory) {
        this.board = new Board(rocks);
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }
    public int globalTurn() { return globalTurn; }
    public BattlePhase phase() { return phase; }
    public BattleResult result() { return result; }
    public Inventory inventory() { return inventory; }
    public Optional<PieceState> pieceState(String id) { return Optional.ofNullable(pieces.get(id)); }
    public Optional<Position> positionOf(String id) { return board.positionOf(id); }
    public Optional<String> pieceAt(Position position) { return board.pieceAt(position); }
    public boolean isEmpty(Position position) { return board.isEmpty(position); }
    public boolean isRock(Position position) { return board.isRock(position); }

    public void addPiece(PieceState piece, Position position) {
        Objects.requireNonNull(piece, "piece");
        if (piece.hp() == 0) throw new IllegalArgumentException("A piece with zero HP cannot occupy the Board");
        if (pieces.containsKey(piece.id())) throw new IllegalArgumentException("Duplicate piece id: " + piece.id());
        board.place(piece.id(), position);
        pieces.put(piece.id(), piece);
    }
    public void movePiece(String id, Position destination) { requireKnown(id); board.move(id, destination); }
    public void removePiece(String id) { requireKnown(id); board.remove(id); pieces.remove(id); }
    public void replacePiece(PieceState updated) {
        Objects.requireNonNull(updated, "updated");
        requireKnown(updated.id());
        if (updated.hp() == 0) throw new IllegalArgumentException("A zero-HP piece must be removed from the Board");
        PieceState current = pieces.get(updated.id());
        if (updated.spawnOrder() != current.spawnOrder())
            throw new IllegalArgumentException("spawnOrder is immutable for a piece");
        pieces.put(updated.id(), updated);
    }
    /** Only the battle controller advances the turn, once per accepted Player Action (TD 5.2). */
    public void advanceTurn() { globalTurn++; }
    public void setPhase(BattlePhase phase) { this.phase = Objects.requireNonNull(phase, "phase"); }
    /** Ending the battle also moves the phase to FINISHED; ONGOING cannot be restored here. */
    public void finish(BattleResult result) {
        Objects.requireNonNull(result, "result");
        if (result == BattleResult.ONGOING) throw new IllegalArgumentException("finish requires a terminal result");
        this.result = result;
        this.phase = BattlePhase.FINISHED;
    }
    /** Aggregate mutation point intended for a successful Equipment Action resolver call. */
    public void consumeEquipment(EquipmentType type) { inventory = inventory.consumeOneCharge(type); }

    public BattleSnapshot snapshot() {
        List<PieceView> views = new ArrayList<>();
        for (PieceState piece : pieces.values()) {
            Position position = board.positionOf(piece.id()).orElseThrow(
                    () -> new IllegalStateException("Piece without position: " + piece.id()));
            views.add(new PieceView(
                piece.id(),
                piece.type(),
                piece.hp(),
                piece.cycle(),
                piece.nextActivationTurn(),
                piece.spawnOrder(),
                position,
                piece.currentDirection()
            ));
        }
        Set<String> boardIds = board.pieceIds();
        if (views.size() != boardIds.size() || views.stream().anyMatch(view -> !boardIds.contains(view.id())))
            throw new IllegalStateException("Board occupants and piece states differ");
        return new BattleSnapshot(globalTurn, phase, result, board.rocks(), views, inventory);
    }
    private void requireKnown(String id) {
        if (!pieces.containsKey(id)) throw new IllegalArgumentException("Unknown piece id: " + id);
    }
}
