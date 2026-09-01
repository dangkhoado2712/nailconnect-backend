package com.nailconnect.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(error(e.getStatusCode().value(),e.getReason()));}
  @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation(MethodArgumentNotValidException e){var fields=e.getBindingResult().getFieldErrors().stream().collect(java.util.stream.Collectors.toMap(x->x.getField(),x->x.getDefaultMessage(),(a,b)->a));return ResponseEntity.badRequest().body(Map.of("status",400,"error","Validation failed","fields",fields,"timestamp",Instant.now()));}
  private Map<String,Object> error(int status,String message){return Map.of("status",status,"error",message==null?HttpStatus.valueOf(status).getReasonPhrase():message,"timestamp",Instant.now());}
}
