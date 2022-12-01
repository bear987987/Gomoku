package state;

public class ConnectState {
    /**
     * Server disconnect
     */
    public static final int SERVER_DISCONNECT = 0;

    /**
     * Server reject client connect
     */
    public static final int SERVER_REJECT = 1;

    /**
     * Server accept client connect
     */
    public static final int SERVER_ACCEPT = 2;

    /**
     * Server ban the client
     */
    public static final int SERVER_BAN = 3;

    /**
     * Server response for ban voting
     */
    public static final int SERVER_BAN_VOTE_RESPONSE = 4;

    /**
     * Client connect
     */
    public static final int CLIENT_CONNECT = 100;

    /**
     * Client disconnect
     */
    public static final int CLIENT_DISCONNECT = 101;

    /**
     * Client no interrupt happened
     */
    public static final int CLIENT_NO_INTERRUPT = 102;

    /**
     * Client terminate cause interrupt
     */
    public static final int CLIENT_TERMINATED_INTERRUPT = 103;

    /**
     * Client sync data done
     */
    public static final int CLIENT_SYNC_DONE = 104;

    /**
     * Client request for ban voting
     */
    public static final int CLIENT_BAN_VOTE_REQUEST = 105;

    /**
     * Client use stream from {@link StreamReactState}
     */
    public static final int CLIENT_REACT_STREAM = 106;

    /**
     * Multicast when player connected
     */
    public static final int MULTICAST_CONNECT = 200;

    /**
     * Multicast when player disconnected
     */
    public static final int MULTICAST_DISCONNECT = 201;

    /**
     * Multicast when interrupted happened
     */
    public static final int MULTICAST_INTERRUPT = 202;

    /**
     * Multicast when player use stream from {@link StreamReactState}
     */
    public static final int MULTICAST_REACT_STREAM = 203;

}