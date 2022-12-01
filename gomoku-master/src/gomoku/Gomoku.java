package gomoku;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import gomoku.room.BuildRoomPane;
import gomoku.room.JoinRoomPane;
import gomoku.room.interact.InteractPanel;
import gomoku.room.member.Identity;
import gomoku.room.member.MemberPanel;
import gomoku.room.member.MemberState;
import resource.ImageLoader;
import worker.ClientWorker;
import worker.ServerWorker;

public class Gomoku extends JFrame {

    private static final long serialVersionUID = 0L;

    // card panel
    private static final JPanel mainPanel = new JPanel(new CardLayout());

    private static final JPanel membersPanel = new JPanel(new GridLayout(2, 5, 10, 10));

    private static final JLabel background = new JLabel();

    private static final Map<String, JPanel> cardPanels = new LinkedHashMap<>();

    private static final InteractPanel interactPanel = new InteractPanel();

    private static ServerWorker serverWorker = null;

    private static ClientWorker clientWorker = null;

    private static boolean isGameStart = false;

    private static GameScene currentScene = null;

    static {
        //// 設定主畫面的選單面板
        final JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        Arrays.asList("開始遊戲", "結束").forEach(name -> {
            final JButton button = new JButton(name);
            button.setFocusable(false);
            button.setAlignmentX(JButton.CENTER_ALIGNMENT);
            button.setFont(Setting.PMingLiUFont.deriveFont(60.f));
            menuPanel.add(Box.createVerticalStrut(120));
            menuPanel.add(button);
        });
        //// 設定主畫面的開始遊戲面板
        final JPanel startPanel = new JPanel();
        startPanel.setLayout(new BoxLayout(startPanel, BoxLayout.Y_AXIS));
        startPanel.setOpaque(false);
        Arrays.asList("建立遊戲", "加入遊戲", "返回標題").forEach(name -> {
            final JButton button = new JButton(name);
            button.setFocusable(false);
            button.setAlignmentX(JButton.CENTER_ALIGNMENT);
            button.setFont(Setting.PMingLiUFont.deriveFont(60.f));
            startPanel.add(Box.createVerticalStrut(70));
            startPanel.add(button);
        });
        Arrays.asList(menuPanel, startPanel).forEach(panel -> {
            final JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            final JLabel name = new JLabel("五子棋");
            name.setFont(Setting.PMingLiUFont.deriveFont(120.f).deriveFont(Font.BOLD));
            namePanel.setMaximumSize(name.getPreferredSize());
            namePanel.setOpaque(false);
            namePanel.add(name);
            panel.add(Box.createVerticalStrut(70), 0);
            panel.add(namePanel, 1);
        });
        //// 設定遊戲室的面板
        final JPanel roomPanel = new JPanel();
        roomPanel.setLayout(new BoxLayout(roomPanel, BoxLayout.Y_AXIS));
        roomPanel.setOpaque(false);
        // 遊戲室標題
        final JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        final JLabel name = new JLabel("遊戲等待室");
        name.setFont(Setting.PMingLiUFont.deriveFont(60.f));
        namePanel.setMaximumSize(new Dimension(namePanel.getMaximumSize().width, namePanel.getPreferredSize().height));
        namePanel.setOpaque(false);
        namePanel.add(name);
        // 成員面板
        membersPanel.setOpaque(false);
        membersPanel.setPreferredSize(new Dimension(membersPanel.getMaximumSize().width, 400));
        // 互動面板
        roomPanel.add(namePanel);
        roomPanel.add(membersPanel);
        roomPanel.add(interactPanel);
        //// 加入Map
        cardPanels.put("menu", menuPanel);
        cardPanels.put("start", startPanel);
        cardPanels.put("room", roomPanel);
        //// 放入主面板
        for (Entry<String, JPanel> entry : cardPanels.entrySet()) {
            mainPanel.add(entry.getValue(), entry.getKey());
        }
    }

