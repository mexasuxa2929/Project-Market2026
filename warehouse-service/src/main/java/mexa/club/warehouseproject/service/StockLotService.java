package mexa.club.warehouseproject.service;

import mexa.club.warehouseproject.entity.StockLot;
import mexa.club.warehouseproject.exception.ResourceConflictException;
import mexa.club.warehouseproject.repository.StockLotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Lot-based inventory costing. Every purchase line creates a {@link StockLot}
 * holding its own unit cost. Outbound stock (sales, transfers, negative
 * adjustments) is consumed FIFO — oldest lots first — and the consumed cost is
 * returned so callers can snapshot COGS on stock movements.
 *
 * <p>Invariant maintained by all mutating methods here: the sum of lot
 * quantities for a (warehouse, product) always equals the corresponding
 * {@code warehouse_stock.quantity}.
 */
@Service
public class StockLotService {

    private final StockLotRepository stockLotRepository;

    public StockLotService(StockLotRepository stockLotRepository) {
        this.stockLotRepository = stockLotRepository;
    }

    /**
     * Creates a new incoming lot (purchase in / transfer in / return in).
     */
    @Transactional
    public StockLot createLot(
            UUID warehouseId,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitCost,
            UUID purchaseId,
            UUID purchaseItemId,
            LocalDateTime receivedDate
    ) {
        StockLot lot = new StockLot();
        lot.setWarehouseId(warehouseId);
        lot.setProductId(productId);
        lot.setQuantity(quantity);
        lot.setUnitCost(unitCost != null ? unitCost : BigDecimal.ZERO);
        lot.setPurchaseId(purchaseId);
        lot.setPurchaseItemId(purchaseItemId);
        lot.setReceivedDate(receivedDate != null ? receivedDate : LocalDateTime.now());
        return stockLotRepository.save(lot);
    }

    /**
     * Consumes {@code quantity} out of stock using FIFO. Returns the weighted
     * average unit cost and total cost of the consumed units so callers can
     * record COGS on the corresponding stock movement.
     */
    @Transactional
    public ConsumptionResult consumeFifo(UUID warehouseId, UUID productId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        List<StockLot> lots = stockLotRepository.findLotsForUpdate(warehouseId, productId);
        BigDecimal remaining = quantity;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalQty = BigDecimal.ZERO;

        for (StockLot lot : lots) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = lot.getQuantity() != null ? lot.getQuantity() : BigDecimal.ZERO;
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal taken = available.min(remaining);
            lot.setQuantity(available.subtract(taken));
            stockLotRepository.save(lot);
            totalCost = totalCost.add(taken.multiply(lot.getUnitCost()));
            totalQty = totalQty.add(taken);
            remaining = remaining.subtract(taken);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new ResourceConflictException(
                    "Insufficient lot stock for product " + productId
                            + " in warehouse " + warehouseId
                            + ": only " + totalQty + " available from lots, requested " + quantity
            );
        }

