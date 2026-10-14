package rogueknight;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import rogueknight.skeleton.WaitDemoController;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;
    private final WaitDemoController controller = new WaitDemoController();
    private String message = "Space: WAIT | Demo only; no enemy queue";

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
    }

    @Override
    public void render() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            var result = controller.submit(
                new WaitDemoController.Request("knight", "WAIT")
            );
            message = result.accepted()
                ? "WAIT accepted | Demo only; no enemy queue"
                : result.error().name();
        }
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        batch.draw(image, 140, 210);
        font.draw(batch, "Skeleton WAIT demo", 20, 100);
        font.draw(batch, "Turn: " + controller.snapshot().globalTurn(), 20, 75);
        font.draw(batch, message, 20, 50);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        font.dispose();
    }
}
