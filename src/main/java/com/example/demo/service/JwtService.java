package com.example.demo.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final UserDetailsService userDetailsService;

    @Value("${jwt.secretkey}") // injecting our secret key
    private String SECRET_KEY;

    public JwtService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    // Generate token with extra claims
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails){

        String token = Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claims(extraClaims)
                .claim("roles", userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .signWith(getSignInkey(), Jwts.SIG.HS256)
                .compact();

        return token;
    }

    public String generateToken(UserDetails userDetails){
        // creates an empty map of claims
        // token will contain the standard claims not extra claims .
        return generateToken(new HashMap<>(), userDetails);
    }


    public SecretKey getSignInkey() {
        //signing key method
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String getUsername(String token){
        return extractClaims(token, Claims::getSubject);
    }


    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver){
        final  Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token){

        return Jwts.parser()
                .verifyWith(getSignInkey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

    }

    private Date getExpiration(String token){
        return extractClaims(token, Claims::getExpiration);
    }
    private Boolean isTokenExpired(String token){
        return getExpiration(token).before(new Date());

    }

    public Boolean isTokenValid(String token, UserDetails userDetails){
        final String userName = getUsername(token);
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public String getEmailFromToken(String token){
        return getUsername(token);
    }

    public String generateRefresh(Map<String, Object> extractClaims, UserDetails userDetails){

        String refreshToken = Jwts.builder()
                .setClaims(extractClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 604800000))
                .signWith(getSignInkey(), Jwts.SIG.HS256)
                .compact();

        return refreshToken;
    }

    public Boolean ValidateToken(String token){

        String userEmail = getUsername(token);

        if (StringUtils.isNotEmpty(userEmail) && !isTokenExpired(token)){

            UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
            return isTokenValid(token, userDetails);

        }

        return false;

    }



    }
