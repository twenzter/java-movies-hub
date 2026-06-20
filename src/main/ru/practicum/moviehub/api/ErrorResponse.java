package ru.practicum.moviehub.api;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponse {
    private final String error;
    private final List<String> details;

    private ErrorResponse(Builder builder) {
        this.error = builder.error;
        this.details = builder.details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String error;
        private List<String> details = new ArrayList<>();

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder details(List<String> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }

    }
}