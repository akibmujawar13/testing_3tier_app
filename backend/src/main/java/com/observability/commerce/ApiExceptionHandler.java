package com.observability.commerce;
import java.time.Instant; import java.util.*; import javax.servlet.http.HttpServletRequest; import org.slf4j.*; import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestControllerAdvice public class ApiExceptionHandler { private static final Logger log=LoggerFactory.getLogger(ApiExceptionHandler.class);
 @ExceptionHandler(ApiException.class) ResponseEntity<Map<String,Object>> api(ApiException e,HttpServletRequest r){return out(e.status,e.getMessage(),r);}
 @ExceptionHandler(Exception.class) ResponseEntity<Map<String,Object>> all(Exception e,HttpServletRequest r){log.error("Unhandled application error",e);return out(HttpStatus.INTERNAL_SERVER_ERROR,"Unable to process request",r);}
 private ResponseEntity<Map<String,Object>> out(HttpStatus s,String m,HttpServletRequest r){Map<String,Object>x=new LinkedHashMap<>();x.put("timestamp",Instant.now().toString());x.put("status",s.value());x.put("error",s.name());x.put("message",m);x.put("requestId",MDC.get("requestId"));x.put("path",r.getRequestURI());return ResponseEntity.status(s).body(x);}
}
