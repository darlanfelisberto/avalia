package br.edu.iffar.avalia.core.resource;

import br.edu.iffar.avalia.core.service.OidcClientService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.util.Set;

@Provider
public class OriginFilter implements ContainerRequestFilter {
    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    @Inject
    private OidcClientService oidcClient;

    @Override
    public void filter(ContainerRequestContext request) {
        if (MUTATING_METHODS.contains(request.getMethod())
                && !oidcClient.frontendOrigin().equals(request.getHeaderString("Origin"))) {
            request.abortWith(Response.status(Response.Status.FORBIDDEN)
                    .header("Cache-Control", "no-store").build());
        }
    }
}
