package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.*;
import com.healthplus.healthplus.repositorio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PerfilController {

    private final PerfilFisicoRepository repo;
    private final UsuarioRepository usuarios;

    public PerfilController(PerfilFisicoRepository repo, UsuarioRepository usuarios) {
        this.repo = repo;
        this.usuarios = usuarios;
    }

    // Añadido: la página de perfil ahora también muestra los datos de la cuenta
    private void datosCuenta(Long idUsuario, Model model) {
        model.addAttribute("usuario", usuarios.findById(idUsuario).orElse(null));
    }

    @GetMapping("/perfil")
    public String ver(HttpSession session, Model model) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        model.addAttribute("nombre", session.getAttribute("nombre"));
        model.addAttribute("perfil", repo.findByIdUsuario(idUsuario).orElse(null));
        datosCuenta(idUsuario, model);
        return "perfil";
    }

    @PostMapping("/perfil")
    public String guardar(@RequestParam(name = "peso", required = false) Double peso,
                          @RequestParam(name = "estatura", required = false) Double estatura,
                          @RequestParam(name = "edad", required = false) Integer edad,
                          @RequestParam(name = "sexo", required = false) String sexo,
                          HttpSession session,
                          Model model) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        model.addAttribute("nombre", session.getAttribute("nombre"));
        datosCuenta(idUsuario, model);

        boolean datosValidos = peso != null && estatura != null && edad != null
                && sexo != null && !sexo.isBlank()
                && peso >= 20 && peso <= 300
                && estatura >= 100 && estatura <= 250
                && edad >= 10 && edad <= 100;

        if (!datosValidos) {
            model.addAttribute("error", "Revisa los datos: ninguno puede estar vacío y deben estar en un rango válido");
            model.addAttribute("perfil", repo.findByIdUsuario(idUsuario).orElse(null));
            return "perfil";
        }

        PerfilFisico p = repo.findByIdUsuario(idUsuario).orElse(new PerfilFisico());
        p.setIdUsuario(idUsuario);
        p.setPesoKg(peso);
        p.setEstaturaCm(estatura);
        p.setEdad(edad);
        p.setSexo(sexo);
        repo.save(p);

        model.addAttribute("mensaje", "Datos físicos guardados. Tu IMC se actualizó.");
        model.addAttribute("perfil", p);
        return "perfil";
    }
}