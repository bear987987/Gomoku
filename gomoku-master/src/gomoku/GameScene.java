package gomoku;

/**
 * Scene of gomoku game
 */
public enum GameScene {
    Menu("menu"), Start("start"), Room("room"), Game("game");

    private final String value;

    private GameScene(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
};