package com.superops.ticket.controller;

import com.superops.ticket.entity.FormRequest;
import com.superops.ticket.service.TicketService;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class TicketController {

    @Autowired
    TicketService ticketService;

    @GetMapping("/")
    public String showTickets(Model model) {
        model.addAttribute("ticketList", ticketService.get());
        return "listTicket";
    }

    @GetMapping("/ticketForm")
    public String showTicketForm(Model model) {
        model.addAttribute("email", new FormRequest());
        return "ticketForm";
    }

    @PostMapping("/saveTicket")
    public String sendEmail(@ModelAttribute FormRequest form) {
        ticketService.save(form);
        Twilio.init(accountSid, accountToken);
        Message message = Message
                .creator(new com.twilio.type.PhoneNumber("+917550015566"),
                        new com.twilio.type.PhoneNumber("+16369999734"),
                        "Testing SMS Via Twilio")
                .create();
        return "redirect:/";
    }
}
