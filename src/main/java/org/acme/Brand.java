package org.acme;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.util.Map;

@Entity
public class Brand extends PanacheEntityBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    public String name;
    public String description;

    // public String logoUrl;

    // Ou como relação com a entidade Image:
    @OneToOne
    public Image logo;
    @Transient
    public Map<String, String> links;

    public String websiteUrl;
    public int release;

    @ManyToOne
    public Segment segment; // Relacionamento para o segmento da marca

    public Brand() {
    }

    public Brand(Long id, String name, String description, Image logo, String websiteUrl, int release, Segment segment) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.logo = logo;
        this.websiteUrl = websiteUrl;
        this.release = release;
        this.segment = segment;
    }
}
