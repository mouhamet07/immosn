package sn.immosn.backend.annonce.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.TypeBienAnnonceRepository;
import sn.immosn.backend.annonce.service.TypeBienAnnonceService;
import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;
import sn.immosn.backend.client.web.annonce.mapper.TypeBienAnnonceMapper;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

@Service
@RequiredArgsConstructor
@Transactional
public class TypeBienAnnonceServiceImpl implements TypeBienAnnonceService {

    private final TypeBienAnnonceRepository typeBienAnnonceRepository;
    private final TypeBienAnnonceMapper typeBienAnnonceMapper;

    @Override
    public TypeBienResponseDto createTypeBien(TypeBienRequestDto request) {
        // Vérifier que le libellé n'existe pas déjà
        if (typeBienAnnonceRepository.existsByLibelleIgnoreCase(request.libelle())) {
            throw new EntityExistException("Un type de bien avec le libellé '" + request.libelle() + "' existe déjà");
        }

        TypeBienAnnonce typeBien = typeBienAnnonceMapper.toEntity(request);
        TypeBienAnnonce saved = typeBienAnnonceRepository.save(typeBien);
        return typeBienAnnonceMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeBienResponseDto> getAllTypesBien() {
        return typeBienAnnonceRepository.findByIsArchivedFalse()
                .stream()
                .map(typeBienAnnonceMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TypeBienResponseDto> getAllTypesBienPaged(Pageable pageable) {
        return typeBienAnnonceRepository.findAll(pageable)
                .map(typeBienAnnonceMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TypeBienResponseDto getTypeBienById(Long id) {
        TypeBienAnnonce typeBien = findActiveByIdOrThrow(id);
        return typeBienAnnonceMapper.toResponseDto(typeBien);
    }

    @Override
    public TypeBienResponseDto updateTypeBien(Long id, TypeBienRequestDto request) {
        TypeBienAnnonce typeBien = findActiveByIdOrThrow(id);

        if (!typeBien.getLibelle().equalsIgnoreCase(request.libelle())
                && typeBienAnnonceRepository.existsByLibelleIgnoreCase(request.libelle())) {
            throw new EntityExistException("Un type de bien avec le libellé '" + request.libelle() + "' existe déjà");
        }

        typeBien.setLibelle(request.libelle());
        TypeBienAnnonce updated = typeBienAnnonceRepository.save(typeBien);
        return typeBienAnnonceMapper.toResponseDto(updated);
    }

    @Override
    public void archiveTypeBien(Long id) {
        TypeBienAnnonce typeBien = findActiveByIdOrThrow(id);
        typeBien.setArchived(true);
        typeBienAnnonceRepository.save(typeBien);
    }

    @Override
    public TypeBienResponseDto restoreTypeBien(Long id) {
        TypeBienAnnonce typeBien = typeBienAnnonceRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Type de bien introuvable : id=" + id));
        if (!typeBien.isArchived()) {
            throw new IllegalStateException("Ce type de bien n'est pas archivé");
        }
        typeBien.setArchived(false);
        return typeBienAnnonceMapper.toResponseDto(typeBienAnnonceRepository.save(typeBien));
    }

    private TypeBienAnnonce findActiveByIdOrThrow(Long id) {
        return typeBienAnnonceRepository.findByIdAndIsArchivedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Type de bien introuvable avec l'id : " + id));
    }

}