        BigDecimal unitCost = totalQty.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalCost.divide(totalQty, 6, RoundingMode.HALF_UP);
        return new ConsumptionResult(totalCost.setScale(2, RoundingMode.HALF_UP), unitCost);
    }

    /**
     * Removes stock coming from a specific purchase item and, if that lot was
     * partially consumed already, tops up from other lots FIFO. Used when
     * rolling back a purchase item (update / delete / purchase delete).
     *
     * @throws ResourceConflictException if the full quantity cannot be freed
     */
    @Transactional
    public void rollbackPurchaseItem(UUID warehouseId, UUID productId, UUID purchaseItemId, BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BigDecimal remaining = quantity;

        List<StockLot> itemLots = stockLotRepository.findByPurchaseItemId(purchaseItemId);
        for (StockLot lot : itemLots) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = lot.getQuantity() != null ? lot.getQuantity() : BigDecimal.ZERO;
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal taken = available.min(remaining);
            lot.setQuantity(available.subtract(taken));
            stockLotRepository.save(lot);
            remaining = remaining.subtract(taken);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            // The purchase-item lot is fully consumed already; free the rest
            // from other lots of the same product (FIFO).
            consumeFifo(warehouseId, productId, remaining);
        }
    }

    /**
     * Adds an inbound quantity to the lot of a specific purchase item when its
     * purchase quantity grows during an edit.
     */
    @Transactional
    public void growPurchaseItemLot(UUID purchaseItemId, BigDecimal delta) {
        if (delta == null || delta.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        List<StockLot> lots = stockLotRepository.findByPurchaseItemId(purchaseItemId);
        if (lots.isEmpty()) {
            throw new ResourceConflictException("Cannot grow lot: no lot found for purchase item " + purchaseItemId);
        }
        StockLot lot = lots.get(0);
        lot.setQuantity((lot.getQuantity() != null ? lot.getQuantity() : BigDecimal.ZERO).add(delta));
        stockLotRepository.save(lot);
    }

    /**
     * Brings the total lot quantity for a (warehouse, product) in line with a
     * target (used after a direct stock-line upsert). If lots exceed the target
     * they are consumed FIFO; if they fall short, a new lot is created at the
     * current weighted-average cost.
     */
    @Transactional
    public void reconcileToQuantity(UUID warehouseId, UUID productId, BigDecimal targetQuantity) {
        BigDecimal current = sumQuantity(warehouseId, productId);
        BigDecimal diff = targetQuantity.subtract(current);
        if (diff.signum() > 0) {
            createLot(warehouseId, productId, diff, averageUnitCost(warehouseId, productId),
                    null, null, LocalDateTime.now());
        } else if (diff.signum() < 0) {
            consumeFifo(warehouseId, productId, diff.abs());
        }
    }

    /**
     * Restores stock into a new lot using a known unit cost (sales-out reversal,
     * returns). The unit cost typically equals the weighted average COGS of the
     * original outbound movement.
     */
    @Transactional
    public void restoreAtCost(UUID warehouseId, UUID productId, BigDecimal quantity, BigDecimal unitCost) {
        createLot(warehouseId, productId, quantity, unitCost != null ? unitCost : BigDecimal.ZERO,
                null, null, LocalDateTime.now());
    }

    /**
     * Deletes every lot belonging to a purchase (used when a purchase is deleted).
     * Because lots may already have been consumed, this only removes the remaining
     * lot quantity — the same amount the stock rollback frees on the stock line.
     */
    @Transactional
    public BigDecimal removeLotsForPurchase(UUID purchaseId) {
        List<StockLot> lots = stockLotRepository.findByPurchaseId(purchaseId);
        BigDecimal removed = BigDecimal.ZERO;
        for (StockLot lot : lots) {
            removed = removed.add(lot.getQuantity() != null ? lot.getQuantity() : BigDecimal.ZERO);
        }
        stockLotRepository.deleteByPurchaseId(purchaseId);
        return removed;
    }

    @Transactional(readOnly = true)
    public BigDecimal sumQuantity(UUID warehouseId, UUID productId) {
        return stockLotRepository.sumQuantity(warehouseId, productId);
    }

    /**
     * Weighted average unit cost of the remaining lots for a (warehouse, product).
     * Falls back to 0 when there are no lots (e.g. inbound adjustment).
     */
    @Transactional(readOnly = true)
    public BigDecimal averageUnitCost(UUID warehouseId, UUID productId) {
        List<StockLot> lots = stockLotRepository.findByWarehouseIdAndProductId(warehouseId, productId);
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        for (StockLot lot : lots) {
            BigDecimal qty = lot.getQuantity() != null ? lot.getQuantity() : BigDecimal.ZERO;
            totalQty = totalQty.add(qty);
            totalCost = totalCost.add(qty.multiply(lot.getUnitCost()));
        }
        if (totalQty.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalCost.divide(totalQty, 6, RoundingMode.HALF_UP);
    }

    public record ConsumptionResult(BigDecimal totalCost, BigDecimal unitCost) {
        static ConsumptionResult zero() {
            return new ConsumptionResult(BigDecimal.ZERO, BigDecimal.ZERO);
        }
    }
}
