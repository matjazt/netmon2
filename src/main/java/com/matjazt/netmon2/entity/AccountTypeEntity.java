package com.matjazt.netmon2.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

/**
 * Account type entity representing roles or permission levels.
 *
 * <p>Examples: Admin, MonitoringDevice, Viewer, etc.
 */
@Entity
@Table(name = "account_type")
@Getter
@Setter
@NoArgsConstructor
public class AccountTypeEntity {

    @Id
    @Column(nullable = false)
    private int id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    // Constructors
    public AccountTypeEntity(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountTypeEntity that = (AccountTypeEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AccountType{" + "id=" + id + ", name='" + name + '\'' + '}';
    }
}
