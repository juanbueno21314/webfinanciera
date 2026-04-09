package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Controller
public class LoginController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/login")
    public String login(Model model,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String error,
                        @RequestParam(required = false) String registrado) {

        if (logout != null) {
            model.addAttribute("mensajeLogout", "Sesión cerrada con éxito.");
        }

        if (error != null) {
            model.addAttribute("mensajeError", "Credenciales incorrectas.");
        }

        if (registrado != null) {
            model.addAttribute("mensajeRegistro", "¡Registro exitoso! Ya puedes iniciar sesión.");
        }

        return "login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model,
                                  @RequestParam(required = false) String errorDatos) {
        model.addAttribute("usuario", new Usuario());

        if (errorDatos != null) {
            model.addAttribute("mensajeErrorDatos",
                    "Debes autorizar el tratamiento de datos personales para completar el registro.");
        }

        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@ModelAttribute Usuario usuario,
                            @RequestParam(value = "aceptaTratamientoDatos", required = false) String aceptaTratamientoDatos) {

        if (aceptaTratamientoDatos == null) {
            return "redirect:/registro?errorDatos=1";
        }

        usuario.setAceptaTratamientoDatos(true);
        usuario.setFechaAceptacionDatos(LocalDateTime.now());
        usuario.setVersionPoliticaDatos("v1-2026-04");

        usuarioService.registrarNuevo(usuario);
        return "redirect:/login?registrado=1";
    }

    @GetMapping("/consentimiento-datos")
    public String mostrarConsentimientoDatos(Authentication authentication) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(authentication);

        if (usuario == null) {
            return "redirect:/login";
        }

        if (usuario.isAceptaTratamientoDatos()) {
            return "redirect:/dashboard";
        }

        return "consentimiento-datos";
    }

    @PostMapping("/consentimiento-datos/aceptar")
    public String aceptarConsentimientoDatos(Authentication authentication) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(authentication);

        if (usuario == null) {
            return "redirect:/login";
        }

        usuario.setAceptaTratamientoDatos(true);
        usuario.setFechaAceptacionDatos(LocalDateTime.now());
        usuario.setVersionPoliticaDatos("v1-2026-04");
        usuarioService.guardar(usuario);

        return "redirect:/dashboard";
    }

    @GetMapping("/politica-datos")
    public String politicaDatos() {
        return "politica-datos";
    }

    @GetMapping("/")
    public String redirigirInicio() {
        return "redirect:/login";
    }
}