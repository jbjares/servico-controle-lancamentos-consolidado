package br.com.exemplo.lancamentos.repository;

import br.com.exemplo.lancamentos.model.Lancamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LancamentoRepository extends JpaRepository<Lancamento, UUID> {
    List<Lancamento> findByDataEfetivaBetween(LocalDate dataInicio, LocalDate dataFim);
}
