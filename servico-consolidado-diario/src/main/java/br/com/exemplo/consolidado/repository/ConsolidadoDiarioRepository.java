package br.com.exemplo.consolidado.repository;

import br.com.exemplo.consolidado.model.ConsolidadoDiario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ConsolidadoDiarioRepository extends JpaRepository<ConsolidadoDiario, LocalDate> {
}
