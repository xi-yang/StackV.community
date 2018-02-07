package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.ArrayList;
import java.util.List;
import net.maxgigapop.mrs.rest.api.model.sense.DomainDescriptionEdgePoints;
import javax.validation.constraints.*;
import javax.validation.Valid;


import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;


public class DomainDescription   {
  
  private @Valid String name = null;
  private @Valid String uri = null;
  private @Valid List<DomainDescriptionEdgePoints> edgePoints = new ArrayList<DomainDescriptionEdgePoints>();

  /**
   **/
  public DomainDescription name(String name) {
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
  public DomainDescription uri(String uri) {
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
  public DomainDescription edgePoints(List<DomainDescriptionEdgePoints> edgePoints) {
    this.edgePoints = edgePoints;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("edge_points")
  public List<DomainDescriptionEdgePoints> getEdgePoints() {
    return edgePoints;
  }
  public void setEdgePoints(List<DomainDescriptionEdgePoints> edgePoints) {
    this.edgePoints = edgePoints;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DomainDescription domainDescription = (DomainDescription) o;
    return Objects.equals(name, domainDescription.name) &&
        Objects.equals(uri, domainDescription.uri) &&
        Objects.equals(edgePoints, domainDescription.edgePoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, uri, edgePoints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DomainDescription {\n");
    
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    uri: ").append(toIndentedString(uri)).append("\n");
    sb.append("    edgePoints: ").append(toIndentedString(edgePoints)).append("\n");
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

