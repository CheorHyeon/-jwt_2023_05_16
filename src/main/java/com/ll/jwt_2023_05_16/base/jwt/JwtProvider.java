package com.ll.jwt_2023_05_16.base.jwt;

import com.ll.jwt_2023_05_16.util.Ut;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Component
public class JwtProvider {
    private SecretKey cachedSecretKey;

    @Value("${custom.jwt.secretKey}")
    private String secretKeyPlain;

    private SecretKey _getSecretKey() {
        // Base64 암호화
        String keyBase64Encoded = Base64.getEncoder().encodeToString(secretKeyPlain.getBytes());
        // 해시함수 먹여서 시크릿 키 생성
        return Keys.hmacShaKeyFor(keyBase64Encoded.getBytes());
    }

    public SecretKey getSecretKey() {
        // 시크릿키가 없다면 생성(원래 시크릿 키가 바뀌진 않으니 변할 일이 없으므로 재생성 필요 x)
        if (cachedSecretKey == null) cachedSecretKey = _getSecretKey();

        return cachedSecretKey;
    }

    public String genToken(Map<String, Object> claims, int seconds) {
        long now = new Date().getTime();
        // JWT에서는 만료 시간을 밀리초로 변환해야 하기 때문에 1000을 곱함
        Date accessTokenExpiresIn = new Date(now + 1000L * seconds);

        return Jwts.builder()
                .claim("body", Ut.json.toStr(claims))
                .setExpiration(accessTokenExpiresIn)
                .signWith(getSecretKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    // JWT 유효한지 검증
    public boolean verify(String token) {
        try {
            Jwts.parserBuilder()
                    // Secret Key를 활용하여 검증
                    .setSigningKey(getSecretKey())
                    .build()
                    // 검증할 JWT
                    .parseClaimsJws(token);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    // JWT에서 정보추출(clain)
    public Map<String, Object> getClaims(String token) {
        String body = Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("body", String.class);

        return Ut.json.toMap(body);
    }
}