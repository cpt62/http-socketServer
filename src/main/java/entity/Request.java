package entity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Request {
    private String method;
    private String message;
    private String protocol;
    private Map<String, List<String>> headers;
    private Map<String, String> queryParams;

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

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    private void setQueryParam(String string) {
        queryParams = new HashMap<>();
        if (string.contains("?")) {
            int indexQueryParam = string.indexOf("?");
            String query = string.substring(indexQueryParam + 1);
            String[] params = query.split("&");
            for (String p : params) {
                queryParams.put(p.substring(0, p.indexOf("=")), p.substring(p.indexOf("=") + 1));
            }
        }
    }

    public static Optional<Request> getInstance(String requestLine) throws IllegalArgumentException {
        Request request = new Request();
        String[] arr = requestLine.split(" ");
        if (arr.length != 3) return Optional.empty();
        request.method = arr[0];
        if (arr[1].contains("?")) {
            request.message = arr[1].substring(0, arr[1].indexOf("?"));
        } else {
            request.message = arr[1];
        }
        request.protocol = arr[2];
        request.setQueryParam(arr[1]);
        return Optional.of(request);
    }
}
