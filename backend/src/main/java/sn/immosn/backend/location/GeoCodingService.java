package sn.immosn.backend.location;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GeoCodingService {

    private static final Logger log = LoggerFactory.getLogger(GeoCodingService.class);
    private static final String NOMINATIM_URL =
        "https://nominatim.openstreetmap.org/search?q={q}&format=json&limit=1";
    private static final DateTimeFormatter LOG_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RestTemplate restTemplate;

    /**
     * Géocode une annonce à partir de ses champs de localisation.
     * Retourne [latitude, longitude] ou Optional.empty() si impossible.
     */
    public Optional<double[]> geocode(String quartier, String departement, String adresse) {
        try {
            String query = buildQuery(quartier, departement, adresse);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "ImmoSN/1.0 (contact@immosn.sn)");
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                NOMINATIM_URL,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {},
                Map.of("q", query)
            );

            List<Map<String, Object>> results = response.getBody();
            if (results == null || results.isEmpty()) {
                log.warn("Nominatim: aucun résultat pour '{}'", query);
                return Optional.empty();
            }

            Map<String, Object> first = results.get(0);
            double lat = Double.parseDouble((String) first.get("lat"));
            double lon = Double.parseDouble((String) first.get("lon"));
            return Optional.of(new double[]{lat, lon});

        } catch (Exception e) {
            log.error("Erreur géocodage pour quartier='{}', departement='{}' : {}",
                quartier, departement, e.getMessage());
            return Optional.empty();
        }
    }

    public void logGeolocation(Long annonceId) {
        String timestamp = LocalDateTime.now().format(LOG_FORMATTER);
        log.info("[{}] GEOLOCATION GENERATED ANNONCE:{}", timestamp, annonceId);
    }

    private String buildQuery(String quartier, String departement, String adresse) {
        if (adresse != null && !adresse.isBlank()) {
            return adresse + ", " + quartier + ", " + departement + ", Senegal";
        }
        return quartier + ", " + departement + ", Senegal";
    }
}
