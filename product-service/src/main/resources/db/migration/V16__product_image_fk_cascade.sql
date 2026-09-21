ALTER TABLE product_image DROP CONSTRAINT fk_product_image_product;
ALTER TABLE product_image ADD CONSTRAINT fk_product_image_product
    FOREIGN KEY (product_id) REFERENCES product(id) ON DELETE CASCADE;
