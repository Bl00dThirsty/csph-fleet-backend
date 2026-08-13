package com.gpl.user.dto;
import com.gpl.common.dto.ModificationSubObject;
import com.gpl.user.model.UserRoleAssignment;
import com.gpl.user.model.UserSiteAssignment;
import lombok.Data;
import java.time.Instant;
import java.util.List;
@Data
public class PersonResponse {
    private String id;
    private String personId;
    private Long personUid;
    private String organizationId;
    private String orgId;
    private String primarySiteId;
    private String siteId;
    private String locationOrg;
    private String locationSite;
    private String location;
    private String email;
    private String firstName;
    private String lastName;
    private String displayName;
    private String title;
    private String jobCode;
    private String jobCodeDescription;
    private String primaryPhone;
    private String addressLine1;
    private String city;
    private String language;
    private String avatarUrl;
    private String supervisorId;
    private boolean isActive;
    private boolean isLocked;
    private boolean isCertified;
    private boolean acceptingWfMail;
    private boolean locToServReq;
    private boolean statusIface;
    private Integer deviceClass;
    private String deviceClassDescription;
    private String wfMailElection;
    private String wfMailElectionDescription;
    private String transEmailElection;
    private String transEmailElectionDescription;
    private String status;
    private String statusDescription;
    private Instant statusDate;
    
    private List<PhoneDto> phones;
    private List<EmailDto> emails;
    private List<UserRoleAssignment> roles;
    private List<UserSiteAssignment> sites;
    
    private String modificationsRef;
    private List<ModificationSubObject> recentModifications;
}
