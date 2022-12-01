package gomoku;

import java.awt.Font;
import java.util.UUID;
import java.util.regex.Pattern;

import gomoku.room.member.Identity;

public class Setting {

    public static final int MAX_NICKNAME_LENGTH = 10;

    public static final int DEFAULT_PORT = 8000;

    public static final String DEFAULT_IP = "127.0.0.1";

    public static final int MAX_PORT = 65535;

    public static final String resPath = "res/";

    public static final Font PMingLiUFont = new Font("PMingLiU", Font.PLAIN, 20);

    public static final int MAX_MEMBER = 10;

    public static final int MIN_MEMBER = 2;

    /**
     * Reraise ban voting interval
     */
    public static final int BAN_VOTE_INTERVAL_SECOND = 300;

    /**
     * Ban voting duration
     */
    public static final int BAN_VOTE_DURATION_SECOND = 30;

    /**
     * Rejoin into same room when banned
     */
    public static final int BANNED_INTERVAL_SECOND = 180;

    public static final UUID uniqueID = UUID.randomUUID();

    public static int blackMember = MIN_MEMBER / 2 + MIN_MEMBER % 2;

    public static int whiteMember = MIN_MEMBER / 2;

    public static int port = DEFAULT_PORT;

    public static String serverIP = DEFAULT_IP;

    public static String nickname = "";

    public static Identity id = null;

    public static final Pattern IP_PATTERN = Pattern
            .compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
}