package id.ac.ui.cs.advprog.bidmart.auth.security;

import org.aspectj.lang.JoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.annotation.Annotation;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PermissionAspectTest {

    private final PermissionAspect aspect = new PermissionAspect();
    private final JoinPoint joinPoint = mock(JoinPoint.class);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminBypassesPermissionCheck() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
        );
        assertDoesNotThrow(() -> aspect.checkPermission(joinPoint, perm("profile:view")));
    }

    @Test
    void userWithPermission_Passes() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null,
                        List.of(new SimpleGrantedAuthority("profile:view")))
        );
        assertDoesNotThrow(() -> aspect.checkPermission(joinPoint, perm("profile:view")));
    }

    @Test
    void userWithoutPermission_Throws() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", null,
                        List.of(new SimpleGrantedAuthority("profile:view")))
        );
        assertThrows(AccessDeniedException.class,
                () -> aspect.checkPermission(joinPoint, perm("role:manage")));
    }

    @Test
    void noAuthentication_Throws() {
        assertThrows(AccessDeniedException.class,
                () -> aspect.checkPermission(joinPoint, perm("profile:view")));
    }

    private RequiresPermission perm(String value) {
        return new RequiresPermission() {
            @Override public String value() { return value; }
            @Override public Class<? extends Annotation> annotationType() { return RequiresPermission.class; }
        };
    }
}
