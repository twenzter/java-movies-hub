package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private final HttpServer server;
    private final MoviesStore moviesStore;

    private static final int BACKLOG_DEFAULT = 0;
    private static final int STOP_DELAY_IMMEDIATE = 0;

    public MoviesServer(MoviesStore moviesStore, int port) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), BACKLOG_DEFAULT);
            this.moviesStore = moviesStore;
            this.server.createContext("/movies", new MoviesHandler(moviesStore));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        server.stop(STOP_DELAY_IMMEDIATE);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore clearMoviesStore() {
        moviesStore.getMovies().clear();
        return moviesStore;
    }
}

