package com.miproyecto.appfinanciera.repository;

import com.miproyecto.appfinanciera.model.PuntajeMinijuego;
import com.miproyecto.appfinanciera.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PuntajeMinijuegoRepository extends JpaRepository<PuntajeMinijuego, Long> {
    List<PuntajeMinijuego> findByUsuario(Usuario usuario);

    List<PuntajeMinijuego> findByUsuarioAndJuegoOrderByPuntajeDesc(Usuario usuario, String juego);

    Optional<PuntajeMinijuego> findTopByUsuarioAndJuegoOrderByPuntajeDesc(Usuario usuario, String juego);


        List<PuntajeMinijuego> findTop5ByUsuarioAndJuegoOrderByPuntajeDesc(Usuario usuario, String juego);
}

