package sn.immosn.backend.client.web.favoris.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FavorisStatusDto(
    Long annonceId,
    @JsonProperty("isFavoris") boolean isFavoris
) {}
