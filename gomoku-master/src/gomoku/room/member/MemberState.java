package gomoku.room.member;

/**
 * State of member
 */
public enum MemberState {
    Ready("準備完成"), Preparing("準備中"), Start("準備開始"), Empty("空");

    private final String value;

    private MemberState(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
};