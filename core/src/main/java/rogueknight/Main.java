package rogueknight;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import rogueknight.ai.EnemyAI;
import rogueknight.battle.EquipmentType;
import rogueknight.core.input.BoardInputHandler;
import rogueknight.core.input.BattleControllerAdapter;
import rogueknight.core.input.BattleControllerPort;
import rogueknight.core.view.BattlePlayback;
import rogueknight.core.view.BattlePresentationModel;
import rogueknight.core.view.BoardProjection;
import rogueknight.core.view.BoardRenderer;
import rogueknight.data.BattleFactory;

import java.io.IOException;
import java.util.List;

/** Desktop application entry point and presentation playback coordinator. */
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
    private BattleControllerPort controller;
    private BoardInputHandler inputHandler;
    private BoardRenderer renderer;
    private BattlePlayback playback;
    private String runtimeFailure;

    @Override
    public void create() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
        camera.position.set(VIRTUAL_WIDTH / 2f, VIRTUAL_HEIGHT / 2f, 0);

        projection = new BoardProjection(BOARD_ORIGIN_X, BOARD_ORIGIN_Y, TILE_SIZE);
        try {
            controller = new BattleControllerAdapter(
                new BattleFactory().createMap01Controller(
                    EquipmentType.CANNON_MOVEMENT,
                    EquipmentType.DIAGONAL_SKEWER,
                    new EnemyAI()
                )
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Không thể khởi tạo trận Map01", exception);
        }

        inputHandler = new BoardInputHandler(projection, controller);
        playback = new BattlePlayback(
            BattlePresentationModel.from(inputHandler.getCurrentSnapshot()),
            0.18f
        );
        renderer = new BoardRenderer();

        // Handle playback controls through backend key events, not character polling.
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.EQUALS
                    || keycode == Input.Keys.PLUS
                    || keycode == Input.Keys.NUMPAD_ADD) {
                    playback.faster();
                    return true;
                }
                if (keycode == Input.Keys.MINUS
                    || keycode == Input.Keys.NUMPAD_SUBTRACT) {
                    playback.slower();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void render() {
        if (runtimeFailure == null) {
            try {
                // Lock player input until the previous event stream has finished.
                if (!playback.isPlaying()) {
                    processKeyboardInput();
                    processMouseInput();
                    List<rogueknight.action.BattleEvent> events = inputHandler.drainEvents();
                    playback.enqueue(events);
                }
                playback.update(Gdx.graphics.getDeltaTime());
            } catch (RuntimeException failure) {
                // DEC-12: stop the application battle loop without rolling back valid actions.
                runtimeFailure = failure.getClass().getSimpleName() + ": "
                    + (failure.getMessage() == null ? "Unexpected battle runtime failure" : failure.getMessage());
                playback.stop();
                Gdx.app.error("RogueKnight", "Battle stopped after runtime failure", failure);
            }
        }

        ScreenUtils.clear(0.12f, 0.12f, 0.16f, 1f);
        viewport.apply();
        camera.update();
        renderer.render(camera, inputHandler, playback);
        if (runtimeFailure != null) renderer.renderRuntimeFailure(camera, runtimeFailure);
    }

    private void processKeyboardInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            inputHandler.selectAction(0);
            return;
        }
        for (int index = 0; index <= 9; index++) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0 + index)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0 + index)) {
                inputHandler.selectAction(index);
                break;
            }
        }
    }

    private void processMouseInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;
        touchPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(touchPoint);
        inputHandler.handleWorldClick(touchPoint.x, touchPoint.y);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
    }
}
