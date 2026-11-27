package rogueknight.core.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import rogueknight.core.input.ActionMode;
import rogueknight.core.input.BoardInputHandler;
import rogueknight.core.input.FixtureBattleController;

import java.util.Objects;
import java.util.Set;

/** Renders the board, the playback projection, highlights, coordinates and HUD. */
public final class BoardRenderer {
    private final ShapeRenderer shapeRenderer = new ShapeRenderer();
    private final SpriteBatch batch = new SpriteBatch();
    private final BitmapFont font = new BitmapFont();
    private final GlyphLayout glyphLayout = new GlyphLayout();

    private final Color lightSquareColor = new Color(0.82f, 0.77f, 0.69f, 1f);
    private final Color darkSquareColor = new Color(0.48f, 0.40f, 0.33f, 1f);
    private final Color legalHighlightColor = new Color(0.2f, 0.85f, 0.35f, 0.45f);
    private final Color legalBorderColor = new Color(0.1f, 0.7f, 0.25f, 0.9f);
    private final Color selectedKnightColor = new Color(0.2f, 0.6f, 0.95f, 0.55f);
    private final Color rockTextColor = new Color(0.9f, 0.9f, 0.95f, 1f);
    private final Color enemyColor = new Color(0.85f, 0.25f, 0.25f, 0.95f);
    private final Color fallenKnightColor = new Color(0.8f, 0.2f, 0.2f, 1f);
    private final Color knightColor = new Color(0.1f, 0.95f, 1.0f, 1f);
    private final Color fixtureLabelColor = new Color(1f, 0.5f, 0.2f, 1f);

    public BoardRenderer() {
        font.getData().setScale(1.0f);
    }

    public void render(
        OrthographicCamera camera,
        BoardInputHandler inputHandler,
        BattlePlayback playback
    ) {
        Objects.requireNonNull(camera, "camera must not be null");
        Objects.requireNonNull(inputHandler, "inputHandler must not be null");
        Objects.requireNonNull(playback, "playback must not be null");
        BattlePresentationModel presentation = playback.model();

        BoardProjection projection = inputHandler.getProjection();
        BattleViewSnapshot snapshot = inputHandler.getCurrentSnapshot();
        Set<BoardPosition> legalMoves = playback.isPlaying()
            ? Set.of()
            : inputHandler.getHighlightTargets();
        PresentationPiece knight = presentation.piece("knight").orElse(null);
        BoardPosition knightPos = knight != null ? knight.position() : snapshot.knightPosition();
        boolean knightAlive = knight != null && knight.hp() > 0;
        float tileSize = projection.getTileSize();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
                shapeRenderer.setColor((col + row) % 2 != 0 ? lightSquareColor : darkSquareColor);
                shapeRenderer.rect(
                    projection.getCellLeft(col), projection.getCellBottom(row), tileSize, tileSize
                );
            }
        }
        shapeRenderer.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(legalHighlightColor);
        for (BoardPosition legalPos : legalMoves) {
            shapeRenderer.rect(
                projection.getCellLeft(legalPos.column()),
                projection.getCellBottom(legalPos.row()),
                tileSize,
                tileSize
            );
        }
        shapeRenderer.setColor(selectedKnightColor);
        if (knightAlive) {
            shapeRenderer.rect(
                projection.getCellLeft(knightPos.column()),
                projection.getCellBottom(knightPos.row()),
                tileSize,
                tileSize
            );
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(legalBorderColor);
        for (BoardPosition legalPos : legalMoves) {
            shapeRenderer.rect(
                projection.getCellLeft(legalPos.column()) + 1,
                projection.getCellBottom(legalPos.row()) + 1,
                tileSize - 2,
                tileSize - 2
            );
        }
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(
            projection.getOriginX(), projection.getOriginY(),
            projection.getBoardWidth(), projection.getBoardHeight()
        );
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            String label = String.valueOf((char) ('a' + col));
            float x = projection.getCellCenterX(col) - 4;
            font.draw(batch, label, x, projection.getOriginY() - 8);
            font.draw(batch, label, x, projection.getOriginY() + projection.getBoardHeight() + 18);
        }
        for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
            String label = String.valueOf(row + 1);
            float y = projection.getCellCenterY(row) + 5;
            font.draw(batch, label, projection.getOriginX() - 18, y);
            font.draw(batch, label, projection.getOriginX() + projection.getBoardWidth() + 8, y);
        }

        font.setColor(rockTextColor);
        for (BoardPosition rockPos : snapshot.rocks()) {
            drawCenteredText("[ROCK]", projection.getCellCenterX(rockPos.column()), projection.getCellCenterY(rockPos.row()));
        }

        for (PresentationPiece piece : presentation.piecesById().values()) {
            BoardPosition position = piece.position();
            float centerX = projection.getCellCenterX(position.column());
            float centerY = projection.getCellCenterY(position.row());
            if ("knight".equals(piece.id())) {
                if (piece.hp() > 0) {
                    font.setColor(knightColor);
                    drawCenteredText(">>> KNIGHT <<<", centerX, centerY + 8);
                    drawCenteredText("(" + position.notation() + ")", centerX, centerY - 8);
                } else {
                    font.setColor(fallenKnightColor);
                    drawCenteredText("[FALLEN]", centerX, centerY);
                }
            } else {
                font.setColor(enemyColor);
                String label = "[" + piece.type().name() + " " + piece.hp() + "HP]";
                drawCenteredText(label, centerX, centerY);
            }
        }
        if (!knightAlive && knight == null) {
            font.setColor(fallenKnightColor);
            drawCenteredText("[FALLEN]", projection.getCellCenterX(knightPos.column()), projection.getCellCenterY(knightPos.row()));
            drawCenteredText("(" + knightPos.notation() + ")", projection.getCellCenterX(knightPos.column()), projection.getCellCenterY(knightPos.row()) - 8);
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "RogueKnight", 20, 465);
        if (snapshot.isFixtureDemo()) {
            font.setColor(fixtureLabelColor);
            font.draw(batch, FixtureBattleController.DEMO_LABEL, 20, 445);
        }

        font.setColor(Color.WHITE);
        String headerInfo = "Turn " + presentation.globalTurn()
            + " | Knight: " + (knightAlive ? knightPos.notation() : "DEAD")
            + " | " + presentation.result().map(Enum::name).orElse("ONGOING");
        font.draw(batch, headerInfo, 20, 425);

        font.setColor(Color.CYAN);
        font.draw(batch, "Thông báo: " + inputHandler.getFeedbackMessage(), 20, 35);
        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "[0] Wait | [1] Move / Attack | [2+] Equipment | Bấm lại để bỏ chọn | Click ô hợp lệ để thực hiện", 20, 18);

        float cardY = 400f;
        font.setColor(Color.WHITE);
        font.draw(batch, "ACTIONS", 8, cardY);
        java.util.List<ActionMode> availableActions = inputHandler.getAvailableModes();
        for (int index = 0; index < availableActions.size(); index++) {
            ActionMode available = availableActions.get(index);
            cardY -= 20f;
            font.setColor(available == inputHandler.getMode() ? Color.YELLOW : Color.LIGHT_GRAY);
            font.draw(batch, "[" + index + "] " + available.label(), 8, cardY);
        }

        batch.end();
    }

    private void drawCenteredText(String text, float centerX, float centerY) {
        glyphLayout.setText(font, text);
        font.draw(batch, text, centerX - glyphLayout.width / 2f, centerY + glyphLayout.height / 2f);
    }

    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
