package net.maxgigapop.mrs.rest.api.model.sense;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;

public class TopologyDescription {

  private @Valid List<DomainDescription> domains = new ArrayList<DomainDescription>();
  private @Valid List<ServiceDescription> services = new ArrayList<ServiceDescription>();

  /**
   **/
  public TopologyDescription domains(List<DomainDescription> domains) {
    this.domains = domains;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("domains")
  public List<DomainDescription> getDomains() {
    return domains;
  }

  public void setDomains(List<DomainDescription> domains) {
    this.domains = domains;
  }

  /**
   **/
  public TopologyDescription services(List<ServiceDescription> services) {
    this.services = services;
    return this;
  }

  @ApiModelProperty(value = "")
  @JsonProperty("services")
  public List<ServiceDescription> getServices() {
    return services;
  }

  public void setServices(List<ServiceDescription> services) {
    this.services = services;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TopologyDescription topologyDescription = (TopologyDescription) o;
    return Objects.equals(domains, topologyDescription.domains)
        && Objects.equals(services, topologyDescription.services);
  }

  @Override
  public int hashCode() {
    return Objects.hash(domains, services);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TopologyDescription {\n");

    sb.append("    domains: ").append(toIndentedString(domains)).append("\n");
    sb.append("    services: ").append(toIndentedString(services)).append("\n");
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
