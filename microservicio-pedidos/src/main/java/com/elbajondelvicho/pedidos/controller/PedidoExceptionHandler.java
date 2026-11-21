package com.elbajondelvicho.pedidos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce errores de entrada del cliente a HTTP 400 con detail para React. */
@RestControllerAdvice(assignableTypes = PedidoController.class)
public class PedidoExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> solicitudInvalida(IllegalArgumentException error) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, error.getMessage());
        return ResponseEntity.badRequest().body(problema);
    }
}
