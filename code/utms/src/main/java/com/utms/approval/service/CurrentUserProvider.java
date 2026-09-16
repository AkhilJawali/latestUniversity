package com.utms.approval.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the acting user id for approval actions (A4-19 PD-103). RBAC is not yet built
 * (endpoints are permitAll), so until the Auth module lands the actor is taken from the
 * {@code X-User-Id} request header, falling back to {@code "system"}. When Spring Security
 * is wired, this becomes the authenticated principal. The id is never taken from the
 * request body.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private static final String USER_HEADER = "X-User-Id";
    private static final String FALLBACK_USER = "system";

    private final HttpServletRequest request;

    public String currentUserId() {
        String header = request.getHeader(USER_HEADER);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return FALLBACK_USER;
    }
}
