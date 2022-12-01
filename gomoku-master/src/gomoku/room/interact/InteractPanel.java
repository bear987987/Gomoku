package gomoku.room.interact;

import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import gomoku.GameScene;
import gomoku.Gomoku;
import gomoku.Setting;
import gomoku.room.member.Identity;
import gomoku.room.member.MemberPanel;
import gomoku.room.member.MemberState;

public class InteractPanel extends JPanel {

    private static final long serialVersionUID = 11L;

    private final JPanel decidePanel = new JPanel();

    private final JButton stateChangeButton = new JButton("準備");

    private final JButton quitButton = new JButton("離開房間");

    private final ChatPanel chatPanel = new ChatPanel();

    private boolean isAllMemberReady = false;

    public InteractPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        decidePanel.setLayout(new BoxLayout(decidePanel, BoxLayout.Y_AXIS));
        decidePanel.setOpaque(false);
        stateChangeButton.setMaximumSize(new Dimension(150, 60));
        stateChangeButton.setFont(Setting.PMingLiUFont);
        stateChangeButton.setFocusable(false);
        quitButton.setMaximumSize(new Dimension(150, 60));
        quitButton.setFont(Setting.PMingLiUFont);
        quitButton.setFocusable(false);
        decidePanel.add(stateChangeButton);
        decidePanel.add(Box.createVerticalStrut(20));
        decidePanel.add(quitButton);
        add(chatPanel);
        add(decidePanel);
        chatPanel.setPreferredSize(chatPanel.getSize());
    }

    /**
     * Add listener to {@code InteractPanel}
     * 
     * @param parent
     */
    public void addListener(final Gomoku parent) {
        stateChangeButton.addActionListener(e -> {
            final MemberPanel member = parent.getMemberPanel(Setting.nickname);
            final MemberState previousState = member.getState();
            final String text = stateChangeButton.getText();
            if (text.equals("準備")) {
                stateChangeButton.setText("取消準備");
                member.setState(MemberState.Ready);
            } else if (text.equals("開始")) {
                final int choosed = JOptionPane.showConfirmDialog(parent, "確定開始?", "Confirm",
                        JOptionPane.YES_NO_OPTION);
                if (choosed != JOptionPane.YES_OPTION)
                    return;
                stateChangeButton.setText("等待開始");
                stateChangeButton.setEnabled(false);
                member.setState(MemberState.Start);
            } else {
                stateChangeButton.setText("準備");
                member.setState(MemberState.Preparing);
            }
            member.fireStateChange(previousState, member.getState());
        });
        quitButton.addActionListener(e -> {
            final int choosed = JOptionPane.showConfirmDialog(parent, "確定要" + quitButton.getText() + "?", "Exit room",
                    JOptionPane.YES_NO_OPTION);
            if (choosed == JOptionPane.YES_OPTION) {
                parent.shutdown();
                parent.changeScene(GameScene.Start);
            }
        });
    }

    /**
     * Switch and set {@code Identity} in interact panel
     * 
     * @param newId new {@code Identity}
     */
    public void switchInteractIdentity(final Identity newId) {
        if (Setting.id == newId)
            return;
        switch (newId) {
        case Director:
            stateChangeButton.setText("開始");
            stateChangeButton.setEnabled(false);
            quitButton.setText("關閉房間");
            break;
        default:
            stateChangeButton.setText("準備");
            stateChangeButton.setEnabled(true);
            quitButton.setText("離開房間");
            break;
        }
        Setting.id = newId;
    }

    /**
     * Set all members is ready or not
     * 
     * @param isAllReady
     */
    public void setAllMemberReady(boolean isAllReady) {
        if (isAllReady == isAllMemberReady)
            return;
        if (Setting.id == Identity.Director) {
            stateChangeButton.setEnabled(isAllReady);
            if (!isAllReady)
                stateChangeButton.setText("開始");
        } else if (Setting.id == Identity.Member) {
            System.out.println(123);
        }
        isAllMemberReady = isAllReady;
    }

    /**
     * @return is all ready or not
     */
    public boolean isAllMemberReady() {
        return isAllMemberReady;
    }

    /**
     * @return {@code ChatPanel}
     */
    public ChatPanel getChatPanel() {
        return chatPanel;
    }
}