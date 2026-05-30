class OfflineAiNotesService {
  const OfflineAiNotesService();

  String generateAppointmentNote({
    required String citizenName,
    required String purpose,
    String? scheme,
    String? department,
    String? appointmentType,
    String? remarks,
  }) {
    final target = [
      if ((scheme ?? '').trim().isNotEmpty) scheme!.trim(),
      if ((department ?? '').trim().isNotEmpty) department!.trim(),
    ].join(' / ');
    final type = (appointmentType ?? '').trim();
    final extra = (remarks ?? '').trim();
    return 'Citizen ${citizenName.trim().isEmpty ? 'applicant' : citizenName.trim()} '
        'visited for ${purpose.trim().isEmpty ? 'an appointment request' : purpose.trim()}. '
        'The request is related to ${target.isEmpty ? 'the concerned office' : target}. '
        '${type.isEmpty ? '' : 'Appointment type: $type. '}'
        '${extra.isEmpty ? '' : 'DEO remarks: $extra. '}'
        'Details were recorded locally and the appointment is pending review.';
  }
}
