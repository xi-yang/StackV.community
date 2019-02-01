package net.maxgigapop.mrs.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.ContextNotEmptyException;
import javax.naming.InvalidNameException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class MD2Connect {

    private final Hashtable<String, String> env = new Hashtable<>(11);

    public MD2Connect(String url, String domain, String principal, String credentials) {
        // Identify service provider to use
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + url + "/" + domain);
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
    }

    public boolean validate() {
        try {
            get("");
        } catch (AuthenticationException | InvalidNameException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws AuthenticationException, InvalidNameException {
        MD2Connect conn = new MD2Connect("180-133.research.maxgigapop.net", "dc=research,dc=maxgigapop,dc=net",
                "uid=admin,cn=users,cn=accounts,dc=research,dc=maxgigapop,dc=net", "MAX1234!");
        System.out.println(conn.validate());
    }

    public Attributes get(String filter) throws AuthenticationException, InvalidNameException {
        try {
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // Retrieve all attr and return;
            Attributes attrs = ctx.getAttributes(filter);
            ctx.close();
            return attrs;
        } catch (NameNotFoundException e) {
            return null;
        } catch (AuthenticationException | InvalidNameException e) {
            throw e;
        } catch (NamingException e) {
            System.err.println("Problem getting attribute: " + e);
            return null;
        }
    }

    public String search(String filter) throws AuthenticationException, InvalidNameException {
        try {
            List<String> ret = new ArrayList<>();
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // List names and return
            NamingEnumeration<NameClassPair> list = ctx.list(filter);
            while (list.hasMore()) {
                NameClassPair nc = (NameClassPair) list.next();
                ret.add(nc.getName());
            }

            return Arrays.toString((String[]) ret.toArray(new String[0]));
        } catch (AuthenticationException | InvalidNameException e) {
            throw e;
        } catch (NamingException e) {
            System.err.println("Problem getting attribute: " + e);
            return null;
        }
    }

    public String add(HashMap<String, String[]> entry) throws AuthenticationException, InvalidNameException {
        try {
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // Create attrs
            Attributes attrs = new BasicAttributes(true);
            String cn = entry.get("cn")[0];
            entry.remove("cn");
            for (String entryName : entry.keySet()) {
                Attribute attr = new BasicAttribute(entryName);
                for (String entryAttr : entry.get(entryName)) {
                    attr.add(entryAttr);
                }
                attrs.put(attr);
            }

            // Create the subcontext
            ctx.createSubcontext(cn, attrs);
            ctx.close();
            return "success";
        } catch (AuthenticationException | InvalidNameException e) {
            throw e;
        } catch (NamingException e) {
            System.err.println("Problem adding entry: " + e);
            return "failure";
        }
    }

    public String remove(String filter) throws AuthenticationException, InvalidNameException {
        DirContext ctx = null;
        try {
            // Create the initial directory context from config
            ctx = new InitialDirContext(this.env);

            // Attempt clean removal
            ctx.destroySubcontext(filter);
            ctx.close();
            return "success";
        } catch (ContextNotEmptyException e) {
            // Trim Leaves
            try {
                removeRecur(filter, ctx);

                ctx.close();
            } catch (NamingException ex) {
                System.err.println("Problem recursively removing entry: " + ex);
                return "failure";
            }
            return "pending";
        } catch (AuthenticationException | InvalidNameException e) {
            throw e;
        } catch (NamingException e) {
            System.err.println("Problem removing entry: " + e);
            return "failure";
        }
    }

    private void removeRecur(String filter, DirContext ctx) throws NamingException {
        NamingEnumeration<NameClassPair> list = ctx.list(filter);
        while (list.hasMore()) {
            String newFilter = list.next().getName() + "," + filter;
            removeRecur(newFilter, ctx);
        }
        ctx.destroySubcontext(filter);
    }
}
