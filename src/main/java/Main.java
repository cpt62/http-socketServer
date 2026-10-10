import server.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Server server = Server.getInstance();

        server.addHandler("GET:/classic.html", (request, output) -> {
            Path path = Path.of("public", "classic.html");
            byte[] response;
            byte[] content;
            try {
                content = Files.readAllBytes(path);
                String mime = Files.probeContentType(path);
                response =
                        ("HTTP/1.1 200 OK\r\n" +
                                "Content-Type: " + mime + "; charset=UTF-8\r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n\r\n").getBytes();
            } catch (IOException e) {
                throw new RuntimeException("Файла не существует");
            }
            try {
                output.write(response);
                output.write(content);
//                Files.copy(path, output);
                output.flush();
            } catch (IOException e) {
                throw new RuntimeException("Ошибка выходного потока. " +
                        "Проверьте наличие контента (файла - " + path.getFileName() + ")");
            }
        });

        server.addHandler("POST:/classic.html", ((request, outputStream) -> {

        }));

        server.start();
    }
}
