package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.model.Rol;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.RolRepository;
import com.miproyecto.appfinanciera.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public Usuario registrarNuevo(Usuario usuario) {
        if (usuario.getContrasena() != null && !usuario.getContrasena().isBlank()) {
            usuario.setContrasena(passwordEncoder.encode(usuario.getContrasena()));
        }

        if (usuario.getProveedor() == null || usuario.getProveedor().isBlank()) {
            usuario.setProveedor("LOCAL");
        }

        Rol rolUsuario = rolRepo.findByNombre("ROLE_USER");
        usuario.setRoles(Collections.singleton(rolUsuario));

        return usuarioRepo.save(usuario);
    }

    public Usuario guardar(Usuario usuario) {
        return usuarioRepo.save(usuario);
    }

    public boolean cambiarContrasena(Usuario usuario, String contrasenaActual, String nuevaContrasena) {
        if ("GOOGLE".equalsIgnoreCase(usuario.getProveedor())) return false;
        if (!passwordEncoder.matches(contrasenaActual, usuario.getContrasena())) return false;
        usuario.setContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepo.save(usuario);
        return true;
    }

    public Usuario buscarPorEmail(String email) {
        Optional<Usuario> usuario = usuarioRepo.findByEmail(email);
        return usuario.orElse(null);
    }

    public Usuario obtenerUsuarioPorAuthentication(Authentication authentication) {
        if (authentication == null) return null;

        Object principal = authentication.getPrincipal();
        String email = null;

        if (principal instanceof OidcUser oidcUser) {
            email = oidcUser.getEmail();
        } else if (principal instanceof CustomOAuth2User customOAuth2User) {
            email = customOAuth2User.getEmail();
        } else if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        }

        if (email != null) {
            return usuarioRepo.findByEmail(email).orElse(null);
        }

        return null;
    }
}