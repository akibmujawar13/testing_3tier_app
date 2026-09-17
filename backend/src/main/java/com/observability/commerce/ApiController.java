package com.observability.commerce;
import java.time.*; import java.util.*; import org.springframework.beans.factory.annotation.*; import org.springframework.http.*; import org.springframework.jdbc.core.JdbcTemplate; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1") public class ApiController { private final CommerceService service; private final JdbcTemplate db; @Value("${app.diagnostic.enabled}") boolean diagnostics; @Value("${app.diagnostic.delay-ms}") long delay; @Value("${app.diagnostic.slow-db-enabled}") boolean slowDb; @Value("${APP_VERSION:1.0.0}") String version;
 public ApiController(CommerceService s,JdbcTemplate d){service=s;db=d;}
 @GetMapping("/products") public Map<String,Object> products(@RequestParam(required=false)String q,@RequestParam(required=false)String category,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){return wrap(service.products(q,category,Math.max(0,page),Math.min(100,Math.max(1,size))));}
 @GetMapping("/products/{id}") public Map<String,Object> product(@PathVariable long id){return wrap(service.product(id));}
 @GetMapping("/customers") public Map<String,Object> customers(@RequestParam(required=false)String q){return wrap(service.customers(q));}
 @GetMapping("/customers/{id}/orders") public Map<String,Object> customerOrders(@PathVariable long id){return wrap(service.customerOrders(id));}
 @GetMapping("/orders/{id}") public Map<String,Object> order(@PathVariable long id){return wrap(service.order(id));}
 @PatchMapping("/orders/{id}/status") public Map<String,Object> status(@PathVariable long id,@RequestBody Map<String,String>b){String s=b.get("status");if(s==null||!s.matches("ORDER_CREATED|PAYMENT_PENDING|PAYMENT_COMPLETED|INVENTORY_RESERVED|PROCESSING|SHIPPED|DELIVERED|PAYMENT_FAILED|CANCELLED"))throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Invalid order status");if(db.update("UPDATE orders SET status=?,updated_at=now() WHERE id=?",s,id)==0)throw new ApiException(HttpStatus.NOT_FOUND,"Order not found");db.update("INSERT INTO order_status_history(order_id,status,message) VALUES(?,?,?)",id,s,"Status updated through API");return wrap(service.order(id));}
 @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(@RequestBody Map<String,Object>b){return wrap(service.createOrder(b));}
 @GetMapping("/inventory/{productId}") public Map<String,Object> inventory(@PathVariable long productId){Map<String,Object> x=new LinkedHashMap<>();x.put("product",service.product(productId));return wrap(x);}
 @GetMapping("/dashboard") public Map<String,Object> dashboard(){return wrap(service.dashboard());}
 @GetMapping("/health") public Map<String,Object> health(){return wrap(service.health());}
 @GetMapping("/health/database") public Map<String,Object> database(){return wrap(Collections.<String,Object>singletonMap("status",db.queryForObject("SELECT 1",Integer.class)==1?"UP":"DOWN"));}
 @GetMapping("/health/external-services") public Map<String,Object> extHealth(){Map<String,Object>x=new LinkedHashMap<>();x.put("payment","CONFIGURED");x.put("shipping","CONFIGURED");return wrap(x);}
 @GetMapping("/operations") public Map<String,Object> ops(){Map<String,Object>x=service.health();x.put("version",version);x.put("javaVersion",System.getProperty("java.version"));x.put("environment",System.getenv().getOrDefault("APP_ENV","dev"));x.put("recentEvents",db.queryForList("SELECT event_type,message,created_at FROM application_events ORDER BY created_at DESC LIMIT 10"));return wrap(x);}
 @GetMapping("/metrics") public Map<String,Object> metrics(){return wrap(service.dashboard());}
 @GetMapping("/payments/{id}") public Map<String,Object> payment(@PathVariable long id){return wrap(db.queryForMap("SELECT * FROM payments WHERE id=?",id));}
 @GetMapping("/shipments/{id}") public Map<String,Object> shipment(@PathVariable long id){return wrap(db.queryForMap("SELECT * FROM shipments WHERE id=?",id));}
 @GetMapping("/discounts") public Map<String,Object> discounts(){return wrap(db.queryForList("SELECT code,percentage,expires_at FROM discounts WHERE active=true"));}
 @GetMapping("/diagnostics/normal") public Map<String,Object> normal(){enabled();return wrap(Collections.<String,Object>singletonMap("message","Normal diagnostic transaction completed"));}
 @GetMapping("/diagnostics/slow") public Map<String,Object> slow() throws InterruptedException {enabled();Thread.sleep(Math.min(delay,15000));return wrap(Collections.<String,Object>singletonMap("delayMs",delay));}
 @GetMapping("/diagnostics/error") public Map<String,Object> error(){enabled();throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,"Controlled diagnostic business error");}
 @GetMapping("/diagnostics/not-found") public Map<String,Object> missing(){enabled();throw new ApiException(HttpStatus.NOT_FOUND,"Controlled diagnostic resource not found");}
 @GetMapping("/diagnostics/server-error") public Map<String,Object> boom(){enabled();throw new RuntimeException("Controlled diagnostic server error");}
 @GetMapping("/diagnostics/database-error") public Map<String,Object> dbError(){enabled();db.queryForList("SELECT * FROM intentionally_missing_observability_table");return wrap(Collections.emptyMap());}
 @GetMapping("/diagnostics/database/slow-query") public Map<String,Object> slowDb(){enabled();if(!slowDb)throw new ApiException(HttpStatus.FORBIDDEN,"Slow DB tests are disabled; set ENABLE_SLOW_DB_TESTS=true");return wrap(db.queryForMap("SELECT count(*) total FROM products p CROSS JOIN products p2 CROSS JOIN products p3"));}
 @GetMapping("/diagnostics/external-service-error") public Map<String,Object> externalError(){enabled();throw new ApiException(HttpStatus.BAD_GATEWAY,"Controlled simulated external-service failure");}
 @GetMapping("/diagnostics/timeout") public Map<String,Object> timeout() throws InterruptedException {enabled();Thread.sleep(Math.min(delay+1000,15000));throw new ApiException(HttpStatus.GATEWAY_TIMEOUT,"Controlled upstream timeout");}
 private void enabled(){if(!diagnostics)throw new ApiException(HttpStatus.NOT_FOUND,"Diagnostics disabled");} private Map<String,Object> wrap(Object data){Map<String,Object>x=new LinkedHashMap<>();x.put("data",data);x.put("timestamp",Instant.now().toString());return x;}
}
