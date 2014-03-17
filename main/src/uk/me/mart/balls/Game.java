
package uk.me.mart.balls;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;

public class Game extends com.badlogic.gdx.Game {

    public static final String LOG_TAG = "Game";

    @Override
    public void create() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        Gdx.app.debug(LOG_TAG, "create()");
    }

    @Override
    public void dispose() {
        super.dispose();
        Gdx.app.debug(LOG_TAG, "dispose()");
    }

}
