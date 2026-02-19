package com.pathvision.dto;

public class AuthResponse {
    private String token;
    private String message;
    private String role;
    private String name;

    public AuthResponse() {
    }

    public AuthResponse(String token, String message, String role, String name) {
        this.token = token;
        this.message = message;
        this.role = role;
        this.name = name;
    }

    public static AuthResponseBuilder builder() {
        return new AuthResponseBuilder();
    }

    // Getters and Setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public static class AuthResponseBuilder {
        private String token;
        private String message;
        private String role;
        private String name;

        AuthResponseBuilder() { }

        public AuthResponseBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public AuthResponseBuilder role(String role) {
            this.role = role;
            return this;
        }

        public AuthResponseBuilder name(String name) {
            this.name = name;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(token, message, role, name);
        }
    }
}
