package external;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.testng.annotations.Test;

public class KeycloakTest {
    private static final OkHttpClient httpclient = new OkHttpClient();
    private Map<String, Object> cred = new HashMap<String, Object>() {
        private static final long serialVersionUID = 1L;
        {
            put("secret", "ae53fbea-8812-4c13-918f-0065a1550b7c");
            // put("secret", "b1c063dd-1a2a-464f-8a9f-7fd2fac74a23");
        }
    };;
    private Configuration config = new Configuration("https://k152.maxgigapop.net:8543/auth", "StackV", "StackV", cred,
            null);
    AuthzClient client = AuthzClient.create(config);

    @Test
    public void keycloakTest() throws IOException {
        AccessTokenResponse token = client.obtainAccessToken();
        assert (token != null);

        URL url = new URL(String.format("http://localhost:8080/StackV-web/restapi/app/reload/"));
        Request request = new Request.Builder().url(url).put(null).header("Authorization", "bearer " + token.getToken())
                .build();
        Response response = httpclient.newCall(request).execute();
        assert (response.code() == 204);
    }
}