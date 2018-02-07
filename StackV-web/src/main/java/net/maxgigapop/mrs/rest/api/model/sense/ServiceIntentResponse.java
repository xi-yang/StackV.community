package net.maxgigapop.mrs.rest.api.model;

import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.rest.api.model.ServiceIntentResponseQueries;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentResponse   {
  
  private @Valid String serviceUuid = null;
  private @Valid String queryUuid = null;
  private @Valid List<ServiceIntentResponseQueries> queries = new ArrayList<ServiceIntentResponseQueries>();
  private @Valid String model = null;

  /**
   **/
  public ServiceIntentResponse serviceUuid(String serviceUuid) {
    this.serviceUuid = serviceUuid;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty("service_uuid")
  @NotNull
  public String getServiceUuid() {
    return serviceUuid;
  }
  public void setServiceUuid(String serviceUuid) {
    this.serviceUuid = serviceUuid;
  }

  /**
   **/
  public ServiceIntentResponse queryUuid(String queryUuid) {
    this.queryUuid = queryUuid;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("query_uuid")
  public String getQueryUuid() {
    return queryUuid;
  }
  public void setQueryUuid(String queryUuid) {
    this.queryUuid = queryUuid;
  }

  /**
   **/
  public ServiceIntentResponse queries(List<ServiceIntentResponseQueries> queries) {
    this.queries = queries;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("queries")
  public List<ServiceIntentResponseQueries> getQueries() {
    return queries;
  }
  public void setQueries(List<ServiceIntentResponseQueries> queries) {
    this.queries = queries;
  }

  /**
   **/
  public ServiceIntentResponse model(String model) {
    this.model = model;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("model")
  public String getModel() {
    return model;
  }
  public void setModel(String model) {
    this.model = model;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentResponse serviceIntentResponse = (ServiceIntentResponse) o;
    return Objects.equals(serviceUuid, serviceIntentResponse.serviceUuid) &&
        Objects.equals(queryUuid, serviceIntentResponse.queryUuid) &&
        Objects.equals(queries, serviceIntentResponse.queries) &&
        Objects.equals(model, serviceIntentResponse.model);
  }

  @Override
  public int hashCode() {
    return Objects.hash(serviceUuid, queryUuid, queries, model);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentResponse {\n");
    
    sb.append("    serviceUuid: ").append(toIndentedString(serviceUuid)).append("\n");
    sb.append("    queryUuid: ").append(toIndentedString(queryUuid)).append("\n");
    sb.append("    queries: ").append(toIndentedString(queries)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
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

