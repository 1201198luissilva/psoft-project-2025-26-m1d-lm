package pt.psoft.g1.psoftg1.bookmanagement.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewAMQP;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthorEventRabbitmqReceiver {

    private final AuthorService authorService;

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Books_Created.name}")
    public void receiveAuthorCreated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            JsonNode jsonNode = objectMapper.readTree(jsonReceived);

            String name = jsonNode.get("name").asText();

            System.out.println(" [x] Received Author Created by AMQP: " + msg + ".");
            try {
                authorService.create(new AuthorViewAMQP(name));
                System.out.println(" [x] New author inserted from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Author already exists. No need to store it.");
            }
        } catch (Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }

}