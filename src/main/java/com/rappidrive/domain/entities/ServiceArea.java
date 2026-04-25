package com.rappidrive.domain.entities;

import com.rappidrive.domain.valueobjects.ServiceAreaId;
import com.rappidrive.domain.valueobjects.TenantId;

import java.time.Instant;
import java.util.Objects;

/**
 * ServiceArea Domain Entity
 * Represents a geographic operating zone for a tenant (city/region).
 * Contains a GeoJSON polygon defining the boundaries where the service operates.
 */
public class ServiceArea {
    
    private final ServiceAreaId id;
    private final TenantId tenantId;
    private final String name;
    private final String geoJsonPolygon; // GeoJSON representation of the operating zone
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    // Private constructor to enforce usage of Builder
    private ServiceArea(Builder builder) {
        this.id = Objects.requireNonNull(builder.id, "ServiceAreaId cannot be null");
        this.tenantId = Objects.requireNonNull(builder.tenantId, "TenantId cannot be null");
        this.name = Objects.requireNonNull(builder.name, "Name cannot be null");
        this.geoJsonPolygon = Objects.requireNonNull(builder.geoJsonPolygon, "GeoJSON polygon cannot be null");
        this.active = builder.active;
        this.createdAt = Objects.requireNonNull(builder.createdAt, "CreatedAt cannot be null");
        this.updatedAt = Objects.requireNonNull(builder.updatedAt, "UpdatedAt cannot be null");
        
        validateGeoJson(geoJsonPolygon);
    }

    private void validateGeoJson(String geoJson) {
        if (geoJson.isBlank()) {
            throw new IllegalArgumentException("GeoJSON polygon cannot be blank");
        }
        // Basic validation - should contain Polygon type
        if (!geoJson.contains("\"type\"") || !geoJson.contains("\"coordinates\"")) {
            throw new IllegalArgumentException("Invalid GeoJSON format");
        }
    }

    // Static factory method for new service areas
    public static ServiceArea create(TenantId tenantId, String name, String geoJsonPolygon) {
        Instant now = Instant.now();
        return new Builder()
                .id(ServiceAreaId.generate())
                .tenantId(tenantId)
                .name(name)
                .geoJsonPolygon(geoJsonPolygon)
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // Behavior: Rename service area
    public ServiceArea rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Service area name cannot be blank");
        }
        return new Builder().from(this).name(newName).updatedAt(Instant.now()).build();
    }

    // Behavior: Deactivate service area
    public ServiceArea deactivate() {
        if (!this.active) {
            throw new IllegalStateException("ServiceArea is already inactive");
        }
        return new Builder()
                .from(this)
                .active(false)
                .updatedAt(Instant.now())
                .build();
    }

    // Behavior: Activate service area
    public ServiceArea activate() {
        if (this.active) {
            throw new IllegalStateException("ServiceArea is already active");
        }
        return new Builder()
                .from(this)
                .active(true)
                .updatedAt(Instant.now())
                .build();
    }

    // Getters
    public ServiceAreaId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getGeoJsonPolygon() {
        return geoJsonPolygon;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceArea that = (ServiceArea) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ServiceArea{" +
                "id=" + id +
                ", tenantId=" + tenantId +
                ", name='" + name + '\'' +
                ", active=" + active +
                '}';
    }

    // Builder pattern
    public static class Builder {
        private ServiceAreaId id;
        private TenantId tenantId;
        private String name;
        private String geoJsonPolygon;
        private boolean active = true;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(ServiceAreaId id) {
            this.id = id;
            return this;
        }

        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder geoJsonPolygon(String geoJsonPolygon) {
            this.geoJsonPolygon = geoJsonPolygon;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder from(ServiceArea serviceArea) {
            this.id = serviceArea.id;
            this.tenantId = serviceArea.tenantId;
            this.name = serviceArea.name;
            this.geoJsonPolygon = serviceArea.geoJsonPolygon;
            this.active = serviceArea.active;
            this.createdAt = serviceArea.createdAt;
            this.updatedAt = serviceArea.updatedAt;
            return this;
        }

        public ServiceArea build() {
            return new ServiceArea(this);
        }
    }
}
