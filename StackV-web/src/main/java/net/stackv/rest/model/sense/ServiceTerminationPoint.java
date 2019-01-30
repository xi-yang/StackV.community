package net.stackv.rest.model.sense;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class ServiceTerminationPoint {

  private @Valid String type = null;
  private @Valid String uri = null;
  private @Valid String label = null;

  /**
   **/
  public ServiceTerminationPoint type(String type) {
    this.type = type;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  /**
   **/
  public ServiceTerminationPoint uri(String uri) {
    this.uri = uri;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("uri")
  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   **/
  public ServiceTerminationPoint label(String label) {
    this.label = label;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServiceTerminationPoint serviceTerminationPoint = (ServiceTerminationPoint) o;
    return Objects.equals(type, serviceTerminationPoint.type) && Objects.equals(uri, serviceTerminationPoint.uri)
        && Objects.equals(label, serviceTerminationPoint.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, uri, label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ServiceTerminationPoint {\n");

    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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
