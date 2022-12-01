package state;

public class StreamReactState {
    /**
     * Chat stream react
     */
    public static final String CHAT_REACT = "CHAT_REACT";

    /**
     * Ban react, only for {@code Director} use
     */
    public static final String BAN_REACT = "BAN_REACT";

    /**
     * Ban vote react, only for {@code Member} use
     */
    public static final String BAN_VOTE_REACT = "BAN_VOTE_REACT";

    /**
     * Ban vote response react
     */
    public static final String BAN_VOTE_RESPONSE_REACT = "BAN_VOTE_RESPONSE_REACT";

    /**
     * Ban result react
     */
    public static final String BAN_RESULT_REACT = "BAN_RESULT_REACT";

    /**
     * All ready for start countdown react
     */
    public static final String READY_COUNTDOWN_REACT = "READY_COUNTDOWN_REACT";

    /**
     * Countdowning react
     */
    public static final String COUNTDOWN_REACT = "COUNTDOWN_REACT";

    /**
     * Interrupt happened when countdowning react
     */
    public static final String COUNTDOWN_INTERRUPT_REACT = "COUNTDOWN_INTERRUPT_REACT";

    /**
     * Member id changed stream react
     */
    public static final String MEMBER_ID_REACT = "MEMBER_ID_REACT";

    /**
     * Member state changed stream react
     */
    public static final String MEMBER_STATE_REACT = "MEMBER_STATE_REACT";

    /**
     * Game start react
     */
    public static final String GAME_START_REACT = "GAME_START_REACT";

    /**
     * Game finish react
     */
    public static final String GAME_FINISH_REACT = "GAME_FINISH_REACT";
}