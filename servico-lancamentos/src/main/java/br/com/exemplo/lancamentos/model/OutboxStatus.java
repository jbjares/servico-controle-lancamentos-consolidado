package br.com.exemplo.lancamentos.model;

public enum OutboxStatus {
    PENDENTE,
    PROCESSANDO,
    PUBLICADO,
    ERRO
}
