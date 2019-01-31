package net.maxgigapop.mrs.common;

import java.util.Set;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

public class KeycloakHandler {

    public static boolean verifyUserRole(HttpRequest httpRequest, String role) {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                .getAttribute(KeycloakSecurityContext.class.getName());
        final AccessToken accessToken = securityContext.getToken();

        Set<String> roleSet;
        roleSet = accessToken.getRealmAccess().getRoles();

        return roleSet.contains(role);
    }
}