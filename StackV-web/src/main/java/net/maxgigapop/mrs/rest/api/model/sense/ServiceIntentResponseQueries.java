package net.maxgigapop.mrs.rest.api.model;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentResponseQueries   {
  
  private @Valid String asked = null;
  private @Valid List<Object> results = new ArrayList<Object>();

  /**
   **/
  public ServiceIntentResponseQueries asked(String asked) {
    this.asked = asked;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("asked")
  public String getAsked() {
    return asked;
  }
  public void setAsked(String asked) {
    this.asked = asked;
  }

  /**
   **/
  public ServiceIntentResponseQueries results(List<Object> results) {
    this.results = results;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("results")
  public List<Object> getResults() {
    return results;
  }
  public void setResults(List<Object> results) {
    this.results = results;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentResponseQueries serviceIntentResponseQueries = (ServiceIntentResponseQueries) o;
    return Objects.equals(asked, serviceIntentResponseQueries.asked) &&
        Objects.equals(results, serviceIntentResponseQueries.results);
  }

  @Override
  public int hashCode() {
    return Objects.hash(asked, results);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentResponseQueries {\n");
    
    sb.append("    asked: ").append(toIndentedString(asked)).append("\n");
    sb.append("    results: ").append(toIndentedString(results)).append("\n");
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

