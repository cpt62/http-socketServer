package server;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    private static volatile Server server;
    private InetAddress host;
    private int port;
    private int backlog;
    private volatile boolean isRunning;
    private ConcurrentHashMap<String, String> concurrentHashMap;

    private Server() {
        getServerConfig();
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
    }


}
