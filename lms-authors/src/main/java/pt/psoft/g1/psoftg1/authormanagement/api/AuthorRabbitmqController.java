package pt.psoft.g1.psoftg1.authormanagement.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.authormanagement.services.AuthorService;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AuthorRabbitmqController {

    private final AuthorService authorService;

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Created.name}")
    public void receiveAuthorCreated(Message msg) {

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [x] Received Author Created by AMQP: " + msg + ".");
            try {
                authorService.create(authorViewAMQP);
                System.out.println(" [x] New author inserted from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Author already exists. No need to store it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Updated.name}")
    public void receiveAuthorUpdated(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);

            System.out.println(" [x] Received Author Updated by AMQP: " + msg + ".");
            try {
                authorService.update(authorViewAMQP);
                System.out.println(" [x] Author updated from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Author does not exists or wrong version. Nothing stored.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }

    @RabbitListener(queues = "#{autoDeleteQueue_Author_Deleted.name}")
    public void receiveAuthorDeleted(Message msg) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            String jsonReceived = new String(msg.getBody(), StandardCharsets.UTF_8);
            AuthorViewAMQP authorViewAMQP = objectMapper.readValue(jsonReceived, AuthorViewAMQP.class);
            Long authorNumber = authorViewAMQP.getAuthorNumber();

            System.out.println(" [x] Received Author Removed by AMQP: " + msg + ".");
            try {
                authorService.deleteByAuthorNumber(authorNumber);
                System.out.println(" [x] New author removed from AMQP: " + msg + ".");
            } catch (Exception e) {
                System.out.println(" [x] Author dont exists. No need to remove it.");
            }
        }
        catch(Exception ex) {
            System.out.println(" [x] Exception receiving author event from AMQP: '" + ex.getMessage() + "'");
        }
    }
}
