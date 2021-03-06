package labyrinth3D.engine;


import java.awt.Graphics2D;

import labyrinth3D.gamestates.GameStateIntro;
import labyrinth3D.gamestates.GameStateIntroScene;
import labyrinth3D.gamestates.GameStateIsland;
import labyrinth3D.gamestates.GameStateMaze3D;
import labyrinth3D.gamestates.GameStateMenu;
import labyrinth3D.gamestates.GameStateSmithy;

public class GameStateHandler {

	public int currentGameState;

	public static final int MENU = 0;
	public static final int GAME = 1;
	public static final int NAMING = 2;
	public static final int CREATION = 3;
	public static final int ISLAND = 4;
	public static final int MAZE_10 = 5;
	public static final int MAZE_20 = 6;
	public static final int MAZE_50 = 7;
	public static final int SMITHY = 8;
	public static final int CRASH = 9;
	public static final int INTRO = 10;

	public static final GameState[] states = new GameState[20];

	public GameStateHandler() {
		currentGameState = MENU;

		loadState(MENU);
	}

	public int getCurrentGameState() {
		return currentGameState;
	}


	public void draw(Graphics2D g) {
		states[currentGameState].draw(g);
	}

	public void update() {
		states[currentGameState].update();
	}

	public void changeGameState(int state) {
		unloadState(currentGameState);
		currentGameState = state;
		loadState(state);
	}

	private void unloadState(int state){
		states[state] = null;
	}

	private void loadState(int state){
		switch(state){
		case MENU:
			states[state] = new GameStateMenu(this);
			break;
		case MAZE_10:
			states[state] = new GameStateMaze3D(this, 10, 3);
			break;
		case ISLAND:
			states[state] = new GameStateIsland(this);
			break;
		case SMITHY:
			states[state] = new GameStateSmithy(this);
			break;
		case CRASH:
			states[state] = new GameStateIntro(this);
			break;
		case INTRO:
			states[state] = new GameStateIntroScene(this);
			break;
		}
	}
}
