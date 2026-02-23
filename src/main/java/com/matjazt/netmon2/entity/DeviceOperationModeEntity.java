package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity for device_operation_mode reference table.
 *
 * <p>This entity exists for OpenJPA schema validation and foreign key relationships.
 *
 * <p>Runtime code uses DeviceOperationMode enum directly - this entity is never queried.
 */
@Entity
@Table(name = "device_operation_mode")
@Getter
@Setter
@NoArgsConstructor
public class DeviceOperationModeEntity {

    @Id
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    // No getters/setters needed - this entity is never used in runtime code
}
