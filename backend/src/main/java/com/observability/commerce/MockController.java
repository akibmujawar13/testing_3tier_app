package com.observability.commerce;
import java.time.*; import java.util.*; import org.springframework.beans.factory.annotation.Value; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/mock") public class MockController { @Value("${app.payment.delay-ms}") long paymentDelay; @Value("${app.shipping.delay-ms}") long shippingDelay; @Value("${app.payment.failure-rate}") double paymentFailures; @Value("${app.shipping.failure-rate}") double shippingFailures;
 @PostMapping("/payment/process") public Map<String,Object> payment(@RequestBody(required=false)Map<String,Object>b)throws InterruptedException{return response("payment",paymentDelay,paymentFailures);}
 @PostMapping("/shipping/process") public Map<String,Object> shipment(@RequestBody(required=false)Map<String,Object>b)throws InterruptedException{return response("shipment",shippingDelay,shippingFailures);}
 private Map<String,Object> response(String name,long d,double failure)throws InterruptedException{Thread.sleep(Math.min(d,10000));if(Math.random()*100<failure)throw new ApiException(HttpStatus.BAD_GATEWAY,"Simulated "+name+" failure");Map<String,Object>x=new LinkedHashMap<>();x.put("status","APPROVED");x.put("reference",name+"-"+UUID.randomUUID());x.put("timestamp",Instant.now().toString());return x;}
}
