package web;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.http.impl.client.DefaultHttpClient;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.representations.AccessTokenResponse;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.Test;

import net.maxgigapop.mrs.common.MD2Connect;

public class MD2Test {
    Map<String, Object> cred = new HashMap<String, Object>() {
        private static final long serialVersionUID = -2605000112217200282L;

        {
            put("secret", "ae53fbea-8812-4c13-918f-0065a1550b7c");
        }
    };
    Configuration config = new Configuration("https://k152.maxgigapop.net:8543/auth", "StackV", "StackV", cred,
            new DefaultHttpClient());

    AuthzClient authzClient = AuthzClient.create(config);
    AccessTokenResponse response = authzClient.obtainAccessToken("xyang", "MAX1234!");
    MD2Connect conn = new MD2Connect("180-133.research.maxgigapop.net:389",
            "uid=admin,cn=users,cn=accounts,dc=research,dc=maxgigapop,dc=net", "MAX1234!");

    // Checkin Tests
    @Test(groups = { "check.adapter" })
    public void testSearch() {
        System.out.println("TEST");
        System.out.println(conn.search("cn=stackv"));
    }

    @Test(groups = { "check.adapter" })
    public void testGet() throws NamingException {
        Attributes res = conn.get("");
        assertEquals(res == null, false);
    }

    @Test(groups = { "check.adapter" }, dependsOnMethods = { "testGet" })
    public void testAdd() throws NamingException {
        // Prep
        HashMap<String, String[]> testEntry = new HashMap<>();
        testEntry.put("cn", new String[] { "cn=test,cn=stackv" });
        testEntry.put("objectclass", new String[] { "top", "dckConfig" });
        testEntry.put("name", new String[] { "testing entry" });

        // Test
        conn.add(testEntry);

        Attributes res = conn.get("cn=test,cn=stackv");
        assertEquals(res.get("name").get(), "testing entry");
    }

    @Test(groups = { "check.adapter" }, dependsOnMethods = { "testAdd" })
    public void testDelete() {
        // Test
        conn.remove("cn=test,cn=stackv");

        Attributes res = conn.get("cn=test,cn=stackv");
        assertEquals(res, null);
    }

    @Test(groups = { "check.adapter" }, dependsOnMethods = { "testAdd", "testDelete" })
    public void testTreeDelete() {
        // Prep
        HashMap<String, String[]> testEntry = new HashMap<>();
        testEntry.put("cn", new String[] { "cn=test,cn=stackv" });
        testEntry.put("objectclass", new String[] { "top", "dckContainer" });
        conn.add(testEntry);
        testEntry.put("cn", new String[] { "cn=testleaf1,cn=test,cn=stackv" });
        conn.add(testEntry);
        testEntry.put("cn", new String[] { "cn=testleaf2,cn=test,cn=stackv" });
        conn.add(testEntry);
        testEntry.put("cn", new String[] { "cn=testleafleaf,cn=testleaf1,cn=test,cn=stackv" });
        conn.add(testEntry);

        // Test
        conn.remove("cn=test,cn=stackv");

        Attributes res = conn.get("cn=test,cn=stackv");
        assertEquals(res, null);
    }

    // Logical
    @AfterGroups({ "check.adapter" })
    public void clean() {
        conn.remove("cn=test,cn=stackv");
    }

    // Functional Tests
    @Test(groups = { "func.adapter" })
    public void apiRegister() {
    }
}