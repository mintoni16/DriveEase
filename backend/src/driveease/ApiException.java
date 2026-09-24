package driveease;

/** Thrown anywhere in a handler to send {"error": message} with an HTTP status. */
final class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    static ApiException badRequest(String message) { return new ApiException(400, message); }
    static ApiException notFound(String message)   { return new ApiException(404, message); }
    static ApiException conflict(String message)   { return new ApiException(409, message); }
}
