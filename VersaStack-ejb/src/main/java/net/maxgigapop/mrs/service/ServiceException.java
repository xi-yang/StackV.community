/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service;

import javax.ejb.EJBException;

/**
 *
 * @author xyang
 */
public class ServiceException extends EJBException {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(Exception ex) {
        super(ex);
    }

    public ServiceException(String message, Exception ex) {
        super(message, ex);
    } 
}
