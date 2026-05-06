package com.rappidrive.infrastructure.persistence.mappers;

import com.rappidrive.domain.entities.Driver;
import com.rappidrive.domain.valueobjects.DriverLicense;
import com.rappidrive.domain.valueobjects.Location;
import com.rappidrive.infrastructure.persistence.entities.DriverJpaEntity;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Driver domain entity and DriverJpaEntity.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DriverMapper {
    
    /**
     * Maps JPA entity to domain entity using reconstruction constructor.
     */
    default Driver toDomain(DriverJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        
        DriverLicense license = new DriverLicense(
            entity.getDriverLicenseNumber(),
            entity.getDriverLicenseCategory(),
            entity.getDriverLicenseIssueDate(),
            entity.getDriverLicenseExpirationDate(),
            entity.getDriverLicenseIsDefinitive()
        );
        
        Location location = null;
        if (entity.getLocationLatitude() != null && entity.getLocationLongitude() != null) {
            location = new Location(entity.getLocationLatitude(), entity.getLocationLongitude());
        }
        
        return new Driver(
            entity.getId(),
            entity.getKeycloakId(),
            entity.getTenantId(),
            entity.getFullName(),
            entity.getEmail(),
            entity.getCpf(),
            entity.getPhone(),
            license,
            entity.getStatus(),
            location
        );
    }
    
    /**
     * Maps domain entity to JPA entity.
     */
    @Mapping(target = "driverLicenseNumber", source = "driverLicense.number")
    @Mapping(target = "driverLicenseCategory", source = "driverLicense.category")
    @Mapping(target = "driverLicenseIssueDate", source = "driverLicense.issueDate")
    @Mapping(target = "driverLicenseExpirationDate", source = "driverLicense.expirationDate")
    @Mapping(target = "driverLicenseIsDefinitive", source = "driverLicense.definitive")
    @Mapping(target = "locationLatitude", source = ".", qualifiedByName = "extractLatitude")
    @Mapping(target = "locationLongitude", source = ".", qualifiedByName = "extractLongitude")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DriverJpaEntity toJpaEntity(Driver domain);
    
    /**
     * Updates an existing JPA entity from domain entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "driverLicenseNumber", source = "driverLicense.number")
    @Mapping(target = "driverLicenseCategory", source = "driverLicense.category")
    @Mapping(target = "driverLicenseIssueDate", source = "driverLicense.issueDate")
    @Mapping(target = "driverLicenseExpirationDate", source = "driverLicense.expirationDate")
    @Mapping(target = "driverLicenseIsDefinitive", source = "driverLicense.definitive")
    @Mapping(target = "locationLatitude", source = ".", qualifiedByName = "extractLatitude")
    @Mapping(target = "locationLongitude", source = ".", qualifiedByName = "extractLongitude")
    void updateJpaEntity(@MappingTarget DriverJpaEntity entity, Driver domain);
    
    @Named("mapDriverLicense")
    default DriverLicense mapDriverLicense(DriverJpaEntity entity) {
        if (entity.getDriverLicenseNumber() == null) {
            return null;
        }
        return new DriverLicense(
            entity.getDriverLicenseNumber(),
            entity.getDriverLicenseCategory(),
            entity.getDriverLicenseIssueDate(),
            entity.getDriverLicenseExpirationDate(),
            entity.getDriverLicenseIsDefinitive()
        );
    }
    
    @Named("mapLocation")
    default Location mapLocation(DriverJpaEntity entity) {
        if (entity.getLocationLatitude() == null || entity.getLocationLongitude() == null) {
            return null;
        }
        return new Location(entity.getLocationLatitude(), entity.getLocationLongitude());
    }
    
    @Named("extractLatitude")
    default Double extractLatitude(Driver driver) {
        return driver.getCurrentLocation().map(Location::getLatitude).orElse(null);
    }
    
    @Named("extractLongitude")
    default Double extractLongitude(Driver driver) {
        return driver.getCurrentLocation().map(Location::getLongitude).orElse(null);
    }
}
