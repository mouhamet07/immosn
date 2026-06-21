package sn.immosn.backend.proprietaire.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.TypeTransaction;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.mapper.AnnonceMapper;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireCreateRequestDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireResponseDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireStatsDto;
import sn.immosn.backend.client.web.proprietaire.dto.ProprietaireUpdateRequestDto;
import sn.immosn.backend.client.web.proprietaire.mapper.ProprietaireMapper;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.proprietaire.data.entity.Proprietaire;
import sn.immosn.backend.proprietaire.data.repository.ProprietaireRepository;
import sn.immosn.backend.proprietaire.service.ProprietaireService;
import sn.immosn.backend.shared.exception.EntityNotFoundException;
import sn.immosn.backend.visite.data.repository.DemandeVisiteRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProprietaireServiceImpl implements ProprietaireService {

    private final ProprietaireRepository proprietaireRepository;
    private final AnnonceRepository annonceRepository;
    private final DemandeVisiteRepository demandeVisiteRepository;
    private final ContratRepository contratRepository;
    private final ProprietaireMapper proprietaireMapper;
    private final AnnonceMapper annonceMapper;

    @Override
    @Transactional
    public ProprietaireResponseDto create(ProprietaireCreateRequestDto request) {
        Proprietaire proprietaire = proprietaireMapper.toEntity(request);
        return proprietaireMapper.toResponse(proprietaireRepository.save(proprietaire));
    }

    @Override
    @Transactional
    public ProprietaireResponseDto update(Long id, ProprietaireUpdateRequestDto request) {
        Proprietaire proprietaire = proprietaireRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Propriétaire non trouvé : id=" + id));

        if (request.nomComplet() != null) proprietaire.setNomComplet(request.nomComplet());
        if (request.telephone() != null) proprietaire.setTelephone(request.telephone());
        if (request.email() != null) proprietaire.setEmail(request.email());
        if (request.adresse() != null) proprietaire.setAdresse(request.adresse());
        if (request.notes() != null) proprietaire.setNotes(request.notes());

        return proprietaireMapper.toResponse(proprietaireRepository.save(proprietaire));
    }

    @Override
    @Transactional(readOnly = true)
    public ProprietaireResponseDto getById(Long id) {
        Proprietaire proprietaire = proprietaireRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Propriétaire non trouvé : id=" + id));
        return proprietaireMapper.toResponse(proprietaire);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProprietaireResponseDto> getAll(Pageable pageable) {
        return proprietaireRepository.findAllByOrderByNomCompletAsc(pageable).map(proprietaireMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProprietaireStatsDto getStats(Long id) {
        if (!proprietaireRepository.existsById(id)) {
            throw new EntityNotFoundException("Propriétaire non trouvé : id=" + id);
        }
        long totalBiens = annonceRepository.countByProprietaireId(id);
        long biensVente = annonceRepository.countByProprietaireIdAndTypeTransaction(id, TypeTransaction.VENTE);
        long biensLocation = annonceRepository.countByProprietaireIdAndTypeTransaction(id, TypeTransaction.LOCATION);
        long annoncesActives = annonceRepository.countByProprietaireIdAndIsArchivedFalse(id);
        long visites = demandeVisiteRepository.countByAnnonce_ProprietaireId(id);
        long contrats = contratRepository.countByAnnonce_ProprietaireId(id);
        BigDecimal revenus = contratRepository.sumMontantByProprietaireIdAndStatutActifOuExpire(id);

        return new ProprietaireStatsDto(totalBiens, biensVente, biensLocation, annoncesActives, visites, contrats, revenus);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnnonceListDto> getBiens(Long id, Pageable pageable) {
        if (!proprietaireRepository.existsById(id)) {
            throw new EntityNotFoundException("Propriétaire non trouvé : id=" + id);
        }
        return annonceRepository.findByProprietaireIdOrderByCreatedAtDesc(id, pageable).map(annonceMapper::toListDto);
    }
}
