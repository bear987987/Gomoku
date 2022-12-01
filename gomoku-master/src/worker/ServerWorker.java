package worker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingWorker;

import gomoku.Gomoku;
import gomoku.Setting;
import gomoku.room.member.MemberPanel;
import state.ConnectState;
import state.StreamReactState;

public class ServerWorker implements SyncWriter {

    private final SwingWorker<Void, Void> worker;

    private final Gomoku parent;

    private final Map<String, Socket> clients = new ConcurrentHashMap<>();

    private final Map<String, UUID> uniqueIDRecords = new ConcurrentHashMap<>();

    private final Map<UUID, Long> bannedRecords = new ConcurrentHashMap<>();

    private Future<?> countdownFuture = null;

    private long lastBanVoteMillis = 0;

    private String lastVoteName = "";

    private int agreeBan = 0;

    private int oppositeBan = 0;

    private int synchronizedClients = 0;

    private Object lock = new Object();

    /**
     * Create server worker for accepting client
     * 
     * @param server
     * @param clients
     */
    public ServerWorker(final ServerSocket server, final Gomoku parent) {
        this.parent = parent;
        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (true) {
                    final Socket accept = server.accept();
                    synchronizedClients = 0;
                    try {
                        final BufferedReader br = new BufferedReader(
                                new InputStreamReader(accept.getInputStream(), StandardCharsets.UTF_8));
                        final BufferedWriter bw = new BufferedWriter(
                                new OutputStreamWriter(accept.getOutputStream(), StandardCharsets.UTF_8));
                        final int interruptState = br.read();
                        final String nickname = br.readLine();
                        final String id = br.readLine();
                        final UUID uuid = UUID.fromString(br.readLine());
                        String reason = null;
                        if (bannedRecords.containsKey(uuid)) {
                            long elapsed = (System.currentTimeMillis() - bannedRecords.get(uuid)) / 1000;
                            if (elapsed < Setting.BANNED_INTERVAL_SECOND)
                                reason = "已被踢除: 目前剩下" + (Setting.BANNED_INTERVAL_SECOND - elapsed) + "秒才可再次進去該房間!";
                            else
                                bannedRecords.remove(uuid);
                        } else if (clients.size() >= Setting.blackMember + Setting.whiteMember)
                            reason = "房間人數已滿!";
                        else if (clients.containsKey(nickname))
                            reason = "名稱 \"" + nickname + "\" 已重複!";
                        if (interruptState != ConnectState.CLIENT_NO_INTERRUPT) {
                            if (reason == null)
                                multicast(ConnectState.MULTICAST_INTERRUPT, null, nickname + "(" + id + ") 嘗試連線已中止");
                            continue;
                        }
                        if (reason != null) {
                            write(bw, ConnectState.SERVER_REJECT, null, reason);
                            accept.close();
                            continue;
                        }
                        clients.put(nickname, accept);
                        uniqueIDRecords.put(nickname, uuid);
                        Executors.newFixedThreadPool(1).submit((Callable<Void>) () -> {
                            try {
                                while (true) {
                                    int state = br.read();
                                    switch (state) {
                                    case ConnectState.CLIENT_SYNC_DONE:
                                        synchronizedClients += 1;
                                        break;
                                    case ConnectState.CLIENT_CONNECT:
                                        multicast(
                                                ConnectState.MULTICAST_CONNECT, null, br.readLine() + "\r\n" + nickname
                                                        + "\r\n" + id + "\r\n" + nickname + "(" + id + ") 已加入房間",
                                                accept);
                                        break;
                                    case ConnectState.CLIENT_DISCONNECT:
                                        removeMulticast(ConnectState.MULTICAST_DISCONNECT, null,
                                                br.readLine() + "\r\n" + nickname + "(" + id + ") 已離開房間", accept);
                                        return null;
                                    case ConnectState.CLIENT_BAN_VOTE_REQUEST:
                                        final long elapsedTime = (System.currentTimeMillis() - lastBanVoteMillis)
                                                / 1000;
                                        write(bw, ConnectState.SERVER_BAN_VOTE_RESPONSE, null,
                                                Math.max(Setting.BAN_VOTE_INTERVAL_SECOND - elapsedTime, 0) + "\r\n"
                                                        + br.readLine());
                                        break;
                                    case ConnectState.CLIENT_REACT_STREAM:
                                        dealStreamReact(br, bw, nickname, id, br.readLine());
                                        break;
                                    default:
                                        break;
                                    }
                                }
                            } catch (IOException ioe) {
                                ioe.printStackTrace();
                                clients.remove(nickname);
                                uniqueIDRecords.remove(nickname);
                                accept.close();
                            }
                            return null;
                        });
                        final List<Object> list = new ArrayList<>();
                        for (MemberPanel member : parent.getMemberPanels()) {
                            list.add(member.getNickname());
                            list.add(member.getId().toString());
                            list.add(member.getState().toString());
                        }
                        write(bw, ConnectState.SERVER_ACCEPT, null, Setting.blackMember, Setting.whiteMember, list);
                        synchronized (lock) {
                            try {
                                while (synchronizedClients < clients.size())
                                    lock.wait(1000);
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                        clients.remove(Setting.nickname);
                        uniqueIDRecords.remove(Setting.nickname);
                        accept.close();
                    }
                }
            }

            @Override
            protected void done() {
                try {
                    clients.clear();
                    uniqueIDRecords.clear();
                    server.close();
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
        clients.forEach((key, value) -> {
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(value.getOutputStream(), StandardCharsets.UTF_8))) {
                write(bw, ConnectState.SERVER_DISCONNECT, null);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        });
        worker.cancel(true);
    }

    /**
     * @return Same as {@link SwingWorker#isDone()}
     */
    public boolean isDone() {
        return worker.isDone();
    }

    private void multicast(Integer connectState, String reactState, String castString, Socket... ignoreSockets) {
        multicast(connectState, reactState, castString, false, Arrays.asList(ignoreSockets));
    }

    private void removeMulticast(Integer connectState, String reactState, String castString, Socket removeSocket,
            Socket... elseRemoves) {
        List<Socket> list = Arrays.asList(removeSocket);
        for (Socket rm : elseRemoves)
            list.add(rm);
        multicast(connectState, reactState, castString, true, list);
    }

    private void multicast(Integer connectState, String reactState, String castString, boolean isRemove,
            List<Socket> selectsSockets) {
        clients.entrySet().removeIf(entry -> {
            try {
                final Socket client = entry.getValue();
                if (selectsSockets.contains(client)) {
                    if (isRemove)
                        client.close();
                    return isRemove;
                }
                final BufferedWriter bufw = new BufferedWriter(
                        new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
                write(bufw, connectState, reactState, castString);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            return false;
        });
    }

    private void dealStreamReact(final BufferedReader br, final BufferedWriter bw, String nickname, final String id,
            final String streamType) throws IOException {
        switch (streamType) {
        case StreamReactState.CHAT_REACT:
            final String text = br.readLine();
            if (!parent.isGameStart()) {
                multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.CHAT_REACT,
                        nickname + "(" + id + "): " + text);
                break;
            }
            if (text.matches("^[\\\\/]all\\s.*$"))
                multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.CHAT_REACT,
                        "[所有人]\r\n" + br.readLine() + "\r\n" + nickname + ": " + text.replaceAll("^[\\\\/]all\\s", ""));
            else
                multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.CHAT_REACT,
                        br.readLine() + "\r\n" + nickname + ": " + text);
            break;
        case StreamReactState.BAN_REACT:
            final String banName = br.readLine();
            final MemberPanel banMember = parent.getMemberPanel(banName);
            final Socket client = clients.get(banName);
            bannedRecords.put(uniqueIDRecords.get(banName), System.currentTimeMillis());
            write(client, ConnectState.SERVER_BAN, null, "從房間被踢除!");
            removeMulticast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.BAN_RESULT_REACT,
                    banName + "\r\n" + banName + "(" + banMember.getId() + ") 已被室長踢除", client);
            break;
        case StreamReactState.BAN_VOTE_REACT:
            lastVoteName = br.readLine();
            agreeBan = 0;
            oppositeBan = 0;
            lastBanVoteMillis = System.currentTimeMillis();
            final int currentPlayerNumber = clients.size();
            multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.BAN_VOTE_REACT,
                    "投票踢除\"" + lastVoteName + "\"(" + parent.getMemberPanel(lastVoteName).getId() + "): \r\n"
                            + "輸入\\yes(\\y) 或 \\no(\\n) 進行票決(30秒)",
                    clients.get(lastVoteName));
            Executors.newSingleThreadScheduledExecutor().schedule(() -> {
                String result = null;
                final Socket votedClient = clients.get(lastVoteName);
                if (agreeBan - oppositeBan <= currentPlayerNumber / 3.f
                        || agreeBan + oppositeBan < currentPlayerNumber / 2.f)
                    result = "\r\n未踢除玩家\"" + lastVoteName + "\" " + agreeBan + "票同意 " + oppositeBan + "票反對";
                else {
                    result = lastVoteName + "\r\n已踢除玩家\"" + lastVoteName + "\" " + agreeBan + "票同意 " + oppositeBan
                            + "票反對";
                    write(votedClient, ConnectState.SERVER_BAN, null, "從房間被踢除!");
                }
                bannedRecords.put(uniqueIDRecords.get(lastVoteName), System.currentTimeMillis());
                multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.BAN_RESULT_REACT, result);
                return null;
            }, Setting.BAN_VOTE_DURATION_SECOND, TimeUnit.SECONDS);
            break;
        case StreamReactState.BAN_VOTE_RESPONSE_REACT:
            final String playerChoosed = br.readLine();
            if (playerChoosed.isEmpty())
                break;
            if (playerChoosed.equals("yes"))
                agreeBan += 1;
            else
                oppositeBan += 1;
            break;
        case StreamReactState.READY_COUNTDOWN_REACT:
            if (countdownFuture != null) {
                try {
                    countdownFuture.get(2, TimeUnit.SECONDS);
                } catch (Exception ex) {
                    countdownFuture.cancel(false);
                }
                countdownFuture = null;
            }
            countdownFuture = Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(new Runnable() {
                private int times = 5;

                @Override
                public void run() {
                    if (countdownFuture.isCancelled()) {
                        return;
                    } else if (times == 0) {
                        multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.GAME_START_REACT,
                                "輸入\\all(或/all)進行所有人聊天");
                        countdownFuture.cancel(false);
                        return;
                    }
                    multicast(ConnectState.MULTICAST_REACT_STREAM, StreamReactState.COUNTDOWN_REACT,
                            "遊戲將在 " + times-- + " 秒後開始");
                }
            }, 0, 1, TimeUnit.SECONDS);
            break;
        case StreamReactState.MEMBER_ID_REACT:
        case StreamReactState.MEMBER_STATE_REACT:
            multicast(ConnectState.MULTICAST_REACT_STREAM, streamType, br.readLine() + "\r\n" + br.readLine(),
                    clients.get(nickname));
            break;
        default:
            throw new IOException("Stream state access error!");
        }
    }
}