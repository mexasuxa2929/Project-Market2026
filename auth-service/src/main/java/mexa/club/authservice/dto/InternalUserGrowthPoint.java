package mexa.club.authservice.dto;

import java.time.LocalDate;

public record InternalUserGrowthPoint(LocalDate date, long newUsers, long totalUsers) {}
