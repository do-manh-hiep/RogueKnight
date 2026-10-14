package com.rogueknight.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.rogueknight.game.logic.ActionResolver;
import com.rogueknight.game.logic.Enemy;
import com.rogueknight.game.logic.EnemyQueue;
import com.rogueknight.game.logic.TurnCoordinator;
import com.rogueknight.game.presentation.InputController;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;

    private TurnCoordinator turnCoordinator;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();

        ActionResolver resolver = new ActionResolver();
        EnemyQueue enemyQueue = new EnemyQueue();
        enemyQueue.addEnemy(new Enemy("Goblin_1"));
        turnCoordinator = new TurnCoordinator(resolver, enemyQueue);

        InputController controller = new InputController(turnCoordinator);
        Gdx.input.setInputProcessor(controller);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        batch.begin();
        batch.draw(image, 140, 210);
        font.draw(batch, "Turn: " + turnCoordinator.getTurnNumber(), 20, 460);
        font.draw(batch, "Nhan SPACE hoac phim W de Wait (qua luot)", 20, 430);
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        font.dispose();
    }
}