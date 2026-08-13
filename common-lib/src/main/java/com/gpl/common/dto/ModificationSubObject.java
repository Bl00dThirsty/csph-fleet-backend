package com.gpl.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.gpl.common.model.EntityModification;
import lombok.*;

import java.time.Instant;
import java.util.List;

/**
 * DTO pour embarquer les modifications en tant que sous-objets d'une entité.
 * Utilisé dans les réponses API pour fournir l'historique des modifications
 * directement dans le corps de l'entité (pattern Maximo collection_ref).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModificationSubObject {

    private String modificationId;
    private String action;
    private String actionDescription;
    private String changeby;
    private String changebyDisplayName;
    private Instant changedate;
    private String description;
    private List<FieldChangeDto> changes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldChangeDto {
        private String fieldName;
        private String fieldLabel;
        private String oldValue;
        private String oldValueDescription;
        private String newValue;
        private String newValueDescription;
        private String valueType;
    }

    /**
     * Convertit une EntityModification JPA en DTO embarquable.
     */
    public static ModificationSubObject fromEntity(EntityModification mod) {
        return ModificationSubObject.builder()
                .modificationId(mod.getId())
                .action(mod.getAction())
                .actionDescription(mod.getActionDescription())
                .changeby(mod.getChangeby())
                .changebyDisplayName(mod.getChangebyDisplayName())
                .changedate(mod.getChangedate())
                .description(mod.getDescription())
                .changes(mod.getChanges() == null ? List.of() :
                        mod.getChanges().stream()
                                .map(fc -> FieldChangeDto.builder()
                                        .fieldName(fc.getFieldName())
                                        .fieldLabel(fc.getFieldLabel())
                                        .oldValue(fc.getOldValue())
                                        .oldValueDescription(fc.getOldValueDescription())
                                        .newValue(fc.getNewValue())
                                        .newValueDescription(fc.getNewValueDescription())
                                        .valueType(fc.getValueType())
                                        .build())
                                .toList())
                .build();
    }
}
