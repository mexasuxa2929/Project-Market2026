package mexa.club.analyticsservice.client.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InternalAuthStatsPayload(long totalUsers, long newUsersInPeriod, long verifiedUsers) {}
