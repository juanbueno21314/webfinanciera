package com.miproyecto.appfinanciera.config;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ConsentimientoDatosInterceptor implements HandlerInterceptor {

    private final UsuarioService usuarioService;

    public ConsentimientoDatosInterceptor(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();

        if (uri.startsWith("/login")
                || uri.startsWith("/registro")
                || uri.startsWith("/oauth2")
                || uri.startsWith("/politica-datos")
                || uri.startsWith("/consentimiento-datos")
                || uri.startsWith("/css")
                || uri.startsWith("/js")
                || uri.startsWith("/images")
                || uri.startsWith("/webjars")
                || uri.equals("/error")
                || uri.equals("/favicon.ico")) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);

        if (usuario != null && !usuario.isAceptaTratamientoDatos()) {
            response.sendRedirect("/consentimiento-datos");
            return false;
        }

        return true;
    }
}