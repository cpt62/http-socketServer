package entity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Request {
    private String method;
    private String message;
    private String protocol;
    private Map<String, List<String>> headers;

    private Request() {
    }

    public String getMethod() {
        return method;
    }

    public String getMessage() {
        return message;
    }

    public String getProtocol() {
        return protocol;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public static Optional<Request> getInstance(String requestLine) throws IllegalArgumentException {
        Request request = new Request();
        String[] arr = requestLine.split(" ");
        if (arr.length != 3) return Optional.empty();
        request.method = arr[0];
        request.message = arr[1];
        request.protocol = arr[2];
        return Optional.of(request);
    }
}
