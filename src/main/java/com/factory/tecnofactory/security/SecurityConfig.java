package com.factory.tecnofactory.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

    private static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";
    private static final String ROL_VENDEDOR = "VENDEDOR";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Lo usa AuthService para comprobar correo y clave contra la base de datos. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider proveedor = new DaoAuthenticationProvider(userDetailsService);
        proveedor.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(proveedor);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) throws Exception {

        ManejadorErroresSeguridad errores = new ManejadorErroresSeguridad(resolver);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Públicos: documentación y salud
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Públicos: login y registro de clientes
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()

                        // Cualquier usuario con sesión: su propio perfil
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/me").authenticated()

                        // El resto de /api/usuarios: solo administrador
                        .requestMatchers("/api/usuarios/**").hasRole(ROL_ADMINISTRADOR)

                        // Blogs e imágenes: lectura pública (un <img> no envía JWT)
                        .requestMatchers(HttpMethod.GET, "/api/blogs/**", "/api/imagenes/**").permitAll()

                        // Crear blogs, subir imágenes y cambiar portadas: administrador o vendedor
                        .requestMatchers(HttpMethod.POST, "/api/blogs/**").hasAnyRole(ROL_ADMINISTRADOR, ROL_VENDEDOR)
                        .requestMatchers(HttpMethod.PUT, "/api/blogs/**").hasAnyRole(ROL_ADMINISTRADOR, ROL_VENDEDOR)

                        // Públicos: imágenes subidas (un <img> del navegador no envía el JWT)
                        .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()

                        // Administrador y vendedor: subir y borrar imágenes
                        .requestMatchers(HttpMethod.POST, "/api/imagenes/**").hasAnyRole(ROL_ADMINISTRADOR, ROL_VENDEDOR)
                        .requestMatchers(HttpMethod.DELETE, "/api/imagenes/**").hasAnyRole(ROL_ADMINISTRADOR, ROL_VENDEDOR)

                        // Todo lo demás exige token válido
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(errores)
                        .accessDeniedHandler(errores))
                .addFilterBefore(new JwtAuthFilter(jwtService, userDetailsService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
