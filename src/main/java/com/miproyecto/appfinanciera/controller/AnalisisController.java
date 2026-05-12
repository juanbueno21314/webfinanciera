package com.miproyecto.appfinanciera.controller;

import com.miproyecto.appfinanciera.model.Deuda;
import com.miproyecto.appfinanciera.model.Ingreso;
import com.miproyecto.appfinanciera.model.Usuario;
import com.miproyecto.appfinanciera.repository.DeudaRepository;
import com.miproyecto.appfinanciera.repository.IngresoRepository;
import com.miproyecto.appfinanciera.service.ClaudeAIService;
import com.miproyecto.appfinanciera.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/analisis")
public class AnalisisController {

    @Autowired
    private IngresoRepository ingresoRepository;

    @Autowired
    private DeudaRepository deudaRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ClaudeAIService claudeAIService;
    @GetMapping
    public String mostrarAnalisis(Model model, Authentication auth) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);

        List<Ingreso> ingresos = ingresoRepository.findByUsuario(usuario);
        List<Deuda> deudas = deudaRepository.findByUsuario(usuario);

        double totalIngresos = ingresos.stream()
                .mapToDouble(i -> i.getMonto() != null ? i.getMonto() : 0.0)
                .sum();

        double totalDeudas = deudas.stream()
                .mapToDouble(d -> d.getMonto() != null ? d.getMonto() : 0.0)
                .sum();

        double porcentajeEndeudamiento = totalIngresos > 0
                ? (totalDeudas / totalIngresos) * 100
                : 0;

        // ✅ Formato de monto con separador de miles
        String totalIngresosFormateado = "$" + String.format("%,.2f", totalIngresos);
        String totalDeudasFormateado = "$" + String.format("%,.2f", totalDeudas);

        // ✅ Consejo visual según nivel
        String consejo;
        String tipoConsejo;

        if (porcentajeEndeudamiento < 20) {
            consejo = "✅ Excelente. Estás aprovechando muy bien tus ingresos.";
            tipoConsejo = "success";
        } else if (porcentajeEndeudamiento < 30) {
            consejo = "🟡 Bien hecho. Solo mantén tus deudas controladas.";
            tipoConsejo = "warning";
        } else if (porcentajeEndeudamiento < 40) {
            consejo = "🟠 Cuidado. Tu nivel de deuda está subiendo.";
            tipoConsejo = "danger";
        } else {
            consejo = "🔴 Alerta. Estás muy endeudado. Busca apoyo financiero.";
            tipoConsejo = "dark";
        }

        // Análisis narrativo generado por IA
        String analisisIA = generarAnalisisIA(ingresos, deudas, porcentajeEndeudamiento);

        // ✅ Atributos para Thymeleaf
        model.addAttribute("ingresos", ingresos);
        model.addAttribute("deudas", deudas);
        model.addAttribute("totalIngresos", totalIngresos);
        model.addAttribute("totalIngresosFormateado", totalIngresosFormateado);
        model.addAttribute("totalDeudas", totalDeudas);
        model.addAttribute("totalDeudasFormateado", totalDeudasFormateado);
        model.addAttribute("porcentajeEndeudamiento", Math.round(porcentajeEndeudamiento * 10.0) / 10.0);
        model.addAttribute("consejoFinanciero", consejo);
        model.addAttribute("tipoConsejo", tipoConsejo);
        model.addAttribute("analisisIA", analisisIA);

        return "analisis/analisis";
    }


    @PostMapping("/agregarIngreso")
    public String agregarIngreso(@RequestParam String nombre,
                                 @RequestParam Double monto,
                                 @RequestParam String categoria,
                                 Authentication auth) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);

        Ingreso ingreso = new Ingreso();
        ingreso.setNombre(nombre);
        ingreso.setMonto(monto);
        ingreso.setCategoria(categoria); // Nuevo
        ingreso.setUsuario(usuario);

        ingresoRepository.save(ingreso);
        return "redirect:/analisis";
    }


    @PostMapping("/agregarDeuda")
    public String agregarDeuda(@RequestParam String nombre,
                               @RequestParam Double monto,
                               @RequestParam String categoria,
                               Authentication auth) {
        Usuario usuario = usuarioService.obtenerUsuarioPorAuthentication(auth);

        Deuda deuda = new Deuda();
        deuda.setNombre(nombre);
        deuda.setMonto(monto);
        deuda.setCategoria(categoria); // Nuevo
        deuda.setUsuario(usuario);

        deudaRepository.save(deuda);
        return "redirect:/analisis";
    }


    @GetMapping("/eliminarIngreso")
    public String eliminarIngreso(@RequestParam Long id) {
        ingresoRepository.deleteById(id);
        return "redirect:/analisis";
    }

    @GetMapping("/eliminarDeuda")
    public String eliminarDeuda(@RequestParam Long id) {
        deudaRepository.deleteById(id);
        return "redirect:/analisis";
    }

    private String generarAnalisisIA(List<Ingreso> ingresos, List<Deuda> deudas, double porcentajeEndeudamiento) {
        if (ingresos.isEmpty() && deudas.isEmpty()) {
            return "Registra tus ingresos y deudas para recibir un análisis personalizado de tu situación financiera.";
        }

        StringBuilder detalle = new StringBuilder();
        ingresos.forEach(i -> detalle.append("Ingreso: ").append(i.getNombre())
                .append(" (").append(i.getCategoria()).append(") $").append(i.getMonto()).append(". "));
        deudas.forEach(d -> detalle.append("Deuda: ").append(d.getNombre())
                .append(" (").append(d.getCategoria()).append(") $").append(d.getMonto()).append(". "));

        String prompt = String.format("""
                Eres un asesor financiero experto. Analiza la situación financiera de este usuario:
                %s
                Porcentaje de endeudamiento: %.1f%%

                Proporciona un análisis claro en 3 oraciones en español:
                1. Diagnóstico de su situación actual.
                2. El mayor riesgo o punto a mejorar.
                3. Una acción concreta que puede tomar esta semana.
                Sin asteriscos ni markdown, solo texto plano.
                """, detalle.toString(), porcentajeEndeudamiento);

        return claudeAIService.preguntar(prompt);
    }
}
