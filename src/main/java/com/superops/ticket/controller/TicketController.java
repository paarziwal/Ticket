package com.superops.ticket.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superops.ticket.entity.FormRequest;
import com.superops.ticket.entity.TicketEntry;
import com.superops.ticket.model.SlackSendPayload;
import com.superops.ticket.service.TicketEntryService;
import com.superops.ticket.service.TicketService;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketEntryService ticketEntryService;

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
//        Message message = Message
//                .creator(new com.twilio.type.PhoneNumber("+917550015566"),
//                        new com.twilio.type.PhoneNumber("+16369999734"),
//                        "Testing SMS Via Twilio")
//                .create();
        Message message = Message
                .creator(new com.twilio.type.PhoneNumber("whatsapp:+917550015566"),
                        new com.twilio.type.PhoneNumber("whatsapp:+14155238886"),
                        "Your appointment is coming up on July 21 at 3PM")
                .create();
        System.out.println("Message Body " + message.getBody());
        return "redirect:/";
    }

    @PostMapping("/receive-sms")
    public String receiveMessage(@RequestParam Map<String, String> params) {
        String from = params.get("From");
        String body = params.get("Body");

        System.out.println("Received message from: " + from);
        System.out.println("Message: " + body);
        return "redirect:/";
    }

    @PostMapping("/slack/send")
    public ResponseEntity<String> sendReply(@RequestBody SlackSendPayload slackSendPayload) {
        System.out.println("Looking for ticket with ID: " + slackSendPayload.getTicketId());
        TicketEntry ticket = ticketEntryService.getByTicketId(slackSendPayload.getTicketId());
        if(ticket != null) {
            System.out.println("Ticket found: " + ticket);
            sendSlackThreadReply(ticket.getChannelId(), ticket.getTs(), slackSendPayload);
            return ResponseEntity.ok("Replied Successfully");
        } else {
            System.out.println("Ticket not found");
        }
        return ResponseEntity.ok("No Ticket Found");
    }


    @PostMapping("/slack/receive")
    public ResponseEntity<String> handleSlackEvent(@RequestBody Map<String, Object> payload) {
        System.out.println("Received Slack Event: " + payload);

        if ("url_verification".equals(payload.get("type"))) {
            String challenge = (String) payload.get("challenge");
            return ResponseEntity.ok(challenge);
        }

        if ("event_callback".equals(payload.get("type"))) {
            Map<String, Object> event = (Map<String, Object>) payload.get("event");
            String text = (String) event.get("text");
            String user = (String) event.get("user");
            String channel = (String) event.get("channel");
            String ts = (String) event.get("ts");
            String subtype = (String) event.get("subtype");
            String threadTs = (String) event.get("thread_ts");

            System.out.println("TS " + ts + " Thread-ID " + threadTs);

            if ("message".equals(event.get("type"))
                    && subtype == null
                    && event.get("bot_id") == null) {

                if (threadTs == null) {
                    System.out.println("New Message from " + user + ": " + text);
                    sendBotReply(channel, ts, "✅ Ticket created successfully!");
                    TicketEntry ticketEntry =  new TicketEntry();
                    ticketEntry.setId(UUID.randomUUID().toString());
                    ticketEntry.setTs(ts);
                    ticketEntry.setChannelId(channel);
                    ticketEntryService.saveTicket(ticketEntry);
                }
            }
        }

        return ResponseEntity.ok("Event received");
    }

    public void sendSlackThreadReply(String channel, String threadTs, SlackSendPayload payload) {
        String ts = sendSlackMessage(channel, threadTs, payload.getMessage());
        ticketEntryService.updateTicketEntryByTs(payload.getTicketId(), ts);
    }

    public void sendBotReply(String channel, String threadTs, String message) {
        sendSlackMessage(channel, threadTs, message);
    }

    private String sendSlackMessage(String channel, String threadTs, String message) {
        try {
            String url = "https://slack.com/api/chat.postMessage";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + slackToken);

            Map<String, String> body = new HashMap<>();
            body.put("channel", channel);
            body.put("text", message);
            if (threadTs != null) {
                body.put("thread_ts", threadTs);
            }

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(response.getBody());
            return rootNode.path("ts").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
