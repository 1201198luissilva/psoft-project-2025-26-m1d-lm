package pt.psoft.g1.psoftg1.configuration;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import pt.psoft.g1.psoftg1.shared.model.AuthorEvents;

@Profile("!test")
@Configuration
public class RabbitmqClientConfig {

    @Bean
    public DirectExchange directAuthorExchange() {
        return new DirectExchange("LMS.authors");
    }

    private static class ReceiverConfig {

        @Bean(name = "autoDeleteQueue_Author_Created")
        public Queue autoDeleteQueue_Author_Created() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Author_Updated() {
            return new AnonymousQueue();
        }

        @Bean
        public Queue autoDeleteQueue_Author_Deleted() {
            return new AnonymousQueue();
        }

        @Bean
        public Binding bindingAuthorCreated(DirectExchange directAuthorExchange,
                                            @Qualifier("autoDeleteQueue_Author_Created") Queue autoDeleteQueue_Author_Created) {
            return BindingBuilder.bind(autoDeleteQueue_Author_Created)
                    .to(directAuthorExchange)
                    .with(AuthorEvents.AUTHOR_CREATED);
        }

        @Bean
        public Binding bindingAuthorUpdated(DirectExchange directAuthorExchange,
                                            Queue autoDeleteQueue_Author_Updated) {
            return BindingBuilder.bind(autoDeleteQueue_Author_Updated)
                    .to(directAuthorExchange)
                    .with(AuthorEvents.AUTHOR_UPDATED);
        }

        @Bean
        public Binding bindingAuthorDeleted(DirectExchange directAuthorExchange,
                                            Queue autoDeleteQueue_Author_Deleted) {
            return BindingBuilder.bind(autoDeleteQueue_Author_Deleted)
                    .to(directAuthorExchange)
                    .with(AuthorEvents.AUTHOR_DELETED);
        }
    }
}
