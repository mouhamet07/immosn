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
            "Diamaguène", "Guinaw Rails", "Keur Massar", "Pikine Est",
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
        Map.entry("Thiès", List.of(
            "Khombole", "Mbour", "Thiès Est", "Thiès Nord",
            "Thiès Ouest", "Tivaouane"
        )),
        Map.entry("Saint-Louis", List.of(
            "Dagana", "Matam", "Podor",
            "Saint-Louis Nord", "Saint-Louis Sud"
        )),
        Map.entry("Ziguinchor", List.of(
            "Bignona", "Oussouye", "Ziguinchor Centre"
        )),
        Map.entry("Kaolack", List.of(
            "Fatick", "Guinguinéo", "Kaolack Centre", "Nioro du Rip"
        )),
        Map.entry("Diourbel", List.of(
            "Bambey", "Diourbel Centre", "Mbacké", "Touba"
        )),
        Map.entry("Louga", List.of(
            "Kébémer", "Linguère", "Louga Centre"
        )),
        Map.entry("Tambacounda", List.of(
            "Bakel", "Goudiry", "Tambacounda Centre", "Vélingara"
        )),
        Map.entry("Kolda", List.of(
            "Kolda Centre", "Médina Yoro Fula", "Vélingara Sud"
        )),
        Map.entry("Fatick", List.of(
            "Fatick Centre", "Foundiougne", "Gossas"
        )),
        Map.entry("Matam", List.of(
            "Kanel", "Matam Centre", "Ranérou"
        )),
        Map.entry("Kaffrine", List.of(
            "Birkilane", "Kaffrine Centre", "Koungheul"
        )),
        Map.entry("Kédougou", List.of(
            "Kédougou Centre", "Saraya", "Salemata"
        )),
        Map.entry("Sédhiou", List.of(
            "Bounkiling", "Goudomp", "Sédhiou Centre"
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
