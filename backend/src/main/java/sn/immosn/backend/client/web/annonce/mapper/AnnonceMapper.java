package sn.immosn.backend.client.web.annonce.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sn.immosn.backend.annonce.data.entity.Annonce;
import sn.immosn.backend.annonce.data.entity.AnnonceCommodite;
import sn.immosn.backend.annonce.data.entity.Commodite;
import sn.immosn.backend.annonce.data.entity.TypeBienAnnonce;
import sn.immosn.backend.client.web.annonce.dto.AnnonceCreateRequestDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceListDto;
import sn.immosn.backend.client.web.annonce.dto.AnnonceResponseDto;

@Component
@RequiredArgsConstructor
public class AnnonceMapper {

    private final TypeBienAnnonceMapper typeBienAnnonceMapper;
    private final CommoditeMapper commoditeMapper;

    public AnnonceResponseDto toResponse(Annonce a) {
        return new AnnonceResponseDto(
            a.getId(),
            a.getLibelle(),
            a.getDescription(),
            a.getNbrPieces(),
            a.getSurface(),
            a.getPrix(),
            a.getAdresse(),
            a.getRegion(),
            a.getDepartement(),
            a.getQuartier(),
            a.getLatitude(),
            a.getLongitude(),
            typeBienAnnonceMapper.toResponseDto(a.getTypeBien()),
            a.getAnnonceCommodites().stream()
                .map(ac -> commoditeMapper.toResponse(ac.getCommodite()))
                .collect(Collectors.toList()),
            a.getImages() != null ? new ArrayList<>(a.getImages()) : new ArrayList<>(),
            a.isArchived(),
            a.getCreatedAt(),
            a.getUpdatedAt()
        );
    }

    public AnnonceListDto toListDto(Annonce a) {
        String firstImage = (a.getImages() != null && !a.getImages().isEmpty())
            ? a.getImages().get(0) : null;
        return new AnnonceListDto(
            String.valueOf(a.getId()),
            a.getLibelle(),
            a.getPrix(),
            a.getAdresse(),
            a.getDepartement(),
            a.getQuartier(),
            a.getLatitude(),
            a.getLongitude(),
            typeBienAnnonceMapper.toResponseDto(a.getTypeBien()),
            a.getNbrPieces(),
            a.getSurface(),
            firstImage,
            a.getCreatedAt(),
            a.isArchived()
        );
    }

    public Annonce toEntity(AnnonceCreateRequestDto dto, TypeBienAnnonce typeBien, List<Commodite> commodites) {
        Annonce annonce = Annonce.builder()
            .libelle(dto.libelle())
            .description(dto.description())
            .nbrPieces(dto.nbrPieces())
            .surface(dto.surface())
            .prix(dto.prix())
            .region(dto.region())
            .departement(dto.departement())
            .quartier(dto.quartier())
            .typeBien(typeBien)
            .images(dto.images())
            .isArchived(false)
            .build();

            for (Commodite commodite : commodites) {
            AnnonceCommodite ac = AnnonceCommodite.builder()
                .annonce(annonce)
                .commodite(commodite)
                .build();
            annonce.getAnnonceCommodites().add(ac);
        }
        return annonce;
    }
    

}