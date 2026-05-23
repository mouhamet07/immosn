package sn.immosn.backend.annonce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.annonce.data.repository.CommoditeRepository;
import sn.immosn.backend.annonce.service.CommoditeService;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;
import sn.immosn.backend.client.web.annonce.mapper.CommoditeMapper;
import sn.immosn.backend.shared.exception.EntityExistException;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommoditeServiceImpl implements CommoditeService {

    private final CommoditeRepository commoditeRepository;
    private final CommoditeMapper commoditeMapper;

    @Override
    public CommoditeResponseDto createCommodite(CommoditeRequestDto requestDto) {
        // Fix : était EntityNotFoundException (404) au lieu de EntityExistException (409)
        if (commoditeRepository.existsByLibelleIgnoreCase(requestDto.libelle())) {
            throw new EntityExistException("Une commodité avec le libellé '" + requestDto.libelle() + "' existe déjà");
        }
        Commodite commodite = commoditeMapper.toEntity(requestDto);
        return commoditeMapper.toResponse(commoditeRepository.save(commodite));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommoditeResponseDto> getAllCommodites() {
        return commoditeRepository.findByIsArchivedFalse().stream()
                .map(commoditeMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CommoditeResponseDto> getAllCommoditesPaged(Pageable pageable) {
        // Fix : affiche toutes les commodités (actives + archivées) pour la vue admin paginée
        return commoditeRepository.findAll(pageable).map(commoditeMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CommoditeResponseDto getCommoditeById(Long id) {
        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité introuvable : id=" + id));
        if (commodite.isArchived()) {
            throw new EntityNotFoundException("Cette commodité n'est plus disponible");
        }
        return commoditeMapper.toResponse(commodite);
    }

    @Override
    public CommoditeResponseDto updateCommodite(Long id, CommoditeRequestDto requestDto) {
        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité introuvable : id=" + id));
        if (commodite.isArchived()) {
            throw new IllegalStateException("Impossible de modifier une commodité archivée");
        }
        if (requestDto.libelle() != null
                && !commodite.getLibelle().equalsIgnoreCase(requestDto.libelle())) {
            // Fix : était EntityNotFoundException (404) au lieu de EntityExistException (409)
            if (commoditeRepository.existsByLibelleIgnoreCase(requestDto.libelle())) {
                throw new EntityExistException("Une commodité avec le libellé '" + requestDto.libelle() + "' existe déjà");
            }
            commodite.setLibelle(requestDto.libelle());
        }
        return commoditeMapper.toResponse(commoditeRepository.save(commodite));
    }

    @Override
    public void archiveCommodite(Long id) {
        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité introuvable : id=" + id));
        if (commodite.isArchived()) {
            throw new IllegalStateException("Cette commodité est déjà archivée");
        }
        commodite.setArchived(true);
        commoditeRepository.save(commodite);
    }

    @Override
    public CommoditeResponseDto restoreCommodite(Long id) {
        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité introuvable : id=" + id));
        if (!commodite.isArchived()) {
            throw new IllegalStateException("Cette commodité n'est pas archivée");
        }
        commodite.setArchived(false);
        return commoditeMapper.toResponse(commoditeRepository.save(commodite));
    }
}
