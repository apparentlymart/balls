
package uk.me.mart.balls;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.Input.Keys;

public class PlayScreen implements Screen {

    public Game game;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private Texture ballTexture;
    private TextureRegion ballTextureRegion;
    private Box2DDebugRenderer debugRenderer;
    private World physicsWorld;
    private Body ballBody;
    private float worldAngle = 0f;
    private Vector2 cameraPos = new Vector2(0, 0);
    private Vector2 cameraPosDelta = new Vector2(0, 0);

    private static final float PIXELS_PER_METER = 32f;

    public PlayScreen(Game game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        Vector2 pos = this.ballBody.getPosition();
        float angle = this.ballBody.getAngle() * MathUtils.radiansToDegrees;

        cameraPosDelta.x = pos.x * PIXELS_PER_METER;
        cameraPosDelta.y = pos.y * PIXELS_PER_METER;
        cameraPosDelta.sub(cameraPos);
        cameraPosDelta.limit(32f);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(
            ballTextureRegion,
            (pos.x * PIXELS_PER_METER) - 64f,
            (pos.y * PIXELS_PER_METER) - 64f,
            64f,
            64f,
            128f,
            128f,
            1f,
            1f,
            angle
        );
        batch.end();

        debugRenderer.render(physicsWorld, new Matrix4(camera.combined).scale(PIXELS_PER_METER, PIXELS_PER_METER, 1f));

        if (Gdx.input.isKeyPressed(Keys.A)) {
            this.ballBody.applyLinearImpulse(-0.8f, 0, pos.x, pos.y, true);
        }
        if (Gdx.input.isKeyPressed(Keys.D)) {
            this.ballBody.applyLinearImpulse(0.8f, 0, pos.x, pos.y, true);
        }

        float angleChange = 0.0f;
        boolean changedAngle = false;
        if (Gdx.input.isKeyPressed(Keys.LEFT)) {
            angleChange = -4f;
        }
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) {
            angleChange = 4f;
            changedAngle = true;
        }

        if (angleChange != 0f) {
            camera.rotate(angleChange, 0f, 0f, 1f);
            worldAngle += angleChange;
            Vector2 newGravity = new Vector2(0, -10);
            newGravity.rotate(worldAngle);
            this.physicsWorld.setGravity(newGravity);
            pos = this.ballBody.getPosition();
            // Turn the ball so it looks like it hasn't actually turned but the
            // world has turned around it.
            this.ballBody.setTransform(pos, (angle + angleChange) / MathUtils.radiansToDegrees);

            // Wake up the ball since changing gravity doesn't automatically wake it.
            // FIXME: Gonna need to find a better solution for this in the "real thing"
            // since there will be many entities to awaken.
            this.ballBody.setAwake(true);
        }

        camera.translate(cameraPosDelta);
        cameraPos.add(cameraPosDelta);

        physicsWorld.step(1/30f, 6, 2);

    }

    @Override
    public void show() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        camera = new OrthographicCamera(w, h);
        batch = new SpriteBatch();
        ballTexture = new Texture(Gdx.files.internal("ball.png"));
        ballTextureRegion = new TextureRegion(ballTexture, 0, 0, 128, 128);
        debugRenderer = new Box2DDebugRenderer();
        physicsWorld = new World(new Vector2(0, -10), true);

        BodyDef ballBodyDef = new BodyDef();
        ballBodyDef.type = BodyDef.BodyType.DynamicBody;
        ballBodyDef.position.set(0, 20);

        ballBody = physicsWorld.createBody(ballBodyDef);

        CircleShape ballShape = new CircleShape();
        ballShape.setRadius(64f / PIXELS_PER_METER);

        FixtureDef ballFixtureDef = new FixtureDef();
        ballFixtureDef.shape = ballShape;
        ballFixtureDef.density = 0.5f;
        ballFixtureDef.friction = 0.5f;
        ballFixtureDef.restitution = 0.6f;

        Fixture ballFixture = ballBody.createFixture(ballFixtureDef);

        ballShape.dispose();

        BodyDef groundBodyDef = new BodyDef();
        groundBodyDef.position.set(new Vector2(0, 2));

        Body groundBody = physicsWorld.createBody(groundBodyDef);

        ChainShape groundShape = new ChainShape();
        groundShape.createLoop(
            new Vector2[] {
                new Vector2(-12.0f, 2.0f),
                new Vector2(-11.0f, 0.8f),
                new Vector2(-10.0f, 0.1f),
                new Vector2(-9.0f, 0f),
                new Vector2(12.0f, 2.0f),
                new Vector2(12.0f, 30f),
                new Vector2(-12.0f, 30f),
            }
        );

        // Or the ground could be a rectangle.
        //PolygonShape groundShape = new PolygonShape();
        //groundShape.setAsBox(camera.viewportWidth / PIXELS_PER_METER, 2.0f);

        groundBody.createFixture(groundShape, 0.0f);
        groundShape.dispose();


    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void hide() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
    }

}
