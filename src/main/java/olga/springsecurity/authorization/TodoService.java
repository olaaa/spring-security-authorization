package olga.springsecurity.authorization;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TodoService {

    @PreAuthorize("hasRole('VERIFIED_USER')")
    public Mono<Todo> create(String taskName) {
        return Mono.just(new Todo(taskName, UUID.randomUUID(), "owner"));
    }
}
