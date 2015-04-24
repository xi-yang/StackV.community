/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.service;

import java.util.concurrent.Future;
import javax.ejb.AsyncResult;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 * @author xyang
 */
@Stateless
@LocalBean
public class HandleServiceCall {  
    //$$ TODO: add workerEjbPath and spaModelTtl as parameters
    public Future<String> runWorkflow(String serviceInstanceUuid) {
        String status = "SUCCESS";
        AsyncResult<String> asyncResult = new AsyncResult<>(status);
        return asyncResult;
    }
}
