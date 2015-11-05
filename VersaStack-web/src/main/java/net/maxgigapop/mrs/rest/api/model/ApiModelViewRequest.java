/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import net.maxgigapop.mrs.common.ModelUtil;

/**
 *
 * @author max
 */
@XmlRootElement(name = "view")
@XmlAccessorType(XmlAccessType.FIELD)
public class ApiModelViewRequest {

    @XmlElementWrapper
    @XmlElement(required = true, name = "filter")
    protected List<ModelUtil.ModelViewFilter> filters = new ArrayList<ModelUtil.ModelViewFilter>();

    public List<ModelUtil.ModelViewFilter> getFilters() {
        return filters;
    }

    public void setFilters(List<ModelUtil.ModelViewFilter> filters) {
        this.filters = filters;
    }

}
