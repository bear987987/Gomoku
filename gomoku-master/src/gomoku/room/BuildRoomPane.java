package gomoku.room;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import gomoku.Setting;

public class BuildRoomPane extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final JTextField portField = new JTextField(12);

    private static final JTextField nicknameField = new JTextField(12);

    private static final JComboBox<Integer> memberOptions = new JComboBox<>();

    private final JButton buildButton = new JButton("建立");

    private final JButton cancelButton = new JButton("取消");

    private final JButton defaultButton = new JButton("還原默認");

    private static ServerSocket server = null;

    static {
        for (int i = Setting.MIN_MEMBER; i <= Setting.MAX_MEMBER; ++i) {
            memberOptions.addItem(i);
        }
        memberOptions.setSelectedItem(Setting.blackMember + Setting.whiteMember);
        memberOptions.setFont(Setting.PMingLiUFont);
        portField.setText(String.valueOf(Setting.DEFAULT_PORT));
        portField.setFont(Setting.PMingLiUFont);
        nicknameField.setText(Setting.nickname);
        nicknameField.setFont(Setting.PMingLiUFont);
    }

    private BuildRoomPane(Frame parent) {
        super(parent, "建立遊戲房", true);
        // repeat call it will clear last server
        if (server != null) {
            try {
                server.close();
                server = null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        getRootPane().setDefaultButton(buildButton);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setResizable(false);
        setIconImage(parent.getIconImage());
        // 設定主面板
        setContentPane(new JPanel());
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        // 設定暱稱面板
        JPanel nicknamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel nicknameLabel = new JLabel("暱稱(Nickname): ");
        nicknameLabel.setFont(Setting.PMingLiUFont);
        nicknamePanel.add(nicknameLabel);
        nicknamePanel.add(nicknameField);
        // 設定port面板
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel portLabel = new JLabel("端口(Port): ");
        portLabel.setFont(Setting.PMingLiUFont);
        portPanel.add(portLabel);
        portPanel.add(portField);
        // 設定人數面板
        JPanel memberPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel memberLabel = new JLabel("人數(Members): ");
        memberLabel.setFont(Setting.PMingLiUFont);
        memberPanel.add(memberLabel);
        memberPanel.add(memberOptions);
        // 決定面版
        JPanel decidePanel = new JPanel();
        decidePanel.setLayout(new BoxLayout(decidePanel, BoxLayout.X_AXIS));
        buildButton.setFont(Setting.PMingLiUFont);
        buildButton.setFocusable(false);
        cancelButton.setFont(Setting.PMingLiUFont);
        cancelButton.setFocusable(false);
        defaultButton.setFont(Setting.PMingLiUFont);
        defaultButton.setFocusable(false);
        decidePanel.add(buildButton);
        decidePanel.add(Box.createHorizontalStrut(30));
        decidePanel.add(cancelButton);
        decidePanel.add(Box.createHorizontalStrut(30));
        decidePanel.add(defaultButton);
        // 放入主面板
        add(nicknamePanel);
        add(Box.createVerticalStrut(10));
        add(portPanel);
        add(Box.createVerticalStrut(10));
        add(memberPanel);
        add(Box.createVerticalStrut(10));
        add(decidePanel);
        pack();
        setLocationRelativeTo(parent);
        setListener(parent);
    }

    /**
     * Display the pane front of user
     * 
     * @param parent parent of dialog
     * @return socket of server
     * @throws NullPointerException if user canceled building room
     */
    public static ServerSocket showDialog(Frame parent) {
        BuildRoomPane pane = new BuildRoomPane(parent);
        pane.setVisible(true);
        if (server == null)
            throw new NullPointerException();
        return server;
    }

    // 設定接聽功能
    private void setListener(Frame parent) {
        buildButton.addActionListener(e -> {
            String portText = portField.getText();
            String nicknameText = nicknameField.getText();
            // 確認Port輸入為數字
            if (portText.matches("^[0-9]+$")) {
                try {
                    BigDecimal selectedPort = new BigDecimal(portText);
                    // 確認Port輸入正確
                    if (selectedPort.compareTo(new BigDecimal(0)) <= 0
                            || selectedPort.compareTo(new BigDecimal(Setting.MAX_PORT)) > 0) {
                        JOptionPane.showMessageDialog(parent,
                                "Invalid port, need between 1 ~ " + Setting.MAX_PORT + " !");
                        return;
                    }
                    // 確認Port沒被占用
                    if (!isPortAvailable(selectedPort.intValue())) {
                        JOptionPane.showMessageDialog(parent, "Port " + selectedPort + " is occupied!");
                        return;
                    }
                    // 暱稱不可空白
                    if (nicknameText.isBlank() || nicknameText.length() > Setting.MAX_NICKNAME_LENGTH) {
                        JOptionPane.showMessageDialog(parent,
                                "Nickname is empty or bigger than " + Setting.MAX_NICKNAME_LENGTH + "!");
                        return;
                    }
                    Setting.port = selectedPort.intValue();
                    server = new ServerSocket(Setting.port);
                    server.setReuseAddress(true);
                    int total = Integer.class.cast(memberOptions.getSelectedItem());
                    Setting.blackMember = total / 2 + total % 2;
                    Setting.whiteMember = total / 2;
                    Setting.nickname = nicknameText;
                    setVisible(false);
                    return;
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            JOptionPane.showMessageDialog(parent, "Invalid Input!");
        });
        cancelButton.addActionListener(e -> {
            portField.setText(String.valueOf(Setting.port));
            memberOptions.setSelectedItem(Setting.blackMember + Setting.whiteMember);
            nicknameField.setText(Setting.nickname);
            setVisible(false);
        });
        defaultButton.addActionListener(e -> {
            memberOptions.setSelectedItem(Setting.MIN_MEMBER);
            portField.setText(String.valueOf(Setting.DEFAULT_PORT));
            nicknameField.setText(Setting.nickname);
        });
    }

    private static boolean isPortAvailable(final int port) {
        try (ServerSocket ss = new ServerSocket(port); DatagramSocket ds = new DatagramSocket(port);) {
            return true;
        } catch (IOException ioe) {
        }
        return false;
    }
}