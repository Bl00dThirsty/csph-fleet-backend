import os, sys

BASE = r'c:\Users\User\Downloads\gpl-rfid-livraisons\backend'

def write_file(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"Created: {path}")

# ==========================================
# 1. COMMON-LIB ENUMS
# ==========================================
enums = {
    "RegionEnum": """
package com.gpl.common.enums;

public enum RegionEnum {
    ADAMAOUA, CENTRE, EST, EXTREMENORD, LITTORAL, NORD, NORDOUEST, OUEST, SUD, SUDOUEST
}
""",
    "SiteFunctionEnum": """
package com.gpl.common.enums;

public enum SiteFunctionEnum {
    CENTREEMPLISSEUR, DEPOT, POINTAPPROVISIONABLE
}
""",
    "SiteStatusEnum": """
package com.gpl.common.enums;

public enum SiteStatusEnum {
    UNASSIGNED, ASSIGNED, ACTIVE, VERIFIED, SUSPENDED, REJECTED
}
""",
    "VehicleTypeEnum": """
package com.gpl.common.enums;

public enum VehicleTypeEnum {
    VRAC, BOUTEILLES50KG
}
""",
    "TourExecutionMode": """
package com.gpl.common.enums;

public enum TourExecutionMode {
    INTERNAL, EXTERNAL
}
""",
    "StopStatus": """
package com.gpl.common.enums;

public enum StopStatus {
    PENDING, REACHED, IN_PROGRESS, COMPLETED, SKIPPED, DELIVERED, PARTIAL, REJECTED
}
""",
    "ScanDirection": """
package com.gpl.common.enums;

public enum ScanDirection {
    IN, OUT
}
""",
    "PickupStatus": """
package com.gpl.common.enums;

public enum PickupStatus {
    DRAFT, VALIDATED, INPROGRESS, COMPLETED, CANCELLED
}
""",
    "DeclarationStatus": """
package com.gpl.common.enums;

public enum DeclarationStatus {
    DRAFT, SUBMITTED, RECONCILED, DISPUTED
}
""",
    "ReconciliationStatus": """
package com.gpl.common.enums;

public enum ReconciliationStatus {
    PENDING, VERIFIED, REDRESSEMENTAPPLIED
}
""",
    "RecoveryStatus": """
package com.gpl.common.enums;

public enum RecoveryStatus {
    ISSUED, PAID, WAIVED
}
""",
    "DeviceType": """
package com.gpl.common.enums;

public enum DeviceType {
    GPS, PDA, RFIDREADER
}
""",
    "DeviceStatus": """
package com.gpl.common.enums;

public enum DeviceStatus {
    UNASSIGNED, ASSIGNED, INMISSION, OFFLINE, PENDINGSYNC, SYNCING, SYNCED, SYNCFAILED, MAINTENANCE, DEPLOYED, REMOVED, LOST
}
""",
    "RfidTagStatus": """
package com.gpl.common.enums;

public enum RfidTagStatus {
    AVAILABLE, ASSIGNEDTOBOTTLE, INTRANSITOUT, INTRANSITIN, LOST, BLOCKED
}
""",
    "RiskLevel": """
package com.gpl.common.enums;

public enum RiskLevel {
    FAIBLE, MODERE, ELEVE, CRITIQUE, CRITIQUEEXTREME
}
""",
    "RiskEntityType": """
package com.gpl.common.enums;

public enum RiskEntityType {
    MARKETEUR, TRANSPORTEUR, LIVREUR, SITE, TOURNEE, CLIENT, CLIENTSITE, VEHICLE
}
""",
    "AnomalyCategory": """
package com.gpl.common.enums;

public enum AnomalyCategory {
    INVESTIGATION, TECHNICAL
}
""",
    "MfaType": """
package com.gpl.common.enums;

public enum MfaType {
    TOTP, SMS, EMAIL
}
""",
    "MfaStatus": """
package com.gpl.common.enums;

public enum MfaStatus {
    DISABLED, PENDINGSETUP, ENABLED, LOCKED
}
""",
    "ReportFormat": """
package com.gpl.common.enums;

public enum ReportFormat {
    PDF, EXCEL, CSV, JSON
}
""",
    "ReportStatus": """
package com.gpl.common.enums;

public enum ReportStatus {
    PENDING, GENERATING, READY, FAILED, EXPIRED
}
"""
}

for name, content in enums.items():
    write_file(os.path.join(BASE, 'common-lib', 'src', 'main', 'java', 'com', 'gpl', 'common', 'enums', f'{name}.java'), content)

print("Enums generated successfully!")
