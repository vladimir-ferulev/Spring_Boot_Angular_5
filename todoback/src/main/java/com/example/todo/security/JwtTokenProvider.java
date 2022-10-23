package com.example.todo.security;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long accessTokenLifetime;
    @Value("${jwt.refresh.expiration}")
    private long refreshTokenLifetime;

    private String refreshToken;
    private Date refreshTokenCreateDate;
    public String lastUrlBeforeRefresh;

    public static final String COOKIE_ACCESS_TOKEN = "access_token";
    public static final String COOKIE_REFRESH_TOKEN = "refresh_token";

    @PostConstruct
    protected void init() {
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createAccessToken(Authentication authentication) {
        Map<String, Object> claimsMap = new HashMap<>();
        claimsMap.put("username", authentication.getName());
        claimsMap.put("authorities", authentication.getAuthorities().stream().findFirst().get().getAuthority());  // пока считаем, что обязательно есть роль и только одна
        Claims claims = Jwts.claims(claimsMap);
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenLifetime * 1000);
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    public String createRefreshToken() {
        refreshToken = UUID.randomUUID().toString();
        refreshTokenCreateDate = new Date();
        return refreshToken;
    }

    public boolean isAccessTokenExpired(String accessToken) {
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken);
            return claimsJws.getBody().getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtAuthenticationException("JWT token invalid", HttpStatus.UNAUTHORIZED);
        }
    }

    public boolean isRefreshTokenExpired() {
        return new Date(refreshTokenCreateDate.getTime() + refreshTokenLifetime * 1000).before(new Date());
    }

    public Authentication getAuthentication(String accessToken) {
        String username;
        Collection<? extends GrantedAuthority> authorities;
        try {
            username = getUsername(accessToken);
            authorities = getAuthorities(accessToken);
        } catch (ExpiredJwtException e) {
            username = e.getClaims().get("username").toString();
            authorities = Collections.singleton(new SimpleGrantedAuthority(e.getClaims().get("authorities").toString()));
        }
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                username, "", authorities);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getUsername(String accessToken) {
        return Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken).getBody().get("username", String.class);
    }

    public Collection<? extends GrantedAuthority> getAuthorities(String accessToken) {
        String authoritiesString = Jwts.parser().setSigningKey(secretKey).parseClaimsJws(accessToken).getBody().get("authorities", String.class);
        return Collections.singleton(new SimpleGrantedAuthority(authoritiesString));        // пока считаем, что обязательно есть роль и только одна
    }

    public String retrieveAccessToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (COOKIE_ACCESS_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String retrieveRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (COOKIE_REFRESH_TOKEN.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public String getStoredRefreshToken() {
        return refreshToken;
    }

    public void cleanRefreshToken() {
        refreshToken = null;
        refreshTokenCreateDate = null;
    }

    public void saveLastUrlBeforeRefreshToken(String servletPath) {
        lastUrlBeforeRefresh = servletPath;
    }
}
