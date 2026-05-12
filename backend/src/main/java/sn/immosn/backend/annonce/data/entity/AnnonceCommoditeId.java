package sn.immosn.backend.annonce.data.entity;

import java.io.Serializable;
import java.util.Objects;

public class AnnonceCommoditeId implements Serializable {
    private Long annonce;
    private Long commodite;

    // Default constructor
    public AnnonceCommoditeId() {}

    public AnnonceCommoditeId(Long annonce, Long commodite) {
        this.annonce = annonce;
        this.commodite = commodite;
    }

    // Getters and setters
    public Long getAnnonce() {
        return annonce;
    }

    public void setAnnonce(Long annonce) {
        this.annonce = annonce;
    }

    public Long getCommodite() {
        return commodite;
    }

    public void setCommodite(Long commodite) {
        this.commodite = commodite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AnnonceCommoditeId that = (AnnonceCommoditeId) o;
        return Objects.equals(annonce, that.annonce) && Objects.equals(commodite, that.commodite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(annonce, commodite);
    }
}