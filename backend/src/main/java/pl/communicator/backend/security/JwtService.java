package pl.communicator.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    // Builds the signing key from the Base64 secret stored in application configuration.
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Creates a JWT token with the user's login as the subject.
    public String generateToken(String login) {
        return Jwts.builder()
                .subject(login)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    // Reads the login stored as the token subject.
    public String extractLogin(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Checks whether the token belongs to the given user and is still active.
    public boolean isTokenValid(String token, String login) {
        final String extractedLogin = extractLogin(token);
        return extractedLogin.equals(login) && !isTokenExpired(token);
    }

    // Verifies if the token expiration date is already in the past.
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // Parses and verifies all claims from the signed JWT token.
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}