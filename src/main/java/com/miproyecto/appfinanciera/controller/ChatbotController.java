package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.DeudaRepository;
import com.miproyecto.appfinanciera.repository.GastoRepository;
import com.miproyecto.appfinanciera.repository.IngresoRepository;
import com.miproyecto.appfinanciera.repository.MetaAhorroRepository;
import com.miproyecto.appfinanciera.service.ClaudeAIService;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@Controller
public class ChatbotController {

    @Autowired
    private ClaudeAIService claudeAIService;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private DeudaRepository deudaRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private MetaAhorroRepository metaAhorroRepository;

    @GetMapping("/chat")
    public String mostrarChat() {
        return "chatbot";
    }

    @PostMapping("/api/chat")
    @ResponseBody
    public ResponseEntity<Map<String, String>> chat(
            @RequestBody Map<String, String> request,
            Authentication auth) {

        String mensaje = request.get("mensaje");
        if (mensaje == null || mensaje.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("respuesta", "Escribe un mensaje para continuar."));
        }

        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);

        double ingresos = ingresoRepository.findByUsuario(usuario).stream()
                .mapToDouble(i -> i.getMonto() != null ? i.getMonto() : 0).sum();

        double deudas = deudaRepository.findByUsuario(usuario).stream()
                .mapToDouble(d -> d.getMonto() != null ? d.getMonto() : 0).sum();

        LocalDate inicio = LocalDate.now().withDayOfMonth(1);
        LocalDate fin = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        double gastos = gastoRepository.findByUsuarioAndFechaBetween(usuario, inicio, fin).stream()
                .mapToDouble(g -> g.getMonto() != null ? g.getMonto() : 0).sum();

        double ahorro = metaAhorroRepository.sumaAbonosByUsuario(usuario.getId()).orElse(0.0);

        String prompt = String.format("""
                Eres un asistente financiero personal amigable dentro de la app AppFinanzas.
                Datos financieros actuales del usuario:
                - Ingresos totales registrados: $%.2f
                - Deudas totales: $%.2f
                - Gastos del mes actual: $%.2f
                - Ahorro acumulado: $%.2f

                Responde en español, de forma concisa y útil (máximo 4 oraciones).
                Sin asteriscos ni markdown, solo texto plano.
                Pregunta del usuario: %s
                """, ingresos, deudas, gastos, ahorro, mensaje);

        String respuesta = claudeAIService.preguntar(prompt);
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }
}
