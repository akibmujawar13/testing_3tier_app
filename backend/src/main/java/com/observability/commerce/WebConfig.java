package com.observability.commerce;

import java.io.IOException;
import java.util.UUID;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {
 @Value("${app.cors.origins}") private String origins;
 @Override public void addCorsMappings(CorsRegistry r) { r.addMapping("/**").allowedOrigins(origins.split(",")).allowedMethods("GET","POST","PUT","PATCH","DELETE","OPTIONS").allowedHeaders("*"); }
 @Bean RestTemplate restTemplate() { SimpleClientHttpRequestFactory f=new SimpleClientHttpRequestFactory(); f.setConnectTimeout(1500); f.setReadTimeout(2500); RestTemplate r=new RestTemplate(f); r.getInterceptors().add((request,body,execution)->{ request.getHeaders().set("X-Request-ID",MDC.get("requestId")); request.getHeaders().set("X-Correlation-ID",MDC.get("correlationId")); return execution.execute(request,body); }); return r; }
 @Bean Filter correlationFilter() { return new Filter() { public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,ServletException { HttpServletRequest r=(HttpServletRequest)req; HttpServletResponse p=(HttpServletResponse)res; String request=id(r,"X-Request-ID"), correlation=id(r,"X-Correlation-ID"); MDC.put("requestId",request); MDC.put("correlationId",correlation); p.setHeader("X-Request-ID",request); p.setHeader("X-Correlation-ID",correlation); try { chain.doFilter(req,res); } finally { MDC.clear(); } } public void init(FilterConfig f){} public void destroy(){} }; }
 private String id(HttpServletRequest r,String key) { String v=r.getHeader(key); return v==null||v.trim().isEmpty()?UUID.randomUUID().toString():v; }
}
