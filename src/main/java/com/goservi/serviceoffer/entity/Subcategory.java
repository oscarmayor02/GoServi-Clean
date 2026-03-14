package com.goservi.serviceoffer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subcategories")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Subcategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
