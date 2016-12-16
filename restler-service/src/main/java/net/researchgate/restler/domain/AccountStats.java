package net.researchgate.restler.domain;

import java.util.List;

/**
 * Account stats
 */
public class AccountStats {
    private Integer publicationCnt;
    private Integer followerCnt;
    private List<Long> scoreBreakdown;

    public AccountStats() {
    }

    public AccountStats(Integer publicationCnt, Integer followerCnt) {
        this.publicationCnt = publicationCnt;
        this.followerCnt = followerCnt;
    }

    public Integer getPublicationCnt() {
        return publicationCnt;
    }

    public void setPublicationCnt(Integer publicationCnt) {
        this.publicationCnt = publicationCnt;
    }

    public Integer getFollowerCnt() {
        return followerCnt;
    }

    public void setFollowerCnt(Integer followerCnt) {
        this.followerCnt = followerCnt;
    }

    public List<Long> getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(List<Long> scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AccountStats that = (AccountStats) o;

        if (publicationCnt != null ? !publicationCnt.equals(that.publicationCnt) : that.publicationCnt != null)
            return false;
        if (followerCnt != null ? !followerCnt.equals(that.followerCnt) : that.followerCnt != null) return false;
        return scoreBreakdown != null ? scoreBreakdown.equals(that.scoreBreakdown) : that.scoreBreakdown == null;

    }

    @Override
    public int hashCode() {
        int result = publicationCnt != null ? publicationCnt.hashCode() : 0;
        result = 31 * result + (followerCnt != null ? followerCnt.hashCode() : 0);
        result = 31 * result + (scoreBreakdown != null ? scoreBreakdown.hashCode() : 0);
        return result;
    }
}
