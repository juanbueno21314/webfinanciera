package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.model.Rol;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.RolRepository;
import com.miproyecto.appfinanciera.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        OAuth2User oauth2User = delegate.loadUser(request);

        String email = oauth2User.getAttribute("email");
        String nombreCompleto = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("No se pudo obtener el correo del usuario.");
        }

        String nombre = "";
        String apellido = "";

        if (nombreCompleto != null && !nombreCompleto.isBlank()) {
            String[] partes = nombreCompleto.trim().split("\\s+");
            nombre = partes[0];
            apellido = partes.length > 1
                    ? String.join(" ", Arrays.copyOfRange(partes, 1, partes.length))
                    : "";
        } else {
            nombre = "Usuario";
        }

        Optional<Usuario> existente = usuarioRepo.findByEmail(email);
        Usuario usuario;

        if (existente.isEmpty()) {
            Usuario nuevo = new Usuario();
            nuevo.setEmail(email);
            nuevo.setNombre(nombre);
            nuevo.setApellido(apellido);
            nuevo.setContrasena(passwordEncoder.encode("oauth_google"));
            nuevo.setHabilitado(true);
            nuevo.setProveedor("GOOGLE");
            nuevo.setAceptaTratamientoDatos(false);
            nuevo.setFechaAceptacionDatos(null);
            nuevo.setVersionPoliticaDatos(null);

            Rol rolUser = obtenerORolUsuario();
            nuevo.setRoles(Collections.singleton(rolUser));

            usuario = usuarioRepo.save(nuevo);
        } else {
            usuario = existente.get();

            boolean actualizar = false;

            if ((usuario.getNombre() == null || usuario.getNombre().isBlank()) && nombre != null && !nombre.isBlank()) {
                usuario.setNombre(nombre);
                actualizar = true;
            }

            if ((usuario.getApellido() == null || usuario.getApellido().isBlank()) && apellido != null && !apellido.isBlank()) {
                usuario.setApellido(apellido);
                actualizar = true;
            }

            if (usuario.getProveedor() == null || usuario.getProveedor().isBlank()) {
                usuario.setProveedor("GOOGLE");
                actualizar = true;
            }

            if (actualizar) {
                usuario = usuarioRepo.save(usuario);
            }
        }

        return new CustomOAuth2User(oauth2User, usuario);
    }

    private Rol obtenerORolUsuario() {
        Rol rolUser = rolRepo.findByNombre("ROLE_USER");
        if (rolUser == null) {
            rolUser = new Rol();
            rolUser.setNombre("ROLE_USER");
            rolUser = rolRepo.save(rolUser);
        }
        return rolUser;
    }
}