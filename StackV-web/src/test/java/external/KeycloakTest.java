package external;

import net.maxgigapop.mrs.common.KeycloakHandler;

public class KeycloakTest {
    // @Test
    public void getUsersTest() {
        KeycloakHandler handler = new KeycloakHandler();
        handler.getUsers();
    }

    // @Test
    public void addUserTest() {
        KeycloakHandler handler = new KeycloakHandler();
        handler.addUser();
    }
}