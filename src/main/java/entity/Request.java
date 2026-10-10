package entity;

public class Request {
    private String method;
    private String message;
    private String protocol;

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

    public static Request getInstance(String requestLine) throws IllegalArgumentException {
        Request request = new Request();
        String[] arr = requestLine.split(" ");
        if (arr.length != 3) throw new IllegalArgumentException("request-line не корректен");
        request.method = arr[0];
        request.message = arr[1];
        request.protocol = arr[2];
        return request;
    }
}
