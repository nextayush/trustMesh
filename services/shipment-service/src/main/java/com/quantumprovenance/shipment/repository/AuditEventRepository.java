package com.quantumprovenance.shipment.repository;

import com.quantumprovenance.shipment.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByShipmentIdOrderByCreatedAtAsc(String shipmentId);
}
