package com.gym.management.security;

import com.gym.management.config.JwtProperties;
import com.gym.management.model.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    public static final String CLAIM_ACCOUNT_TYPE = "accountType";
    public static final String CLAIM_ACCOUNT_ID = "accountId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String ACCOUNT_EMPLOYEE = "EMPLOYEE";
    public static final String ACCOUNT_MEMBER = "MEMBER";

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateEmployeeToken(Long employeeId, String username, UserRole role) {
        return buildToken(ACCOUNT_EMPLOYEE, employeeId, username, role);
    }

    public String generateMemberToken(Long memberId, String username) {
        return buildToken(ACCOUNT_MEMBER, memberId, username, UserRole.AFFILIATE);
    }

    private String buildToken(String accountType, Long accountId, String username, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(accountType + ":" + accountId)
                .claim(CLAIM_ACCOUNT_TYPE, accountType)
                .claim(CLAIM_ACCOUNT_ID, accountId)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.expirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
