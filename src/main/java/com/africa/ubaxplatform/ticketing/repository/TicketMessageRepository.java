package com.africa.ubaxplatform.ticketing.repository;

import com.africa.ubaxplatform.ticketing.entity.TicketMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, UUID> {

  List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
