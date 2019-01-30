package net.stackv.rest.model.sense;

import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class DiscoveryDescriptionEndPoints {

  private @Valid String url = null;
  private @Valid String description = null;

  /**
   **/
  public DiscoveryDescriptionEndPoints url(String url) {
    this.url = url;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  /**
   **/
  public DiscoveryDescriptionEndPoints description(String description) {
    this.description = description;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DiscoveryDescriptionEndPoints discoveryDescriptionEndPoints = (DiscoveryDescriptionEndPoints) o;
    return Objects.equals(url, discoveryDescriptionEndPoints.url)
        && Objects.equals(description, discoveryDescriptionEndPoints.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DiscoveryDescriptionEndPoints {\n");

    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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
