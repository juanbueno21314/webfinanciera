package com.miproyecto.appfinanciera.service;

import com.miproyecto.appfinanciera.model.PuntajeMinijuego;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.PuntajeMinijuegoRepository;
import com.miproyecto.appfinanciera.repository.UsuarioRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PuntajeMinijuegoService {

    @Autowired
    private PuntajeMinijuegoRepository puntajeRepo;

    @Autowired
    private UsuarioRepository usuarioRepo;

    @Transactional
    public void guardarPuntaje(Usuario usuario, String juego, int puntaje) {
        System.out.println("========== GUARDANDO PUNTAJE ==========");
        System.out.println("Usuario: " + (usuario != null ? usuario.getEmail() : "null"));
        System.out.println("ID usuario: " + (usuario != null ? usuario.getId() : "null"));
        System.out.println("Juego: " + juego);
        System.out.println("Puntaje: " + puntaje);

        if (usuario == null || usuario.getId() == null) {
            System.out.println("❌ ERROR: Usuario inválido o sin ID. No se guarda puntaje.");
            return;
        }

        // Aseguramos que el usuario esté gestionado por el contexto de persistencia
        Usuario usuarioPersistido = usuarioRepo.findById(usuario.getId()).orElse(null);
        if (usuarioPersistido == null) {
            System.out.println("❌ Usuario no encontrado en base de datos.");
            return;
        }

        PuntajeMinijuego nuevo = new PuntajeMinijuego();
        nuevo.setUsuario(usuarioPersistido);
        nuevo.setJuego(juego);
        nuevo.setPuntaje(puntaje);
        nuevo.setFecha(LocalDateTime.now());

        puntajeRepo.save(nuevo);
        System.out.println("✅ Puntaje guardado correctamente.");
    }

    public List<PuntajeMinijuego> obtenerTop5Puntajes(Usuario usuario, String juego) {
        return puntajeRepo.findTop5ByUsuarioAndJuegoOrderByPuntajeDesc(usuario, juego);
    }
}
