package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class BandwidthProfile {

  private @Valid String capacity = null;
  private @Valid String unit = null;
  private @Valid String qosClass = null;

  /**
   **/
  public BandwidthProfile capacity(String capacity) {
    this.capacity = capacity;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("capacity")
  public String getCapacity() {
    return capacity;
  }

  public void setCapacity(String capacity) {
    this.capacity = capacity;
  }

  /**
   **/
  public BandwidthProfile unit(String unit) {
    this.unit = unit;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("unit")
  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  /**
   **/
  public BandwidthProfile qosClass(String qosClass) {
    this.qosClass = qosClass;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("qos_class")
  public String getQosClass() {
    return qosClass;
  }

  public void setQosClass(String qosClass) {
    this.qosClass = qosClass;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    BandwidthProfile bandwidthProfile = (BandwidthProfile) o;
    return Objects.equals(capacity, bandwidthProfile.capacity) && Objects.equals(unit, bandwidthProfile.unit)
        && Objects.equals(qosClass, bandwidthProfile.qosClass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(capacity, unit, qosClass);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class BandwidthProfile {\n");

    sb.append("    capacity: ").append(toIndentedString(capacity)).append("\n");
    sb.append("    unit: ").append(toIndentedString(unit)).append("\n");
    sb.append("    qosClass: ").append(toIndentedString(qosClass)).append("\n");
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
