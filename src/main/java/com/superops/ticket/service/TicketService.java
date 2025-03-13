package com.superops.ticket.service;

import com.superops.ticket.entity.FormRequest;
import com.superops.ticket.entity.Ticket;
import com.superops.ticket.repository.TicketRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TicketService {

    @Autowired
    TicketRepo ticketRepo;

    public void save(FormRequest form){
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID().toString());
        ticket.setName(form.getName());
        ticket.setSubject(form.getSubject());
        ticket.setBody(form.getBody());
        ticketRepo.save(ticket);
    }

    public List<Ticket> get(){
        return ticketRepo.findAll();
    }
}
