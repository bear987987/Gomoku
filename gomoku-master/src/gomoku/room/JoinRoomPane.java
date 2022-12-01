package gomoku.room;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import gomoku.Setting;
import state.ConnectState;

public class JoinRoomPane extends JDialog {

    private static final long serialVersionUID = 2L;

    private static final JTextField ipField = new JTextField(12);

    private static final JTextField portField = new JTextField(12);

    private static final JTextField nicknameField = new JTextField(12);

    private static Socket socket = null;

    private final JButton joinButton = new JButton("加入");

    private final JButton cancelButton = new JButton("取消");

    private final JProgressBar progressBar = new JProgressBar();

    private final JButton stopButton = new JButton("中止");

    private SwingWorker<Void, Void> proxyWorker;

    static {
        ipField.setText(Setting.DEFAULT_IP);
        ipField.setFont(Setting.PMingLiUFont);
        portField.setText(String.valueOf(Setting.DEFAULT_PORT));
        portField.setFont(Setting.PMingLiUFont);
        nicknameField.setText(Setting.nickname);
        nicknameField.setFont(Setting.PMingLiUFont);
    }

    private JoinRoomPane(Frame parent) {
        super(parent, "加入遊戲房", true);
        if (socket != null) {
            try {
                socket.close();
                socket = null;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        getRootPane().setDefaultButton(joinButton);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
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
        // 設定IP面板
        JPanel ipPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel ipLabel = new JLabel("伺服器IP(Server IP): ");
        ipLabel.setFont(Setting.PMingLiUFont);
        ipPanel.add(ipLabel);
        ipPanel.add(ipField);
        // 設定port面板
        JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel portLabel = new JLabel("端口(Port): ");
        portLabel.setFont(Setting.PMingLiUFont);
        portPanel.add(portLabel);
        portPanel.add(portField);
        // 決定面版
        JPanel decidePanel = new JPanel();
        decidePanel.setLayout(new BoxLayout(decidePanel, BoxLayout.X_AXIS));
        joinButton.setFont(Setting.PMingLiUFont);
        joinButton.setFocusable(false);
        cancelButton.setFont(Setting.PMingLiUFont);
        cancelButton.setFocusable(false);
        decidePanel.add(joinButton);
        decidePanel.add(Box.createHorizontalStrut(30));
        decidePanel.add(cancelButton);
        // 進度面版
        JPanel progressPanel = new JPanel();
        progressPanel.setLayout(new BoxLayout(progressPanel, BoxLayout.X_AXIS));
        progressBar.setBorder(BorderFactory.createLoweredBevelBorder());
        progressBar.setStringPainted(true);
        progressBar.setString("Initialize");
        progressBar.setFont(Setting.PMingLiUFont.deriveFont(15.f).deriveFont(Font.BOLD));
        stopButton.setFont(Setting.PMingLiUFont.deriveFont(15.f));
        stopButton.setFocusable(false);
        progressPanel.add(progressBar);
        progressPanel.add(stopButton);
        progressPanel.setVisible(false);
        // 放入主面板
        add(nicknamePanel);
        add(Box.createVerticalStrut(10));
        add(ipPanel);
        add(Box.createVerticalStrut(10));
        add(portPanel);
        add(Box.createVerticalStrut(10));
        add(decidePanel);
        add(Box.createVerticalStrut(10));
        add(progressPanel);
        pack();
        setLocationRelativeTo(parent);
        setListener(parent);
    }

    /**
     * Display the join pane front of user
     * 
     * @param parent parent of dialog
     * @return socket of client
     * @throws NullPointerException if user canceled joining room
     */
    public static Socket showDialog(Frame parent) {
        JoinRoomPane pane = new JoinRoomPane(parent);
        pane.setVisible(true);
        if (socket == null)
            throw new NullPointerException();
        return socket;
    }

    // 設定接聽功能
    private void setListener(Frame parent) {
        joinButton.addActionListener(e -> {
            String portText = portField.getText();
            String ipText = ipField.getText();
            String nicknameText = nicknameField.getText();
            // 確認Port輸入為數字
            if (portText.matches("^[0-9]+$")) {
                try {
                    BigDecimal selectedPort = new BigDecimal(portText);
                    // 確認IP格式正確
                    if (!isIPAvailable(ipText)) {
                        JOptionPane.showMessageDialog(parent, "Invalid IP " + ipText + " !(Please enter IPv4 format)");
                        return;
                    }
                    // 確認Port輸入正確
                    if (selectedPort.compareTo(new BigDecimal(0)) <= 0
                            || selectedPort.compareTo(new BigDecimal(Setting.MAX_PORT)) > 0) {
                        JOptionPane.showMessageDialog(parent, "Invalid port, need in 1 ~ " + Setting.MAX_PORT + " !");
                        return;
                    }
                    // 暱稱不可空白
                    if (nicknameText.isBlank() || nicknameText.length() > Setting.MAX_NICKNAME_LENGTH) {
                        JOptionPane.showMessageDialog(parent,
                                "Nickname is empty or bigger than " + Setting.MAX_NICKNAME_LENGTH + "!");
                        return;
                    }
                    Setting.port = selectedPort.intValue();
                    Setting.serverIP = ipText;
                    Setting.nickname = nicknameText;
                    proxyWorker = new SwingWorker<Void, Void>() {

                        private int count = 0;

                        @Override
                        protected Void doInBackground() throws Exception {
                            joinButton.setEnabled(false);
                            cancelButton.setEnabled(false);
                            progressBar.getParent().setVisible(true);
                            pack();
                            for (int i = 0; i <= 60; ++i) {
                                progressBar.setValue(i);
                                if (i < 10)
                                    Thread.sleep(50);
                                else if (i < 20)
                                    Thread.sleep(100);
                                else
                                    Thread.sleep(20);
                            }
                            while (!isDone()) {
                                try {
                                    socket = new Socket(Setting.serverIP, Setting.port);
                                    for (int i = 60; i <= 100; ++i) {
                                        progressBar.setValue(i);
                                        Thread.sleep(10);
                                        if (i == 100)
                                            Thread.sleep(500);
                                    }
                                    return null;
                                } catch (ConnectException ce) {
                                    final String str = progressBar.getString();
                                    int index = str.indexOf('.');
                                    progressBar.setString((index == -1 ? str.substring(0) : str.substring(0, index))
                                            + ".".repeat(count++ % 4));
                                }
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            progressBar.getParent().setVisible(false);
                            joinButton.setEnabled(true);
                            cancelButton.setEnabled(true);
                            pack();
                            try {
                                final BufferedWriter bw = new BufferedWriter(
                                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                                if (!isCancelled())
                                    setVisible(false);
                                else {
                                    bw.write(ConnectState.CLIENT_TERMINATED_INTERRUPT);
                                    // send initial nickname, id and uuid
                                    bw.write(Setting.nickname + "\r\n" + Setting.id + "\r\n" + Setting.uniqueID
                                            + "\r\n");
                                    bw.flush();
                                }
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                            }
                        }
                    };
                    proxyWorker.execute();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return;
            }
            JOptionPane.showMessageDialog(parent, "Invalid Input!");
        });
        cancelButton.addActionListener(e -> {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING, null));
        });
        stopButton.addActionListener(e -> {
            proxyWorker.cancel(true);
            try {
                socket.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            socket = null;
            JOptionPane.showMessageDialog(JoinRoomPane.this, "Canceled connect!", "Message",
                    JOptionPane.INFORMATION_MESSAGE);
        });
        progressBar.addChangeListener(new ChangeListener() {

            private String currentState = "";

            private long previousTime = System.currentTimeMillis();

            private int count = 0;

            @Override
            public void stateChanged(ChangeEvent e) {
                final JProgressBar comp = (JProgressBar) e.getSource();
                final int value = comp.getValue();
                switch (value) {
                case 0:
                case 1:
                    progressBar.setString(currentState = "Initialize");
                    break;
                case 10:
                    progressBar.setString(currentState = "Building local socket");
                    break;
                case 30:
                    progressBar.setString(currentState = "Find room IP(" + Setting.serverIP + ":" + Setting.port + ")");
                    break;
                case 60:
                    progressBar.setString(currentState = "Requesting room for connect");
                    break;
                case 100:
                    progressBar.setString(currentState = "Done!");
                    return;
                default:
                    break;
                }
                final long currentTime = System.currentTimeMillis();
                if (currentTime - previousTime >= 200) {
                    progressBar.setString(currentState + ".".repeat(count++ % 4));
                    previousTime = currentTime;
                }
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!cancelButton.isEnabled())
                    return;
                ipField.setText(Setting.serverIP);
                portField.setText(String.valueOf(Setting.port));
                nicknameField.setText(Setting.nickname);
                setVisible(false);
            }
        });
    }

    private static boolean isIPAvailable(final String ip) {
        return Setting.IP_PATTERN.matcher(ip).matches();
    }
}