    public Gomoku(int width, int height) {
        super("Gomoku");
        setDefaultCloseOperation(Gomoku.DO_NOTHING_ON_CLOSE);
        // 設定背景面板
        setContentPane(background);
        setPreferredSize(new Dimension(width, height));
        setResizable(false);
        try {
            setIconImage(ImageLoader.loadImage("gomoku_icon", null));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        setLayout(new BorderLayout());
        mainPanel.setOpaque(false);
        add(mainPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);
        //// 設定主面板事件
        addPropertyChangeListener("panelChange", e -> {
            GameScene scene = (GameScene) e.getNewValue();
            CardLayout.class.cast(mainPanel.getLayout()).show(mainPanel, scene.toString());
            switch (scene) {
            case Menu:
            case Start:
                setBackground("menu");
                break;
            case Room:
            case Game:
                setBackground(scene.toString());
                break;
            default:
                throw new IllegalArgumentException("Unknown background!");
            }
        });
        changeScene(GameScene.Menu);
        //// 設定事件
        interactPanel.addListener(this);
        // 設定選單事件
        setPanelButtonActionListener(cardPanels.get("menu"), Map.ofEntries(Map.entry("開始遊戲", e -> {
            changeScene(GameScene.Start);
        }), Map.entry("結束", e -> {
            dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING, null));
        })));
        // 設定伺服器事件
        setPanelButtonActionListener(cardPanels.get("start"), Map.ofEntries(Map.entry("建立遊戲", e -> {
            try {
                shutdown();
                interactPanel.switchInteractIdentity(Identity.Director);
                serverWorker = new ServerWorker(BuildRoomPane.showDialog(this), this);
                clientWorker = new ClientWorker(new Socket(Inet4Address.getLoopbackAddress(), Setting.port), this);
                serverWorker.execute();
                clientWorker.execute();
            } catch (NullPointerException nrp) {
                JOptionPane.showMessageDialog(this, "Canceled!", "Message", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }), Map.entry("加入遊戲", e -> {
            try {
                interactPanel.switchInteractIdentity(Identity.Member);
                clientWorker = new ClientWorker(JoinRoomPane.showDialog(this), this);
                clientWorker.execute();
            } catch (NullPointerException nrp) {
                JOptionPane.showMessageDialog(this, "Canceled!", "Message", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }), Map.entry("返回標題", e -> {
            changeScene(GameScene.Menu);
        })));
        // 視窗關閉時
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int choosed = JOptionPane.showConfirmDialog(Gomoku.this, "確定要結束?", "Exit", JOptionPane.YES_NO_OPTION);
                if (choosed == JOptionPane.YES_OPTION) {
                    shutdown();
                    System.exit(0);
                }
            }
        });
    }

    /**
     * Build empty member pane in room
     * 
     * @throws IllegalAccessError when this function call with non {@code Director}
     *                            id
     */
    public void buildMemberPane() {
        if (Setting.id != Identity.Director)
            throw new IllegalAccessError("Build empty with " + Setting.id + "!");
        membersPanel.removeAll();
        for (int i = 0; i < Setting.MAX_MEMBER; ++i) {
            int halfDiff = i - Setting.MAX_MEMBER / 2;
            final Color color = halfDiff < 0 ? Color.BLACK : Color.WHITE;
            if (i < Setting.blackMember || halfDiff >= 0 && halfDiff < Setting.whiteMember)
                membersPanel.add(new MemberPanel(color, false, this));
            else
                membersPanel.add(new MemberPanel(color, true, this));
        }
        pullMemberIn();
    }

    /**
     * Build member pane in room, it will remove all the previous children first
     * 
     * @param members list of all members
     * @throws IllegalAccessError when this function call with non {@code Director}
     *                            id
     */
    public void buildMemberPane(Map<Integer, List<String>> members) {
        if (Setting.id == Identity.Director)
            throw new IllegalAccessError("Copy member to existed " + Setting.id + "!");
        membersPanel.removeAll();
        for (Entry<Integer, List<String>> entry : members.entrySet()) {
            final List<String> list = entry.getValue();
            final int halfDiff = entry.getKey() - Setting.MAX_MEMBER / 2;
            final Color color = halfDiff < 0 ? Color.BLACK : Color.WHITE;
            for (Identity id : Identity.values()) {
                if (id.toString().equals(list.get(1))) {
                    MemberPanel member = new MemberPanel(list.get(0), id, color, this);
                    member.setState(list.get(2));
                    membersPanel.add(member);
                    break;
                }
            }
        }
        pullMemberIn();
    }

    /**
     * Change the scene
     * 
     * @param scene
     */
    public void changeScene(GameScene scene) {
        if (currentScene == scene)
            return;
        firePropertyChange("panelChange", currentScene, scene);
        currentScene = scene;
    }

    /**
     * Start the game
     */
    public void gameStart() {
        isGameStart = true;
    }

    /**
     * Finish the game
     */
    public void gameFinish() {
        isGameStart = false;
    }

