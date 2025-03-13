package com.superops.ticket.controller;

import com.superops.ticket.entity.EmailRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class EmailController {

    @GetMapping("/email-form")
    public String showEmailForm(Model model) {
        model.addAttribute("email", new EmailRequest());
        return "emailForm";
    }

    @PostMapping("/send-email")
    public String sendEmail(@ModelAttribute EmailRequest email) {
        System.out.println("Email Subject: " + email.getSubject());
        System.out.println("Email Body: " + email.getBody());
        return "redirect:/email-form";
    }
}
