package sn.immosn.backend.prospect.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.auth.data.entity.Role;
import sn.immosn.backend.auth.data.entity.RoleType;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.RoleRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.prospect.dto.ConversionResultDto;
import sn.immosn.backend.discussion.data.entity.Discussion;
import sn.immosn.backend.discussion.data.repository.DiscussionRepository;
import sn.immosn.backend.prospect.data.entity.Prospect;
import sn.immosn.backend.prospect.data.repository.ProspectRepository;
import sn.immosn.backend.prospect.service.ProspectConversionService;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.NotificationService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProspectConversionServiceImpl implements ProspectConversionService {

    private static final Logger log = LoggerFactory.getLogger(ProspectConversionServiceImpl.class);

    // Jeux de caractères pour garantir un mot de passe respectant les règles de complexité usuelles.
    private static final String MAJ = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MIN = "abcdefghijkmnpqrstuvwxyz";
    private static final String CHIFFRES = "23456789";
    private static final String SPECIAUX = "!@#$%&*?";
    private static final String TOUS = MAJ + MIN + CHIFFRES + SPECIAUX;
    private static final int LONGUEUR = 14;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ProspectRepository prospectRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DiscussionRepository discussionRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ConversionResultDto convertirProspect(Long prospectId) {
        Prospect prospect = prospectRepository.findById(prospectId)
            .orElseThrow(() -> new EntityNotFoundException("Prospect non trouvé : id=" + prospectId));

        // Idempotence : un prospect déjà converti renvoie son compte sans rien recréer.
        if (prospect.getConvertedUser() != null) {
            User existing = prospect.getConvertedUser();
            log.info("Prospect #{} déjà converti vers User #{}", prospectId, existing.getId());
            return new ConversionResultDto(
                prospectId, existing.getId(), existing.getEmail(), existing.getNomComplet(),
                existing.isCompteGenereAuto(), existing.isMotDePasseAChanger(),
                true, 0, 0);
        }

        // Le prospect ne passe pas par l'inscription classique : on refuse seulement si un compte
        // existe déjà pour cet email (collision avec un utilisateur existant).
        if (userRepository.existsByEmail(prospect.getEmail())) {
            throw new EntityExistException(
                "Un compte existe déjà avec l'email " + prospect.getEmail()
                + " : conversion impossible (rattacher manuellement si nécessaire).");
        }

        // 1 + 2 + 3 : créer le User CLIENT avec un mot de passe temporaire sécurisé encodé BCrypt
        String motDePasseTemporaire = genererMotDePasseTemporaire();
        User user = new User();
        user.setNomComplet(nomComplet(prospect));
        user.setEmail(prospect.getEmail());
        user.setTelephone(prospect.getTelephone());
        user.setAdresse(prospect.getAdresse());
        user.setPassword(passwordEncoder.encode(motDePasseTemporaire));
        user.setArchived(false);
        // 4 : marqueurs de compte généré et de changement de mot de passe obligatoire
        user.setCompteGenereAuto(true);
        user.setMotDePasseAChanger(true);
        user.getRoles().add(loadRole(RoleType.CLIENT));
        User saved = userRepository.save(user);

        // 5 : rattacher discussions et visites du prospect au nouveau compte
        List<Discussion> discussions = discussionRepository.findByProspectId(prospectId);
        discussions.forEach(d -> d.setClient(saved));
        discussionRepository.saveAll(discussions);

        List<DemandeVisite> visites = visiteRepository.findByProspectId(prospectId);
        visites.forEach(v -> v.setClient(saved));
        visiteRepository.saveAll(visites);

        prospect.setConvertedUser(saved);
        prospectRepository.save(prospect);

        // Transmission des identifiants (canal réel non encore branché — voir NotificationService)
        notificationService.sendCredentials(saved, motDePasseTemporaire);

        log.info("Prospect #{} converti → User #{} (CLIENT, compte généré). {} discussion(s) et {} visite(s) rattachées.",
            prospectId, saved.getId(), discussions.size(), visites.size());

        return new ConversionResultDto(
            prospectId, saved.getId(), saved.getEmail(), saved.getNomComplet(),
            true, true, false, discussions.size(), visites.size());
    }

    private String nomComplet(Prospect p) {
        String prenom = p.getPrenom() != null && !p.getPrenom().isBlank() ? p.getPrenom().trim() + " " : "";
        return (prenom + (p.getNom() != null ? p.getNom() : "")).trim();
    }

    /**
     * Génère un mot de passe temporaire : au moins une majuscule, une minuscule, un chiffre
     * et un caractère spécial, complété aléatoirement, puis mélangé.
     */
    private String genererMotDePasseTemporaire() {
        StringBuilder sb = new StringBuilder(LONGUEUR);
        sb.append(MAJ.charAt(RANDOM.nextInt(MAJ.length())));
        sb.append(MIN.charAt(RANDOM.nextInt(MIN.length())));
        sb.append(CHIFFRES.charAt(RANDOM.nextInt(CHIFFRES.length())));
        sb.append(SPECIAUX.charAt(RANDOM.nextInt(SPECIAUX.length())));
        for (int i = sb.length(); i < LONGUEUR; i++) {
            sb.append(TOUS.charAt(RANDOM.nextInt(TOUS.length())));
        }
        // Mélange Fisher–Yates pour ne pas figer la position des caractères garantis
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private Role loadRole(RoleType type) {
        return roleRepository.findByRole(type)
            .orElseThrow(() -> new EntityNotFoundException("Rôle introuvable : " + type));
    }
}
