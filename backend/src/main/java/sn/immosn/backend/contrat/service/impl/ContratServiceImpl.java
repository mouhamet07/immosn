package sn.immosn.backend.contrat.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.repository.AnnonceRepository;
import sn.immosn.backend.auth.data.repository.UserRepository;
import sn.immosn.backend.client.web.contrat.dto.*;
import sn.immosn.backend.client.web.contrat.mapper.ContratMapper;
import sn.immosn.backend.contrat.data.entity.Contrat;
import sn.immosn.backend.contrat.data.entity.StatutContrat;
import sn.immosn.backend.contrat.data.repository.ContratRepository;
import sn.immosn.backend.contrat.service.ContratService;
import sn.immosn.backend.lead.data.entity.StatutLead;
import sn.immosn.backend.lead.data.repository.LeadRepository;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
public class ContratServiceImpl implements ContratService {

    private final ContratRepository contratRepository;
    private final UserRepository userRepository;
    private final AnnonceRepository annonceRepository;
    private final LeadRepository leadRepository;
    private final ContratMapper mapper;

    @Override
    @Transactional
    public ContratResponseDto create(ContratCreateRequestDto request) {
        var client  = userRepository.findById(request.clientId())
            .orElseThrow(() -> new EntityNotFoundException("Client non trouvé"));
        var annonce = annonceRepository.findByIdAndIsArchivedFalse(request.annonceId())
            .orElseThrow(() -> new EntityNotFoundException("Annonce non trouvée"));

        Contrat.ContratBuilder builder = Contrat.builder()
            .client(client)
            .annonce(annonce)
            .dateDebut(request.dateDebut())
            .dateFin(request.dateFin())
            .montant(request.montant())
            .documentUrl(request.documentUrl())
            .notes(request.notes());

        if (request.leadId() != null) {
            var lead = leadRepository.findById(request.leadId())
                .orElseThrow(() -> new EntityNotFoundException("Lead non trouvé"));
            builder.lead(lead);
            lead.setStatut(StatutLead.CONVERTI);
            leadRepository.save(lead);
        }

        return mapper.toDto(contratRepository.save(builder.build()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratResponseDto> getClientContrats(String clientEmail, StatutContrat statut, Pageable pageable) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        Page<Contrat> page = statut != null
            ? contratRepository.findByClientIdAndStatutOrderByCreatedAtDesc(client.getId(), statut, pageable)
            : contratRepository.findByClientIdOrderByCreatedAtDesc(client.getId(), pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratResponseDto> getAllContrats(StatutContrat statut, Pageable pageable) {
        Page<Contrat> page = statut != null
            ? contratRepository.findByStatutOrderByCreatedAtDesc(statut, pageable)
            : contratRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(mapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratResponseDto getById(Long id, String userEmail, boolean isAdmin) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé avec l'ID: " + id));
        if (!isAdmin && !contrat.getClient().getEmail().equals(userEmail)) {
            throw new EntityNotFoundException("Contrat non trouvé");
        }
        return mapper.toDto(contrat);
    }

    @Override
    @Transactional
    public ContratResponseDto update(Long id, ContratUpdateRequestDto request) {
        Contrat contrat = contratRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé avec l'ID: " + id));
        if (request.dateDebut()   != null) contrat.setDateDebut(request.dateDebut());
        if (request.dateFin()     != null) contrat.setDateFin(request.dateFin());
        if (request.montant()     != null) contrat.setMontant(request.montant());
        if (request.statut()      != null) contrat.setStatut(request.statut());
        if (request.documentUrl() != null) contrat.setDocumentUrl(request.documentUrl());
        if (request.notes()       != null) contrat.setNotes(request.notes());
        return mapper.toDto(contratRepository.save(contrat));
    }

    @Override
    @Transactional
    public ContratResponseDto demanderResiliation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        contrat.setStatut(StatutContrat.RESILIE);
        if (dto.motif() != null) contrat.setNotes("Résiliation : " + dto.motif());
        return mapper.toDto(contratRepository.save(contrat));
    }

    @Override
    @Transactional
    public ContratResponseDto demanderProlongation(Long id, ContratActionDto dto, String clientEmail) {
        Contrat contrat = loadClientContrat(id, clientEmail);
        if (dto.nouvelleDate() != null) contrat.setDateFin(dto.nouvelleDate());
        if (dto.motif() != null) contrat.setNotes("Prolongation demandée : " + dto.motif());
        return mapper.toDto(contratRepository.save(contrat));
    }

    private Contrat loadClientContrat(Long id, String clientEmail) {
        var client = userRepository.findByEmail(clientEmail)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé"));
        return contratRepository.findByIdAndClientId(id, client.getId())
            .orElseThrow(() -> new EntityNotFoundException("Contrat non trouvé"));
    }
}
