package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Map;

@Entity
public class Image extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String url;
    public String description;

    @Transient
    public Map<String, String> links;


    public Image() {
    }

    public Image(Long id, String url, String description) {
        this.id = id;
        this.url = url;
        this.description = description;
    }
}
