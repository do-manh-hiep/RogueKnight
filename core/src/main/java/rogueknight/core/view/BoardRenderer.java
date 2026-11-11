package rogueknight.core.view;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import rogueknight.core.input.BoardInputHandler;
import rogueknight.core.input.FixtureBattleController;

import java.util.Objects;
import java.util.Set;

/**
 * Renders the 8x8 chessboard, coordinate labels (a..h, 1..8), highlights, pieces,
 * obstacles, and status HUD using LibGDX ShapeRenderer and SpriteBatch.
 */
public final class BoardRenderer {
    private final ShapeRenderer shapeRenderer;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // Theme colors
    private final Color lightSquareColor = new Color(0.82f, 0.77f, 0.69f, 1f);
    private final Color darkSquareColor = new Color(0.48f, 0.40f, 0.33f, 1f);
    private final Color legalHighlightColor = new Color(0.2f, 0.85f, 0.35f, 0.45f);
    private final Color legalBorderColor = new Color(0.1f, 0.7f, 0.25f, 0.9f);
    private final Color selectedKnightColor = new Color(0.2f, 0.6f, 0.95f, 0.55f);
    private final Color rockColor = new Color(0.28f, 0.28f, 0.32f, 0.95f);
    private final Color enemyColor = new Color(0.85f, 0.25f, 0.25f, 0.95f);

    public BoardRenderer() {
        this.shapeRenderer = new ShapeRenderer();
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.font.getData().setScale(1.0f);
    }

    public void render(OrthographicCamera camera, BoardInputHandler inputHandler) {
        Objects.requireNonNull(camera, "camera must not be null");
        Objects.requireNonNull(inputHandler, "inputHandler must not be null");

        BoardProjection projection = inputHandler.getProjection();
        BattleViewSnapshot snapshot = inputHandler.getCurrentSnapshot();
        Set<BoardPosition> legalMoves = snapshot.legalMoves();
        BoardPosition knightPos = snapshot.knightPosition();
        float tileSize = projection.getTileSize();

        // 1. Draw Board Squares and Highlights using ShapeRenderer
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw 8x8 checkerboard
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
                boolean isLight = (col + row) % 2 != 0;
                shapeRenderer.setColor(isLight ? lightSquareColor : darkSquareColor);
                shapeRenderer.rect(
                    projection.getCellLeft(col),
                    projection.getCellBottom(row),
                    tileSize,
                    tileSize
                );
            }
        }
        shapeRenderer.end();

        // Draw translucent highlights (Legal Moves & Selected Knight)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Highlight legal moves
        shapeRenderer.setColor(legalHighlightColor);
        for (BoardPosition legalPos : legalMoves) {
            shapeRenderer.rect(
                projection.getCellLeft(legalPos.column()),
                projection.getCellBottom(legalPos.row()),
                tileSize,
                tileSize
            );
        }

        // Highlight selected Knight square
        shapeRenderer.setColor(selectedKnightColor);
        shapeRenderer.rect(
            projection.getCellLeft(knightPos.column()),
            projection.getCellBottom(knightPos.row()),
            tileSize,
            tileSize
        );

        shapeRenderer.end();

        // Draw border outlines for legal moves
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

        // Outer board border
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.rect(
            projection.getOriginX(),
            projection.getOriginY(),
            projection.getBoardWidth(),
            projection.getBoardHeight()
        );

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // 2. Draw Text, Labels, Pieces and HUD using SpriteBatch
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // Draw column labels (a..h) along bottom and top
        font.setColor(Color.WHITE);
        for (int col = 0; col < BoardPosition.BOARD_SIZE; col++) {
            String colLabel = String.valueOf((char) ('a' + col));
            float x = projection.getCellCenterX(col) - 4;
            // Bottom label
            font.draw(batch, colLabel, x, projection.getOriginY() - 8);
            // Top label
            font.draw(batch, colLabel, x, projection.getOriginY() + projection.getBoardHeight() + 18);
        }

        // Draw row labels (1..8) along left and right
        for (int row = 0; row < BoardPosition.BOARD_SIZE; row++) {
            String rowLabel = String.valueOf(row + 1);
            float y = projection.getCellCenterY(row) + 5;
            // Left label
            font.draw(batch, rowLabel, projection.getOriginX() - 18, y);
            // Right label
            font.draw(batch, rowLabel, projection.getOriginX() + projection.getBoardWidth() + 8, y);
        }

        // Draw Rocks
        font.setColor(new Color(0.9f, 0.9f, 0.95f, 1f));
        for (BoardPosition rockPos : snapshot.rocks()) {
            drawCenteredText("[ROCK]", projection.getCellCenterX(rockPos.column()), projection.getCellCenterY(rockPos.row()));
        }

        // Draw Enemy pieces
        font.setColor(enemyColor);
        snapshot.enemyPieces().forEach((pos, name) -> {
            drawCenteredText("[" + name.toUpperCase() + "]", projection.getCellCenterX(pos.column()), projection.getCellCenterY(pos.row()));
        });

        // Draw Knight
        font.setColor(new Color(0.1f, 0.95f, 1.0f, 1f));
        drawCenteredText(">>> KNIGHT <<<", projection.getCellCenterX(knightPos.column()), projection.getCellCenterY(knightPos.row()) + 8);
        drawCenteredText("(" + knightPos.notation() + ")", projection.getCellCenterX(knightPos.column()), projection.getCellCenterY(knightPos.row()) - 8);

        // 3. Draw HUD Information Header and Footer
        font.setColor(Color.YELLOW);
        font.draw(batch, "RogueKnight - T-02.02: Click và thấy Knight di chuyển", 20, 465);

        if (snapshot.isFixtureDemo()) {
            font.setColor(new Color(1f, 0.5f, 0.2f, 1f));
            font.draw(batch, FixtureBattleController.DEMO_LABEL, 20, 445);
        }

        font.setColor(Color.WHITE);
        font.draw(batch, "Turn: " + snapshot.globalTurn() + "  |  Vị trí Knight: " + knightPos.notation(), 20, 425);

        // Feedback message
        font.setColor(Color.CYAN);
        font.draw(batch, "Thông báo: " + inputHandler.getFeedbackMessage(), 20, 35);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Hướng dẫn: Click ô viền xanh lá để di chuyển | Knight đi L-shape hoặc 1 ô kề trống", 20, 18);

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
