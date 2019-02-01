package net.maxgigapop.mrs.common;

import java.util.Arrays;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakHandler {
    private static String serverURL = "https://k152.maxgigapop.net:8543/auth";
    private static String serverRealm = "StackV";
    private static String clientID = "StackV";
    private static String clientSecret = "ae53fbea-8812-4c13-918f-0065a1550b7c";

    private RealmResource realm;

    public KeycloakHandler() {
        // Keycloak keycloak =
        // KeycloakBuilder.builder().serverUrl(serverURL).realm(serverRealm).grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientId(clientID).clientSecret(clientSecret).build();
        realm = null;// keycloak.realm(serverRealm);
    }

    // STATIC
    public static boolean verifyUserRole(HttpRequest httpRequest, String role) {
        KeycloakSecurityContext securityContext = (KeycloakSecurityContext) httpRequest
                .getAttribute(KeycloakSecurityContext.class.getName());
        final AccessToken accessToken = securityContext.getToken();

        Set<String> roleSet;
        roleSet = accessToken.getRealmAccess().getRoles();

        return roleSet.contains(role);
    }

    // Admin Client
    public void addUser() {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue("temppass");
        credential.setTemporary(true);

        UserRepresentation user = new UserRepresentation();
        user.setUsername("ABCTestUser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setCredentials(Arrays.asList(credential));
        user.setEnabled(true);

        // Create testuser
        Response result = realm.users().create(user);
        if (result.getStatus() != 201) {
            System.err.println("Couldn't create user.");
            System.exit(0);
        }

        // Delete testuser
        String locationHeader = result.getHeaderString("Location");
        String userId = locationHeader.replaceAll(".*/(.*)$", "$1");
        realm.users().get(userId).remove();
    }

    public UsersResource getUsers() {
        return realm.users();
    }
}