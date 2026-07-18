package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class JwtService {

    private final UserDetailsService userDetailsService;

    @Value("${jwt.secret}") // injecting our secret key
    private String SECRET_KEY;

    public JwtService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){

        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    public Key key() {
        //signing key method
        return Keys.hmacShaKeyFor(
                Decoders.BASE64URL.decode(SECRET_KEY)
        );
    }

    public String getUsername(String token){
        return extractClaims(token, Claims::getSubject);
    }


    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver){
        final  Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token){
        return (Claims) Jwts.parser().setSigningKey(key()).build();
    }

    private Date getExpiration(String token){
        return extractClaims(token, Claims::getExpiration);
    }
    private Boolean isTokenExpired(String token){
        return getExpiration(token).before(new Date());

    }

    private Boolean isTokenValid(String token, UserDetails userDetails){
        final String userName = getUsername(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String getEmailFromToken(String token){
        return getUsername(token);
    }

    public String generateToken(UserDetails userDetails){
        // creates an empty map of claims
        // token will contain the standard claims not extra claims .
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generatedRefresh(Map<String, Object> extractClaims, UserDetails userDetails){

        String token = Jwts.builder()
                .setClaims(extractClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 604800000))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();

        return token;
    }

    public Boolean ValidateToken(String token){

        String userEmail = getUsername(token);

        if (StringUtils.isNotEmpty(userEmail) && !isTokenExpired(token)){
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(token);
            return isTokenValid(token, userDetails);
        }

        return false;

    }



    }
