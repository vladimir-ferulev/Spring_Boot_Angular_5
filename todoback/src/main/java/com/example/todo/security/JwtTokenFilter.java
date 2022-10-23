package com.example.todo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.example.todo.controller.AuthController.LOGIN_PATH;
import static com.example.todo.controller.AuthController.REFRESH_PATH;

@Component
public class JwtTokenFilter extends GenericFilterBean {
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public JwtTokenFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (isPublicResource(((HttpServletRequest) request).getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = jwtTokenProvider.retrieveAccessToken((HttpServletRequest) request);
        try {
            if (accessToken == null) {
                ((HttpServletResponse) response).sendRedirect(LOGIN_PATH);
                return;
            }

            if (jwtTokenProvider.isAccessTokenExpired(accessToken)) {
                jwtTokenProvider.saveLastUrlBeforeRefreshToken(((HttpServletRequest) request).getServletPath());
                ((HttpServletResponse) response).sendRedirect(REFRESH_PATH);
                return;
            } else {
                Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (JwtAuthenticationException e) {
            ((HttpServletResponse) response).sendError(e.getHttpStatus().value());
            throw new JwtAuthenticationException("JWT token invalid");
        }
        jwtTokenProvider.lastUrlBeforeRefresh = null;
        filterChain.doFilter(request, response);
    }

    private boolean isPublicResource(String servletPath) {
        return LOGIN_PATH.equals(servletPath)
                || REFRESH_PATH.equals(servletPath)
                || "/error".equals(servletPath);
    }
}
