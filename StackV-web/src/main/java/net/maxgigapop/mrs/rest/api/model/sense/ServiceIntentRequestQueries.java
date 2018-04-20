package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentRequestQueries   {
  
  private @Valid String ask = null;
  private @Valid List<Object> options = new ArrayList<Object>();

  /**
   **/
  public ServiceIntentRequestQueries ask(String ask) {
    this.ask = ask;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("ask")
  public String getAsk() {
    return ask;
  }
  public void setAsk(String ask) {
    this.ask = ask;
  }

  /**
   **/
  public ServiceIntentRequestQueries options(List<Object> options) {
    this.options = options;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("options")
  public List<Object> getOptions() {
    return options;
  }
  public void setOptions(List<Object> options) {
    this.options = options;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentRequestQueries serviceIntentRequestQueries = (ServiceIntentRequestQueries) o;
    return Objects.equals(ask, serviceIntentRequestQueries.ask) &&
        Objects.equals(options, serviceIntentRequestQueries.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ask, options);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentRequestQueries {\n");
    
    sb.append("    ask: ").append(toIndentedString(ask)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
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

