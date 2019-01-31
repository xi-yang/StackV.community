package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class DomainDescriptionEdgePoints {

  private @Valid ServiceTerminationPoint stp = null;
  private @Valid List<CapabilityDescription> capailities = new ArrayList<CapabilityDescription>();
  private @Valid String peerUri = null;
  private @Valid String peerName = null;

  /**
   **/
  public DomainDescriptionEdgePoints stp(ServiceTerminationPoint stp) {
    this.stp = stp;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("stp")
  public ServiceTerminationPoint getStp() {
    return stp;
  }

  public void setStp(ServiceTerminationPoint stp) {
    this.stp = stp;
  }

  /**
   **/
  public DomainDescriptionEdgePoints capailities(List<CapabilityDescription> capailities) {
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
  public DomainDescriptionEdgePoints peerUri(String peerUri) {
    this.peerUri = peerUri;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("peer_uri")
  public String getPeerUri() {
    return peerUri;
  }

  public void setPeerUri(String peerUri) {
    this.peerUri = peerUri;
  }

  /**
   **/
  public DomainDescriptionEdgePoints peerName(String peerName) {
    this.peerName = peerName;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("peer_name")
  public String getPeerName() {
    return peerName;
  }

  public void setPeerName(String peerName) {
    this.peerName = peerName;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainDescriptionEdgePoints domainDescriptionEdgePoints = (DomainDescriptionEdgePoints) o;
    return Objects.equals(stp, domainDescriptionEdgePoints.stp)
        && Objects.equals(capailities, domainDescriptionEdgePoints.capailities)
        && Objects.equals(peerUri, domainDescriptionEdgePoints.peerUri)
        && Objects.equals(peerName, domainDescriptionEdgePoints.peerName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stp, capailities, peerUri, peerName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DomainDescriptionEdgePoints {\n");

    sb.append("    stp: ").append(toIndentedString(stp)).append("\n");
    sb.append("    capailities: ").append(toIndentedString(capailities)).append("\n");
    sb.append("    peerUri: ").append(toIndentedString(peerUri)).append("\n");
    sb.append("    peerName: ").append(toIndentedString(peerName)).append("\n");
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
