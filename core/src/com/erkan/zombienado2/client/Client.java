package com.erkan.zombienado2.client;

import box2dLight.ConeLight;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.erkan.zombienado2.client.world.BrokenLamp;
import com.erkan.zombienado2.data.world.Map;
import com.erkan.zombienado2.client.world.Structure;
import com.erkan.zombienado2.graphics.Transform;
import com.erkan.zombienado2.client.networking.ConnectionListener;
import com.erkan.zombienado2.client.networking.ServerProxy;
import com.erkan.zombienado2.data.weapons.WeaponData;
import com.erkan.zombienado2.networking.ServerHeaders;
import com.badlogic.gdx.graphics.Texture;

import java.util.*;
import java.util.ArrayList;

import static com.erkan.zombienado2.graphics.Transform.*;

public class Client extends ApplicationAdapter implements ConnectionListener {
	Texture ground;
	ConeLight test1;
	ConeLight test2;
	BrokenLamp bl;
	List<Structure> structures = new ArrayList();
	Sound music;

	List<Zombie> zombies = new ArrayList<>();
	int current_wave = 0;


	BitmapFont font;
	SpriteBatch batch;
	SpriteBatch batch_hud;

	Box2DDebugRenderer dDebugRenderer;


	static Vector3 camera_world_coordinates = new Vector3(); //used for sound calculations ://// not nice

	OrthographicCamera camera;
	Self self;
	int my_id;

	float zoom = 1f;
	float zoom_to = 1f;

	TeamMate[] teamMates;

	Vector2[][] bullets = new Vector2[0][0];

	public Client(final String IP, final int PORT){
		ServerProxy.addListener(this);
		ServerProxy.connect(IP, PORT);
	}

	@Override
	public void create () {
		Weapon.init();
 		ground = new Texture("misc/test_ground.png");
 		ground.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

 		Zombie.init();
		PhysicsHandler.init();
		dDebugRenderer = new Box2DDebugRenderer();
		font = new BitmapFont();
		self = new Self("n00b", Character.OFFICER);
		zoom_to = self.getWeapon().getWeaponData().scope;
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		//ServerProxy.join("n00b", Character.BUSINESS.toString());
		batch = new SpriteBatch();
		batch_hud = new SpriteBatch();

		test1 = PhysicsHandler.createConeLight(to_screen_space(18.7f), to_screen_space(5.9f), new Color(1,0,0,1), 200, testAngle, 40);
		test2 = PhysicsHandler.createConeLight(to_screen_space(18.7f), to_screen_space(5.9f), new Color(0,0,1,1), 200, testAngle + 180, 40);
		bl = new BrokenLamp(15, -5, 95);
		music = Gdx.audio.newSound(Gdx.files.internal("audio/music.mp3"));
		music.loop();
		music.play(.01f);

		Map.TEST_MAP.getStructures().stream().forEach(structure -> {
			PhysicsHandler.createPrefab(structure.getFirst());
			structures.add(new Structure(structure.getFirst(), structure.getSecond()));
		});
	}

	float testAngle = 0;

