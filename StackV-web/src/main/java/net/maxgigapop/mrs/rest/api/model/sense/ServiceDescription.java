package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.rest.api.model.sense.BandwidthProfile;
import net.maxgigapop.mrs.rest.api.model.sense.ServiceTerminationPoint;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceDescription   {
  
  private @Valid String name = null;
  private @Valid String uuid = null;
  private @Valid BandwidthProfile bandwidth = null;
  private @Valid List<ServiceTerminationPoint> terminals = new ArrayList<ServiceTerminationPoint>();
  private @Valid String status = null;

  /**
   **/
  public ServiceDescription name(String name) {
    this.name = name;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("name")
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  /**
   **/
  public ServiceDescription uuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("uuid")
  public String getUuid() {
    return uuid;
  }
  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  /**
   **/
  public ServiceDescription bandwidth(BandwidthProfile bandwidth) {
    this.bandwidth = bandwidth;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("bandwidth")
  public BandwidthProfile getBandwidth() {
    return bandwidth;
  }
  public void setBandwidth(BandwidthProfile bandwidth) {
    this.bandwidth = bandwidth;
  }

  /**
   **/
  public ServiceDescription terminals(List<ServiceTerminationPoint> terminals) {
    this.terminals = terminals;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("terminals")
  public List<ServiceTerminationPoint> getTerminals() {
    return terminals;
  }
  public void setTerminals(List<ServiceTerminationPoint> terminals) {
    this.terminals = terminals;
  }

  /**
   **/
  public ServiceDescription status(String status) {
    this.status = status;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("status")
  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceDescription serviceDescription = (ServiceDescription) o;
    return Objects.equals(name, serviceDescription.name) &&
        Objects.equals(uuid, serviceDescription.uuid) &&
        Objects.equals(bandwidth, serviceDescription.bandwidth) &&
        Objects.equals(terminals, serviceDescription.terminals) &&
        Objects.equals(status, serviceDescription.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, uuid, bandwidth, terminals, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceDescription {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    bandwidth: ").append(toIndentedString(bandwidth)).append("\n");
    sb.append("    terminals: ").append(toIndentedString(terminals)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

