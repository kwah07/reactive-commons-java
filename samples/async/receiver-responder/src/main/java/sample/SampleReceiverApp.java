package sample;

import lombok.RequiredArgsConstructor;
import org.reactivecommons.api.domain.DomainEvent;
import org.reactivecommons.api.domain.DomainEventBus;
import org.reactivecommons.async.api.handlers.QueryHandler;
import org.reactivecommons.async.api.HandlerRegistry;
import org.reactivecommons.async.impl.config.annotations.EnableDomainEventBus;
import org.reactivecommons.async.impl.config.annotations.EnableMessageListeners;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import static org.reactivecommons.async.api.HandlerRegistry.*;

@SpringBootApplication
@EnableMessageListeners
@EnableDomainEventBus
public class SampleReceiverApp {
    public static void main(String[] args) {
        SpringApplication.run(SampleReceiverApp.class, args);
    }

    @Bean
    public HandlerRegistry handlerRegistry(MemberReceiver receiver) {
        return register()
            .serveQuery("serveQuery.register.member", receiver)
            .serveQuery("serveQuery.register.member.new", new QueryHandler<MemberRegisteredEvent, AddMemberCommand>(){
                @Override
                public Mono<MemberRegisteredEvent> handle(AddMemberCommand command) {
                    return Mono.just(new MemberRegisteredEvent("42", 69));
                }
            })
            .serveQuery("test.query", message -> {
                return Mono.error(new RuntimeException("Falla Generada Query"));
            }, AddMemberCommand.class);
    }

    @Bean
    public HandlerRegistry handlerRegistryForEmpty(EmptyReceiver emptyReceiver) {
        return register()
            .serveQuery("serveQuery.empty", emptyReceiver);
    }

    @Bean
    public HandlerRegistry eventListeners(SampleUseCase useCase) {
        return register()
            .listenEvent("persona.registrada", useCase::reactToPersonaEvent, MemberRegisteredEvent.class);
    }


    @Bean
    public SampleUseCase sampleUseCase(DomainEventBus eventBus) {
        return new SampleUseCase(eventBus);
    }

    @RequiredArgsConstructor
    public static class SampleUseCase {
        private final DomainEventBus eventBus;

        Mono<Void> reactToPersonaEvent(DomainEvent<MemberRegisteredEvent> event){
            return Mono.from(eventBus.emit(new DomainEvent<>("persona.procesada", "213", event.getData())))
                .doOnSuccess(_v -> System.out.println("Persona procesada"));
        }
    }

}
