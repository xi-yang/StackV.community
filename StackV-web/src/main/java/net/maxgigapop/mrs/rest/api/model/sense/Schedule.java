package net.maxgigapop.mrs.rest.api.model.sense;

import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class Schedule   {
  
  private @Valid String start = null;
  private @Valid String end = null;
  private @Valid String duration = null;

  /**
   **/
  public Schedule start(String start) {
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
  public Schedule end(String end) {
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

  /**
   **/
  public Schedule duration(String duration) {
    this.duration = duration;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("duration")
  public String getDuration() {
    return duration;
  }
  public void setDuration(String duration) {
    this.duration = duration;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Schedule schedule = (Schedule) o;
    return Objects.equals(start, schedule.start) &&
        Objects.equals(end, schedule.end) &&
        Objects.equals(duration, schedule.duration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end, duration);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Schedule {\n");
    
    sb.append("    start: ").append(toIndentedString(start)).append("\n");
    sb.append("    end: ").append(toIndentedString(end)).append("\n");
    sb.append("    duration: ").append(toIndentedString(duration)).append("\n");
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

