package net.researchgate.restler.domain;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;

import java.util.Objects;

/**
 * Publication entity
 */
@Entity("publications")
public class Publication {
    @Id
    private Long publicationUid;
    private String title;
    private String lastModificationClientId;

    public Long getPublicationUid() {
        return publicationUid;
    }

    public void setPublicationUid(Long publicationUid) {
        this.publicationUid = publicationUid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLastModificationClientId() {
        return lastModificationClientId;
    }

    public void setLastModificationClientId(String lastModificationClientId) {
        this.lastModificationClientId = lastModificationClientId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Publication that = (Publication) o;
        return Objects.equals(publicationUid, that.publicationUid) &&
                Objects.equals(title, that.title) &&
                Objects.equals(lastModificationClientId, that.lastModificationClientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicationUid, title, lastModificationClientId);
    }

    @Override
    public String toString() {
        return "Publication{" +
                "publicationUid=" + publicationUid +
                ", title='" + title + '\'' +
                ", lastModificationClientId=" + lastModificationClientId +
                '}';
    }
}
