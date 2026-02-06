package olga.springsecurity.authorization;

import org.springframework.aop.Advisor;
import org.springframework.aop.support.JdkRegexpMethodPointcut;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authorization.AuthorityReactiveAuthorizationManager;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.method.AuthorizationManagerBeforeReactiveMethodInterceptor;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * {@linkplain org.springframework.security.web.server.authorization.AuthorizationWebFilter}
 * В списке SecurityWebFiltersOrder в самом конце
 * <p>
 * Префиекса ROLE_ в SPEL выражении не должно быть, но должно бвть а базе
 * - @PreAuthorize("hasRole('ADMIN')"): проверяет доступ перед выполнением метода.
 * - @PostAuthorize("returnObject.owner == principal.username"): проверяет доступ после выполнения метода.
 * returnObject -- ключевое слово. owner -- просто пример
 * - `@PreFilter` и `@PostFilter`: фильтрует входящие и исходящие данные соответственно
 * - `@PostAuthorize("returnObject.present and returnObject.get().owner eq principal.username") , так как
 * returnObject это Optional
 * - @PreFilter("@myService.isTodoOwner(filterObject, principal.username)"), filterObject и principal ключевое слово
 * фильтруем список аргументов
 * <p>
 * org.springframework.security.authorization.method.AuthorizationManagerBeforeReactiveMethodInterceptor
 * до выполнения целевого метода
 * org.springframework.security.authorization.method.AuthorizationManagerAfterReactiveMethodInterceptor
 * после выполнения целевого метода
 */
@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity // для того, чтобы работали аннотации
public class ApplicationConfiguration {
    // AccessDeniedException будет перехвачен org.springframework.security.web.server.authorization.ExceptionTranslationWebFilter
//    если нет исключения, то цепочка выполняется дальше chain.filter(exchange)

    //    порядок правил имеет значение! проверка останавливается на первом подошедшем правиле
    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(spec ->
                                spec
                                        .pathMatchers(HttpMethod.HEAD).denyAll()
//                                .pathMatchers(RegexRequestMatcher.regexMatcher("")).authenticated() ошибка компиляции
//                                mvc request matcher
//                                .pathMatchers("/api/orders/{id:\\d}").access(new WebExpressionAuthorizationManager("#id"))
                                        .pathMatchers("/api/orders/{id:\\d}").authenticated()
//                               .access(new WebExpressionAuthorizationManager()) spel. @ -- это бин
                                        .pathMatchers("/api/orders?search").authenticated()
                                        .pathMatchers("/api/**").authenticated() // AntPathRequestMatcher. ** сколько угодно сегментов, * только одно слово.
                                        .pathMatchers(HttpMethod.GET, "/permit-all").permitAll()
//                                .matchers(наследник ServerWebExchangeMatcher)
//      MediaTypeServerWebExchangeMatcher -- правило к типу данных
//                                .matchers(new IpAddressServerWebExchangeMatcher("110.0.0.1")).denyAll() // если нас дидосят
                                        .pathMatchers("/deny-all").denyAll()
//                                .dispatcherTypeMatchers(FORWARD, ERROR) нет в вебфлюксе
                                        .pathMatchers("/anonymous") // показываем страницу регистрации или восстановления пароля или страницу входа
                                        .access((authentication, authorizationContext) ->
                                                authentication.map(auth -> auth == null || auth.getPrincipal() == null)
                                                        .map(AuthorizationDecision::new)
                                                        .defaultIfEmpty(new AuthorizationDecision(true)))
                                        .pathMatchers("/authenticated").authenticated() // либо аутетицикации либо rememberMe
//                                .pathMatchers("/remember-me").rememberMe(), во флюксе его нет
//           во флюксе его нет   .pathMatchers("/fully-authenticated").fullyAuthrnticated() исключаем сессии которые были восстановлены через rememberMe
                                        .pathMatchers("/has-authority").hasAuthority("view")
                                        .pathMatchers("/has-any-authority").hasAnyAuthority("view", "edit")
                                        .pathMatchers("/has-admin-role").hasRole("admin")
                                        .pathMatchers("/has-any-role").hasAnyRole("admin", "user")
                ).build();
    }

    /**
     * Альтернатива:
     * Вместо использования аннотаций можно создать Советчик (advisor)
     * CreateTodo -- это бизнесовый эндпоинт, который создаёт задачу(тудушку)
     * <p>
     * *Аннотация `@Role(BeanDefinition.ROLE_INFRASTRUCTURE)`**
     * - Указывает, что этот бин предназначен для использования как инфраструктурный компонент (например, в целях AOP).
     * - Это дает понять, что компонент выполняет низкоуровневую задачу, связанную с работой фреймворка,
     * а не предоставляет бизнес-логику.
     * <p>
     * - `execution`: Указывает, что мы хотим выбрать метод по сигнатуре.
     * - `*`: Любой возвращаемый тип.
     * - `olga.springsecurity.authorization.TodoService`: Полное имя целевого класса.
     * - `createTodo(**)`: Метод с любым списком параметров.
     *
     * TODO возможно не поддерживает иерархию ролей!
     */
//    @Bean
//    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    static Advisor protectCreateTodoMethodPointcut() {
//        перехват вызова определенного метода
        var jdkRegexpMethodPointcut = new JdkRegexpMethodPointcut();
//        нужно регулярное выражение, а не выражение на языке aspectj
//        "execution(* olga.springsecurity.authorization.TodoService.createTodo(**))"
        jdkRegexpMethodPointcut.setPattern("olga.springsecurity.authorization.TodoService.createTodo()");
//    - Это AOP-интерсептор (перехватчик), который настраивается для выполнения проверок безопасности **до выполнения целевого метода**.
        return new AuthorizationManagerBeforeReactiveMethodInterceptor(jdkRegexpMethodPointcut,
                AuthorityReactiveAuthorizationManager.hasRole("VERIFIED_USER"));
    }

    /**
     * Иерархия ролей
     * левая роль расширяется правой ролью
     * create_posts > view_posts -- не роли, а полномочия
     */
    @Bean
    RoleHierarchy roleHierarchy() {
//        RoleHierarchyUtils.roleHierarchyFromMap(); -- если получаем иерархию с внешнего источника
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_MANAGER > ROLE_VERIFIED_USER
                delete_posts > create_posts
                create_posts > view_posts""");
    }

    // TODO возможно этого нет в webflux. Хотя используется в
    //  org.springframework.security.config.annotation.method.configuration.ReactiveAuthorizationManagerMethodSecurityConfiguration
    // только в том случае, когда используем protectCreateTodoMethodPointcut!
//    @Bean
    MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
    }
}
