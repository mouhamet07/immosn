package sn.immosn.backend.location;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sn.immosn.backend.shared.response.RestResponse;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService   locationService;
    private final GeoCodingService  geoCodingService;

    @GetMapping("/departements")
    public ResponseEntity<RestResponse<List<String>>> getDepartements() {
        return ResponseEntity.ok(
            RestResponse.success(locationService.getDepartements(), HttpStatus.OK)
        );
    }

    @GetMapping("/quartiers")
    public ResponseEntity<RestResponse<List<String>>> getQuartiers(
            @RequestParam String departement) {
        return ResponseEntity.ok(
            RestResponse.success(locationService.getQuartiersByDepartement(departement), HttpStatus.OK)
        );
    }

    @GetMapping("/geocode")
    public ResponseEntity<RestResponse<GeocodeResponseDto>> geocode(
            @RequestParam String departement,
            @RequestParam String quartier,
            @RequestParam(required = false) String adresse) {
        double[] coords = geoCodingService.geocode(quartier, departement, adresse).orElse(null);
        GeocodeResponseDto result = coords != null
            ? new GeocodeResponseDto(coords[0], coords[1])
            : new GeocodeResponseDto(null, null);
        return ResponseEntity.ok(RestResponse.success(result, HttpStatus.OK));
    }
}
