package com.reptithcm.edu.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {
    // mọi request từ client tới thì i qua authfilter nhầm mục đích dịch token từ httprequest gửi lên
    /* luồng hoạt động của onceperRequestFilter
    * 1. request từ client
    * 2. filter này can thiệp vào bằng việc thực hiện doFilterInternal
    * 3. Trích token -> gọi hàm getToken
    * 4. validate token -> kiểm tra token có okela không
    * 5. Thiết lập Security Context: -> nêu token hợp lệ thì lấy username từ token -> gọi UserDetailsService lấy UserDetails
    * -> tạo đối tượng Authentication mới Nạp vào SecurityContextHolder.
    * Nếu token không hợp lệ hoặc thiếu: Bạn không nạp gì cả, hoặc nạp null.
    * 6. Ủy quyền
    * Sau khi filter này chạy xong, request sẽ đi tiếp đến các phần khác.
    * Nếu controller của bạn có annotation @PreAuthorize("hasRole('ADMIN')"),
    * Spring Security sẽ nhìn vào cái SecurityContextHolder bạn vừa nạp ở bước 5 để quyết định xem User đó có đủ quyền (Role) để vào hay không.
    * */


    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    private final TokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // 1. Lấy token từ request
            String token = tokenProvider.getToken(request); // viet lại thành Optional.ofNullable(tokenProvider.getToken(request))

            // 2. Kiểm tra token có tồn tại và hợp lệ không
            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                // 3. Lấy username từ token
                String username = tokenProvider.getSubject(token);

                // 4. Load thông tin người dùng từ DB
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 5. Tạo đối tượng xác thực
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 6. Set vào SecurityContext
                SecurityContextHolder.getContext().setAuthentication(authentication);

            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        // 7. Tiếp tục chuỗi Filter
        filterChain.doFilter(request, response);
    }
}
