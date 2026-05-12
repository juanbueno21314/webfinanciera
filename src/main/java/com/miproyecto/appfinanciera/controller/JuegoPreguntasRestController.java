package com.miproyecto.appfinanciera.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miproyecto.appfinanciera.service.ClaudeAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/juegos")
public class JuegoPreguntasRestController {

    private static final Logger log = LoggerFactory.getLogger(JuegoPreguntasRestController.class);

    @Autowired
    private ClaudeAIService claudeAIService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/preguntas")
    public ResponseEntity<Object> generarPreguntas(
            @RequestParam(defaultValue = "5") int cantidad) {

        String prompt = String.format("""
                Genera %d preguntas de verdadero o falso sobre finanzas personales en español.
                Devuelve ÚNICAMENTE un array JSON válido con este formato exacto, sin explicación adicional:
                [
                  {
                    "texto": "Enunciado de la pregunta",
                    "respuesta": true,
                    "explicacion": "Breve explicación de por qué es verdadero o falso"
                  }
                ]
                Las preguntas deben cubrir temas como: ahorro, presupuesto, deudas, inversión, tarjetas de crédito.
                Varía el nivel de dificultad. Mezcla verdaderas y falsas.
                """, cantidad);

        String respuesta = claudeAIService.preguntar(prompt);

        try {
            // Extraer solo el JSON del array si hay texto extra
            int inicio = respuesta.indexOf('[');
            int fin = respuesta.lastIndexOf(']');
            if (inicio == -1 || fin == -1) throw new Exception("No se encontró array JSON");

            String json = respuesta.substring(inicio, fin + 1);
            List<?> preguntas = objectMapper.readValue(json, List.class);
            return ResponseEntity.ok(preguntas);
        } catch (Exception e) {
            log.error("Error parseando preguntas IA: {}", e.getMessage());
            return ResponseEntity.ok(preguntasFallback());
        }
    }

    private List<Map<String, Object>> preguntasFallback() {
        return List.of(
                Map.of("texto", "Ahorrar es guardar lo que sobra después de gastar.", "respuesta", false, "explicacion", "Ahorrar debe ser lo primero que haces al recibir tu ingreso, no lo último."),
                Map.of("texto", "Endeudarse no siempre es malo si sabes manejarlo bien.", "respuesta", true, "explicacion", "Las deudas bien manejadas pueden ayudarte a lograr metas importantes."),
                Map.of("texto", "Tener un presupuesto mensual ayuda a controlar tus gastos.", "respuesta", true, "explicacion", "Con un presupuesto sabes a dónde va tu dinero y tomas mejores decisiones."),
                Map.of("texto", "Es mejor pagar solo el mínimo de la tarjeta de crédito.", "respuesta", false, "explicacion", "Pagar solo el mínimo genera intereses altos y alarga la deuda."),
                Map.of("texto", "Un fondo de emergencia debe cubrir al menos 3 meses de gastos.", "respuesta", true, "explicacion", "Un colchón de 3 a 6 meses te protege ante imprevistos como desempleo o enfermedad.")
        );
    }
}
