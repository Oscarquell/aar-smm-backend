package kg.azem.portfolio.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Value("${app.birthpinger.local-manual-trigger-public:false}")
    private boolean localManualTriggerPublic;

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
                    if (localManualTriggerPublic) {
                        authorize.requestMatchers(HttpMethod.POST, "/api/v1/admin/birthdays/process-today").permitAll();
                    }
                    authorize.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/**", "/actuator/health", "/uploads/**").permitAll()
                            .anyRequest().permitAll();
                })
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
