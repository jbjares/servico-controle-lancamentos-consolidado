package br.com.exemplo.lancamentos.dto;

import java.util.UUID;

public class LancamentoResponse {
    private UUID id;
    private String status;
    private String mensagem;

    public LancamentoResponse() {
    }

    public LancamentoResponse(UUID id, String status, String mensagem) {
        this.id = id;
        this.status = status;
        this.mensagem = mensagem;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }
}
