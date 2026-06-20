package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static final Gson gson = new Gson();

    private static final String MOVIES_PATH = "/movies";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String JSON_TYPE = "application/json; charset=UTF-8";
    private static final String APPLICATION_JSON = "application/json";

    private static final int PORT = 8080;
    private static final int MAX_TIMEOUT_DURATION = 2;

    private HttpRequest.Builder createJsonBuilder(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .headers(CONTENT_TYPE, APPLICATION_JSON);
    }

    @BeforeAll
    static void beforeAll() {
        // !!! Реализуйте метод beforeAll
        server = new MoviesServer(new MoviesStore(), PORT);
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(MAX_TIMEOUT_DURATION))
                .build();
        server.start();
    }

    @BeforeEach
    void beforeEach() {
        server.clearMoviesStore();
    }

    @AfterAll
    static void afterAll() {
        // !!! Реализуйте метод afterAll
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH)) // !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.OK, resp.statusCode(), "GET /movies должен вернуть "
                + BaseHttpHandler.OK);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void postMovie_returnsCreatedJson() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.CREATED, resp.statusCode(), "POST /movies должен вернуть "
                + BaseHttpHandler.CREATED);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается объект JSON");
    }

    @Test
    void postMovie_returnsError415_withoutHeader() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH))// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.UNSUPPORTED_MEDIA_TYPE, resp.statusCode(),
                "POST /movies должен вернуть " + BaseHttpHandler.UNSUPPORTED_MEDIA_TYPE);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Запрос с неправильным значением заголовка\",")
                        && body.endsWith("[\"" + CONTENT_TYPE + " должен быть равен " + APPLICATION_JSON + "\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void postMovie_returnsError422_withWrongYear_NegativeId_EmptyTitle() throws Exception {
        String json = gson.toJson(new Movie(-1, "", 1000));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.UNPROCESSABLE_ENTITY, resp.statusCode(),
                "POST /movies должен вернуть " + BaseHttpHandler.UNPROCESSABLE_ENTITY);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Ошибка валидации\",")
                        && body.endsWith("[\"год должен быть между 1888 и 2026\"," +
                        "\"id не может быть отрицательным\"," +
                        "\"название не может быть пустым\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void postMovie_returnsError422_withLongTitle() throws Exception {
        String json = gson.toJson(new Movie(1, "1".repeat(101), 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.UNPROCESSABLE_ENTITY, resp.statusCode(),
                "POST /movies должен вернуть " + BaseHttpHandler.UNPROCESSABLE_ENTITY);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Ошибка валидации\",")
                        && body.endsWith("[\"название очень длинное\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void postMovie_returnsError422_withNotUniqueId() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String json2 = gson.toJson(new Movie(1, "Fight Club", 1999));
        HttpRequest req2 = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.UNPROCESSABLE_ENTITY, resp2.statusCode(),
                "POST /movies должен вернуть " + BaseHttpHandler.UNPROCESSABLE_ENTITY);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE
                + " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Ошибка валидации\",")
                        && body.endsWith("[\"id должен быть уникальным\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void getMovieById_returnsJson() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/1")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.OK, resp2.statusCode(), "GET /movies/{id} должен вернуть "
                + BaseHttpHandler.OK);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{") && body.endsWith("}"),
                "Ожидается объект JSON");
    }

    @Test
    void getMovieById_returnsError404_NotFoundMovie() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/2")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.NOT_FOUND, resp2.statusCode(), "GET /movies/{id} должен вернуть "
                + BaseHttpHandler.NOT_FOUND);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Фильм не найден\"") && body.endsWith("]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void getMovieById_returnsError400_BadRequest() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/2abc")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.BAD_REQUEST, resp2.statusCode(), "GET /movies/{id} должен вернуть "
                + BaseHttpHandler.BAD_REQUEST);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Некорректный ID\"") && body.endsWith("]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void deleteMovieById_returnsNoContent() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/1")// !!! Добавьте правильный URI
                .DELETE()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.NO_CONTENT, resp2.statusCode(), "DELETE /movies/{id} должен вернуть "
                + BaseHttpHandler.NO_CONTENT);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");
    }

    @Test
    void deleteMovieById_returnsError404_NotFound() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/2")// !!! Добавьте правильный URI
                .DELETE()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.NOT_FOUND, resp2.statusCode(), "DELETE /movies/{id} должен вернуть "
                + BaseHttpHandler.NOT_FOUND);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Фильм не найден\"") && body.endsWith("]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void deleteMovieById_returnsError400_BadRequest() throws Exception {
        String json = gson.toJson(new Movie(1, "the matrix", 1999));

        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req2 = createJsonBuilder(MOVIES_PATH + "/2abc")// !!! Добавьте правильный URI
                .DELETE()
                .build();

        HttpResponse<String> resp2 =
                client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.BAD_REQUEST, resp2.statusCode(),
                "DELETE /movies/{id} должен вернуть " + BaseHttpHandler.BAD_REQUEST);

        String contentTypeHeaderValue =
                resp2.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp2.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Некорректный ID\"") && body.endsWith("]}"),
                "Ожидается объект JSON с описанием ошибки");
    }


    @Test
    void getMovieSortedByYear_returnsArray() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String json2 = gson.toJson(new Movie(2, "1+1", 2011));
        HttpRequest req2 = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req3 = createJsonBuilder(MOVIES_PATH + "?year=1999")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.OK, resp.statusCode(), "GET /movies?year=YYYY должен вернуть "
                + BaseHttpHandler.OK);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken());
        assertEquals(1, movieList.size(), "Количество объектов в массиве должна быть 1 штука");
    }

    @Test
    void getMovieSortedByYear_returnsEmptyArray() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String json2 = gson.toJson(new Movie(2, "1+1", 2011));
        HttpRequest req2 = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req3 = createJsonBuilder(MOVIES_PATH + "?year=2000")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.OK, resp.statusCode(), "GET /movies?year=YYYY должен вернуть "
                + BaseHttpHandler.OK);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        List<Movie> movieList = gson.fromJson(body, new ListOfMoviesTypeToken());
        assertTrue(movieList.isEmpty(), "Массив должен быть пустой");
    }


    @Test
    void getMovieSortedByYear_returnsError400_NotCorrectYear() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String json2 = gson.toJson(new Movie(2, "1+1", 2011));
        HttpRequest req2 = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req3 = createJsonBuilder(MOVIES_PATH + "?year=1000")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.BAD_REQUEST, resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть " + BaseHttpHandler.BAD_REQUEST);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Некорректный параметр запроса — year\"")
                        && body.endsWith("\"год должен быть от " + MoviesHandler.FIRST_MOVIE_YEAR
                        + " до " + MoviesHandler.LAST_MOVIE_YEAR + "\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void getMovieSortedByYear_returnsError400_YearUnknownFormat() throws Exception {
        String json = gson.toJson(new Movie(1, "The matrix", 1999));
        HttpRequest req = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String json2 = gson.toJson(new Movie(2, "1+1", 2011));
        HttpRequest req2 = createJsonBuilder(MOVIES_PATH)// !!! Добавьте правильный URI
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .build();

        client.send(req2, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        HttpRequest req3 = createJsonBuilder(MOVIES_PATH + "?year=abcd")// !!! Добавьте правильный URI
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req3, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.BAD_REQUEST, resp.statusCode(),
                "GET /movies?year=YYYY должен вернуть " + BaseHttpHandler.BAD_REQUEST);

        String contentTypeHeaderValue =
                resp.headers().firstValue(CONTENT_TYPE).orElse("");
        assertEquals(JSON_TYPE, contentTypeHeaderValue, CONTENT_TYPE +
                " должен содержать формат данных и кодировку");

        String body = resp.body().trim();
        assertTrue(body.startsWith("{\"error\":\"Некорректный параметр запроса — year\"")
                        && body.endsWith("\"при запросе должны использоваться только цифры\"]}"),
                "Ожидается объект JSON с описанием ошибки");
    }

    @Test
    void putMovies_returnsNotAllowedMethod() throws Exception {
        String json = gson.toJson(new Movie(1, "1+1", 2011));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MOVIES_PATH)) // !!! Добавьте правильный URI
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(BaseHttpHandler.METHOD_NOT_ALLOWED, resp.statusCode(), "PUT /movies должен вернуть "
                + BaseHttpHandler.METHOD_NOT_ALLOWED);
    }
}