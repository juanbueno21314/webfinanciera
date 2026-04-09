package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.model.Rol;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.RolRepository;
import com.miproyecto.appfinanciera.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Autowired
    private RolRepository rolRepo;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String nombreCompleto = oidcUser.getFullName();

        if (email == null || email.isBlank()) {
            throw new RuntimeException("No se pudo obtener el correo del usuario Google.");
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
            usuario = new Usuario();
            usuario.setEmail(email);
            usuario.setNombre(nombre);
            usuario.setApellido(apellido);
            usuario.setContrasena(passwordEncoder.encode("oauth_google"));
            usuario.setHabilitado(true);
            usuario.setProveedor("GOOGLE");
            usuario.setAceptaTratamientoDatos(false);
            usuario.setFechaAceptacionDatos(null);
            usuario.setVersionPoliticaDatos(null);

            Rol rolUser = obtenerORolUsuario();
            usuario.setRoles(Collections.singleton(rolUser));

            usuarioRepo.save(usuario);
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
                usuarioRepo.save(usuario);
            }
        }

        return oidcUser;
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