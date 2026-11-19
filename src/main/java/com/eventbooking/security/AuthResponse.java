package com.eventbooking.security;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class AuthResponse {
    private AuthResponse() {
    }

    public static void json(HttpServletResponse response, int status, JsonObject body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body.toString());
    }

    public static void error(HttpServletResponse response, int status, String code) throws IOException {
        json(response, status, Json.createObjectBuilder().add("error", code).build());
    }
}
