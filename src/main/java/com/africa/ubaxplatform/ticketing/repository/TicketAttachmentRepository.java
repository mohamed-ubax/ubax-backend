package com.africa.ubaxplatform.ticketing.repository;

import com.africa.ubaxplatform.ticketing.entity.TicketAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, UUID> {

  List<TicketAttachment> findByTicketIdOrderByCreatedAtAsc(UUID ticketId);
}
