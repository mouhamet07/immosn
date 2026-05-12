package sn.immosn.backend.annonce.service.Impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.annonce.data.repository.TypeBienAnnonceRepository;
import sn.immosn.backend.annonce.service.TypeBienAnnonceService;
import sn.immosn.backend.client.web.annonce.dto.TypeBienRequestDto;
import sn.immosn.backend.client.web.annonce.dto.TypeBienResponseDto;

@Service
@RequiredArgsConstructor
@Transactional
public class TypeBienAnnonceServiceImpl implements TypeBienAnnonceService {

    private final TypeBienAnnonceRepository typeBienAnnonceRepository;

    @Override
    public TypeBienResponseDto createTypeBien(TypeBienRequestDto request) {
        if (typeBienAnnonceRepository.existsByLibelleIgnoreCase(request.libelle())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Un type de bien avec le libellé '" + request.libelle() + "' existe déjà"
            );
        }

        TypeBienAnnonce typeBien = TypeBienAnnonce.builder()
                .libelle(request.libelle())
                .build();

        TypeBienAnnonce saved = typeBienAnnonceRepository.save(typeBien);
        return toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TypeBienResponseDto> getAllTypesBien() {
        return typeBienAnnonceRepository.findByIsArchivedFalse()
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TypeBienResponseDto getTypeBienById(Long id) {
        TypeBienAnnonce typeBien = findByIdOrThrow(id);
        return toResponseDto(typeBien);
    }

    @Override
    public TypeBienResponseDto updateTypeBien(Long id, TypeBienRequestDto request) {
        TypeBienAnnonce typeBien = findByIdOrThrow(id);

        if (!typeBien.getLibelle().equalsIgnoreCase(request.libelle())
                && typeBienAnnonceRepository.existsByLibelleIgnoreCase(request.libelle())) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Un type de bien avec le libellé '" + request.libelle() + "' existe déjà"
            );
        }

        typeBien.setLibelle(request.libelle());
        TypeBienAnnonce updated = typeBienAnnonceRepository.save(typeBien);
        return toResponseDto(updated);
    }

    @Override
    public void archiveTypeBien(Long id) {
        TypeBienAnnonce typeBien = findByIdOrThrow(id);

        if (Boolean.TRUE.equals(typeBien.getIsArchived())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Le type de bien avec l'id " + id + " est déjà archivé"
            );
        }

        typeBien.setIsArchived(true);
        typeBienAnnonceRepository.save(typeBien);
    }

   

    private TypeBienAnnonce findByIdOrThrow(Long id) {
        return typeBienAnnonceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Type de bien introuvable avec l'id : " + id
                ));
    }

    private TypeBienResponseDto toResponseDto(TypeBienAnnonce typeBien) {
        return new TypeBienResponseDto(typeBien.getId(), typeBien.getLibelle());
    }
}