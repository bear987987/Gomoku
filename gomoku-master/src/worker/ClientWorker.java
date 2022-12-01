package worker;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import gomoku.GameScene;
import gomoku.Gomoku;
import gomoku.Setting;
import gomoku.room.interact.ChatPanel;
import gomoku.room.interact.InteractPanel;
import gomoku.room.member.Identity;
import gomoku.room.member.MemberPanel;
import gomoku.room.member.MemberState;
import state.ConnectState;
import state.StreamReactState;

public class ClientWorker implements SyncWriter {

    private final SwingWorker<Void, Void> worker;

    private final Pattern clearPattern = Pattern.compile("^[\\\\/][Cc]lear$");

    private final Pattern yesVotePattern = Pattern.compile("^[\\\\/]([Yy]es|[Yy])$");

    private final Pattern noVotePattern = Pattern.compile("^[\\\\/]([Nn]o|[Nn])$");

    private final Gomoku parent;

    private boolean isBanVoting = false;

    /**
     * Create client worker for accepting input
     * 
     * @param local
     * @param parent
     */
    public ClientWorker(final Socket local, final Gomoku parent) {
        this.parent = parent;
        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                final BufferedReader br = new BufferedReader(
                        new InputStreamReader(local.getInputStream(), StandardCharsets.UTF_8));
                final BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(local.getOutputStream(), StandardCharsets.UTF_8));
                final InteractPanel interpanel = parent.getInteractPanel();
                final ChatPanel cp = interpanel.getChatPanel();
                cp.setEnterActionListener(e -> {
                    String input = cp.getInputText();
                    if (!input.isBlank()) {
                        try {
                            if (clearPattern.matcher(input).matches())
                                cp.clearChatRecord();
                            else if (parent.isGameStart())
                                write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.CHAT_REACT, input,
                                        parent.getMemberPanel(Setting.nickname).getFaction().getRGB());
                            else if (isBanVoting) {
                                if (yesVotePattern.matcher(input).matches()) {
                                    isBanVoting = false;
                                    cp.append("你選擇同意踢除\r\n", Color.BLUE);
                                    write(bw, ConnectState.CLIENT_REACT_STREAM,
                                            StreamReactState.BAN_VOTE_RESPONSE_REACT, "yes");
                                } else if (noVotePattern.matcher(input).matches()) {
                                    isBanVoting = false;
                                    cp.append("你選擇拒絕踢除\r\n", Color.BLUE);
                                    write(bw, ConnectState.CLIENT_REACT_STREAM,
                                            StreamReactState.BAN_VOTE_RESPONSE_REACT, "no");
                                } else
                                    write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.CHAT_REACT, input);
                            } else
                                write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.CHAT_REACT, input);
                            cp.clearInput();
                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }
                    }
                    cp.clearInput();
                });
                // send initial nickname, id and uuid
                write(bw, ConnectState.CLIENT_NO_INTERRUPT, null, Setting.nickname, Setting.id, Setting.uniqueID);
                try {
                    while (true) {
                        int state = br.read();
                        switch (state) {
                        case ConnectState.SERVER_ACCEPT:
                            Setting.blackMember = Integer.valueOf(br.readLine());
                            Setting.whiteMember = Integer.valueOf(br.readLine());
                            final String arrayString = br.readLine();
                            final Map<Integer, List<String>> allMembers = new LinkedHashMap<>();
                            final String[] memArray = arrayString.substring(1, arrayString.length() - 1).split(", ");
                            if (memArray.length >= 3)
                                for (int i = 0; i < memArray.length; i += 3)
                                    allMembers.put(i / 3, Arrays.asList(memArray[i], memArray[i + 1], memArray[i + 2]));
                            SwingUtilities.invokeAndWait(() -> {
                                if (Setting.id == Identity.Director)
                                    parent.buildMemberPane();
                                else
                                    parent.buildMemberPane(allMembers);
                                parent.changeScene(GameScene.Room);
                            });
                            for (MemberPanel mem : parent.getMemberPanels()) {
                                mem.setBanListener(e -> {
                                    try {
                                        if (Setting.nickname.equals(mem.getNickname()) || mem.isEmptyMember())
                                            return;
                                        switch (Setting.id) {
                                        // for director ban permission
                                        case Director:
                                            int dChoosed = JOptionPane.showConfirmDialog(parent,
                                                    "確定要踢除\"" + mem.getNickname() + "\"(" + mem.getId() + ")?", "踢除玩家",
                                                    JOptionPane.YES_NO_OPTION);
                                            if (dChoosed == JOptionPane.YES_OPTION)
                                                write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.BAN_REACT,
                                                        mem.getNickname());
                                            return;
                                        // for member ban permission
                                        case Member:
                                            if (mem.getId() == Identity.Director)
                                                return;
                                            int mChoosed = JOptionPane.showConfirmDialog(parent,
                                                    "確定要投票踢除\"" + mem.getNickname() + "\"(" + mem.getId() + ")?",
                                                    "投票踢除", JOptionPane.YES_NO_OPTION);
                                            if (mChoosed == JOptionPane.YES_OPTION)
                                                write(bw, ConnectState.CLIENT_BAN_VOTE_REQUEST, null,
                                                        mem.getNickname());
                                            return;
                                        default:
                                            break;
                                        }
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                });
                                mem.setSwitchListener(e -> {
                                    if (parent.getMemberPanel(Setting.nickname).getState() != MemberState.Ready)
                                        return;
                                });
                                mem.setStateChangeListener(e -> {
                                    try {
                                        final MemberState newMemState = MemberState.class.cast(e.getNewValue());
                                        write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.MEMBER_STATE_REACT,
                                                parent.getMemberPanelIndex(mem), newMemState);
                                        if (interpanel.isAllMemberReady() && newMemState == MemberState.Start) {
                                            if (Setting.id == Identity.Director)
                                                write(bw, ConnectState.CLIENT_REACT_STREAM,
                                                        StreamReactState.READY_COUNTDOWN_REACT);
                                        }
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                });
                                mem.setIdChangeListener(e -> {
                                    try {
                                        write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.MEMBER_ID_REACT,
                                                parent.getMemberPanelIndex(mem), e.getNewValue());
                                    } catch (IOException ioe) {
                                        ioe.printStackTrace();
                                    }
                                });
                            }
                            cp.clearChatRecord();
                            if (Setting.id == Identity.Director)
                                cp.append("已建立房間\r\n", Color.BLUE);
                            else
                                cp.append("已加入房間\r\n", Color.BLUE);
                            // accept done will send sync and connect package
                            write(bw, ConnectState.CLIENT_CONNECT, null, parent.getMemberPanelIndex(Setting.nickname));
                            write(bw, ConnectState.CLIENT_SYNC_DONE, null);
                            break;
                        case ConnectState.SERVER_REJECT:
                            parent.changeScene(GameScene.Start);
                            JOptionPane.showMessageDialog(parent, br.readLine(), "Reject",
                                    JOptionPane.INFORMATION_MESSAGE);
                            local.close();
                            return null;
                        case ConnectState.SERVER_BAN:
                            parent.changeScene(GameScene.Start);
                            JOptionPane.showMessageDialog(parent, br.readLine(), "Banned",
                                    JOptionPane.INFORMATION_MESSAGE);
                            local.close();
                            return null;
                        case ConnectState.SERVER_BAN_VOTE_RESPONSE:
                            final long remainTime = Long.valueOf(br.readLine());
                            if (remainTime > 0)
                                JOptionPane.showMessageDialog(parent, "還剩下" + remainTime + "秒才能再次發起踢除投票!", "投票踢除間隔",
                                        JOptionPane.INFORMATION_MESSAGE);
                            else
                                write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.BAN_VOTE_REACT,
                                        br.readLine());
                            break;
                        case ConnectState.SERVER_DISCONNECT:
                            JOptionPane.showMessageDialog(parent, "伺服器無法連接!", "Server disconnect",
                                    JOptionPane.INFORMATION_MESSAGE);
                            parent.changeScene(GameScene.Start);
                            local.close();
                            return null;
                        case ConnectState.MULTICAST_CONNECT:
                            final int connectIndex = Integer.valueOf(br.readLine());
                            parent.getMemberPanels()[connectIndex].setMember(br.readLine(), br.readLine());
                            parent.updateState(false);
                            parent.toFront();
                            cp.append(br.readLine() + "\r\n", Color.BLUE);
                            write(bw, ConnectState.CLIENT_SYNC_DONE, null);
                            break;
                        case ConnectState.MULTICAST_DISCONNECT:
                            final int disconnectIndex = Integer.valueOf(br.readLine());
                            parent.getMemberPanels()[disconnectIndex].setMember("", Identity.None);
                            parent.updateState(true);
                            parent.toFront();
                            cp.append(br.readLine() + "\r\n", Color.BLUE);
                            break;
                        case ConnectState.MULTICAST_INTERRUPT:
                            cp.append(br.readLine() + "\r\n", Color.BLUE);
                            parent.toFront();
                            break;
                        case ConnectState.MULTICAST_REACT_STREAM:
                            dealStreamReact(br, bw, br.readLine());
                            break;
                        default:
                            break;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                if (local.isClosed())
                    return;
                try (BufferedWriter bw = new BufferedWriter(
                        new OutputStreamWriter(local.getOutputStream(), StandardCharsets.UTF_8))) {
                    write(bw, ConnectState.CLIENT_DISCONNECT, null, parent.getMemberPanelIndex(Setting.nickname));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        };
    }

    /**
     * Same as {@link SwingWorker#execute()}
     */
    public void execute() {
        worker.execute();
    }

    /**
     * Use {@link SwingWorker#cancel(TRUE)} and send {@code SERVER_DISCONNECT}
     */
    public void shutdown() {
        worker.cancel(true);
    }

    /**
     * @return Same as {@link SwingWorker#isDone()}
     */
    public boolean isDone() {
        return worker.isDone();
    }

    private void dealStreamReact(final BufferedReader br, final BufferedWriter bw, final String streamType)
            throws IOException {
        final InteractPanel interpanel = parent.getInteractPanel();
        final ChatPanel cp = interpanel.getChatPanel();
        switch (streamType) {
        case StreamReactState.CHAT_REACT:
            parent.toFront();
            if (!parent.isGameStart()) {
                cp.append(br.readLine() + "\r\n");
                break;
            }
            final String deterStr = br.readLine();
            Color sourceFaction = null;
            if (deterStr.equals("[所有人]")) {
                sourceFaction = new Color(Integer.valueOf(br.readLine()));
                cp.append(deterStr + " " + br.readLine(),
                        sourceFaction == parent.getMemberPanel(Setting.nickname).getFaction() ? Color.BLACK
                                : Color.RED);
                break;
            }
            sourceFaction = new Color(Integer.valueOf(deterStr));
            if (sourceFaction == parent.getMemberPanel(Setting.nickname).getFaction())
                cp.append(br.readLine() + "\r\n");
            break;
        case StreamReactState.BAN_RESULT_REACT:
            final String deterResult = br.readLine();
            // if result is nickname will ban member
            if (!deterResult.isEmpty())
                parent.getMemberPanel(deterResult).setMember("", Identity.None);
            cp.append(br.readLine() + "\r\n", Color.RED);
            break;
        case StreamReactState.BAN_VOTE_REACT:
            cp.append(br.readLine() + "\r\n" + br.readLine() + "\r\n", Color.RED);
            isBanVoting = true;
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                if (!isBanVoting)
                    return null;
                write(bw, ConnectState.CLIENT_REACT_STREAM, StreamReactState.BAN_VOTE_RESPONSE_REACT, "");
                isBanVoting = false;
                return null;
            }, Setting.BAN_VOTE_DURATION_SECOND, TimeUnit.SECONDS);
            break;
        case StreamReactState.COUNTDOWN_REACT:
        case StreamReactState.COUNTDOWN_INTERRUPT_REACT:
            cp.append(br.readLine() + "\r\n", Color.RED);
            break;
        case StreamReactState.MEMBER_ID_REACT:
            final int idChangeIndex = Integer.valueOf(br.readLine());
            parent.getMemberPanels()[idChangeIndex].setMember(br.readLine());
            break;
        case StreamReactState.MEMBER_STATE_REACT:
            final int stateChangeIndex = Integer.valueOf(br.readLine());
            final String memStateStr = br.readLine();
            parent.getMemberPanels()[stateChangeIndex].setState(memStateStr);
            parent.updateState(false);
            break;
        case StreamReactState.GAME_START_REACT:
            parent.gameStart();
            cp.append(br.readLine() + "\r\n", Color.BLUE);
            break;
        default:
            throw new IOException("Stream state access error!");
        }
    }
}