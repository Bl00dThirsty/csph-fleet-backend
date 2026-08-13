package com.gpl.user.dto;
import lombok.Data;
import java.time.Instant;
@Data
public class GroupMemberResponse {
    private String personId;
    private String displayName;
    private Instant joinedAt;
    private String addedBy;
    private boolean isActive;
}
