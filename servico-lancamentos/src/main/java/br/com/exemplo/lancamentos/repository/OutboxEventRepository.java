package br.com.exemplo.lancamentos.repository;

import br.com.exemplo.lancamentos.model.OutboxEvent;
import br.com.exemplo.lancamentos.model.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    long countByStatus(OutboxStatus status);

    Optional<OutboxEvent> findFirstByStatusInOrderByCriadoEmAsc(Collection<OutboxStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from OutboxEvent e "
            + "where (e.status in :statuses and e.proximaTentativaEm <= :agora) "
            + "or (e.status = :processando and e.claimExpiraEm <= :agora) "
            + "order by e.criadoEm asc")
    List<OutboxEvent> findDisponiveisParaReserva(
            @Param("statuses") Collection<OutboxStatus> statuses,
            @Param("processando") OutboxStatus processando,
            @Param("agora") OffsetDateTime agora,
            Pageable pageable);
}
