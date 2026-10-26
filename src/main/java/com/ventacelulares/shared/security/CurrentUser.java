package com.ventacelulares.shared.security;

import com.ventacelulares.shared.domain.Sucursal;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private static final Map<String, Sucursal> DEV_BRANCHES = Map.of(
            "vendedor", Sucursal.PALERMO,
            "supervisor", Sucursal.PALERMO);

    public String name() {
        return authentication().getName();
    }

    public String role() {
        return authentication().getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException("El usuario no tiene un rol asignado"));
    }

    public Optional<Sucursal> sucursal() {
        String currentRole = role();
        if (!Roles.VENDEDOR.equals(currentRole) && !Roles.SUPERVISOR.equals(currentRole)) {
            return Optional.empty();
        }
        return Optional.ofNullable(DEV_BRANCHES.get(name()));
    }

    private Authentication authentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AccessDeniedException("Se requiere un usuario autenticado");
        }
        return authentication;
    }
}
