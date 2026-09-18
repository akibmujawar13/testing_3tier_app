package com.observability.commerce;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CommerceService {

    private static final Logger log = LoggerFactory.getLogger(CommerceService.class);

    private final JdbcTemplate db;
    private final RestTemplate http;

    @Value("${app.payment.url}")
    private String paymentUrl;

    @Value("${app.shipping.url}")
    private String shippingUrl;

    public CommerceService(JdbcTemplate db, RestTemplate http) {
        this.db = db;
        this.http = http;
    }

    public List<Map<String, Object>> products(String q, String category, int page, int size) {
        String where = " WHERE p.active=true ";
        List<Object> args = new ArrayList<>();

        if (q != null && !q.isEmpty()) {
            where += " AND (lower(p.name) like ? OR lower(p.sku) like ? )";
            args.add("%" + q.toLowerCase() + "%");
            args.add("%" + q.toLowerCase() + "%");
        }

        if (category != null && !category.isEmpty()) {
            where += " AND c.name=?";
            args.add(category);
        }

        args.add(size);
        args.add(page * size);

        return db.queryForList(
            "SELECT p.id,p.sku,p.name,p.description,p.price,c.name category,"
                + "i.quantity inventory,i.reorder_level "
                + "FROM products p "
                + "JOIN categories c ON c.id=p.category_id "
                + "JOIN inventory i ON i.product_id=p.id"
                + where
                + " ORDER BY p.name LIMIT ? OFFSET ?",
            args.toArray()
        );
    }

    public Map<String, Object> product(long id) {
        try {
            return db.queryForMap(
                "SELECT p.id,p.sku,p.name,p.description,p.price,c.name category,"
                    + "i.quantity inventory,i.reorder_level "
                    + "FROM products p "
                    + "JOIN categories c ON c.id=p.category_id "
                    + "JOIN inventory i ON i.product_id=p.id "
                    + "WHERE p.id=?",
                id
            );
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Product not found");
        }
    }

    public List<Map<String, Object>> customers(String q) {
        String query = q == null ? "" : q;

        return db.queryForList(
            "SELECT id,customer_number,first_name,last_name,email,company,created_at "
                + "FROM customers "
                + "WHERE (?='' OR lower(first_name||' '||last_name||' '||email) LIKE ?) "
                + "ORDER BY id LIMIT 100",
            query,
            "%" + query.toLowerCase() + "%"
        );
    }

    public List<Map<String, Object>> customerOrders(long id) {
        return db.queryForList(
            "SELECT id,order_number,status,total_amount,created_at "
                + "FROM orders WHERE customer_id=? ORDER BY created_at DESC",
            id
        );
    }

    public Map<String, Object> dashboard() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("customers", db.queryForObject("SELECT count(*) FROM customers", Long.class));
        result.put("orders", db.queryForObject("SELECT count(*) FROM orders", Long.class));
        result.put(
            "revenue",
            db.queryForObject(
                "SELECT coalesce(sum(total_amount),0) FROM orders "
                    + "WHERE status NOT IN ('PAYMENT_FAILED','CANCELLED')",
                BigDecimal.class
            )
        );
        result.put(
            "pendingOrders",
            db.queryForObject(
                "SELECT count(*) FROM orders "
                    + "WHERE status IN ('ORDER_CREATED','PAYMENT_PENDING','PROCESSING')",
                Long.class
            )
        );
        result.put(
            "failedOrders",
            db.queryForObject(
                "SELECT count(*) FROM orders WHERE status='PAYMENT_FAILED'",
                Long.class
            )
        );
        result.put(
            "lowStock",
            db.queryForObject(
                "SELECT count(*) FROM inventory WHERE quantity<=reorder_level",
                Long.class
            )
        );
        result.put(
            "recentOrders",
            db.queryForList(
                "SELECT o.id,o.order_number,o.status,o.total_amount,o.created_at,"
                    + "c.first_name||' '||c.last_name customer "
                    + "FROM orders o "
                    + "JOIN customers c ON c.id=o.customer_id "
                    + "ORDER BY o.created_at DESC LIMIT 8"
            )
        );
        result.put(
            "statusDistribution",
            db.queryForList(
                "SELECT status,count(*) total FROM orders "
                    + "GROUP BY status ORDER BY total DESC"
            )
        );

        return result;
    }

    public Map<String, Object> order(long id) {
        try {
            return db.queryForMap(
                "SELECT o.id,o.order_number,o.status,o.total_amount,o.discount_amount,"
                    + "o.created_at,c.first_name||' '||c.last_name customer "
                    + "FROM orders o "
                    + "JOIN customers c ON c.id=o.customer_id "
                    + "WHERE o.id=?",
                id
            );
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Order not found");
        }
    }

    @Transactional
    public Map<String, Object> createOrder(Map<String, Object> body) {
        Number customerId = (Number) body.get("customerId");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");

        if (customerId == null || items == null || items.isEmpty()) {
            throw new ApiException(
                HttpStatus.BAD_REQUEST,
                "customerId and at least one item are required"
            );
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Map<String, Object> item : items) {
            Number productId = (Number) item.get("productId");
            Number quantity = (Number) item.get("quantity");

            if (productId == null || quantity == null || quantity.intValue() < 1) {
                throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid order item");
            }

            Map<String, Object> product = product(productId.longValue());

            if (((Number) product.get("inventory")).intValue() < quantity.intValue()) {
                throw new ApiException(
                    HttpStatus.CONFLICT,
                    "Insufficient inventory for " + product.get("sku")
                );
            }

            BigDecimal price = (BigDecimal) product.get("price");

            total = total.add(
                price.multiply(BigDecimal.valueOf(quantity.longValue()))
            );
        }

        final BigDecimal orderTotal = total;
        String orderNumber = "ORD-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO orders(order_number,customer_id,status,total_amount,discount_amount) "
                    + "VALUES(?,?, 'ORDER_CREATED',?,0)",
                Statement.RETURN_GENERATED_KEYS
            );

            statement.setString(1, orderNumber);
            statement.setLong(2, customerId.longValue());
            statement.setBigDecimal(3, orderTotal);

            return statement;
        }, keyHolder);

        long orderId = keyHolder.getKey().longValue();

        for (Map<String, Object> item : items) {
            long productId = ((Number) item.get("productId")).longValue();
            int quantity = ((Number) item.get("quantity")).intValue();
            BigDecimal price = (BigDecimal) product(productId).get("price");

            db.update(
                "INSERT INTO order_items(order_id,product_id,quantity,unit_price,line_total) "
                    + "VALUES(?,?,?,?,?)",
                orderId,
                productId,
                quantity,
                price,
                price.multiply(BigDecimal.valueOf(quantity))
            );

            db.update(
                "UPDATE inventory SET quantity=quantity-?,updated_at=now() WHERE product_id=?",
                quantity,
                productId
            );
        }

        Map<String, Object> request = new HashMap<>();
        request.put("orderId", orderId);
        request.put("amount", orderTotal);

        Map<String, Object> payment = external(paymentUrl, request);

        db.update(
            "INSERT INTO payments(order_id,status,amount,provider_reference) VALUES(?,?,?,?)",
            orderId,
            "COMPLETED",
            orderTotal,
            String.valueOf(payment.get("reference"))
        );

        Map<String, Object> shipping = external(shippingUrl, request);

        db.update(
            "INSERT INTO shipments(order_id,status,tracking_number) VALUES(?,?,?)",
            orderId,
            "PENDING",
            String.valueOf(shipping.get("reference"))
        );

        db.update(
            "UPDATE orders SET status='PROCESSING',updated_at=now() WHERE id=?",
            orderId
        );

        db.update(
            "INSERT INTO order_status_history(order_id,status,message) VALUES(?,?,?)",
            orderId,
            "PROCESSING",
            "Payment approved; shipment created"
        );

        log.info("Order created orderId={} customerId={}", orderId, customerId);

        return order(orderId);
    }

    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("status", "UP");
        result.put(
            "database",
            db.queryForObject("SELECT 1", Integer.class) == 1 ? "UP" : "DOWN"
        );
        result.put("time", Instant.now().toString());

        return result;
    }

    public Map<String, Object> external(String url, Map<String, Object> body) {
        try {
            return http.postForObject(url + "/process", body, Map.class);
        } catch (RestClientException e) {
            throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                "External service unavailable: " + e.getMessage()
            );
        }
    }
}
