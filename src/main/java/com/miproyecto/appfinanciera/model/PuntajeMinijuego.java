package com.miproyecto.appfinanciera.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class PuntajeMinijuego {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    private String juego; // Ejemplo: "ahorra_gana", "verdadero_falso"

    private int puntaje;

    private LocalDateTime fecha;

    public PuntajeMinijuego() {}

    public PuntajeMinijuego(Usuario usuario, String juego, int puntaje) {
        this.usuario = usuario;
        this.juego = juego;
        this.puntaje = puntaje;
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public String getJuego() {
        return juego;
    }

    public void setJuego(String juego) {
        this.juego = juego;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
