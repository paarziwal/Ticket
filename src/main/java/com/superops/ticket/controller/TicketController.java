package com.superops.ticket.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.slack.api.Slack;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.request.views.ViewsOpenRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;
import com.slack.api.model.block.ActionsBlock;
import com.slack.api.model.block.InputBlock;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.model.block.SectionBlock;
import com.slack.api.model.block.composition.MarkdownTextObject;
import com.slack.api.model.block.composition.OptionObject;
import com.slack.api.model.block.composition.PlainTextObject;
import com.slack.api.model.block.element.ButtonElement;
import com.slack.api.model.block.element.PlainTextInputElement;
import com.slack.api.model.block.element.StaticSelectElement;
import com.slack.api.model.view.View;
import com.slack.api.model.view.ViewClose;
import com.slack.api.model.view.ViewSubmit;
import com.slack.api.model.view.ViewTitle;
import com.superops.ticket.entity.FormRequest;
import com.superops.ticket.entity.TicketEntry;
import com.superops.ticket.model.SlackSendPayload;
import com.superops.ticket.service.TicketEntryService;
import com.superops.ticket.service.TicketService;
import com.twilio.Twilio;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

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
                    TicketEntry ticketEntry = new TicketEntry();
                    String ticketId = UUID.randomUUID().toString();
                    ticketEntry.setId(ticketId);
                    ticketEntry.setTs(ts);
                    ticketEntry.setChannelId(channel);
                    ticketEntry.setStatus("New");
                    ticketEntryService.saveTicket(ticketEntry);
                    sendTicketCreationConfirmation(channel, ts, ticketId);
                }
            }
        }

        return ResponseEntity.ok("Event received");
    }

    private String sendTicketCreationConfirmation(String channel, String threadTs, String ticketId) {
        try {
            Slack slack = Slack.getInstance();

            List<LayoutBlock> blocks = new ArrayList<>();
            blocks.add(SectionBlock.builder()
                    .text(MarkdownTextObject.builder()
                            .text("✅ Ticket #" + ticketId + " created successfully!")
                            .build())
                    .build());

            blocks.add(ActionsBlock.builder()
                    .blockId("ticket_actions_" + ticketId)
                    .elements(Arrays.asList(
                            ButtonElement.builder()
                                    .text(PlainTextObject.builder().text("Update Ticket").build())
                                    .actionId("update_ticket")
                                    .value(ticketId)
                                    .style("primary")
                                    .build(),
                            ButtonElement.builder()
                                    .text(PlainTextObject.builder().text("Close Ticket").build())
                                    .actionId("close_ticket")
                                    .value(ticketId)
                                    .style("danger")
                                    .build()
                    ))
                    .build());
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .token(slackToken)
                    .channel(channel)
                    .threadTs(threadTs)
                    .blocks(blocks)
                    .text("Ticket created")
                    .build();
            slack.methods().chatPostMessage(request);
            return request.getThreadTs();
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/slack/interaction")
    @ResponseBody
    public ResponseEntity<String> handleInteractivity(@RequestParam("payload") String payloadJson) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode payload = mapper.readTree(payloadJson);
            System.out.println("Interaction payload: " + payloadJson);
            String type = payload.path("type").asText();

            if ("block_actions".equals(type)) {
                JsonNode actionsNode = payload.path("actions");
                if (actionsNode.size() > 0) {
                    JsonNode firstAction = actionsNode.get(0);
                    String actionId = firstAction.path("action_id").asText();
                    String ticketId = firstAction.path("value").asText();
                    String channelId = payload.path("channel").path("id").asText();
                    String triggerId = payload.path("trigger_id").asText();
                    System.out.println("Action ID: " + actionId);
                    System.out.println("Ticket ID: " + ticketId);
                    if ("update_ticket".equals(actionId)) {
                        openUpdateTicketModal(triggerId, ticketId);
                    } else if ("close_ticket".equals(actionId)) {
                        TicketEntry ticket = ticketEntryService.getByTicketId(ticketId);
                        if (ticket != null) {
                            ticket.setStatus("Closed");
                            ticketEntryService.saveTicket(ticket);
                            sendSlackMessage(channelId, ticket.getTs(), "✅ This ticket has been closed.");
                        }
                    }
                }
            } else if ("view_submission".equals(type)) {
                return handleViewSubmissionInternal(payload);
            }

            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity
                    .badRequest()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Error processing interaction: " + e.getMessage());
        }
    }

    private ResponseEntity<String> handleViewSubmissionInternal(JsonNode payload) {
        try {
            JsonNode view = payload.path("view");
            String callbackId = view.path("callback_id").asText();
            if ("update_ticket_modal".equals(callbackId)) {
                JsonNode stateValues = view.path("state").path("values");
                String ticketId = stateValues
                        .path("ticket_id_block")
                        .path("ticket_id")
                        .path("value").asText();
                String technician = stateValues
                        .path("technician_block")
                        .path("technician")
                        .path("selected_option")
                        .path("value").asText();
                String status = stateValues
                        .path("status_block")
                        .path("status")
                        .path("selected_option")
                        .path("value").asText();
                String category = stateValues
                        .path("category_block")
                        .path("category")
                        .path("selected_option")
                        .path("value").asText();
                TicketEntry ticket = ticketEntryService.getByTicketId(ticketId);
                if (ticket != null) {
                    ticket.setTechnician(technician);
                    ticket.setStatus(status);
                    ticket.setCategory(category);
                    ticketEntryService.saveTicket(ticket);
                    String message = String.format("Ticket updated: Assigned to %s, Status: %s, Category: %s",
                            technician, status, category);
                    sendSlackMessage(ticket.getChannelId(), ticket.getTs(), message);
                }
            }
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}");
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("response_action", "errors");
            errorResponse.put("errors", Map.of(
                    "ticket_id_block", "An error occurred: " + e.getMessage()
            ));
            try {
                return ResponseEntity
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new ObjectMapper().writeValueAsString(errorResponse));
            } catch (JsonProcessingException jsonEx) {
                return ResponseEntity
                        .badRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{}");
            }
        }
    }

    private void openUpdateTicketModal(String triggerId, String ticketId) throws IOException, SlackApiException {
        Slack slack = Slack.getInstance();
        TicketEntry ticket = ticketEntryService.getByTicketId(ticketId);
        if (ticket == null) {
            return;
        }
        List<OptionObject> technicianOptions = Arrays.asList(
                OptionObject.builder().text(PlainTextObject.builder().text("John Doe").build()).value("john_doe").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Jane Smith").build()).value("jane_smith").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Bob Johnson").build()).value("bob_johnson").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Alice Williams").build()).value("alice_williams").build()
        );
        List<OptionObject> statusOptions = Arrays.asList(
                OptionObject.builder().text(PlainTextObject.builder().text("New").build()).value("new").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Open").build()).value("open").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("InProgress").build()).value("in_progress").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Resolved").build()).value("resolved").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Closed").build()).value("closed").build()
        );
        List<OptionObject> categoryOptions = Arrays.asList(
                OptionObject.builder().text(PlainTextObject.builder().text("Frontend").build()).value("frontend").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Backend").build()).value("backend").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Database").build()).value("database").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Infrastructure").build()).value("infrastructure").build(),
                OptionObject.builder().text(PlainTextObject.builder().text("Security").build()).value("security").build()
        );
        View modal = View.builder()
                .type("modal")
                .callbackId("update_ticket_modal")
                .title(ViewTitle.builder().text("Update Ticket").type("plain_text").build())
                .submit(ViewSubmit.builder().text("Submit").type("plain_text").build())
                .close(ViewClose.builder().text("Cancel").type("plain_text").build())
                .blocks(Arrays.asList(
                        SectionBlock.builder()
                                .text(MarkdownTextObject.builder().text("*Ticket ID:* " + ticketId).build())
                                .build(),
                        InputBlock.builder()
                                .blockId("technician_block")
                                .label(PlainTextObject.builder().text("Technician").build())
                                .element(StaticSelectElement.builder()
                                        .actionId("technician")
                                        .placeholder(PlainTextObject.builder().text("Select a technician").build())
                                        .options(technicianOptions)
                                        .build())
                                .build(),
                        InputBlock.builder()
                                .blockId("status_block")
                                .label(PlainTextObject.builder().text("Status").build())
                                .element(StaticSelectElement.builder()
                                        .actionId("status")
                                        .placeholder(PlainTextObject.builder().text("Select a status").build())
                                        .options(statusOptions)
                                        .build())
                                .build(),
                        InputBlock.builder()
                                .blockId("category_block")
                                .label(PlainTextObject.builder().text("Category").build())
                                .element(StaticSelectElement.builder()
                                        .actionId("category")
                                        .placeholder(PlainTextObject.builder().text("Select a category").build())
                                        .options(categoryOptions)
                                        .build())
                                .build(),
                        InputBlock.builder()
                                .blockId("ticket_id_block")
                                .label(PlainTextObject.builder().text("Ticket ID").build())
                                .element(PlainTextInputElement.builder()
                                        .actionId("ticket_id")
                                        .initialValue(ticketId)
                                        .build())
                                .optional(true)
                                .build()
                ))
                .build();
        ViewsOpenRequest viewsOpenRequest = ViewsOpenRequest.builder()
                .token(slackToken)
                .triggerId(triggerId)
                .view(modal)
                .build();
        slack.methods().viewsOpen(viewsOpenRequest);
    }

    private String sendSlackMessage(String channel, String threadTs, String message) {
        try {
            Slack slack = Slack.getInstance();
            ChatPostMessageRequest request = ChatPostMessageRequest.builder()
                    .token(slackToken)
                    .channel(channel)
                    .threadTs(threadTs)
                    .text(message)
                    .build();
            ChatPostMessageResponse response = slack.methods().chatPostMessage(request);
            if (response.isOk()) {
                return response.getTs();
            } else {
                System.err.println("Error sending message: " + response.getError());
            }
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void sendSlackThreadReply(String channel, String threadTs, SlackSendPayload payload) {
        String ts = sendSlackMessage(channel, threadTs, payload.getMessage());
        ticketEntryService.updateTicketEntryByTs(payload.getTicketId(), ts);
    }
}
