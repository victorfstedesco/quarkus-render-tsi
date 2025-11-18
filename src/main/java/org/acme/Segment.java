package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Map;

@Entity
public class Segment extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    public String description;
    @Transient
    public Map<String, String> links;

    public Segment() {
    }

    public Segment(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }
}
