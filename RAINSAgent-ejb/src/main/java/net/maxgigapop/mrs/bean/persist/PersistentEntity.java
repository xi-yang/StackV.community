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

	boolean persistent = true;

	public boolean isPersistent() {
		return persistent;
	}

	public void setPersistent(boolean aValue) {
		this.persistent = aValue;
	}
}
