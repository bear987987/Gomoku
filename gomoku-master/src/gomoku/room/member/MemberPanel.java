package gomoku.room.member;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.io.FileNotFoundException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import gomoku.Setting;
import resource.ImageLoader;

public class MemberPanel extends JPanel {

    private static final long serialVersionUID = 10L;

    private final int MEMBER_LABEL_HEIGHT = 20;

    private final JButton restrictButton = new JButton("close");

    private final JButton addButton = new JButton("+");

    private final JButton switchButton = new JButton();

    private final JButton banButton = new JButton("x");

    private final JPanel nicknamePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    // color for faction
    private final JPanel factionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    // ready or preparing
    private final JPanel statePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

    private final JLabel idLabel;

    private MemberState state = null;

    private Identity id = null;

    /**
     * Create empty member panel
     * 
     * @param pieceColor {@code Color.BLACK} or {@code Color.WHITE}
     * @param isRestrict is be restrict or not
     * @param parent     parent frame
     */
    public MemberPanel(Color pieceColor, boolean isRestrict, Frame parent) {
        this("", isRestrict ? Identity.Restrict : Identity.None, pieceColor, parent);
    }

    /**
     * Create member panel with nickname
     * 
     * @param nickname   nickname of user
     * @param id         {@link Identity}
     * @param pieceColor {@code Color.BLACK} or {@code Color.WHITE}
     * @param parent     parent frame
     */
    public MemberPanel(String nickname, Identity id, Color pieceColor, Frame parent) {
        if (pieceColor != Color.BLACK && pieceColor != Color.WHITE)
            throw new IllegalArgumentException("Color is not standard!");
        else if (nickname == null)
            throw new IllegalArgumentException("Nickname cannot be null!");
        else if (id == Identity.None && !nickname.isEmpty())
            throw new IllegalArgumentException("Nickname is exist with no id!");
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        setOpaque(false);
        // top
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
        idLabel = new JLabel(id.toString());
        topPanel.setOpaque(false);
        idLabel.setFont(Setting.PMingLiUFont.deriveFont(15.f));
        banButton.setFont(Setting.PMingLiUFont.deriveFont(15.f));
        banButton.setMargin(new Insets(0, 5, 0, 5));
        banButton.setFocusable(false);
        restrictButton.setFont(Setting.PMingLiUFont.deriveFont(15.f));
        restrictButton.setMargin(new Insets(0, 5, 0, 5));
        restrictButton.setFocusable(false);
        topPanel.add(idLabel);
        topPanel.add(Box.createHorizontalGlue());
        topPanel.add(restrictButton);
        topPanel.add(Box.createHorizontalStrut(5));
        topPanel.add(banButton);
        // center
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        factionPanel.setBackground(pieceColor);
        JLabel nicknameLabel = new JLabel(nickname);
        nicknameLabel.setFont(Setting.PMingLiUFont.deriveFont(14.f));
        nicknamePanel.add(nicknameLabel);
        nicknamePanel.setMaximumSize(new Dimension(nicknamePanel.getMaximumSize().width, MEMBER_LABEL_HEIGHT));
        JLabel stateLabel = new JLabel();
        stateLabel.setFont(Setting.PMingLiUFont.deriveFont(16.f));
        statePanel.add(stateLabel);
        statePanel.setMaximumSize(new Dimension(nicknamePanel.getMaximumSize().width, MEMBER_LABEL_HEIGHT));
        addButton.setFont(Setting.PMingLiUFont);
        addButton.setAlignmentX(JButton.CENTER_ALIGNMENT);
        addButton.setMaximumSize(new Dimension(100, 100));
        addButton.setFocusable(false);
        centerPanel.add(factionPanel);
        centerPanel.add(nicknamePanel);
        centerPanel.add(statePanel);
        // bottom
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setOpaque(false);
        try {
            switchButton.setIcon(new ImageIcon(ImageLoader.loadImage("switch", new Dimension(16, 16))));
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
        }
        switchButton.setFocusable(false);
        bottomPanel.add(switchButton);
        // add to main panel
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        // set listener
        nicknamePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                nicknameLabel.setFont(Setting.PMingLiUFont.deriveFont(16.f));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                nicknameLabel.setFont(Setting.PMingLiUFont.deriveFont(14.f));
            }
        });
        addButton.addActionListener(e -> {
            if (Setting.id != Identity.Director)
                return;
            int choosed = JOptionPane.showConfirmDialog(parent, "確定要增加此玩家欄位?", "新增欄位", JOptionPane.YES_NO_OPTION);
            if (choosed == JOptionPane.YES_OPTION) {
                Identity oldID = getId();
                setMember(getNickname(), Identity.None);
                fireIdChange(oldID, Identity.None);
            }
        });
        restrictButton.addActionListener(e -> {
            if (Setting.id != Identity.Director || pieceColor == Color.WHITE && Setting.whiteMember == 1
                    || pieceColor == Color.BLACK && Setting.blackMember == 1)
                return;
            int choosed = JOptionPane.showConfirmDialog(parent, "確定要刪除此玩家欄位?", "移除欄位", JOptionPane.YES_NO_OPTION);
            if (choosed == JOptionPane.YES_OPTION) {
                Identity oldID = getId();
                setMember(getNickname(), Identity.Restrict);
                fireIdChange(oldID, Identity.Restrict);
            }
        });
        setMember(nickname, id);
    }

    /**
     * Set {@code ActionListener} to ban button
     * 
     * @param a
     */
    public void setBanListener(ActionListener a) {
        for (ActionListener rm : banButton.getActionListeners())
            banButton.removeActionListener(rm);
        banButton.addActionListener(a);
    }

    /**
     * Set {@code ActionListener} to switch button
     * 
     * @param a
     */
    public void setSwitchListener(ActionListener a) {
        for (ActionListener rm : banButton.getActionListeners())
            switchButton.removeActionListener(rm);
        switchButton.addActionListener(a);
    }

    /**
     * Set {@code PropertyChangeListener} for {@link MemberState} to panel
     * <p>
     * Old value will be previous state, if null use new value
     * <p>
     * New value will be next state
     * 
     * @param pe
     */
    public void setStateChangeListener(PropertyChangeListener pe) {
        for (PropertyChangeListener pcl : getPropertyChangeListeners("state"))
            removePropertyChangeListener(pcl);
        addPropertyChangeListener("state", pe);
    }

    /**
     * Set {@code PropertyChangeListener} for {@link Identity} to panel
     * <p>
     * Old value will be previous id, if null use new value
     * <p>
     * New value will be next id
     * 
     * @param pe
     */
    public void setIdChangeListener(PropertyChangeListener pe) {
        for (PropertyChangeListener pcl : getPropertyChangeListeners("id"))
            removePropertyChangeListener(pcl);
        addPropertyChangeListener("id", pe);
    }

    /**
     * Similar as {@link Component#firePropertyChange(String, Object, Object)},
     * async property listener of {@code Identity} change
     * 
     * @param oldId
     * @param newId
     */
    public void fireIdChange(Identity oldId, Identity newId) {
        firePropertyChange("id", oldId, newId);
    }

    /**
     * Similar as {@link Component#firePropertyChange(String, Object, Object)},
     * async property listener of {@code MemberState} change
     * 
     * @param oldState
     * @param newState
     */
    public void fireStateChange(MemberState oldState, MemberState newState) {
        firePropertyChange("state", oldState, newState);
    }

    /**
     * Set member info, will also call {@link #setState(state)} according to diff id
     * 
     * @param nickname
     * @param id
     */
    public synchronized void setMember(String nickname, Identity id) {
        if (this.id == id)
            return;
        final Container parent = factionPanel.getParent();
        final Color faction = factionPanel.getBackground();
        // nickname label
        JLabel.class.cast(nicknamePanel.getComponent(0)).setText(nickname);
        // id label
        idLabel.setText(id.toString());
        factionPanel.setOpaque(false);
        restrictButton.setVisible(false);
        if (id == Identity.Restrict) {
            if (this.id == Identity.None) {
                if (faction == Color.WHITE)
                    Setting.whiteMember -= 1;
                else if (faction == Color.BLACK)
                    Setting.blackMember -= 1;
            }
            for (Component comp : parent.getComponents())
                comp.setVisible(false);
            parent.add(addButton);
            setState(MemberState.Empty);
        } else {
            parent.remove(addButton);
            for (Component comp : parent.getComponents())
                comp.setVisible(true);
            if (id == Identity.None) {
                if (this.id == Identity.Restrict) {
                    if (faction == Color.WHITE)
                        Setting.whiteMember += 1;
                    else if (faction == Color.BLACK)
                        Setting.blackMember += 1;
                }
                restrictButton.setVisible(true);
                nicknamePanel.setVisible(false);
                setState(MemberState.Empty);
            } else {
                factionPanel.setOpaque(true);
                nicknamePanel.setVisible(true);
                setState(MemberState.Preparing);
            }
        }
        repaint();
        this.id = id;
    }

    /**
     * Set member info, will also call {@link #setState(state)} according to diff id
     * 
     * @param nickname
     * @param idString
     */
    public void setMember(String nickname, String idString) {
        for (Identity id : Identity.values()) {
            if (id.toString().equals(idString)) {
                setMember(nickname, id);
                return;
            }
        }
        new IllegalArgumentException("Identity name is not exist!");
    }

    /**
     * Set member info, will also call {@link #setState(state)} according to diff id
     * 
     * @param id
     */
    public void setMember(Identity id) {
        String nickname = getNickname();
        if (nickname.isEmpty())
            new IllegalAccessError("Empty nickname change id!");
        setMember(nickname, id);
    }

    /**
     * Set member info, will also call {@link #setState(state)} according to diff id
     * 
     * @param idString
     */
    public void setMember(String idString) {
        String nickname = getNickname();
        if (nickname.isEmpty())
            new IllegalAccessError("Empty nickname change id!");
        setMember(nickname, idString);
    }

    /**
     * Set the state of member panel
     * 
     * @param state new state of member
     */
    public void setState(MemberState state) {
        if (this.state == state)
            return;
        JLabel.class.cast(statePanel.getComponent(0)).setText(state.toString());
        if (state == MemberState.Preparing)
            statePanel.setBackground(Color.GRAY);
        else if (state == MemberState.Ready)
            statePanel.setBackground(Color.YELLOW);
        else if (state == MemberState.Start)
            statePanel.setBackground(Color.RED);
        else
            statePanel.setBackground(Color.WHITE);
        statePanel.repaint();
        this.state = state;
    }

    /**
     * Set the state of member panel
     * 
     * @param stateString
     */
    public void setState(String stateString) {
        for (MemberState state : MemberState.values()) {
            if (state.toString().equals(stateString)) {
                setState(state);
                return;
            }
        }
        new IllegalArgumentException("MemberState name is not exist!");
    }

    /**
     * Set the nickname color of member panel
     * 
     * @param color
     */
    public void setNicknameColor(Color color) {
        JLabel.class.cast(nicknamePanel.getComponent(0)).setForeground(color);
    }

    /**
     * @return {@code Color} of nickname
     */
    public Color getNicknameColor() {
        return JLabel.class.cast(nicknamePanel.getComponent(0)).getForeground();
    }

    /**
     * @return faction {@code Color} of member
     */
    public Color getFaction() {
        return factionPanel.getBackground();
    }

    /**
     * @return member current state
     */
    public MemberState getState() {
        return state;
    }

    /**
     * @return {@code Identity}
     */
    public Identity getId() {
        final String idString = idLabel.getText();
        for (Identity id : Identity.values()) {
            if (id.toString() == idString)
                return id;
        }
        return null;
    }

    /**
     * @return nickname
     */
    public String getNickname() {
        return JLabel.class.cast(nicknamePanel.getComponent(0)).getText();
    }

    /**
     * @return whether member field is empty or not
     */
    public boolean isEmptyMember() {
        return state == MemberState.Empty;
    }

    @Override
    public void paintComponent(Graphics g) {
        Color color = getBackground();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 150));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}