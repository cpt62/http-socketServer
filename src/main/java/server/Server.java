package server;

import entity.Request;
import service.Handler;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static volatile Server server;
    private ServerSocket serverSocket;
    private InetAddress host;
    private int port;
    private int backlog;
    private volatile boolean isRunning;
    private ConcurrentHashMap<String, Handler> handlers;
    private ExecutorService service;
    private int threadCount;
    private static byte[] endRequestLine = new byte[]{'\r', '\n'};
    private static byte[] endHeaders = new byte[]{'\r', '\n', '\r', '\n'};

    private Server() {
        getServerConfig();
        try {
            serverSocket = new ServerSocket(port, backlog, host);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка соаздания сервера. Проверьте корректность файла конфигурации.");
        }
        handlers = new ConcurrentHashMap<>();
        isRunning = true;
        service = Executors.newFixedThreadPool(threadCount);
    }

    public static Server getInstance() {
        if (server == null) {
            synchronized (Server.class) {
                if (server == null) {
                    server = new Server();
                    return server;
                }
            }
        }
        return server;
    }

    private void getServerConfig() {
        Properties properties = new Properties();
        InputStream inputStream = Server.class.getClassLoader().getResourceAsStream("configServer.properties");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка получения конфигуарции сервера");
        }
        if (properties.isEmpty()) throw new RuntimeException("Ошибка чтения файла конфигурации. Файл пуст.");
        try {
            host = InetAddress.getByName(properties.getProperty("host"));
        } catch (UnknownHostException e) {
            throw new RuntimeException("Проверьте корректность IP ServerSocket");
        }
        port = Integer.parseInt(properties.getProperty("port"));
        backlog = Integer.parseInt(properties.getProperty("backlog"));
        threadCount = Integer.parseInt(properties.getProperty("threadCount"));
    }

    public void addHandler(String method, Handler handler) {
        handlers.put(method, handler);
    }

    // возвращаем массив байт в случаях некорретного запроса на сервер
    private byte[] badRequest(String message) {
        return ("HTTP/1.1 404 " + message + " " + "\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n").getBytes();
    }

    // метод поиска индексов окончания блоков  RL и Headers
    private int indexOf(byte[] inputArr, byte[] searchArr, int start, int end) {
        outer:
        for (int i = 0; i < inputArr.length; i++) {
            for (int j = 0; j < searchArr.length; j++) {
                if (inputArr[i + j] != searchArr[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public Map<String, List<String>> getHeaders(String headers) {
        Map<String, List<String>> headersMap = new HashMap<>();
        String[] headersArr = headers.split("\r\n");
        for (String header : headersArr) {
            if (header.isEmpty()) continue;

            int indexSpace = header.indexOf(":");
            if (indexSpace == -1) continue;

            String key = header.substring(0, indexSpace).trim();
            String value = header.substring(indexSpace + 1).trim();

            headersMap.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return headersMap;

    }

    public void start() {
        Thread thread = new Thread(() -> {
            System.out.println("Сервер запущен. Слушает порт " + port + "...");
            while (isRunning) {
                Socket socket = null;
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    System.err.println("Ошибка соединения с клиентом");
                }
                Socket clientSocket = socket;
                service.submit(() -> {
                    int limit = 4096;
                    byte[] bytes = new byte[limit];
                    try (
                            var reader = new BufferedInputStream(clientSocket.getInputStream());
                            var outer = new BufferedOutputStream(clientSocket.getOutputStream());
                    ) {
                        // число байт, которые получили от сервера
                        var read = reader.read(bytes);

                        if (read == limit) {
                            outer.write(badRequest("Too large request"));
                            return;
                        }
                        // получаю request line, создаем экземпляр Request
                        int endRequestLine = indexOf(bytes, Server.endRequestLine, 0, limit);
                        byte[] newArr = Arrays.copyOf(bytes, endRequestLine);
                        Optional<Request> request = Request.getInstance(
                                new String(newArr, 0, endRequestLine, StandardCharsets.UTF_8));
                        Request httpRequest = null;
                        if (request.isEmpty()) {
                            outer.write(badRequest("Bad http Request"));
                            outer.flush();
                            return;
                        }
                        httpRequest = request.orElse(null);
                        endRequestLine += 2;

                        //получаю заголовки
                        int endHeaders = indexOf(bytes, Server.endHeaders, endRequestLine + 2, limit);
                        if (endHeaders != -1) {
                            String headersString = new String(bytes, endRequestLine, endHeaders - endRequestLine, StandardCharsets.UTF_8);
                            httpRequest.setHeaders(getHeaders(headersString));
                        }

                        //Достаем handler
                        Handler handler = handlers.get(httpRequest.getMethod() + ":" + httpRequest.getMessage());
                        if (handler == null) {
                            outer.write(badRequest("Bad request"));
                            outer.flush();
                            return;
                        }
                        handler.handle(httpRequest, outer);

                    } catch (IOException e) {
                        System.out.println("Ошибка создания IO потоков");
                    }
                });
            }
        });
        thread.start();
    }
}