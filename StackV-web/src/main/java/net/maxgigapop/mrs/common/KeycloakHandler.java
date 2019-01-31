package net.maxgigapop.mrs.common;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

public class KeycloakHandler {
    private static String serverURL = "https://k152.maxgigapop.net:8543/auth";
    private static String serverRealm = "StackV";
    private static String clientID = "StackV";
    private static String clientSecret = "ae53fbea-8812-4c13-918f-0065a1550b7c";

    private static Map<String, Object> cred = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;
        {
            put("secret", clientSecret);
        }
    };;
    private static Configuration config = new Configuration(serverURL, serverRealm, clientID, cred, null);
    private static AuthzClient client = AuthzClient.create(config);

    public static DecodedJWT getServiceAccessToken() {
        return JWT.decode(client.obtainAccessToken().getToken());
    }

    public void addUser() {
        Keycloak keycloak = KeycloakBuilder.builder().serverUrl(serverURL).realm(serverRealm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS).clientId(clientID).clientSecret(clientSecret).build();

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
        Response result = keycloak.realm("rest-example").users().create(user);
        if (result.getStatus() != 201) {
            System.err.println("Couldn't create user.");
            System.exit(0);
        }

        // Delete testuser
        String locationHeader = result.getHeaderString("Location");
        String userId = locationHeader.replaceAll(".*/(.*)$", "$1");
        keycloak.realm(serverRealm).users().get(userId).remove();
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
}