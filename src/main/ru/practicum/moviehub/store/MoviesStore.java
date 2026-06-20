package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public class MoviesStore {

    private final List<Movie> movies;

    public MoviesStore() {
        movies = new ArrayList<>();

    }

    public List<Movie> getMovies() {
        return movies;
    }

    public void addNewMovie(Movie movie) {
        movies.add(movie);
    }

}
