package net.maxgigapop.mrs.rest.api.model;

import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.rest.api.model.BandwidthProfile;
import net.maxgigapop.mrs.rest.api.model.ServiceIntentRequestQueries;
import net.maxgigapop.mrs.rest.api.model.ServiceTerminationPoint;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentRequestConnections   {
  
  private @Valid String name = null;
  private @Valid BandwidthProfile bandwidth = null;
  private @Valid List<ServiceTerminationPoint> terminals = new ArrayList<ServiceTerminationPoint>();
  private @Valid List<ServiceIntentRequestQueries> queries = new ArrayList<ServiceIntentRequestQueries>();

  /**
   **/
  public ServiceIntentRequestConnections name(String name) {
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
  public ServiceIntentRequestConnections bandwidth(BandwidthProfile bandwidth) {
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
  public ServiceIntentRequestConnections terminals(List<ServiceTerminationPoint> terminals) {
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
  public ServiceIntentRequestConnections queries(List<ServiceIntentRequestQueries> queries) {
    this.queries = queries;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("queries")
  public List<ServiceIntentRequestQueries> getQueries() {
    return queries;
  }
  public void setQueries(List<ServiceIntentRequestQueries> queries) {
    this.queries = queries;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentRequestConnections serviceIntentRequestConnections = (ServiceIntentRequestConnections) o;
    return Objects.equals(name, serviceIntentRequestConnections.name) &&
        Objects.equals(bandwidth, serviceIntentRequestConnections.bandwidth) &&
        Objects.equals(terminals, serviceIntentRequestConnections.terminals) &&
        Objects.equals(queries, serviceIntentRequestConnections.queries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, bandwidth, terminals, queries);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentRequestConnections {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    bandwidth: ").append(toIndentedString(bandwidth)).append("\n");
    sb.append("    terminals: ").append(toIndentedString(terminals)).append("\n");
    sb.append("    queries: ").append(toIndentedString(queries)).append("\n");
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

