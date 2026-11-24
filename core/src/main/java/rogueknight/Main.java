package rogueknight;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import rogueknight.core.input.ActionMode;
import rogueknight.core.input.BoardInputHandler;
import rogueknight.core.view.BoardProjection;
import rogueknight.core.view.BoardRenderer;

import rogueknight.action.BattleController;
import rogueknight.action.EnemyTurnRunner;
import rogueknight.ai.EnemyAI;
import rogueknight.battle.BattleState;
import rogueknight.battle.EquipmentType;
import rogueknight.core.input.BattleControllerAdapter;
import rogueknight.core.input.BattleControllerPort;
import rogueknight.data.BattleFactory;

import java.io.IOException;

/**
 * Main application listener implementing the UI slice for T-02.02:
 * Click and see Knight move on an 8x8 chessboard.
 */
public class Main extends ApplicationAdapter {
    public static final float VIRTUAL_WIDTH = 640f;
    public static final float VIRTUAL_HEIGHT = 480f;
    public static final float TILE_SIZE = 48f;
    public static final float BOARD_ORIGIN_X = 128f;
    public static final float BOARD_ORIGIN_Y = 48f;

    private OrthographicCamera camera;
    private Viewport viewport;
    private final Vector3 touchPoint = new Vector3();

    private BoardProjection projection;
    private BattleControllerPort  controller;
    private BoardInputHandler inputHandler;
    private BoardRenderer renderer;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        projection = new BoardProjection(BOARD_ORIGIN_X, BOARD_ORIGIN_Y, TILE_SIZE);
        try {
            BattleState battle = new BattleFactory().createMap01(
                EquipmentType.CANNON_MOVEMENT,
                EquipmentType.DIAGONAL_SKEWER
            );

            // T-03.01-T-03.03: real enemy turn for every Enemy type (Pawn, Rook, Bishop, King, Queen).
            controller = new BattleControllerAdapter(
                new BattleController(battle, new EnemyTurnRunner(new EnemyAI()))
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(
                "Không thể khởi tạo trận Map01",
                exception
            );
        }

        inputHandler = new BoardInputHandler(projection, controller);
        renderer = new BoardRenderer();
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            inputHandler.selectMode(ActionMode.WAIT);
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) inputHandler.selectMode(ActionMode.MOVE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) inputHandler.selectMode(ActionMode.CANNON_MOVEMENT);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) inputHandler.selectMode(ActionMode.CANNON_ATTACK);
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) inputHandler.selectMode(ActionMode.DIAGONAL_SKEWER);

        // Handle mouse click input
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
            viewport.unproject(touchPoint);
            inputHandler.handleWorldClick(touchPoint.x, touchPoint.y);
        }

        ScreenUtils.clear(0.12f, 0.12f, 0.16f, 1f);
        viewport.apply();
        camera.update();

        renderer.render(camera, inputHandler);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (renderer != null) {
            renderer.dispose();
        }
    }
}
