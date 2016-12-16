package net.researchgate.restler.domain;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Publication entity
 */
@Entity("publications")
public class Publication {
    @Id
    private Long publicationUid;

    private String title;

    public Long getPublicationUid() {
        return publicationUid;
    }

    public void setPublicationUid(Long publicationUid) {
        this.publicationUid = publicationUid;
    }

    public String getTitle() {
        return title;
    }

    public String lastModificationClientId;

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Publication that = (Publication) o;

        if (publicationUid != null ? !publicationUid.equals(that.publicationUid) : that.publicationUid != null)
            return false;
        if (title != null ? !title.equals(that.title) : that.title != null) return false;
        return lastModificationClientId == that.lastModificationClientId;

    }

    @Override
    public int hashCode() {
        int result = publicationUid != null ? publicationUid.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (lastModificationClientId != null ? lastModificationClientId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Publication{" +
                "publicationUid=" + publicationUid +
                ", title='" + title + '\'' +
                ", lastModificationClientId=" + lastModificationClientId +
                '}';
    }

    public String getLastModificationClientId() {
        return lastModificationClientId;
    }

    public void setLastModificationClientId(String lastModificationClientId) {
        this.lastModificationClientId = lastModificationClientId;
    }
}
