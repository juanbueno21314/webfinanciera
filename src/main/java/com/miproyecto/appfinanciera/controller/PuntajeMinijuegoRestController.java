package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.service.PuntajeMinijuegoService;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/puntajes")
public class PuntajeMinijuegoRestController {

    @Autowired
    private PuntajeMinijuegoService puntajeService;

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping("/guardar")
    public void guardarPuntaje(@RequestBody Map<String, Object> datos, Authentication auth) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);
        String juego = (String) datos.get("juego");
        int puntaje = (int) datos.get("puntaje");

        System.out.println("🎮 Recibido puntaje para guardar");
        System.out.println("Juego: " + juego + " | Puntaje: " + puntaje);

        puntajeService.guardarPuntaje(usuario, juego, puntaje);
    }
}
