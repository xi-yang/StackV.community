/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service.compile;

import javax.ejb.EJBException;

/**
 *
 * @author xyang
 */
public class CompilerFactory {
    static public CompilerBase createCompiler(String compilerClassStr) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        CompilerBase compiler = null;
        try {
            Class<?> aClass = cl.loadClass(compilerClassStr);
            compiler = (CompilerBase) aClass.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new EJBException("CompilerFactory failed to create "+compilerClassStr, ex);
        }
        return compiler;
    }
}
