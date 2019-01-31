package external;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.squareup.okhttp.OkHttpClient;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.testng.annotations.Test;

import net.maxgigapop.mrs.common.KeycloakHandler;

public class KeycloakTest {
    private static String serverURL = "https://k152.maxgigapop.net:8543/auth";
    private static String serverRealm = "StackV";
    private static String clientID = "StackV";
    private static String clientSecret = "ae53fbea-8812-4c13-918f-0065a1550b7c";

    private static final OkHttpClient httpclient = new OkHttpClient();
    private Map<String, Object> cred = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;
        {
            put("secret", clientSecret);
        }
    };;
    private Configuration config = new Configuration(serverURL, serverRealm, clientID, cred, null);
    AuthzClient client = AuthzClient.create(config);

    @Test
    public void clientTest() throws IOException {
        assert (KeycloakHandler.getServiceAccessToken() != null);
    }

    @Test
    public void addUserTest() {
         KeycloakHandler handler = new KeycloakHandler();
        handler.addUser();
    }
}