	@Override
	public synchronized void render () {
		//TEST
		testAngle+=10;
		test1.setDirection(testAngle);
		test2.setDirection(testAngle + 180);
		bl.update(Gdx.graphics.getDeltaTime());

		Zombie.static_update();

		if (zoom < zoom_to){
			float delta = zoom_to - zoom;
			zoom += Math.min(delta/10, 0.1f);

			if (zoom > zoom_to)
				zoom = zoom_to;
		} else if (zoom > zoom_to){
			float delta = zoom - zoom_to;
			zoom -= Math.min(delta/10, 0.1f);

			if (zoom < zoom_to)
				zoom = zoom_to;
		}

		camera.zoom = zoom;

		Vector2 movementvector = new Vector2(0,0);
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			movementvector.x--;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			movementvector.x++;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			movementvector.y++;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			movementvector.y--;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.R)) {
			ServerProxy.reload();
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
			ServerProxy.switch_weapon(WeaponData.PISTOL);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
			ServerProxy.switch_weapon(WeaponData.UZI);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_3)) {
			ServerProxy.switch_weapon(WeaponData.AK47);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_4)) {
			ServerProxy.switch_weapon(WeaponData.ASSAULT_RIFLE);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_5)) {
			ServerProxy.switch_weapon(WeaponData.SHOTGUN_PUMP);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_6)) {
			ServerProxy.switch_weapon(WeaponData.SHOTGUN_AUTO);
		}
		if (Gdx.input.isKeyPressed(Input.Keys.NUM_7)) {
			ServerProxy.switch_weapon(WeaponData.SNIPER);
		}

		float mouse_dx = Gdx.input.getX() - Gdx.graphics.getWidth()/2;
		float mouse_dy = -Gdx.input.getY() + Gdx.graphics.getHeight()/2 ;
		float rot = MathUtils.radiansToDegrees * MathUtils.atan2(mouse_dy, mouse_dx);
		self.distance_to_focus = MathUtils.clamp(new Vector2(mouse_dx, mouse_dy).len()/4 + 300, to_screen_space(4), to_screen_space(10));
		self.rotation = rot;
		self.run(movementvector.cpy());

		ServerProxy.move(movementvector);
		ServerProxy.rotate(self.rotation);

		if (Gdx.input.isButtonPressed(Input.Buttons.LEFT))
			ServerProxy.fire();

		// render
		camera.position.x = to_screen_space(self.position.x) + mouse_dx/2;
		camera.position.y = to_screen_space(self.position.y) + mouse_dy/2;
		camera.update();
		camera_world_coordinates.x = Transform.scale_to_world(camera.position.x);
		camera_world_coordinates.y = Transform.scale_to_world(camera.position.y);

		batch.setProjectionMatrix(camera.combined);
		Gdx.gl.glClearColor(.3f, .3f, .3f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));
		batch.begin();
		for (int i = 0; i < 50; i++){
			for (int j = 0; j < 50; j++){
				batch.draw(ground, (i-25)*500, (j-25)*500, 500, 500);
			}
		}

		structures.stream().forEach(structure -> structure.render_floor(batch));

		zombies.stream().forEach(zombie -> zombie.render_gore(batch));
		zombies.stream().forEach(zombie -> zombie.render(batch));

		if (teamMates != null) {
			for (int i = 0; i < teamMates.length; i++) {
				TeamMate tm = teamMates[i];
				if (tm != null)
					tm.render(batch);
			}
		}

		self.render(batch);


		structures.stream().forEach(structure -> structure.render_walls(batch));
		batch.end();
		PhysicsHandler.update();
		PhysicsHandler.getRayHandler().setCombinedMatrix(camera);
		PhysicsHandler.getRayHandler().updateAndRender();
		batch.begin();
		for (Vector2[] bullets:
				bullets) {
			Sprite b_sprite = new Sprite(Bullet.texture);
			float b_rot = new Vector2(bullets[1].x, bullets[1].y).setLength(1).angle() - 90;
			b_sprite.setRotation(b_rot);

			b_sprite.setCenter(to_screen_space(bullets[0].x) - Bullet.texture.getHeight() * MathUtils.cosDeg(b_rot + 90) / 2, to_screen_space(bullets[0].y)- Bullet.texture.getHeight() * MathUtils.sinDeg(b_rot + 90) / 2);
			//b_sprite.setOrigin(0, 0);
			b_sprite.draw(batch);
		}

		bl.render(batch);
		batch.end();



		//dDebugRenderer.render(PhysicsHandler.getWorld(), camera.combined);

		batch_hud.begin();

		font.draw(batch_hud, "Position: " + (int)(self.position.x * 10) / 10f + ", " + (int)(self.position.y * 10) / 10f, 5, Gdx.graphics.getHeight() - 5);
		font.draw(batch_hud, "Wave: " + current_wave, 5, Gdx.graphics.getHeight() - 20);
		batch_hud.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		//img.dispose();
	}

	@Override
	public synchronized void onMsgReceived(String... args) {

		//System.out.println(args[0]);
		switch (args[0]){
			case ServerHeaders.CONNECT:
				ServerProxy.join("n00b", Character.BUSINESS.toString());
				break;
			case ServerHeaders.RECONNECT:
				//?
				break;
			case ServerHeaders.JOIN_SELF:
				my_id = Integer.parseInt(args[1]);
				System.out.println("My id is: "+my_id);
				teamMates = new TeamMate[Integer.parseInt(args[2])];
				break;
			case ServerHeaders.JOIN_PLAYER:
			{
				int id = Integer.parseInt(args[1]);
				if (id != my_id)
					teamMates[id] = new TeamMate(args[2], Character.getCharacter(args[3])); //TODO: fix
				break;
			}
			case ServerHeaders.UPDATE_PLAYER: {
				int id = Integer.parseInt(args[1]);
				if (id == my_id) {
					self.position.x = Float.parseFloat(args[2]);
					self.position.y = Float.parseFloat(args[3]);
					if (!args[5].equals(self.getWeapon().getWeaponData().toString())){
						WeaponData wd = WeaponData.getWeapon(args[5]);
						self.setWeapon(WeaponData.getWeapon(args[5]));
						zoom_to = wd.scope;
					}
					return;
				}
				teamMates[id].position.x = Float.parseFloat(args[2]);
				teamMates[id].position.y = Float.parseFloat(args[3]);
				teamMates[id].rotation = Float.parseFloat(args[4]);

				if (!args[5].equals(teamMates[id].getWeapon().getWeaponData().toString())){
					WeaponData wd = WeaponData.getWeapon(args[5]);
					teamMates[id].setWeapon(wd);
				}

				break;
			}
			case ServerHeaders.PLAYER_RELOAD:
				if (Integer.parseInt(args[1]) == my_id)
					self.reload();
				else
					teamMates[Integer.parseInt(args[1])].reload();
				break;
			case ServerHeaders.CREATE_BULLET:
				if (Integer.parseInt(args[1]) == my_id)
					self.shoot();
				else
					teamMates[Integer.parseInt(args[1])].shoot();
				// will be used for view stuff
				break;
			case ServerHeaders.UPDATE_BULLETS:
				bullets = new Vector2[(args.length - 1) / 4][2];
				//System.out.println(bullets.length);
				for (int i = 0; i < bullets.length; i++){
					bullets[i][0] = new Vector2(Float.parseFloat(args[4 *i + 1]), Float.parseFloat(args[4 *i + 2]));
					bullets[i][1] = new Vector2(Float.parseFloat(args[4 *i + 3]), Float.parseFloat(args[4 *i + 4]));
				}
				break;
			case ServerHeaders.UPDATE_ZOMBIES:
					for (int i = 0; i < (args.length - 1) / 6; i++) {
						if (zombies.size() <= i) {
							zombies.add(new Zombie(Float.parseFloat(args[6 * i + 4])));
						}

						if (zombies.get(i).isAlive()) {
							zombies.get(i).setPosition(Float.parseFloat(args[6 * i + 1]), Float.parseFloat(args[6 * i + 2]));
							zombies.get(i).setRotation(Float.parseFloat(args[6 * i + 3]));
							zombies.get(i).setHealth(Float.parseFloat(args[6 * i + 4]));
							com.erkan.zombienado2.server.Zombie.Behavior b = args[5 * i + 6].equals("Roaming") ? com.erkan.zombienado2.server.Zombie.Behavior.Roaming : (args[5 * i + 6].equals("Hunting") ? com.erkan.zombienado2.server.Zombie.Behavior.Hunting : com.erkan.zombienado2.server.Zombie.Behavior.Standing);
							zombies.get(i).setBehavior(b);
							//System.out.println(args[6 * i + 6]);
							if (Boolean.parseBoolean(args[6 * i + 5])){
								zombies.get(i).attack();
							}
						}
					}
				break;
			case ServerHeaders.WAVE_END:
				System.out.println("Wave ended");
				Zombie.fade();
				break;
			case ServerHeaders.WAVE_START:
				System.out.println("Wave started");
				zombies.stream().forEach(zombie -> zombie.destroy()); //Potential problem
				zombies = new ArrayList<>();
				Zombie.reset_fade();
				current_wave = Integer.parseInt(args[1]);
				break;
		}
	}
}