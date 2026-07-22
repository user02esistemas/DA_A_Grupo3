package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.Cliente;
import com.arteymetal.ArteyMetal.repository.ClienteRepository;
import com.arteymetal.ArteyMetal.service.ClienteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/clientes")
public class ClienteController {

    @Autowired private ClienteService clienteService;
    @Autowired private ClienteRepository clienteRepository;

    @GetMapping
    public String index(@RequestParam(required = false) String q, Model model) {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Cliente> clientes;
        if (q != null && !q.trim().isEmpty()) {
            clientes = clienteRepository.search(q.trim(), pageable);
        } else {
            clientes = clienteRepository.findAll(pageable);
        }
        model.addAttribute("clientes", clientes);
        model.addAttribute("busqueda", q);
        return "clientes/index";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/create";
    }

    @PostMapping
    public String store(@ModelAttribute Cliente cliente, RedirectAttributes flash, Model model) {
        List<String> errores = new ArrayList<>();

        if (cliente.getNombreCompleto() == null || cliente.getNombreCompleto().trim().isEmpty()) {
            errores.add("El nombre completo es requerido.");
        } else if (cliente.getNombreCompleto().length() > 120) {
            errores.add("El nombre completo no debe exceder 120 caracteres.");
        }

        if (cliente.getTelefono() != null && cliente.getTelefono().length() > 20) {
            errores.add("El telefono no debe exceder 20 caracteres.");
        }

        if (cliente.getCorreo() != null && !cliente.getCorreo().trim().isEmpty()) {
            if (cliente.getCorreo().length() > 120) {
                errores.add("El correo no debe exceder 120 caracteres.");
            } else if (!cliente.getCorreo().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                errores.add("El correo electronico no es valido.");
            }
        }

        if (cliente.getDocumento() != null && !cliente.getDocumento().trim().isEmpty()) {
            if (cliente.getDocumento().length() > 25) {
                errores.add("El documento no debe exceder 25 caracteres.");
            } else if (clienteRepository.existsByDocumento(cliente.getDocumento().trim())) {
                errores.add("El documento ya esta registrado.");
            }
        }

        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            model.addAttribute("cliente", cliente);
            return "clientes/create";
        }

        clienteService.guardar(cliente);
        flash.addFlashAttribute("exito", "Cliente registrado correctamente.");
        return "redirect:/clientes";
    }

    @GetMapping("/{id}")
    public String show(@PathVariable Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("cliente", cliente);
        return "clientes/show";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        model.addAttribute("cliente", cliente);
        return "clientes/edit";
    }

    @PutMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Cliente cliente, RedirectAttributes flash, Model model) {
        Cliente existente = clienteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        List<String> errores = new ArrayList<>();

        if (cliente.getNombreCompleto() == null || cliente.getNombreCompleto().trim().isEmpty()) {
            errores.add("El nombre completo es requerido.");
        } else if (cliente.getNombreCompleto().length() > 120) {
            errores.add("El nombre completo no debe exceder 120 caracteres.");
        }

        if (cliente.getTelefono() != null && cliente.getTelefono().length() > 20) {
            errores.add("El telefono no debe exceder 20 caracteres.");
        }

        if (cliente.getCorreo() != null && !cliente.getCorreo().trim().isEmpty()) {
            if (cliente.getCorreo().length() > 120) {
                errores.add("El correo no debe exceder 120 caracteres.");
            } else if (!cliente.getCorreo().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                errores.add("El correo electronico no es valido.");
            }
        }

        if (cliente.getDocumento() != null && !cliente.getDocumento().trim().isEmpty()) {
            if (cliente.getDocumento().length() > 25) {
                errores.add("El documento no debe exceder 25 caracteres.");
            } else if (!cliente.getDocumento().equals(existente.getDocumento())
                    && clienteRepository.existsByDocumento(cliente.getDocumento().trim())) {
                errores.add("El documento ya esta registrado.");
            }
        }

        if (!errores.isEmpty()) {
            model.addAttribute("errores", errores);
            cliente.setId(id);
            model.addAttribute("cliente", cliente);
            return "clientes/edit";
        }

        clienteService.actualizar(id, cliente);
        flash.addFlashAttribute("exito", "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }

    @DeleteMapping("/{id}")
    public String destroy(@PathVariable Long id, RedirectAttributes flash) {
        clienteService.eliminar(id);
        flash.addFlashAttribute("exito", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }
}
