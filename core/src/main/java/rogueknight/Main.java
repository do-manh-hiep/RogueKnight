package rogueknight;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import rogueknight.core.input.BoardInputHandler;
import rogueknight.core.input.FixtureBattleController;
import rogueknight.core.view.BoardProjection;
import rogueknight.core.view.BoardRenderer;

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
    private FixtureBattleController controller;
    private BoardInputHandler inputHandler;
    private BoardRenderer renderer;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        projection = new BoardProjection(BOARD_ORIGIN_X, BOARD_ORIGIN_Y, TILE_SIZE);
        controller = new FixtureBattleController();
        inputHandler = new BoardInputHandler(projection, controller);
        renderer = new BoardRenderer();
    }

    @Override
    public void render() {
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
