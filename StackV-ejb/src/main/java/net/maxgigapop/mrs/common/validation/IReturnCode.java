/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.common.validation;

/**
 *
 * @author joshua
 */
public interface IReturnCode {
    /**
     * Returns a message string that elaborates on the condition represented by a
     * return code object.
     * @return String
     */
    String getMessage();

    /**
     * Returns integer value representation of a return code object.
     * @return  return code
     */
    int toCode();
}
