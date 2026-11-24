package com.healthplus.healthplus.controlador;

import com.healthplus.healthplus.modelo.*;
import com.healthplus.healthplus.repositorio.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class MetaController {

    private final MetaRepository repo;

    public MetaController(MetaRepository repo) {
        this.repo = repo;
    }

    private void cargarLista(Long idUsuario, HttpSession session, Model model) {
        model.addAttribute("nombre", session.getAttribute("nombre"));
        model.addAttribute("metas", repo.findByIdUsuarioOrderByFechaFinAsc(idUsuario));
    }

    private String validar(String tipo, Double valor, LocalDate inicio, LocalDate fin) {
        if (tipo == null || tipo.isBlank() || valor == null || inicio == null || fin == null) {
            return "La meta está incompleta";
        }
        if (valor <= 0) {
            return "El valor objetivo debe ser mayor que cero";
        }
        if (fin.isBefore(inicio)) {
            return "La fecha final no puede ser anterior a la inicial";
        }
        return null;
    }

    private Optional<Meta> buscarDelUsuario(Long id, Long idUsuario) {
        return repo.findById(id).filter(m -> m.getIdUsuario().equals(idUsuario));
    }

    @GetMapping("/metas")
    public String ver(HttpSession session, Model model) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        cargarLista(idUsuario, session, model);
        return "metas";
    }

    @PostMapping("/metas")
    public String crear(@RequestParam(name = "tipo", required = false) String tipo,
                        @RequestParam(name = "valor", required = false) Double valor,
                        @RequestParam(name = "fechaInicio", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                        @RequestParam(name = "fechaFin", required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                        HttpSession session, Model model, RedirectAttributes ra) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        String error = validar(tipo, valor, fechaInicio, fechaFin);
        if (error != null) {
            model.addAttribute("mensaje", error);
            cargarLista(idUsuario, session, model);
            return "metas";
        }
        Meta m = new Meta();
        m.setIdUsuario(idUsuario);
        m.setTipo(tipo);
        m.setValorObjetivo(valor);
        m.setFechaInicio(fechaInicio);
        m.setFechaFin(fechaFin);
        m.setEstado("Activa");
        repo.save(m);
        ra.addFlashAttribute("mensaje", "Meta creada");
        return "redirect:/metas";
    }

    @GetMapping("/metas/{id}/editar")
    public String editar(@PathVariable Long id, HttpSession session, Model model) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        cargarLista(idUsuario, session, model);
        Optional<Meta> m = buscarDelUsuario(id, idUsuario);
        if (m.isEmpty()) {
            model.addAttribute("mensaje", "La meta ya no existe");
        } else {
            model.addAttribute("metaEditar", m.get());
        }
        return "metas";
    }

    @PostMapping("/metas/{id}/editar")
    public String guardarEdicion(@PathVariable Long id,
                                 @RequestParam(name = "tipo", required = false) String tipo,
                                 @RequestParam(name = "valor", required = false) Double valor,
                                 @RequestParam(name = "fechaInicio", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
                                 @RequestParam(name = "fechaFin", required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
                                 HttpSession session, Model model, RedirectAttributes ra) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        Optional<Meta> om = buscarDelUsuario(id, idUsuario);
        if (om.isEmpty()) {
            ra.addFlashAttribute("mensaje", "La meta ya no existe");
            return "redirect:/metas";
        }
        String error = validar(tipo, valor, fechaInicio, fechaFin);
        if (error != null) {
            model.addAttribute("mensaje", error);
            model.addAttribute("metaEditar", om.get());
            cargarLista(idUsuario, session, model);
            return "metas";
        }
        Meta m = om.get();
        m.setTipo(tipo);
        m.setValorObjetivo(valor);
        m.setFechaInicio(fechaInicio);
        m.setFechaFin(fechaFin);
        repo.save(m);
        ra.addFlashAttribute("mensaje", "Meta actualizada");
        return "redirect:/metas";
    }

    @PostMapping("/metas/{id}/eliminar")
    public String eliminar(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long idUsuario = (Long) session.getAttribute("idUsuario");
        if (idUsuario == null) {
            return "redirect:/";
        }
        Optional<Meta> om = buscarDelUsuario(id, idUsuario);
        if (om.isEmpty()) {
            ra.addFlashAttribute("mensaje", "La meta ya no existe");
        } else {
            repo.delete(om.get());
            ra.addFlashAttribute("mensaje", "Meta eliminada");
        }
        return "redirect:/metas";
    }
}
