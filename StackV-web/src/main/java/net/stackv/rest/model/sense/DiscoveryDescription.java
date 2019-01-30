package net.stackv.rest.model.sense;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class DiscoveryDescription {

  private @Valid String version = null;
  private @Valid List<CapabilityDescription> capailities = new ArrayList<CapabilityDescription>();
  private @Valid List<DiscoveryDescriptionEndPoints> endPoints = new ArrayList<DiscoveryDescriptionEndPoints>();

  /**
   **/
  public DiscoveryDescription version(String version) {
    this.version = version;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("version")
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   **/
  public DiscoveryDescription capailities(List<CapabilityDescription> capailities) {
    this.capailities = capailities;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("capailities")
  public List<CapabilityDescription> getCapailities() {
    return capailities;
  }

  public void setCapailities(List<CapabilityDescription> capailities) {
    this.capailities = capailities;
  }

  /**
   **/
  public DiscoveryDescription endPoints(List<DiscoveryDescriptionEndPoints> endPoints) {
    this.endPoints = endPoints;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("end_points")
  public List<DiscoveryDescriptionEndPoints> getEndPoints() {
    return endPoints;
  }

  public void setEndPoints(List<DiscoveryDescriptionEndPoints> endPoints) {
    this.endPoints = endPoints;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiscoveryDescription discoveryDescription = (DiscoveryDescription) o;
    return Objects.equals(version, discoveryDescription.version)
        && Objects.equals(capailities, discoveryDescription.capailities)
        && Objects.equals(endPoints, discoveryDescription.endPoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, capailities, endPoints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DiscoveryDescription {\n");

    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    capailities: ").append(toIndentedString(capailities)).append("\n");
    sb.append("    endPoints: ").append(toIndentedString(endPoints)).append("\n");
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
