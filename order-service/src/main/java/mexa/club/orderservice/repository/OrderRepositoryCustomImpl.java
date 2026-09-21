package mexa.club.orderservice.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import mexa.club.orderservice.entity.Order;
import mexa.club.orderservice.entity.OrderItem;
import mexa.club.orderservice.entity.OrderStatus;
import mexa.club.orderservice.entity.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Dinamik order qidiruv: faqat berilgan filterlar predicate bo'ladi
 * (null parametr yuborilmaydi — Postgres "could not determine data type"
 * xatosi shu sababli chiqardi, eski @Query yondashuvda).
 */
@Repository
public class OrderRepositoryCustomImpl implements OrderRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<Order> searchOrders(
            OrderStatus status,
            PaymentStatus paymentStatus,
            UUID userId,
            String orderNumber,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            UUID warehouseId,
            Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Order> cq = cb.createQuery(Order.class);
        Root<Order> o = cq.from(Order.class);
        cq.select(o).distinct(warehouseId != null);
        cq.where(buildPredicates(cb, cq, o, status, paymentStatus, userId,
                orderNumber, dateFrom, dateTo, warehouseId).toArray(new Predicate[0]));
        cq.orderBy(cb.desc(o.get("createdAt")));

        TypedQuery<Order> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<Order> content = query.getResultList();

        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Order> co = countCq.from(Order.class);
        countCq.select(cb.countDistinct(co));
        countCq.where(buildPredicates(cb, countCq, co, status, paymentStatus, userId,
                orderNumber, dateFrom, dateTo, warehouseId).toArray(new Predicate[0]));
        Long total = em.createQuery(countCq).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    private List<Predicate> buildPredicates(
            CriteriaBuilder cb,
            CriteriaQuery<?> cq,
            Root<Order> o,
            OrderStatus status,
            PaymentStatus paymentStatus,
            UUID userId,
            String orderNumber,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            UUID warehouseId) {
        List<Predicate> predicates = new ArrayList<>();
        if (status != null) {
            predicates.add(cb.equal(o.get("status"), status));
        }
        if (paymentStatus != null) {
            predicates.add(cb.equal(o.get("paymentStatus"), paymentStatus));
        }
        if (userId != null) {
            predicates.add(cb.equal(o.get("userId"), userId));
        }
        if (orderNumber != null && !orderNumber.isBlank()) {
            predicates.add(cb.like(o.get("orderNumber"), "%" + orderNumber + "%"));
        }
        if (dateFrom != null) {
            predicates.add(cb.greaterThanOrEqualTo(o.get("createdAt"), dateFrom));
        }
        if (dateTo != null) {
            predicates.add(cb.lessThanOrEqualTo(o.get("createdAt"), dateTo));
        }
        if (warehouseId != null) {
            Subquery<UUID> sub = cq.subquery(UUID.class);
            Root<OrderItem> item = sub.from(OrderItem.class);
            sub.select(item.get("id"));
            sub.where(
                    cb.equal(item.get("order"), o),
                    cb.equal(item.get("warehouseId"), warehouseId));
            predicates.add(cb.exists(sub));
        }
        return predicates;
    }
}
