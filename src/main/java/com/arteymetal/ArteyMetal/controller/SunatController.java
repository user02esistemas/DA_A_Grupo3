package com.arteymetal.ArteyMetal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/sunat")
public class SunatController {

    @Value("${decolecta.base-url:https://api.decolecta.com}")
    private String baseUrl;

    @Value("${decolecta.api-key:}")
    private String apiKey;

    @Value("${decolecta.sunat-ruc-path:/v1/sunat/ruc}")
    private String sunatRucPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/consultar-ruc")
    public Map<String, Object> consultarRuc(@RequestParam String numero) {
        Map<String, Object> resp = new LinkedHashMap<>();

        if (numero == null || !numero.matches("^[0-9]{11}$")) {
            resp.put("message", "El RUC debe tener exactamente 11 digitos.");
            resp.put("status", 422);
            return resp;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            resp.put("message", "No hay API key configurada para Decolecta.");
            resp.put("status", 500);
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
                resp.put("message", "No se pudo consultar SUNAT en este momento.");
                resp.put("status", response.getStatusCode().value());
                try {
                    JsonNode errBody = objectMapper.readTree(response.getBody());
                    resp.put("error", errBody.has("message") ? errBody.get("message").asText(null) : null);
                } catch (Exception ignored) {
                    resp.put("error", null);
                }
                return resp;
            }

            JsonNode data = objectMapper.readTree(response.getBody());
            Map<String, Object> result = objectMapper.convertValue(data, LinkedHashMap.class);
            return result;

        } catch (Exception e) {
            resp.put("message", "Error al consultar SUNAT: " + e.getMessage());
            resp.put("status", 500);
            return resp;
        }
    }
}
