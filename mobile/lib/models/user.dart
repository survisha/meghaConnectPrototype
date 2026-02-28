enum UserRole {
  HCM,
  ADMIN,
  SAIDUL_OSD,
  APPROVER_JT_SECY,
  CMO_OFFICER,
  DATA_ENTRY_OPERATOR,
  PUBLIC,
}

extension UserRoleExtension on UserRole {
  String get displayName {
    switch (this) {
      case UserRole.HCM:
        return 'Hon. Chief Minister';
      case UserRole.ADMIN:
        return 'System Admin';
      case UserRole.SAIDUL_OSD:
        return 'OSD';
      case UserRole.APPROVER_JT_SECY:
        return 'Joint Secretary';
      case UserRole.CMO_OFFICER:
        return 'CMO Officer';
      case UserRole.DATA_ENTRY_OPERATOR:
        return 'Data Entry Operator';
      case UserRole.PUBLIC:
        return 'Public / Citizen';
    }
  }

  String get badgeLabel {
    switch (this) {
      case UserRole.HCM:
        return 'HCM';
      case UserRole.ADMIN:
        return 'ADMIN';
      case UserRole.SAIDUL_OSD:
        return 'OSD';
      case UserRole.APPROVER_JT_SECY:
        return 'JT. SECY';
      case UserRole.CMO_OFFICER:
        return 'CMO';
      case UserRole.DATA_ENTRY_OPERATOR:
        return 'DEO';
      case UserRole.PUBLIC:
        return 'PUBLIC';
    }
  }

  bool get isFullControl =>
      this == UserRole.HCM ||
      this == UserRole.ADMIN ||
      this == UserRole.SAIDUL_OSD;

  bool get isStaff => this != UserRole.PUBLIC;
}

class AuthUser {
  final String username;
  final String fullName;
  final UserRole role;

  const AuthUser({
    required this.username,
    required this.fullName,
    required this.role,
  });

  Map<String, dynamic> toJson() => {
        'username': username,
        'fullName': fullName,
        'role': role.name,
      };

  factory AuthUser.fromJson(Map<String, dynamic> json) => AuthUser(
        username: json['username'] as String,
        fullName: json['fullName'] as String,
        role: UserRole.values.byName(json['role'] as String),
      );
}
