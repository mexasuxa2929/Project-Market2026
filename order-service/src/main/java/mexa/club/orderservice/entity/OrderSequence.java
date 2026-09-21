package mexa.club.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "order_sequence")
@Getter
@Setter
public class OrderSequence {
    @Id
    @Column(name = "seq_year")
    private Integer year;
    @Column(nullable = false)
    private Long value;
}
