package com.superops.ticket.service;

import com.superops.ticket.entity.TicketEntry;
import com.superops.ticket.repository.TicketEntryRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TicketEntryService {


    @Autowired
    private TicketEntryRepo ticketEntryRepo;

    public void saveTicket(TicketEntry ticketEntry) {
        ticketEntryRepo.save(ticketEntry);
    }

    public TicketEntry getByTicketId(String ticketId) {
        return ticketEntryRepo.getTicketById(ticketId);
    }

    public void updateTicketEntryByTs(String ticketId, String ts) {
        TicketEntry ticketEntry = ticketEntryRepo.getTicketById(ticketId);
        ticketEntry.setTs(ts);
        saveTicket(ticketEntry);
    }
}
