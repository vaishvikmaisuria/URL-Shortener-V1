package db;

public final class DBConstants {
    private DBConstants() {}

    public enum RequestType {
        GET,
        PUT,
        POST,
        DELETE
    };

    public enum Response {
        GOOD,
        BAD
    };
}