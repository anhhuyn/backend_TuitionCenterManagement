package com.management.student_center.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(JwtFilter jwtFilter, CorsConfigurationSource corsConfigurationSource) {
        this.jwtFilter = jwtFilter;
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        System.out.println("=== SecurityConfig === Configuring security filter chain");

        http.csrf(csrf -> csrf.disable()); // tắt CSRF
        http.cors(cors -> cors.configurationSource(corsConfigurationSource)); // CHỈNH ĐÚNG

        http.authorizeHttpRequests(auth -> auth
        	.requestMatchers("/uploads/**").permitAll()
            .requestMatchers(HttpMethod.POST, "/v1/api/login").permitAll()
            .requestMatchers("/v1/api/logout").permitAll()
            .requestMatchers("/v1/api/forgot-password").permitAll() // gửi OTP
            .requestMatchers("/v1/api/forgot-password/verify-otp").permitAll() // verify OTP
            .requestMatchers("/v1/api/reset-password").permitAll()
            .requestMatchers("/v1/api/subjects/**").permitAll()
            .requestMatchers("/v1/api/subjects").permitAll()
            .requestMatchers("/v1/api/teachers/basic").permitAll()
            .requestMatchers("/v1/api/session/**").permitAll()
            .requestMatchers("/v1/api/schedule/**").permitAll()
            .requestMatchers("/v1/api/rooms").permitAll()
            .requestMatchers("/v1/api/subject-schedules").permitAll()
            .requestMatchers("/v1/api/subject-students").permitAll()
            .requestMatchers("/v1/api/subject-students/**").permitAll()
            .requestMatchers("/v1/api/students/**").permitAll()
            .requestMatchers("/v1/api/materials/**").permitAll()
            .requestMatchers("/v1/api/subject/**").permitAll()
            .requestMatchers("/v1/api/attendance/**").permitAll()
            .requestMatchers("/v1/api/assignments/**").permitAll()
            .requestMatchers("/v1/api/assign/**").permitAll()
            .requestMatchers("/v1/api/by-assignment/**").permitAll()
            .requestMatchers("/v1/api/assign/update/**").permitAll()
            .requestMatchers("/v1/api/announcements/**").permitAll()
            .anyRequest().authenticated()
        );

        http.httpBasic(httpBasic -> httpBasic.disable());
        http.formLogin(form -> form.disable());

        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}

/*package com.management.student_center.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // TẮT tất cả security
        http.csrf(csrf -> csrf.disable());
        http.authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // CHO PHÉP TẤT CẢ
        );

        // Tắt login form & http basic
        http.formLogin(form -> form.disable());
        http.httpBasic(httpBasic -> httpBasic.disable());

        return http.build();
    }
}
*/
