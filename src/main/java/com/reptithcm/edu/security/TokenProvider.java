package com.reptithcm.edu.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenProvider {
    // secret
    @Value("${app.jwt.secret}")
    private String secret;
    // accessExpMs
    @Value("${app.jwt.access-expires-in-mili-seconds}")
    private Long accessExpMs;

    // secretKey + init()
    private SecretKey secretKey;
    @PostConstruct
    void init() {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /*
    * 1. generateAccessTokenFromUsername -> đóng vai trò như thời điểm bắt đầu(người dùng đăng nhập thành công)
    * chức năng: nhận username -> tạo ra accessToken hoàn chỉnh
    *
    * 2. generateAccessToken -> phương thức cốt lõi tạo nên accesstoken
    * mục đích chính là nhận thông tin người (username - role) -> gọi generateAccessTokenFrom__ -> tạo token
    *
    * 3. validateToken -> nhận token kiểm tra hợp lệ của nó
    * mục đích chính của nó là kiểm tra token có còn hạn, có sạch hay không
    * sử dụng Jwts.Parser()
    *
    * 4. getSubject -> nhận vào token
    * sau khi validate xong thì lấy ra username để định danh người dùng
    * chức năng là: trích xuất sub từ payload của token -> thường là username
    *
    * 5. getToken -> phương thức hổ trợ lấy token tu người dung
    * cách hoạt động: tìm header có tên authorization -> kiểm tra bắt đầu từ bearee không -> cắt bỏ bearer để lấy jwt
    * */

    public String generateAccessTokenFromUsername(String userName){
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessExpMs);

        return Jwts.builder()
                .subject(userName) // đối tượng mã hóa thành jwt
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey) // loại ký tên/ loại chuyển sang jwt
                .compact();
    }

    public String generateAccessToken(Authentication authentication){ // Authentication -> SecurityContextHolder lưu trử authentication này
        return generateAccessTokenFromUsername(authentication.getName());
    }

    public boolean validateToken(String token){
        try {
            Jwts.parser().verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public String getToken(HttpServletRequest request){
        String bearer =request.getHeader("authorization");
        return (StringUtils.hasText(bearer)) && bearer.startsWith("Bearer ") ? bearer.substring(7) : null;
    }

    public String getSubject(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();       // 0.12.x use getPayload() instead of getBody()
        return claims.getSubject();
    }
}
