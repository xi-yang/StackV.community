package net.maxgigapop.mrs.common;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;

import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.NamingException;

import java.util.HashMap;
import java.util.Hashtable;

public class MD2Connect {
    private final Hashtable<String, String> env = new Hashtable<>(11);

    public MD2Connect(String url, String principal, String credentials) {
        // Identify service provider to use
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + url + "/dc=research,dc=maxgigapop,dc=net");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);
    }

    Attributes get(String filter) {
        try {
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // Retrieve all attr and return;
            Attributes attrs = ctx.getAttributes(filter);
            ctx.close();
            return attrs;
        } catch (NamingException e) {
            System.err.println("Problem getting attribute: " + e);
            return null;
        }
    }

    String add(HashMap<String, String> entry) {
        try {
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // Create attrs
            Attributes attrs = new BasicAttributes(true);
            Attribute objclass = new BasicAttribute("objectclass");
            objclass.add("top");
            objclass.add("dckContainer");
            attrs.put(objclass);

            String cn = entry.get("cn");
            entry.remove("cn");
            for (String entryAttr : entry.keySet()) {
                Attribute attr = new BasicAttribute(entryAttr);
                attr.add(entry.get(entryAttr));
                attrs.put(attr);
            }

            // Create the subcontext
            ctx.createSubcontext(cn, attrs);
            ctx.close();
            return "success";
        } catch (NamingException e) {
            System.err.println("Problem adding entry: " + e);
            return "failure";
        }
    }

    String remove(String filter) {
        try {
            // Create the initial directory context from config
            DirContext ctx = new InitialDirContext(this.env);

            // Retrieve all attr and return;
            ctx.destroySubcontext(filter);
            ctx.close();
            return "success";
        } catch (NamingException e) {
            System.err.println("Problem removing entry: " + e);
            return "failure";
        }
    }
}