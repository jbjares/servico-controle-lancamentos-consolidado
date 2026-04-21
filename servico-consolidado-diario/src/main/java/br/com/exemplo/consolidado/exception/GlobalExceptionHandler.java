package br.com.exemplo.consolidado.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarValidacao(IllegalArgumentException ex) {
        LOGGER.warn("Erro de validacao de negocio: {}", ex.getMessage());
        return erro("VALIDACAO_NEGOCIO", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> tratarFormatoInvalido(MethodArgumentTypeMismatchException ex) {
        LOGGER.warn("Parametro invalido: {} valor={}", ex.getName(), ex.getValue());
        return erro("PARAMETRO_INVALIDO", "Parametro invalido: " + ex.getName());
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, Object> tratarNaoEncontrado(ResponseStatusException ex) {
        LOGGER.info("Recurso nao encontrado: {}", ex.getReason());
        return erro("NAO_ENCONTRADO", ex.getReason());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> tratarErroNaoEsperado(Exception ex) {
        LOGGER.error("Erro nao esperado no servico de consolidado", ex);
        return erro("ERRO_INTERNO", "Erro interno ao processar solicitacao");
    }

    private Map<String, Object> erro(String codigo, String mensagem) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("codigo", codigo);
        body.put("mensagem", mensagem);
        body.put("timestamp", OffsetDateTime.now());
        return body;
    }
}
