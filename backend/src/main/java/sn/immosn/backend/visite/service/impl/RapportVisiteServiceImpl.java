package sn.immosn.backend.visite.service.impl;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sn.immosn.backend.auth.data.entity.User;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteCreateRequestDto;
import sn.immosn.backend.client.web.visite.dto.RapportVisiteResponseDto;
import sn.immosn.backend.client.web.visite.mapper.DemandeVisiteMapper;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.shared.service.FileStorageService;
import sn.immosn.backend.visite.data.entity.DemandeVisite;
import sn.immosn.backend.visite.data.entity.RapportVisite;
import sn.immosn.backend.visite.data.entity.StatutDemandeVisite;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;
import sn.immosn.backend.visite.data.repository.RapportVisiteRepository;
import sn.immosn.backend.visite.service.RapportVisiteService;
import sn.immosn.backend.visite.service.VisiteHistoryService;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RapportVisiteServiceImpl implements RapportVisiteService {

    private static final Logger log = LoggerFactory.getLogger(RapportVisiteServiceImpl.class);

    /**
     * États depuis lesquels une visite peut recevoir un rapport.
     * Un admin doit avoir été affecté à la visite au préalable (AFFECTEE) : ACCEPTEE est exclue
     * pour éviter un rapport orphelin sans admin responsable.
     */
    private static final Set<StatutDemandeVisite> ETATS_RAPPORTABLES =
        EnumSet.of(StatutDemandeVisite.AFFECTEE, StatutDemandeVisite.RAPPORT_REDIGE);

    private final RapportVisiteRepository rapportRepository;
    private final DemandeVisiteRepository visiteRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final VisiteHistoryService visiteHistoryService;
    private final DemandeVisiteMapper mapper;

    @Override
    @Transactional
    public RapportVisiteResponseDto creerRapport(Long visiteId, RapportVisiteCreateRequestDto request, String adminEmail) {
        DemandeVisite visite = loadVisite(visiteId);
        if (visite.getAdminResponsable() == null) {
            throw new IllegalStateException(
                "Aucun admin n'est affecté à cette visite : affectez un admin avant de rédiger le rapport.");
        }
        if (!ETATS_RAPPORTABLES.contains(visite.getStatut())) {
            throw new IllegalStateException(
                "Un rapport ne peut être rédigé que pour une visite AFFECTEE. "
                + "Statut actuel : " + visite.getStatut());
        }
        if (rapportRepository.existsByDemandeVisiteId(visiteId)) {
            throw new IllegalStateException("Un rapport existe déjà pour cette visite #" + visiteId);
        }

        User auteur = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new EntityNotFoundException("Administrateur non trouvé : " + adminEmail));
        if (!visite.getAdminResponsable().getId().equals(auteur.getId())) {
            throw new IllegalStateException(
                "Seul l'admin affecté à cette visite (" + visite.getAdminResponsable().getEmail()
                + ") peut rédiger le rapport.");
        }

        RapportVisite rapport = RapportVisite.builder()
            .demandeVisite(visite)
            .auteurAdmin(auteur)
            .compteRendu(request.compteRendu())
            .aboutie(Boolean.TRUE.equals(request.aboutie()))
            .build();
        RapportVisite saved = rapportRepository.save(rapport);

        StatutDemandeVisite ancien = visite.getStatut();
        visite.setStatut(StatutDemandeVisite.RAPPORT_REDIGE);
        visiteRepository.save(visite);
        visiteHistoryService.record(visite, ancien, StatutDemandeVisite.RAPPORT_REDIGE,
            "RAPPORT_REDIGE", request.compteRendu(), null, null);

        log.info("Rapport #{} créé pour visite #{} (auteur={}, aboutie={})",
            saved.getId(), visiteId, adminEmail, saved.isAboutie());
        return mapper.toRapportDto(saved);
    }

    @Override
    @Transactional
    public RapportVisiteResponseDto uploadDocument(Long visiteId, MultipartFile file) {
        RapportVisite rapport = rapportRepository.findByDemandeVisiteId(visiteId)
            .orElseThrow(() -> new EntityNotFoundException("Aucun rapport pour la visite #" + visiteId));
        String existingUrl = rapport.getDocumentUrl();
        if (existingUrl != null && existingUrl.startsWith("/uploads/")) {
            fileStorageService.delete(existingUrl);
        }
        String url = fileStorageService.store(file, "rapports");
        rapport.setDocumentUrl(url);
        RapportVisite saved = rapportRepository.save(rapport);
        log.info("Document uploadé pour rapport de visite #{} : {}", visiteId, url);
        return mapper.toRapportDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public RapportVisiteResponseDto consulterRapport(Long visiteId) {
        RapportVisite rapport = rapportRepository.findByDemandeVisiteId(visiteId)
            .orElseThrow(() -> new EntityNotFoundException("Aucun rapport pour la visite #" + visiteId));
        return mapper.toRapportDto(rapport);
    }

    private DemandeVisite loadVisite(Long id) {
        return visiteRepository.findByIdAndIsArchivedFalse(id)
            .orElseThrow(() -> new EntityNotFoundException("Demande de visite non trouvée avec l'ID: " + id));
    }
}
