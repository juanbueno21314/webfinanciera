package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.PuntajeMinijuego;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.PuntajeMinijuegoRepository;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class JuegoController {

    @Autowired
    private PuntajeMinijuegoRepository puntajeRepo;

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/juegos")
    public String mostrarJuegos(Model model, Authentication authentication) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(authentication);

        int mejorVF = 0;
        int mejorAhorra = 0;
        int mejorDesafio = 0;

        if (usuario != null) {
            Optional<PuntajeMinijuego> topVF = puntajeRepo.findTopByUsuarioAndJuegoOrderByPuntajeDesc(usuario, "verdadero_falso");
            Optional<PuntajeMinijuego> topAhorra = puntajeRepo.findTopByUsuarioAndJuegoOrderByPuntajeDesc(usuario, "ahorra_gana");
            Optional<PuntajeMinijuego> topDesafio = puntajeRepo.findTopByUsuarioAndJuegoOrderByPuntajeDesc(usuario, "modo_desafio");

            mejorVF = topVF.map(PuntajeMinijuego::getPuntaje).orElse(0);
            mejorAhorra = topAhorra.map(PuntajeMinijuego::getPuntaje).orElse(0);
            mejorDesafio = topDesafio.map(PuntajeMinijuego::getPuntaje).orElse(0);
        }

        Optional<PuntajeMinijuego> topMillonario = puntajeRepo.findTopByUsuarioAndJuegoOrderByPuntajeDesc(usuario, "millonario");
        int mejorMillonario = topMillonario.map(PuntajeMinijuego::getPuntaje).orElse(0);

        model.addAttribute("mejorVF", mejorVF);
        model.addAttribute("mejorAhorra", mejorAhorra);
        model.addAttribute("mejorDesafio", mejorDesafio);
        model.addAttribute("mejorMillonario", mejorMillonario);

        return "juegos/index";
    }

    @GetMapping("/juegos/verdaderoFalso")
    public String juegoVerdaderoFalso() { return "juegos/verdaderoFalso"; }

    @GetMapping("/juegos/desafio")
    public String mostrarModoDesafio() { return "juegos/desafio"; }

    @GetMapping("/juegos/ahorraGana")
    public String mostrarJuegoAhorraYGana() { return "juegos/ahorraGana"; }

    @GetMapping("/juegos/millonario")
    public String mostrarMillonario() { return "juegos/millonario"; }
}
