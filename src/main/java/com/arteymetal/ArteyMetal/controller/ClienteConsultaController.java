package com.arteymetal.ArteyMetal.controller;

import com.arteymetal.ArteyMetal.entity.Cliente;
import com.arteymetal.ArteyMetal.repository.ClienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/cliente-consulta")
public class ClienteConsultaController {

    @Autowired private ClienteRepository clienteRepository;

    @Value("${decolecta.base-url:https://api.decolecta.com}")
    private String baseUrl;

    @Value("${decolecta.api-key:}")
    private String apiKey;

    @Value("${decolecta.reniec-dni-path:/v1/reniec/dni}")
    private String reniecDniPath;

    @Value("${decolecta.sunat-ruc-path:/v1/sunat/ruc}")
    private String sunatRucPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/consultar-por-documento")
    public Map<String, Object> consultarPorDocumento(@RequestParam String numero) {
        String limpio = numero.replaceAll("\\D+", "").trim();

        if (limpio.length() != 8 && limpio.length() != 11) {
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", false);
            resp.put("fuente", "manual");
            resp.put("message", "El documento debe tener 8 digitos (DNI) o 11 digitos (RUC).");
            resp.put("cliente", null);
            return resp;
        }

        String tipo = limpio.length() == 11 ? "ruc" : "dni";

        Optional<Cliente> clienteLocal = clienteRepository.findByDocumento(limpio);
        if (clienteLocal.isPresent()) {
            Cliente c = clienteLocal.get();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("ok", true);
            resp.put("fuente", "local");
            resp.put("tipo", tipo);
            resp.put("message", "Cliente encontrado en el sistema.");
            Map<String, Object> clienteData = new LinkedHashMap<>();
            clienteData.put("id", c.getId());
            clienteData.put("nombre", c.getNombreCompleto());
            clienteData.put("documento", c.getDocumento());
            clienteData.put("telefono", c.getTelefono());
            clienteData.put("correo", c.getCorreo());
            clienteData.put("direccion", c.getDireccion());
            resp.put("cliente", clienteData);
            return resp;
        }

        if ("dni".equals(tipo)) {
            return consultarDniReniec(limpio);
        } else {
            return consultarRucSunat(limpio);
        }
    }

    private Map<String, Object> consultarDniReniec(String numero) {
        Map<String, Object> resp = new LinkedHashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            resp.put("ok", false);
            resp.put("fuente", "manual");
            resp.put("tipo", "dni");
            resp.put("message", "No hay API key configurada para consulta RENIEC.");
            resp.put("cliente", null);
            return resp;
        }

        try {
            String url = baseUrl + reniecDniPath + "?numero=" + numero + "&token=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                int status = response.getStatusCode().value();
                String mensaje = status == 401
                    ? "RENIEC rechazo la autenticacion (token invalido o expirado)."
                    : "No se pudo consultar RENIEC.";
                resp.put("ok", false);
                resp.put("fuente", "manual");
                resp.put("tipo", "dni");
                resp.put("message", mensaje + " Completa los datos manualmente.");
                resp.put("status", status);
                resp.put("cliente", null);
                return resp;
            }

            JsonNode data = objectMapper.readTree(response.getBody());
            String fullName = data.has("full_name") ? data.get("full_name").asText(null) : null;
            if (fullName == null) {
                String ape1 = data.has("first_last_name") ? data.get("first_last_name").asText("") : "";
                String ape2 = data.has("second_last_name") ? data.get("second_last_name").asText("") : "";
                String nom = data.has("first_name") ? data.get("first_name").asText("") : "";
                fullName = (ape1 + " " + ape2 + " " + nom).trim();
            }
            String nombreLimpio = fullName.isEmpty() ? null : fullName.replaceAll("\\s+", " ");

            resp.put("ok", true);
            resp.put("fuente", "reniec");
            resp.put("tipo", "dni");
            resp.put("message", "DNI encontrado en RENIEC. Datos cargados para completar el pedido.");
            Map<String, Object> clienteData = new LinkedHashMap<>();
            clienteData.put("id", null);
            clienteData.put("nombre", nombreLimpio);
            clienteData.put("documento", data.has("numero_documento") ? data.get("numero_documento").asText(null)
                : (data.has("document_number") ? data.get("document_number").asText(null) : numero));
            clienteData.put("telefono", null);
            clienteData.put("correo", null);
            clienteData.put("direccion", null);
            resp.put("cliente", clienteData);
            return resp;

        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("fuente", "manual");
            resp.put("tipo", "dni");
            resp.put("message", "Error al consultar RENIEC: " + e.getMessage());
            resp.put("cliente", null);
            return resp;
        }
    }

    private Map<String, Object> consultarRucSunat(String numero) {
        Map<String, Object> resp = new LinkedHashMap<>();

        if (apiKey == null || apiKey.isEmpty()) {
            resp.put("ok", false);
            resp.put("fuente", "manual");
            resp.put("tipo", "ruc");
            resp.put("message", "No hay API key configurada para consulta SUNAT.");
            resp.put("cliente", null);
            return resp;
        }

        try {
            String url = baseUrl + sunatRucPath + "?numero=" + numero + "&token=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                int status = response.getStatusCode().value();
                String mensaje = status == 401
                    ? "SUNAT rechazo la autenticacion (token invalido o expirado)."
                    : "No se pudo consultar SUNAT.";
                resp.put("ok", false);
                resp.put("fuente", "manual");
                resp.put("tipo", "ruc");
                resp.put("message", mensaje + " Completa los datos manualmente.");
                resp.put("status", status);
                resp.put("cliente", null);
                return resp;
            }

            JsonNode data = objectMapper.readTree(response.getBody());
            String direccion = data.has("direccion") ? data.get("direccion").asText("").trim() : "";

            resp.put("ok", true);
            resp.put("fuente", "sunat");
            resp.put("tipo", "ruc");
            resp.put("message", "RUC encontrado en SUNAT. Datos cargados para completar el pedido.");
            Map<String, Object> clienteData = new LinkedHashMap<>();
            clienteData.put("id", null);
            clienteData.put("nombre", data.has("razon_social") ? data.get("razon_social").asText(null) : null);
            clienteData.put("documento", data.has("numero_documento") ? data.get("numero_documento").asText(null) : numero);
            clienteData.put("telefono", null);
            clienteData.put("correo", null);
            clienteData.put("direccion", direccion.isEmpty() ? null : direccion);
            clienteData.put("distrito", data.has("distrito") ? data.get("distrito").asText(null) : null);
            clienteData.put("estado", data.has("estado") ? data.get("estado").asText(null) : null);
            clienteData.put("condicion", data.has("condicion") ? data.get("condicion").asText(null) : null);
            resp.put("cliente", clienteData);
            return resp;

        } catch (Exception e) {
            resp.put("ok", false);
            resp.put("fuente", "manual");
            resp.put("tipo", "ruc");
            resp.put("message", "Error al consultar SUNAT: " + e.getMessage());
            resp.put("cliente", null);
            return resp;
        }
    }
}
