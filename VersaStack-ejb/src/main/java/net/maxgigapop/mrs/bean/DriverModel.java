/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.bean;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 *
 * @author xyang
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class DriverModel extends ModelBase {

    @ManyToOne
    @JoinColumn(name = "driverInstanceId")
    protected DriverInstance driverInstance = null;

    @Override
    public String toString() {
        return "net.maxgigapop.mrs.model.DriverModel[ id=" + id + " ]";
    }
}
