package mexa.club.authservice.dto;

public record InternalAuthStatsResponse(long totalUsers, long newUsersInPeriod, long verifiedUsers) {}
