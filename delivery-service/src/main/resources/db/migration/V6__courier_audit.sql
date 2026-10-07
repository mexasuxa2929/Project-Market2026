-- Kuryer ustidan bajarilgan amallar tarixi: kim (admin/superadmin), qachon, nima.
CREATE TABLE IF NOT EXISTS courier_action_audit (
    id UUID PRIMARY KEY,
    courier_id UUID,
    courier_name VARCHAR(150),
    action VARCHAR(32) NOT NULL,
    delivery_id UUID,
    order_id UUID,
    warehouse_id UUID,
    actor_id UUID,
    actor_username VARCHAR(150),
    actor_role VARCHAR(32),
    reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_courier_audit_courier_id ON courier_action_audit(courier_id);
CREATE INDEX IF NOT EXISTS idx_courier_audit_actor_id ON courier_action_audit(actor_id);
CREATE INDEX IF NOT EXISTS idx_courier_audit_created_at ON courier_action_audit(created_at);
