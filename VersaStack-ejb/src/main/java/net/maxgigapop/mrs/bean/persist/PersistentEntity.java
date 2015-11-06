/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean.persist;

import javax.annotation.PostConstruct;

/**
 *
 * @author xyang
 */
@SuppressWarnings("unchecked")
public class PersistentEntity {

    // for most use cases, we keep this to false so the original entity instance got managed and we see instant ID change
    boolean persistent = false;

    public boolean isPersistent() {
        return persistent;
    }

    public void setPersistent(boolean aValue) {
        this.persistent = aValue;
    }
}
