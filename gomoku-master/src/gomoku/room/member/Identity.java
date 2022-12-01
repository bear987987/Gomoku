package gomoku.room.member;

public enum Identity {
    Observer("觀局"), Director("室長"), Member("成員"), None("空"), Restrict("欄位已限制");

    private final String value;

    private Identity(String value) {
        this.value = value;
    }

    public String toString() {
        return value;
    }
};