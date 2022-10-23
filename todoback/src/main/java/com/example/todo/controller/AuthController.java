package com.example.todo.controller;

import com.example.todo.model.AuthParams;
import com.example.todo.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.example.todo.security.JwtTokenProvider.COOKIE_ACCESS_TOKEN;
import static com.example.todo.security.JwtTokenProvider.COOKIE_REFRESH_TOKEN;
import static org.springframework.util.StringUtils.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    public static final String LOGIN_PATH = "/auth/login";
    public static final String REFRESH_PATH = "/auth/refresh";

    private AuthenticationConfiguration authenticationConfiguration;
    private JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationConfiguration authenticationConfiguration, JwtTokenProvider jwtTokenProvider) {
        this.authenticationConfiguration = authenticationConfiguration;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @GetMapping("/login")
    public String getLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticate(AuthParams request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationConfiguration.getAuthenticationManager()
                    .authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
            String accessToken = jwtTokenProvider.createAccessToken(authentication);
            String refreshToken = jwtTokenProvider.createRefreshToken();

            response.addCookie(createTokenCookie(COOKIE_ACCESS_TOKEN, accessToken, "/"));
            response.addCookie(createTokenCookie(COOKIE_REFRESH_TOKEN, refreshToken, "/auth"));

            return new ResponseEntity<>("Success login", HttpStatus.OK);
        } catch (AuthenticationException e) {
            return new ResponseEntity<>("Invalid email/password combination", HttpStatus.FORBIDDEN);
        } catch (Exception e) {
            return new ResponseEntity<>("Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        jwtTokenProvider.cleanRefreshToken();
        response.addCookie(createTokenCookie(COOKIE_ACCESS_TOKEN, null, "/"));
        response.addCookie(createTokenCookie(COOKIE_REFRESH_TOKEN, null, "/auth"));
        return "login";
    }

    @GetMapping("/refresh")
    public ModelAndView refresh(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String accessToken = jwtTokenProvider.retrieveAccessToken(request);
        String refreshToken = jwtTokenProvider.retrieveRefreshToken(request);
        if (!hasLength(accessToken) || !hasLength(refreshToken)
                || !refreshToken.equals(jwtTokenProvider.getStoredRefreshToken())
                || jwtTokenProvider.isRefreshTokenExpired()) {
            jwtTokenProvider.cleanRefreshToken();
            return new ModelAndView("login");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
        String newAccessToken = jwtTokenProvider.createAccessToken(authentication);
        String newRefreshToken = jwtTokenProvider.createRefreshToken();
        response.addCookie(createTokenCookie(COOKIE_ACCESS_TOKEN, newAccessToken, "/"));
        response.addCookie(createTokenCookie(COOKIE_REFRESH_TOKEN, newRefreshToken, "/auth"));

        RedirectView view = new RedirectView(jwtTokenProvider.lastUrlBeforeRefresh != null ? jwtTokenProvider.lastUrlBeforeRefresh : "/");
        return new ModelAndView(view);
    }

    private static Cookie createTokenCookie(String cookieAccessToken, String accessToken, String uri) {
        Cookie accessTokenCookie = new Cookie(cookieAccessToken, accessToken);
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setMaxAge(Integer.MAX_VALUE);
        accessTokenCookie.setPath(uri);
        return accessTokenCookie;
    }
}
