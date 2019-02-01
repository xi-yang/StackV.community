package external;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;

import javax.naming.AuthenticationException;
import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import net.maxgigapop.mrs.common.MD2Connect;

public class MD2Test {
    MD2Connect conn = new MD2Connect("180-133.research.maxgigapop.net:389", "dc=research,dc=maxgigapop,dc=net",
            "uid=admin,cn=users,cn=accounts,dc=research,dc=maxgigapop,dc=net", "MAX1234!");

    @Test
    public void testSearch() throws AuthenticationException, InvalidNameException {
        conn.search("cn=stackv");
    }

    @Test
    public void testGet() throws NamingException {
        Attributes res = conn.get("");
        assertEquals(res == null, false);
    }

    @Test(dependsOnMethods = { "testGet" })
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

    @Test(dependsOnMethods = { "testAdd" })
    public void testDelete() throws AuthenticationException, InvalidNameException {
        // Prep
        HashMap<String, String[]> testEntry = new HashMap<>();
        testEntry.put("cn", new String[] { "cn=test,cn=stackv" });
        testEntry.put("objectclass", new String[] { "top", "dckConfig" });
        testEntry.put("name", new String[] { "testing entry" });
        conn.add(testEntry);

        // Test
        conn.remove("cn=test,cn=stackv");

        Attributes res = conn.get("cn=test,cn=stackv");
        assertEquals(res, null);
    }

    @Test(dependsOnMethods = { "testAdd", "testDelete" })
    public void testTreeDelete() throws AuthenticationException, InvalidNameException {
        // Prep
        HashMap<String, String[]> testEntry = new HashMap<>();
        testEntry.put("cn", new String[] { "cn=test,cn=stackv" });
        testEntry.put("objectclass", new String[] { "top", "dckContainer" });
        testEntry.put("name", new String[] { "testing entry" });
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
    @AfterMethod
    public void clean() throws AuthenticationException, InvalidNameException {
        conn.remove("cn=test,cn=stackv");
    }
}