/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common;

import java.util.List;
import javax.ejb.EJBException;

/**
 *
 * @author xyang
 */
public class EJBExceptionNegotiable extends EJBException {
    protected List modifierDeltaList = null;

    public List getModifierDeltaList() {
        return modifierDeltaList;
    }

    public void setModifierDeltaList(List modifierDeltaList) {
        this.modifierDeltaList = modifierDeltaList;
    }
}
