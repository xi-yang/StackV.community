package net.maxgigapop.mrs.rest.api.model.sense;

import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ServiceIntentRequestIpRanges   {
  
  private @Valid String start = null;
  private @Valid String end = null;

  /**
   **/
  public ServiceIntentRequestIpRanges start(String start) {
    this.start = start;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("start")
  public String getStart() {
    return start;
  }
  public void setStart(String start) {
    this.start = start;
  }

  /**
   **/
  public ServiceIntentRequestIpRanges end(String end) {
    this.end = end;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("end")
  public String getEnd() {
    return end;
  }
  public void setEnd(String end) {
    this.end = end;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentRequestIpRanges serviceIntentRequestIpRanges = (ServiceIntentRequestIpRanges) o;
    return Objects.equals(start, serviceIntentRequestIpRanges.start) &&
        Objects.equals(end, serviceIntentRequestIpRanges.end);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentRequestIpRanges {\n");
    
    sb.append("    start: ").append(toIndentedString(start)).append("\n");
    sb.append("    end: ").append(toIndentedString(end)).append("\n");
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

