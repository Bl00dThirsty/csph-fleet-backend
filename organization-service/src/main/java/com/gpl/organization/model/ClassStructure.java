package com.gpl.organization.model;

import com.gpl.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "class_structures", indexes = {
    @Index(name = "idx_class_classification", columnList = "classificationId"),
    @Index(name = "idx_class_parent", columnList = "parentClassStructureId"),
    @Index(name = "idx_class_hierarchy", columnList = "hierarchyPath"),
    @Index(name = "idx_class_object", columnList = "objectName")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassStructure extends BaseEntity {

    @Id
    @Column(name = "classStructureId", updatable = false, nullable = false)
    private String id; // Overriding inherited UUID ID

    private String classificationId;
    private String description;
    private String hierarchyPath;
    private String parentClassStructureId;
    private String objectName;
    private int sortOrder;

    @Builder.Default
    private boolean show = true;

    @Builder.Default
    private boolean useClassInDesc = true;

    @Builder.Default
    private boolean isTopLevel = false;

    private String orgId;
    private String siteId;
}
