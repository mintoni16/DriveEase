package driveease;

/** Base API exception. Specialized exceptions inherit its HTTP-status behavior. */
class ApiException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    final int status;

    ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    static ApiException badRequest(String message) { return new ValidationException(message); }
    static ApiException notFound(String message)   { return new ResourceNotFoundException(message); }
    static ApiException conflict(String message)   { return new ConflictException(message); }
}

/** HTTP 400: the request contains invalid input. */
final class ValidationException extends ApiException {
    private static final long serialVersionUID = 1L;
    ValidationException(String message) { super(400, message); }
}

/** HTTP 404: the requested page or record does not exist for this user. */
final class ResourceNotFoundException extends ApiException {
    private static final long serialVersionUID = 1L;
    ResourceNotFoundException(String message) { super(404, message); }
}

/** HTTP 409: the request conflicts with the current database state. */
final class ConflictException extends ApiException {
    private static final long serialVersionUID = 1L;
    ConflictException(String message) { super(409, message); }
}
