package net.stackv.rest.model.sense;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class ServiceIntentRequestConnections {

  private @Valid String name = null;
  private @Valid BandwidthProfile bandwidth = null;
  private @Valid Schedule schedule = null;
  private @Valid List<ServiceTerminationPoint> terminals = new ArrayList<ServiceTerminationPoint>();

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
  public ServiceIntentRequestConnections schedule(Schedule schedule) {
    this.schedule = schedule;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("schedule")
  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
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

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceIntentRequestConnections serviceIntentRequestConnections = (ServiceIntentRequestConnections) o;
    return Objects.equals(name, serviceIntentRequestConnections.name)
        && Objects.equals(bandwidth, serviceIntentRequestConnections.bandwidth)
        && Objects.equals(schedule, serviceIntentRequestConnections.schedule)
        && Objects.equals(terminals, serviceIntentRequestConnections.terminals);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, bandwidth, schedule, terminals);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceIntentRequestConnections {\n");

    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    bandwidth: ").append(toIndentedString(bandwidth)).append("\n");
    sb.append("    schedule: ").append(toIndentedString(schedule)).append("\n");
    sb.append("    terminals: ").append(toIndentedString(terminals)).append("\n");
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
