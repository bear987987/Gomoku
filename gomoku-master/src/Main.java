import javax.swing.UIManager;

import gomoku.Gomoku;

public class Main {
    public static void main(String[] arg) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            Gomoku app = new Gomoku(800, 800);
            app.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}