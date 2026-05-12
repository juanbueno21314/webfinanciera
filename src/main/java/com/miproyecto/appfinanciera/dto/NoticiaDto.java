package com.miproyecto.appfinanciera.dto;

public class NoticiaDto {
    private String titulo;
    private String enlace;
    private String imagen;
    private String fuente;

    public NoticiaDto(String titulo, String enlace, String imagen, String fuente) {
        this.titulo = titulo;
        this.enlace = enlace;
        this.imagen = imagen;
        this.fuente = fuente;
    }

    public String getTitulo() { return titulo; }
    public String getEnlace() { return enlace; }
    public String getImagen() { return imagen; }
    public String getFuente() { return fuente; }
}
