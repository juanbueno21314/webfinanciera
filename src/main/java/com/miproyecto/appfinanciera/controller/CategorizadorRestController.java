package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.CategoriaGasto;
import com.miproyecto.appfinanciera.service.ClaudeAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/gastos")
public class CategorizadorRestController {

    @Autowired
    private ClaudeAIService claudeAIService;

    private static final String CATEGORIAS = Arrays.stream(CategoriaGasto.values())
            .map(Enum::name)
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

    @PostMapping("/categorizar")
    public ResponseEntity<Map<String, String>> categorizar(@RequestBody Map<String, String> request) {
        String nombre = request.get("nombre");
        if (nombre == null || nombre.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("categoria", "OTROS"));
        }

        String prompt = String.format("""
                Clasifica el siguiente gasto en una sola de estas categorías: %s
                Gasto: "%s"
                Responde ÚNICAMENTE con el nombre exacto de la categoría, sin explicación ni puntuación.
                """, CATEGORIAS, nombre.trim());

        String respuesta = claudeAIService.preguntar(prompt).trim().toUpperCase();

        // Validar que la respuesta sea una categoría válida
        try {
            CategoriaGasto.valueOf(respuesta);
            return ResponseEntity.ok(Map.of("categoria", respuesta));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("categoria", "OTROS"));
        }
    }
}