    /**
     * Shutdown the worker
     */
    public void shutdown() {
        if (clientWorker != null && !clientWorker.isDone())
            clientWorker.shutdown();
        if (serverWorker != null && !serverWorker.isDone())
            serverWorker.shutdown();
    }

    /**
     * Update {@code MemberState}
     * 
     * @param isAnyDisconnect is any player disconnected
     */
    public void updateState(boolean isAnyDisconnect) {
        if (Setting.id != Identity.Director)
            return;
        final boolean isAllReady = getMemberPanels(MemberState.Ready).length == getMemberPanels(Identity.Member).length;
        if (isAllReady && !isAnyDisconnect) {
            interactPanel.setAllMemberReady(true);
            return;
        }
        if (interactPanel.isAllMemberReady()) {
            final MemberPanel director = getMemberPanel(Setting.nickname);
            final MemberState previousState = director.getState();
            director.setState(MemberState.Preparing);
            director.fireStateChange(previousState, MemberState.Preparing);
            interactPanel.setAllMemberReady(false);
        }
        interactPanel.setAllMemberReady(isAllReady);
    }

    /**
     * @return {@code InteractPanel}
     */
    public InteractPanel getInteractPanel() {
        return interactPanel;
    }

    /**
     * @param <T>
     * @param filters
     * @return array of {@code MemberPanel}
     */
    @SafeVarargs
    public final <T extends Enum<T>> MemberPanel[] getMemberPanels(T... filters) {
        final List<T> lists = Arrays.asList(filters);
        final Class<?> type = filters.getClass().getComponentType();
        final Stream<Component> comps = Stream.of(membersPanel.getComponents());
        return filters.length != 0 ? comps.filter(element -> {
            final MemberPanel member = MemberPanel.class.cast(element);
            if (type.equals(MemberState.class))
                return lists.contains(member.getState());
            else if (type.equals(Identity.class))
                return lists.contains(member.getId());
            return false;
        }).toArray(MemberPanel[]::new) : comps.toArray(MemberPanel[]::new);
    }

    /**
     * Use nickname to find member
     * 
     * @param nickname nickname of {@code MemberPanel}
     * @return {@code MemberPanel} of member, null if no found
     */
    public MemberPanel getMemberPanel(String nickname) {
        for (MemberPanel member : getMemberPanels())
            if (member.getNickname().equals(nickname))
                return member;
        return null;
    }

    /**
     * Use nickname to find index
     * 
     * @param nickname nickname of {@code MemberPanel}
     * @return index of this member in member panel, -1 if no found
     */
    public int getMemberPanelIndex(String nickname) {
        MemberPanel[] members = getMemberPanels();
        for (int i = 0; i < getMemberPanels().length; ++i)
            if (members[i].getNickname().equals(nickname))
                return i;
        return -1;
    }

    /**
     * Use member to find index
     * 
     * @param member {@code MemberPanel}
     * @return index of this member in member panel, -1 if no found
     */
    public int getMemberPanelIndex(MemberPanel member) {
        MemberPanel[] members = getMemberPanels();
        for (int i = 0; i < getMemberPanels().length; ++i)
            if (members[i] == member)
                return i;
        return -1;
    }

    /**
     * @return game started or not
     */
    public boolean isGameStart() {
        return isGameStart;
    }

    private void setBackground(String bgName) {
        try {
            background.setIcon(new ImageIcon(ImageLoader.loadImage(bgName, getPreferredSize())));
            repaint();
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
    }

    private void setPanelButtonActionListener(JPanel panel, Map<String, ActionListener> eMap) {
        for (Component comp : panel.getComponents()) {
            if (!(comp instanceof AbstractButton))
                continue;
            final AbstractButton button = AbstractButton.class.cast(comp);
            button.addActionListener(eMap.get(button.getText()));
        }
    }

    private void pullMemberIn() {
        final MemberPanel[] array = getMemberPanels();
        int targetIndex = -1;
        for (int i = 0; i < 5; ++i) {
            if (Identity.None.toString().equals(array[i].getId().toString())) {
                targetIndex = i;
                break;
            } else if (Identity.None.toString().equals(array[i + 5].getId().toString())) {
                targetIndex = i + 5;
                break;
            }
        }
        final MemberPanel member = getMemberPanels()[targetIndex];
        member.setMember(Setting.nickname, Setting.id);
        member.setNicknameColor(Color.GREEN);
    }
}