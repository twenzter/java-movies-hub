package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private static final String CT_REQUEST_JSON = "application/json";
    private static final int MOVIE_COMPONENTS = 2;
    private static final int MOVIE_ID_COMPONENTS = 3;

    private static final int INDEX_OF_ID_IN_PATH = 2;
    private static final int INDEX_OF_QUERY_KEY = 1;
    private static final int INDEX_OF_QUERY_VALUE = 0;

    public static final int FIRST_MOVIE_YEAR = 1888;
    public static final int LAST_MOVIE_YEAR = 2026;
    public static final int MIN_TITLE_LENGTH = 0;
    public static final int MAX_TITLE_LENGTH = 100;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        int amountComponents = ex.getRequestURI().getPath().split("/").length;
        if (method.equalsIgnoreCase("GET") && amountComponents == MOVIE_COMPONENTS) {
            getMovies(ex);
        } else if (method.equalsIgnoreCase("POST") && amountComponents == MOVIE_COMPONENTS) {
            postMovie(ex);
        } else if (method.equalsIgnoreCase("GET") && amountComponents == MOVIE_ID_COMPONENTS) {
            getMovieById(ex);
        } else if (method.equalsIgnoreCase("DELETE") && amountComponents == MOVIE_ID_COMPONENTS) {
            deleteMovieById(ex);
        } else {
            sendError(ex, METHOD_NOT_ALLOWED, "неподдерживаемый HTTP-метод");
        }
    }


    public void getMovies(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        if (query == null || !query.contains("year=")) {
            sendJson(ex, OK, gson.toJson(moviesStore.getMovies()));
            return;
        }

        getMoviesByYear(ex, query);
    }

    public void getMoviesByYear(HttpExchange ex, String query) throws IOException {
        try {
            String stringYear = query.split("year=")[INDEX_OF_QUERY_KEY].split("&")[INDEX_OF_QUERY_VALUE];
            int year = Integer.parseInt(stringYear);
            if (!(year > FIRST_MOVIE_YEAR && year <= LAST_MOVIE_YEAR)) {
                sendError(ex, BAD_REQUEST, "Некорректный параметр запроса — year",
                        List.of("год должен быть от " + FIRST_MOVIE_YEAR + " до " + LAST_MOVIE_YEAR));
            }
            List<Movie> sortedMoviesByYear = moviesStore.getMovies().stream()
                    .filter(mov -> mov.getYear() == year)
                    .toList();
            sendJson(ex, OK, gson.toJson(sortedMoviesByYear));
        } catch (NumberFormatException e) {
            sendError(ex, BAD_REQUEST, "Некорректный параметр запроса — year",
                    List.of("при запросе должны использоваться только цифры"));
        }
    }

    public void postMovie(HttpExchange ex) throws IOException {

        String headerContentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (headerContentType == null || !headerContentType.equals(CT_REQUEST_JSON)) {
            sendError(ex, UNSUPPORTED_MEDIA_TYPE, "Запрос с неправильным значением заголовка",
                    List.of("Content-Type должен быть равен " + CT_REQUEST_JSON));
            return;
        }

        try (InputStream is = ex.getRequestBody()) {
            Movie movie = gson.fromJson(new String(is.readAllBytes(), StandardCharsets.UTF_8), Movie.class);
            List<String> details = getDetails(movie);

            if (details.isEmpty()) {
                moviesStore.addNewMovie(movie);
                sendJson(ex, CREATED, gson.toJson(moviesStore.getMovies().getLast()));
                return;
            }
            sendError(ex, UNPROCESSABLE_ENTITY, "Ошибка валидации", details);
        } catch (Exception e) {
            sendError(ex, BAD_REQUEST, "Некорректный JSON");
        }
    }

    private List<String> getDetails(Movie movie) {
        List<String> details = new ArrayList<>();
        boolean isCorrectId = moviesStore.getMovies().stream()
                .noneMatch(mov -> mov.getId() == movie.getId());

        if (!isCorrectId) {
            details.add("id должен быть уникальным");
        }
        if (movie.getId() < MIN_TITLE_LENGTH) {
            details.add("id не может быть отрицательным");
        }
        if (movie.getTitle().isBlank()) {
            details.add("название не может быть пустым");
        }
        if (movie.getTitle().length() > MAX_TITLE_LENGTH) {
            details.add("название очень длинное");
        }
        if (!(movie.getYear() >= FIRST_MOVIE_YEAR && movie.getYear() <= LAST_MOVIE_YEAR)) {
            details.add("год должен быть между " + FIRST_MOVIE_YEAR + " и " + LAST_MOVIE_YEAR);
        }
        return details;
    }


    public void getMovieById(HttpExchange ex) throws IOException {
        String stringId = ex.getRequestURI().getPath().split("/")[INDEX_OF_ID_IN_PATH];
        try {
            int id = Integer.parseInt(stringId);
            Optional<Movie> movieOpt = moviesStore.getMovies().stream()
                    .filter(mov -> mov.getId() == id)
                    .findFirst();
            if (movieOpt.isPresent()) {
                sendJson(ex, OK, gson.toJson(movieOpt.get()));
                return;
            }
            sendError(ex, NOT_FOUND, "Фильм не найден");
        } catch (NumberFormatException e) {
            sendError(ex, BAD_REQUEST, "Некорректный ID");
        }
    }


    public void deleteMovieById(HttpExchange ex) throws IOException {
        String stringId = ex.getRequestURI().getPath().split("/")[INDEX_OF_ID_IN_PATH];
        try {
            int id = Integer.parseInt(stringId);
            Optional<Movie> movieOpt = moviesStore.getMovies().stream()
                    .filter(mov -> mov.getId() == id)
                    .findFirst();
            if (movieOpt.isPresent()) {
                moviesStore.getMovies().remove(movieOpt.get());
                sendNoContent(ex);
                return;
            }
            sendError(ex, NOT_FOUND, "Фильм не найден");
        } catch (NumberFormatException e) {
            sendError(ex, BAD_REQUEST, "Некорректный ID");
        }
    }
}


