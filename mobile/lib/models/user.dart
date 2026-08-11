enum UserRole {
  SUPER_ADMIN,
  DEPARTMENT_ADMIN,
  DEO,
  DEPARTMENT_PA,
  HEAD_DEPARTMENT,
  HCM,
  ADMIN,
  APPROVER,
  SECURITY_POLICE,
  PUBLIC,
}

extension UserRoleExtension on UserRole {
  String get displayName {
    switch (this) {
      case UserRole.SUPER_ADMIN:
        return 'Super Admin';
      case UserRole.DEPARTMENT_ADMIN:
        return 'Department Admin';
      case UserRole.DEPARTMENT_PA:
        return 'Department PA';
      case UserRole.HEAD_DEPARTMENT:
        return 'Head of Department';
      case UserRole.HCM:
        return 'Hon. Chief Minister';
      case UserRole.ADMIN:
        return 'System Admin';
      case UserRole.APPROVER:
        return 'Approver';
      case UserRole.DEO:
        return 'Data Entry Operator';
      case UserRole.SECURITY_POLICE:
        return 'Security / Police';
      case UserRole.PUBLIC:
        return 'Public / Citizen';
    }
  }

  String get badgeLabel {
    switch (this) {
      case UserRole.SUPER_ADMIN:
        return 'SUPER ADMIN';
      case UserRole.DEPARTMENT_ADMIN:
        return 'DEPT ADMIN';
      case UserRole.DEPARTMENT_PA:
        return 'DEPT PA';
      case UserRole.HEAD_DEPARTMENT:
        return 'HEAD';
      case UserRole.HCM:
        return 'HCM';
      case UserRole.ADMIN:
        return 'ADMIN';
      case UserRole.APPROVER:
        return 'APPROVER';
      case UserRole.DEO:
        return 'DEO';
      case UserRole.SECURITY_POLICE:
        return 'SECURITY';
      case UserRole.PUBLIC:
        return 'PUBLIC';
    }
  }

  bool get isFullControl =>
      this == UserRole.SUPER_ADMIN ||
      this == UserRole.DEPARTMENT_ADMIN ||
      this == UserRole.HCM ||
      this == UserRole.ADMIN ||
      this == UserRole.APPROVER;

  bool get isStaff => this != UserRole.PUBLIC;
}

class AuthUser {
  final String username;
  final String fullName;
  final UserRole role;
  final int? visitorId;
  final int? departmentId;
  final String? departmentCode;
  final String? departmentName;
  final bool passwordChangeRequired;

  const AuthUser({
    required this.username,
    required this.fullName,
    required this.role,
    this.visitorId,
    this.departmentId,
    this.departmentCode,
    this.departmentName,
    this.passwordChangeRequired = false,
  });

  Map<String, dynamic> toJson() => {
        'username': username,
        'fullName': fullName,
        'role': role.name,
        if (visitorId != null) 'visitorId': visitorId,
        if (departmentId != null) 'departmentId': departmentId,
        if (departmentCode != null) 'departmentCode': departmentCode,
        if (departmentName != null) 'departmentName': departmentName,
        'passwordChangeRequired': passwordChangeRequired,
      };

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        username: json['username'] as String,
        fullName: json['fullName'] as String,
        role: UserRole.values.byName(json['role'] as String),
        visitorId: (json['visitorId'] as num?)?.toInt(),
        departmentId: (json['departmentId'] as num?)?.toInt(),
        departmentCode: json['departmentCode'] as String?,
        departmentName: json['departmentName'] as String?,
        passwordChangeRequired: json['passwordChangeRequired'] == true,
      );
}
