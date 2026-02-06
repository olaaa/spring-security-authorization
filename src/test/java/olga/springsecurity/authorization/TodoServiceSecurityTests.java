package olga.springsecurity.authorization;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.test.context.support.WithMockUser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class TodoServiceTest {

    @Autowired
    private TodoService todoService;

    private static final String VERIFIED_ROLE = "ROLE_VERIFIED_USER";
    private static final String INVALID_ROLE = "ROLE_USER";

    // Тест на успешное создание todo с помощью @WithMockUser
    @Test
    @WithMockUser(username = "mockUser", roles = "VERIFIED_USER")
    void testCreateWithMockUser_ReturnsCreatedTodo() {
        String task = "Mocked Task";

        StepVerifier.create(
                        todoService.create(task)
                )
                .assertNext(todo -> {
                    assertNotNull(todo);
                    assertNotNull(todo.id());
                    assertNotNull(todo.owner());
                    assertEquals(task, todo.name());
                })
                .verifyComplete();
    }

    @Test
    void testCreateWithValidRole() {
        // Создаем SecurityContext с требуемой ролью VERIFIED_USER
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password", VERIFIED_ROLE);
        var securityContext = new SecurityContextImpl(authentication);

        StepVerifier.create(
                        // Устанавливаем SecurityContext и вызываем метод create
                        todoService.create("Valid Task")
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                .assertNext(todo -> {
                    // Проверяем, что объект Todo создан корректно
                    assertNotNull(todo);
                    assertEquals("Valid Task", todo.name());
                })
                .verifyComplete();
    }

    @Test
    void testCreateWithInvalidRole() {
        // Создаем SecurityContext с "неправильной" ролью USER
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "password", INVALID_ROLE);
        SecurityContextImpl securityContext = new SecurityContextImpl(authentication);

        StepVerifier.create(
                        // Устанавливаем SecurityContext и проверяем вызов метода create
                        todoService.create("Invalid Task")
                                .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)))
                )
                // Ожидаем исключение AccessDeniedException
                .expectErrorMatches(throwable -> throwable instanceof org.springframework.security.access.AccessDeniedException)
                .verify();
    }

    @Test
    void testCreateWithoutAuthentication() {
        StepVerifier.create(
                        // Пытаемся вызвать метод без аутентификации
                        todoService.create("No Auth Task")
                )
                // Ожидаем AccessDeniedException
                .expectErrorMatches(throwable -> throwable instanceof org.springframework.security.access.AccessDeniedException)
                .verify();
    }
}