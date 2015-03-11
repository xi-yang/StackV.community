/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.maxgigapop.mrs.rest.api.model;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import net.maxgigapop.mrs.bean.VersionItem;

/**
 *
 * @author max
 */
public class APIVersionGroup {
        @XmlElement(required=true)
        private Long id;
        @XmlElement(required=true)
        private String refUuid = null;
        @XmlElement(required=true)
        private List<VersionItem> versionItems = null;
        @XmlElement(required=true)
        private String status = "";
        
        public Long getId(){
            return id;
        }
        
        public void setId(Long id){
            this.id = id;
        }
        
        public String getRefUuid() {
            return refUuid;
        }

        public void setRefUuid(String refUuid) {
            this.refUuid = refUuid;
        }
        
        public List<VersionItem> getVersionItems() {
            return versionItems;
        }

        public void setVersionItems(List<VersionItem> versionItems) {
            this.versionItems = versionItems;
        }
    
        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        @Override
        public String toString() {
            return "net.maxgigapop.mrs.bean.VersionGroup[ id=" + id + " ]";
        }

}
