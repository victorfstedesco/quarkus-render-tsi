package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Brand extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Vamos gerar automaticamente no banco
    public Long id;
    public String name;
    public String description;
    public String logoUrl;
    public String websiteUrl;
    public int release;

    public Brand() {
    }

    public Brand(Long id, String name, String description, String logoUrl, String websiteUrl, int release) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logoUrl = logoUrl;
        this.websiteUrl = websiteUrl;
        this.release = release;
    }
}