package com.superops.ticket.controller;

import com.superops.ticket.entity.FormRequest;
import com.superops.ticket.entity.Ticket;
import com.superops.ticket.service.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class TicketController {

    @Autowired
    TicketService ticketService;

    @GetMapping("/tickets")
    public List<Ticket> showTickets() {
        return ticketService.get();
    }

    @PostMapping("/send")
    public String sendEmail(@ModelAttribute FormRequest form) {
        ticketService.save(form);
        return "true";
    }
}
