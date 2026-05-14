package sn.immosn.backend.annonce.service.Impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.annonce.data.repository.CommoditeRepository;
import sn.immosn.backend.annonce.service.CommoditeService;
import sn.immosn.backend.client.web.annonce.dto.CommoditeRequestDto;
import sn.immosn.backend.client.web.annonce.dto.CommoditeResponseDto;
import sn.immosn.backend.client.web.annonce.mapper.CommoditeMapper;
import sn.immosn.backend.shared.exception.EntityNotFoundException;

import java.util.List;

@Service
@RequiredArgsConstructor

public class CommoditeServiceImpl implements CommoditeService {

    private final CommoditeRepository commoditeRepository;
    private final CommoditeMapper commoditeMapper;

    @Override
    public CommoditeResponseDto createCommodite(CommoditeRequestDto requestDto) {

        if (commoditeRepository.existsByLibelleIgnoreCase(requestDto.libelle())) {
            throw new EntityNotFoundException("Une commodité avec ce libellé existe déjà.");
        }

        Commodite commodite = commoditeMapper.toEntity(requestDto);
        Commodite savedCommodite = commoditeRepository.save(commodite);
        return commoditeMapper.toResponse(savedCommodite);
    }

    @Override
    public List<CommoditeResponseDto> getAllCommodites() {
        List<Commodite> commodites = commoditeRepository.findByIsArchivedFalse();
        return commodites.stream()
                .map(commoditeMapper::toResponse)
                .toList();
    }


    @Override
    public Page<CommoditeResponseDto> getAllCommoditesPaged(Pageable pageable) {
        return commoditeRepository
                .findByIsArchivedFalse(pageable)
                .map(commoditeMapper::toResponse);
    }

    @Override
    public CommoditeResponseDto getCommoditeById(Long id) {

        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité non trouvée avec l'ID: " + id));

        if (commodite.isArchived()) {
            throw new EntityNotFoundException("Cette commodité n'est plus disponible");
        }

        return commoditeMapper.toResponse(commodite);
    }

    @Override
    public CommoditeResponseDto updateCommodite(Long id, CommoditeRequestDto requestDto) {

        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité non trouvée avec l'ID: " + id));

        if (commodite.isArchived()) {
            throw new EntityNotFoundException("Cette commodité n'est plus disponible");
        }


        if (requestDto.libelle() != null && !commodite
                .getLibelle()
                .equalsIgnoreCase(requestDto.libelle())) {

            if (commoditeRepository.existsByLibelleIgnoreCase(requestDto.libelle())) {
                throw new EntityNotFoundException("Ce libellé existe déjà pour une autre commodité");
            }

            commodite.setLibelle(requestDto.libelle());
        }

        commoditeRepository.save(commodite);
        return commoditeMapper.toResponse(commodite);
    }

    @Override
    public void archiveCommodite(Long id) {

        Commodite commodite = commoditeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Commodité non trouvée avec l'ID: " + id));

        if (commodite.isArchived()) {
            throw new EntityNotFoundException("Cette commodité est déjà archivée");
        }

        commodite.setArchived(true);
        commoditeRepository.save(commodite);
    }
}
