package net.researchgate.restler.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import net.researchgate.restler.serde.ObjectIdDeserializer;
import net.researchgate.restler.serde.ObjectIdSerializer;
import net.researchgate.restler.serde.Rfc3339DateDeserializer;
import net.researchgate.restler.serde.Rfc3339DateSerializer;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Property;
import org.mongodb.morphia.utils.IndexType;

import java.util.Date;
import java.util.List;

/**
 * Account entity
 */

@Entity("accounts")
 @Indexes({
         @Index(fields = @Field("rating")),
//         @Index(fields = @Field("deleted")),
         @Index(fields = {@Field("stats.scoreBreakdown"), @Field(value = "rating", type = IndexType.DESC)}),
         @Index(fields = @Field("nickname"))
 })
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
public class Account {

    @Id
    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    private ObjectId id;

    private List<Long> publicationUids;

    private Boolean deleted;

    private List<Publication> publications;

    @JsonSerialize(using = ObjectIdSerializer.class)
    @JsonDeserialize(using = ObjectIdDeserializer.class)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ObjectId mentorAccountId;

    private String newFiled;

    private String oldField;

    private Long rating;

    private AccountState state;

    @JsonSerialize(using = Rfc3339DateSerializer.class)
    @JsonDeserialize(using = Rfc3339DateDeserializer.class)
    @Property("cd")
    private Date createdAt;

    @JsonSerialize(using = Rfc3339DateSerializer.class)
    @JsonDeserialize(using = Rfc3339DateDeserializer.class)
    @Property("ud")
    private Date modifiedAt;

    private Date longDate;

    private AccountStats stats;

    private List<AccountStats> additionalStats;

    private String nickname;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public List<Long> getPublicationUids() {
        return publicationUids;
    }

    public void setPublicationUids(List<Long> publicationUids) {
        this.publicationUids = publicationUids;
    }

    public ObjectId getMentorAccountId() {
        return mentorAccountId;
    }

    public void setMentorAccountId(ObjectId mentorAccountId) {
        this.mentorAccountId = mentorAccountId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Date modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public Long getRating() {
        return rating;
    }

    public void setRating(Long rating) {
        this.rating = rating;
    }

    public AccountStats getStats() {
        return stats;
    }

    public void setStats(AccountStats stats) {
        this.stats = stats;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public AccountState getState() {
        return state;
    }

    public void setState(AccountState state) {
        this.state = state;
    }

    public Date getLongDate() {
        return longDate;
    }

    public void setLongDate(Date longDate) {
        this.longDate = longDate;
    }

    public List<Publication> getPublications() {
        return publications;
    }

    public void setPublications(List<Publication> publications) {
        this.publications = publications;
    }

    public List<AccountStats> getAdditionalStats() {
        return additionalStats;
    }

    public void setAdditionalStats(List<AccountStats> additionalStats) {
        this.additionalStats = additionalStats;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Account account = (Account) o;

        if (id != null ? !id.equals(account.id) : account.id != null) return false;
        if (publicationUids != null ? !publicationUids.equals(account.publicationUids) : account.publicationUids != null)
            return false;
        if (publications != null ? !publications.equals(account.publications) : account.publications != null)
            return false;
        if (mentorAccountId != null ? !mentorAccountId.equals(account.mentorAccountId) : account.mentorAccountId != null)
            return false;
        if (rating != null ? !rating.equals(account.rating) : account.rating != null) return false;
        if (state != account.state) return false;
        if (createdAt != null ? !createdAt.equals(account.createdAt) : account.createdAt != null) return false;
        if (modifiedAt != null ? !modifiedAt.equals(account.modifiedAt) : account.modifiedAt != null) return false;
        if (stats != null ? !stats.equals(account.stats) : account.stats != null) return false;
        return nickname != null ? nickname.equals(account.nickname) : account.nickname == null;

    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (publicationUids != null ? publicationUids.hashCode() : 0);
        result = 31 * result + (publications != null ? publications.hashCode() : 0);
        result = 31 * result + (mentorAccountId != null ? mentorAccountId.hashCode() : 0);
        result = 31 * result + (rating != null ? rating.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (createdAt != null ? createdAt.hashCode() : 0);
        result = 31 * result + (stats != null ? stats.hashCode() : 0);
        result = 31 * result + (nickname != null ? nickname.hashCode() : 0);
        return result;
    }
}
