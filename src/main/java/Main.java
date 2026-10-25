import server.Server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        Server server = Server.getInstance();

        server.addHandler("GET:/classic.html", (request, output) -> {
            Path path = Path.of("public", "classic.html");

            try {
                // Читаем файл
                byte[] content = Files.readAllBytes(path);
                String mime = Files.probeContentType(path);
                if (mime == null) {
                    mime = "application/octet-stream";
                }

                // Формируем успешный ответ
                String headers = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: " + mime + "; charset=UTF-8\r\n" +
                        "Content-Length: " + content.length + "\r\n" +
                        "Connection: close\r\n\r\n";

                // Отправляем ответ
                output.write(headers.getBytes());
                output.write(content);
                output.flush();

            } catch (NoSuchFileException e) {
                // Файл действительно не найден
                String errorResponse = "HTTP/1.1 404 Not Found\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close\r\n\r\n" +
                        "<h1>404 Not Found</h1>";
                try {
                    output.write(errorResponse.getBytes());
                    output.flush();
                } catch (IOException ex) {
                    ex.printStackTrace(); // Не смогли даже ошибку отправить
                }

            } catch (IOException e) {
                // Другие ошибки IO (чтение файла или запись в сокет)
                e.printStackTrace();

                String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close\r\n\r\n" +
                        "<h1>Internal Server Error</h1>";
                try {
                    output.write(errorResponse.getBytes());
                    output.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        server.addHandler("GET:/forms", (request, output) -> {
            Path path = Path.of("public", "forms.html");
            byte[] bytes;
            byte[] content;
            String response = null;
            if (request.getQueryParams().isEmpty()) {
                try {
                    content = Files.readAllBytes(path);
                    String mime = Files.probeContentType(path);
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html; charset=UTF-8\r\n" +
                            "Content-Length: " + content.length + "\r\n" +
                            "Connection: close\r\n\r\n";

                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.write(content);
                    output.flush();

                } catch (NoSuchFileException e) {
                    String errorResponse = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: close\r\n\r\n" +
                            "<h1>404 Not Found</h1>";
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {
                bytes = request
                        .getQueryParams()
                        .entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("\r\n")).getBytes(StandardCharsets.UTF_8);
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "Connection: close\r\n\r\n";
                try {
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        server.addHandler("GET:/messages", ((request, output) -> {
            byte[] bytes = null;
            String response = null;
            if (request.getQueryParams().isEmpty()) {
                bytes = "В данном HTTP запосе отсутствуют параметры".getBytes(StandardCharsets.UTF_8);
                try {
                    response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain; charset=UTF-8\r\n" +
                            "Content-Length: " + bytes.length + "\r\n" +
                            "Connection: close\r\n\r\n";
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.write(bytes);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                bytes = request
                        .getQueryParams()
                        .entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("\r\n")).getBytes(StandardCharsets.UTF_8);
                response = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "Connection: close\r\n\r\n";
                try {
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.write(bytes);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }
        ));

        server.addHandler("POST:/classic.html", ((request, outputStream) -> {

        }));

        server.start();
    }
}
