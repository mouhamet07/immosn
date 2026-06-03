package sn.immosn.backend.location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import sn.immosn.backend.shared.exception.EntityNotFoundException;

@Service
public class LocationService {

    private static final Map<String, List<String>> DEPARTEMENTS_QUARTIERS = Map.ofEntries(
        Map.entry("Dakar", List.of(
            "Almadies", "Biscuiterie", "Fann", "Grand Dakar", "Gueule Tapée",
            "Liberté 6", "Mamelles", "Médina", "Mermoz", "Ngor",
            "Ouakam", "Plateau", "Point E", "Sacré-Cœur", "Yoff"
        )),
        Map.entry("Pikine", List.of(
            "Diamaguène", "Guinaw Rails", "Pikine Est",
            "Pikine Ouest", "Tally Boubess", "Thiaroye", "Yeumbeul"
        )),
        Map.entry("Guédiawaye", List.of(
            "Golf Sud", "Hamo", "Ndiarème Limamoulaye",
            "Sam Notaire", "Wakhinane Nimzatt"
        )),
        Map.entry("Rufisque", List.of(
            "Bargny", "Diamniadio", "Rufisque Est",
            "Rufisque Nord", "Rufisque Ouest", "Sébikotane"
        )),
        Map.entry("Keur Massar", List.of(
            "Jaxaay-Parcelles", "Keur Massar Nord", "Keur Massar Sud",
            "Malika", "Niakoul Rap", "Tivaouane Peulh"
        ))
    );

    public List<String> getDepartements() {
        List<String> list = new ArrayList<>(DEPARTEMENTS_QUARTIERS.keySet());
        list.sort(Comparator.naturalOrder());
        return list;
    }

    public List<String> getQuartiersByDepartement(String departement) {
        List<String> quartiers = DEPARTEMENTS_QUARTIERS.get(departement);
        if (quartiers == null) {
            throw new EntityNotFoundException("Département non trouvé : " + departement);
        }
        return quartiers;
    }
}
