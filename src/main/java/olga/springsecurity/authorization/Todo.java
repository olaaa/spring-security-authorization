package olga.springsecurity.authorization;

import java.util.UUID;

public record Todo(String name, UUID id, String owner) {
}
