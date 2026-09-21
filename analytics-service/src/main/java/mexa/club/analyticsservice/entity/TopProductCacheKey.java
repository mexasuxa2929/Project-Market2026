package mexa.club.analyticsservice.entity;

import java.io.Serializable;
import java.util.Objects;

public class TopProductCacheKey implements Serializable {

    private String period;
    private Integer rankPosition;

    public TopProductCacheKey() {}

    public TopProductCacheKey(String period, Integer rankPosition) {
        this.period = period;
        this.rankPosition = rankPosition;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Integer rankPosition) {
        this.rankPosition = rankPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TopProductCacheKey that)) return false;
        return Objects.equals(period, that.period) && Objects.equals(rankPosition, that.rankPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, rankPosition);
    }
}
