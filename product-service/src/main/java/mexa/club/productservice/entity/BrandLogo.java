package mexa.club.productservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "brand_logo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BrandLogo {

    @Id
    @Column(name = "brand_id")
    private UUID brandId;

    @Column(name = "path", nullable = false, length = 512)
    private String path;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
