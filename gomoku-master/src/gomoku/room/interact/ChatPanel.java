package gomoku.room.interact;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import gomoku.Setting;

public class ChatPanel extends JPanel {

    private static final long serialVersionUID = 100L;

    private final int INPUT_HEIGHT = 20;

    private final JTextPane textPane = new JTextPane();

    private final JScrollPane scrollPane = new JScrollPane();

    private final JTextField textField = new JTextField();

    private final JButton enterButton = new JButton("送出");

    private final JButton clearButton = new JButton("清除");

    private final SimpleAttributeSet attr = new SimpleAttributeSet();

    public ChatPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        DefaultCaret.class.cast(textPane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        textPane.setOpaque(false);
        textPane.setEditable(false);
        scrollPane.setViewportView(textPane);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        final JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        inputPanel.add(textField);
        inputPanel.add(enterButton);
        inputPanel.add(clearButton);
        inputPanel.setMaximumSize(new Dimension(textField.getMaximumSize().width, INPUT_HEIGHT));
        enterButton.setFont(Setting.PMingLiUFont.deriveFont(16.f));
        enterButton.setFocusable(false);
        clearButton.setFont(Setting.PMingLiUFont.deriveFont(16.f));
        clearButton.setFocusable(false);
        StyleConstants.setFontFamily(attr, Setting.PMingLiUFont.getFamily());
        add(scrollPane);
        add(inputPanel);
        enterButton.addActionListener(e -> {
            textField.postActionEvent();
            textField.requestFocus();
        });
        clearButton.addActionListener(e -> {
            textField.setText("");
            textField.requestFocus();
        });
        textPane.addFocusListener(new FocusListener() {
            @Override
            public void focusLost(FocusEvent e) {
                textPane.setCaretPosition(textPane.getDocument().getLength());
                DefaultCaret.class.cast(textPane.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            }

            @Override
            public void focusGained(FocusEvent e) {
                DefaultCaret.class.cast(textPane.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            }
        });
    }

    /**
     * Set user input enter listener
     * 
     * @param a
     */
    public void setEnterActionListener(ActionListener a) {
        for (ActionListener rm : textField.getActionListeners())
            textField.removeActionListener(rm);
        textField.addActionListener(a);
    }

    /**
     * Append string to text pane
     * 
     * @param str
     */
    public void append(String str, Color color) {
        final StyledDocument sdoc = textPane.getStyledDocument();
        StyleConstants.setForeground(attr, color);
        try {
            sdoc.insertString(sdoc.getLength(), str, attr);
        } catch (BadLocationException ble) {
            ble.printStackTrace();
        }
    }

    /**
     * Append string to text pane
     * 
     * @param str
     */
    public void append(String str) {
        append(str, Color.BLACK);
    }

    /**
     * Clear the chat record on chat pane
     */
    public void clearChatRecord() {
        textPane.setText("");
    }

    /**
     * Clear the input text
     */
    public void clearInput() {
        textField.setText("");
        textField.requestFocus();
    }

    /**
     * @return user input text
     */
    public String getInputText() {
        return textField.getText();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Color color = getBackground();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 200));
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
    }
}