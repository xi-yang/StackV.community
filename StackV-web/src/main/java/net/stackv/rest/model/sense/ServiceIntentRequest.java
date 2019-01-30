package net.stackv.rest.model.sense;

import java.util.ArrayList;
import java.util.List;
import net.stackv.rest.model.sense.ServiceIntentRequestConnections;
import net.stackv.rest.model.sense.ServiceIntentRequestIpRanges;
import net.stackv.rest.model.sense.ServiceIntentRequestQueries;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentRequest   {
  
  private @Valid String serviceType = null;
  private @Valid String serviceAlias = null;
  private @Valid List<ServiceIntentRequestIpRanges> ipRanges = new ArrayList<ServiceIntentRequestIpRanges>();
  private @Valid List<ServiceIntentRequestConnections> connections = new ArrayList<ServiceIntentRequestConnections>();
  private @Valid List<ServiceIntentRequestQueries> queries = new ArrayList<ServiceIntentRequestQueries>();

  /**
   **/
  public ServiceIntentRequest serviceType(String serviceType) {
    this.serviceType = serviceType;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("service_type")
  @NotNull
  public String getServiceType() {
    return serviceType;
  }
  public void setServiceType(String serviceType) {
    this.serviceType = serviceType;
  }

  /**
   **/
  public ServiceIntentRequest serviceAlias(String serviceAlias) {
    this.serviceAlias = serviceAlias;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("service_alias")
  public String getServiceAlias() {
    return serviceAlias;
  }
  public void setServiceAlias(String serviceAlias) {
    this.serviceAlias = serviceAlias;
  }

  /**
   **/
  public ServiceIntentRequest ipRanges(List<ServiceIntentRequestIpRanges> ipRanges) {
    this.ipRanges = ipRanges;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("ip_ranges")
  public List<ServiceIntentRequestIpRanges> getIpRanges() {
    return ipRanges;
  }
  public void setIpRanges(List<ServiceIntentRequestIpRanges> ipRanges) {
    this.ipRanges = ipRanges;
  }

  /**
   **/
  public ServiceIntentRequest connections(List<ServiceIntentRequestConnections> connections) {
    this.connections = connections;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("connections")
  @NotNull
  public List<ServiceIntentRequestConnections> getConnections() {
    return connections;
  }
  public void setConnections(List<ServiceIntentRequestConnections> connections) {
    this.connections = connections;
  }

  /**
   **/
  public ServiceIntentRequest queries(List<ServiceIntentRequestQueries> queries) {
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
    ServiceIntentRequest serviceIntentRequest = (ServiceIntentRequest) o;
    return Objects.equals(serviceType, serviceIntentRequest.serviceType) &&
        Objects.equals(serviceAlias, serviceIntentRequest.serviceAlias) &&
        Objects.equals(ipRanges, serviceIntentRequest.ipRanges) &&
        Objects.equals(connections, serviceIntentRequest.connections) &&
        Objects.equals(queries, serviceIntentRequest.queries);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceType, serviceAlias, ipRanges, connections, queries);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentRequest {\n");
    
    sb.append("    serviceType: ").append(toIndentedString(serviceType)).append("\n");
    sb.append("    serviceAlias: ").append(toIndentedString(serviceAlias)).append("\n");
    sb.append("    ipRanges: ").append(toIndentedString(ipRanges)).append("\n");
    sb.append("    connections: ").append(toIndentedString(connections)).append("\n");
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

