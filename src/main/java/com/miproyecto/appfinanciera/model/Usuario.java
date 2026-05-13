package com.miproyecto.appfinanciera.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.*;

@Entity
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @Column(unique = true)
    private String email;

    private String contrasena;

    private boolean habilitado = true;

    private String proveedor;

    @Column(nullable = false)
    private boolean aceptaTratamientoDatos = false;

    private LocalDateTime fechaAceptacionDatos;

    private String versionPoliticaDatos;

    @Column(length = 64)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "usuarios_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<Rol> roles = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    public void setHabilitado(boolean habilitado) {
        this.habilitado = habilitado;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> roles) {
        this.roles = roles;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public boolean isAceptaTratamientoDatos() {
        return aceptaTratamientoDatos;
    }

    public void setAceptaTratamientoDatos(boolean aceptaTratamientoDatos) {
        this.aceptaTratamientoDatos = aceptaTratamientoDatos;
    }

    public LocalDateTime getFechaAceptacionDatos() {
        return fechaAceptacionDatos;
    }

    public void setFechaAceptacionDatos(LocalDateTime fechaAceptacionDatos) {
        this.fechaAceptacionDatos = fechaAceptacionDatos;
    }

    public String getVersionPoliticaDatos() {
        return versionPoliticaDatos;
    }

    public void setVersionPoliticaDatos(String versionPoliticaDatos) {
        this.versionPoliticaDatos = versionPoliticaDatos;
    }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public String getNombreCompleto() {
        String n = nombre != null ? nombre : "";
        String a = apellido != null ? apellido : "";
        return (n + " " + a).trim();
    }
}