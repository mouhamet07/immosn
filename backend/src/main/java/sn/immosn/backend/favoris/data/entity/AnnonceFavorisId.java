package sn.immosn.backend.favoris.data.entity;

import java.io.Serializable;
import java.util.Objects;

public class AnnonceFavorisId implements Serializable {

    private Long client;
    private Long annonce;

    public AnnonceFavorisId() {}

    public AnnonceFavorisId(Long client, Long annonce) {
        this.client = client;
        this.annonce = annonce;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnonceFavorisId that)) return false;
        return Objects.equals(client, that.client) && Objects.equals(annonce, that.annonce);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client, annonce);
    }
}
