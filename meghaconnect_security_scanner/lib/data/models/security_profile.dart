import '../../core/utils/json_helpers.dart';

class SecurityProfile {
  const SecurityProfile({
    required this.username,
    required this.fullName,
    required this.role,
    this.gateName,
    this.location,
  });

  final String username;
  final String fullName;
  final String role;
  final String? gateName;
  final String? location;

  factory SecurityProfile.fromJson(Map<String, dynamic> json) {
    return SecurityProfile(
      username: readString(json, ['username', 'userName', 'loginId']) ?? '',
      fullName: readString(json, ['fullName', 'name', 'displayName']) ?? '',
      role: readString(json, ['role', 'authority', 'userRole']) ?? '',
      gateName: readString(json, ['gateName', 'assignedGate', 'gate']),
      location: readString(json, ['location', 'assignedLocation']),
    );
  }
}
