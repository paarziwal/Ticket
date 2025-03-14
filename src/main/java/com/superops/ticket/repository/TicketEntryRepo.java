package com.superops.ticket.repository;

import com.superops.ticket.entity.TicketEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketEntryRepo extends JpaRepository<TicketEntry, String> {

    @Query("SELECT t FROM TicketEntry t WHERE t.id = :id")
    TicketEntry getTicketById(@Param("id") String ticketId);
}
