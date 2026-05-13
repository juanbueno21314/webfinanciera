package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/perfil")
    public String mostrarPerfil(Authentication auth, Model model) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);
        model.addAttribute("usuario", usuario);
        model.addAttribute("esGoogle", "GOOGLE".equalsIgnoreCase(usuario.getProveedor()));
        return "perfil";
    }

    @PostMapping("/perfil/cambiar-contrasena")
    public String cambiarContrasena(Authentication auth,
                                    @RequestParam String contrasenaActual,
                                    @RequestParam String nuevaContrasena,
                                    @RequestParam String confirmarContrasena,
                                    RedirectAttributes redirectAttrs) {
        if (!nuevaContrasena.equals(confirmarContrasena)) {
            redirectAttrs.addFlashAttribute("errorContrasena", "Las contraseñas no coinciden.");
            return "redirect:/perfil";
        }
        if (nuevaContrasena.length() < 8) {
            redirectAttrs.addFlashAttribute("errorContrasena", "La contraseña debe tener al menos 8 caracteres.");
            return "redirect:/perfil";
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);
        boolean ok = usuarioService.cambiarContrasena(usuario, contrasenaActual, nuevaContrasena);
        if (!ok) {
            redirectAttrs.addFlashAttribute("errorContrasena", "La contraseña actual es incorrecta.");
            return "redirect:/perfil";
        }
        redirectAttrs.addFlashAttribute("exitoContrasena", "✅ Contraseña actualizada correctamente.");
        return "redirect:/perfil";
    }
}
