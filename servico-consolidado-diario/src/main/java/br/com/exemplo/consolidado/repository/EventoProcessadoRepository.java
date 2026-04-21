package br.com.exemplo.consolidado.repository;

import br.com.exemplo.consolidado.model.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EventoProcessadoRepository extends JpaRepository<EventoProcessado, UUID> {
}
