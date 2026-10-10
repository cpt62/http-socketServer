package server;

import entity.Request;
import service.Handler;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Properties;
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
                    try (
                            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            BufferedOutputStream outer = new BufferedOutputStream(clientSocket.getOutputStream());
                    ) {
                        String requestLine = reader.readLine();
                        if (!requestLine.isEmpty()) {
                            Request request = Request.getInstance(requestLine);
                            String method = request.getMethod();
                            String message = request.getMessage();
//                            String protocol = request.getProtocol();
                            Handler handler = handlers.get(method + ":" + message);
                            if (handler == null) {
                                outer.write(("HTTP/1.1 404 Not Found\r\n" +
                                        "Content-Length: 0\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n").getBytes());
                                outer.flush();
                                return;
                            }
                            handler.handle(request, outer);
                        } else {
                            System.err.println("Request-line не соответствует RFC");
                            return;
                        }
                    } catch (IOException e) {
                        System.out.println("Ошибка создания IO потоков");
                    }
                });
            }
        });
        thread.start();
    }

}
