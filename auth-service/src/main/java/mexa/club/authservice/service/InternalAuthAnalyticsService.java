package mexa.club.authservice.service;

import mexa.club.authservice.dto.InternalAuthStatsResponse;
import mexa.club.authservice.dto.InternalUserGrowthPoint;
import mexa.club.authservice.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InternalAuthAnalyticsService {

    private final UserRepository userRepository;

    public InternalAuthAnalyticsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public InternalAuthStatsResponse stats(LocalDateTime from, LocalDateTime to) {
        long totalUsers = userRepository.count();
        long newUsersInPeriod = userRepository.countByCreatedAtBetween(from, to);
        long verifiedUsers = userRepository.countByVerifiedTrue();
        return new InternalAuthStatsResponse(totalUsers, newUsersInPeriod, verifiedUsers);
    }

    @Transactional(readOnly = true)
    public List<InternalUserGrowthPoint> growth(LocalDate from, LocalDate to) {
        return userRepository.userGrowthSeries(from, to).stream()
                .map(row -> new InternalUserGrowthPoint(
                        toLocalDate(row[0]),
                        ((Number) row[1]).longValue(),
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }

    private static LocalDate toLocalDate(Object raw) {
        if (raw instanceof LocalDate d) {
            return d;
        }
        if (raw instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type: " + raw);
    }
}
