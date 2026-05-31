import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../core/config/app_config.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/connectivity_service.dart';
import '../services/navigation_service.dart';
import '../services/offline_ai_notes_service.dart';
import '../services/offline_repository.dart';
import '../services/sync_service.dart';
import 'visitor_registration_screen.dart';

class NewAppointmentScreen extends StatefulWidget {
  final bool isWalkIn;
  final bool isPublic;
  final Map<String, dynamic>? initialVisitor;
  final VoidCallback? onViewAppointments;

  const NewAppointmentScreen({
    super.key,
    this.isWalkIn = false,
    this.isPublic = false,
    this.initialVisitor,
    this.onViewAppointments,
  });

  @override
  State<NewAppointmentScreen> createState() => _NewAppointmentScreenState();
}

class _NewAppointmentScreenState extends State<NewAppointmentScreen> {
  final _formKey = GlobalKey<FormState>();

  final _searchMobileCtrl = TextEditingController();
  final _searchEpicCtrl = TextEditingController();
  final _searchReferenceCtrl = TextEditingController();

  // Appointment Details
  String _agendaType = 'A4';
  String _location = 'SHILLONG';
  bool _isOrganisation = false;
  bool _includeSchemeDetails = false;
  String _applicationType = 'NEW';
  String _projectCategory = 'Community Infrastructure';
  String _beneficiaryType = 'Community';
  String _beneficiaryCount = '1-50';
  final _agendaBriefCtrl = TextEditingController();
  final _profileCtrl = TextEditingController();
  final _schemeTypeCtrl = TextEditingController();
  final _projectNameCtrl = TextEditingController();
  final _estimatedCostCtrl = TextEditingController();
  final _communityContributionCtrl = TextEditingController();
  final _justificationCtrl = TextEditingController();

  bool _submitted = false;
  bool _loading = false;
  bool _searching = false;
  bool _visitorLoading = false;
  bool _consentAccepted = false;
  String? _contextError;
  String? _submittedAppId;
  String? _submittedToken;
  Map<String, dynamic>? _selectedVisitor;
  List<Map<String, dynamic>> _searchResults = [];

  static const _agendaTypes = ['A1', 'A2', 'A3', 'A4', 'B1', 'B2'];
  static const _locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];
  static const _applicationTypes = ['NEW', 'REMINDER'];
  static const _projectCategories = [
    'Community Infrastructure',
    'Education',
    'Health',
    'Livelihood',
    'Sports',
    'Other',
  ];
  static const _beneficiaryTypes = [
    'Individual',
    'Community',
    'Institution',
    'Village',
  ];
  static const _beneficiaryCounts = ['1-50', '51-100', '101-500', '500+'];

  static const _agendaDescriptions = {
    'A1': 'Cabinet/Flight/State Function',
    'A2': 'Events & Public Programs',
    'A3': 'File Clearing & Admin',
    'A4': 'Individual Appointment',
    'B1': 'Public Durbar',
    'B2': 'Walk-in',
  };

  @override
  void initState() {
    super.initState();
    if (widget.isWalkIn) _agendaType = 'B2';
    if (widget.initialVisitor != null) {
      _selectedVisitor = Map<String, dynamic>.from(widget.initialVisitor!);
      _agendaBriefCtrl.text =
          widget.initialVisitor!['briefDescription']?.toString() ?? '';
      final agenda = widget.initialVisitor!['agendaType']?.toString();
      if (agenda != null && _agendaTypes.contains(agenda)) _agendaType = agenda;
    }
    Future.microtask(_loadPublicVisitor);
  }

  @override
  void dispose() {
    _searchMobileCtrl.dispose();
    _searchEpicCtrl.dispose();
    _searchReferenceCtrl.dispose();
    _agendaBriefCtrl.dispose();
    _profileCtrl.dispose();
    _schemeTypeCtrl.dispose();
    _projectNameCtrl.dispose();
    _estimatedCostCtrl.dispose();
    _communityContributionCtrl.dispose();
    _justificationCtrl.dispose();
    super.dispose();
  }

  int? get _selectedVisitorId {
    final id = _selectedVisitor?['id'];
    if (id is num) return id.toInt();
    return int.tryParse(id?.toString() ?? '');
  }

  Future<void> _loadPublicVisitor() async {
    if (!widget.isPublic || !mounted) return;
    final auth = context.read<AuthService>();
    final visitorId = auth.user?.visitorId;
    if (visitorId == null || visitorId <= 0) {
      setState(() {
        _contextError =
            'Visitor context is missing. Please login again before booking an appointment.';
      });
      return;
    }

    setState(() {
      _visitorLoading = true;
      _contextError = null;
      _selectedVisitor = {
        'id': visitorId,
        'fullName': auth.user?.fullName ?? 'Visitor',
        'phoneNumber': auth.user?.username ?? '',
      };
    });

    final profile = await ApiService.getVisitorById(visitorId);
    if (!mounted) return;
    setState(() {
      _visitorLoading = false;
      if (profile != null) _selectedVisitor = profile;
    });
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    final visitorId = _selectedVisitorId;
    if (visitorId == null || visitorId <= 0) {
      setState(() {
        _contextError = widget.isPublic
            ? 'Visitor context is missing. Please login again before submitting.'
            : 'Search and select a visitor before creating the appointment.';
      });
      return;
    }
    if (widget.isPublic && !_consentAccepted) {
      setState(() {
        _contextError =
            'Please provide consent for appointment data processing.';
      });
      return;
    }

    setState(() => _loading = true);
    final payload = <String, dynamic>{
      'applicantId': visitorId,
      'eventType': _agendaType,
      'subject': _agendaDescriptions[_agendaType] ?? _agendaType,
      'appointmentType': _agendaDescriptions[_agendaType] ?? _agendaType,
      'agendaType': _agendaDescriptions[_agendaType] ?? _agendaType,
      'agendaBrief': _agendaBriefCtrl.text.trim(),
      'requestedLocation': _location,
      'isWalkIn': widget.isWalkIn,
      'isOrganisation': _isOrganisation,
      'schemeType': _includeSchemeDetails ? _schemeTypeCtrl.text.trim() : '',
      'applicationType': _includeSchemeDetails ? _applicationType : '',
      'projectCategory': _includeSchemeDetails ? _projectCategory : '',
      'projectName': _includeSchemeDetails ? _projectNameCtrl.text.trim() : '',
      'beneficiaryType': _includeSchemeDetails ? _beneficiaryType : '',
      'beneficiaryCount': _includeSchemeDetails ? _beneficiaryCount : '',
      'estimatedCost':
          _includeSchemeDetails ? _estimatedCostCtrl.text.trim() : '',
      'communityContribution':
          _includeSchemeDetails ? _communityContributionCtrl.text.trim() : '',
      'justification':
          _includeSchemeDetails ? _justificationCtrl.text.trim() : '',
    };
    if (widget.isPublic) {
      payload.addAll({
        'consentAccepted': _consentAccepted,
        'consentVersion': AppConfig.consentVersion,
        'consentTimestamp': DateTime.now().toUtc().toIso8601String(),
        'privacyPolicyUrl': AppConfig.privacyPolicyUrl,
        'termsUrl': AppConfig.termsUrl,
      });
    }
    final offline = context.read<ConnectivityService>().isOffline;
    final result = offline
        ? {'success': false, 'message': 'Network error. Please try again.'}
        : await ApiService.createAppointment(payload);
    if (!mounted) return;
    if (result != null && result['success'] != false) {
      setState(() {
        _loading = false;
        _submittedAppId =
            result['applicationId'] as String? ?? result['id']?.toString();
        _submittedToken = result['walkInTokenNumber']?.toString() ??
            result['tokenNumber']?.toString() ??
            result['token']?.toString();
        _submitted = true;
      });
      return;
    }

    final canSaveOffline = offline ||
        (result?['message']?.toString().toLowerCase().contains('network') ??
            false);
    if (canSaveOffline) {
      final visitorLocalId = _selectedVisitor?['localId']?.toString();
      final saved = await OfflineRepository().saveAppointmentOffline(
        payload,
        visitorLocalId: visitorLocalId,
      );
      final note = const OfflineAiNotesService().generateAppointmentNote(
        citizenName: _selectedVisitor?['fullName']?.toString() ?? 'Citizen',
        purpose: _agendaBriefCtrl.text,
        department: _profileCtrl.text,
        appointmentType: _agendaDescriptions[_agendaType],
        remarks: _profileCtrl.text,
      );
      await OfflineRepository().saveAiNoteOffline(
        appointmentLocalId: saved.localId,
        noteText: note,
        payload: {
          'appointmentLocalId': saved.localId,
          'appointmentNumber': saved.referenceNumber,
          'noteText': note,
        },
      );
      if (!mounted) return;
      context.read<SyncService>().syncNow();
      setState(() {
        _loading = false;
        _submittedAppId = saved.referenceNumber;
        _submittedToken = null;
        _submitted = true;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content:
              Text('Appointment saved offline. AI note generated offline.'),
        ),
      );
      return;
    }

    setState(() {
      _loading = false;
      _contextError = result?['message']?.toString() ??
          'Unable to submit appointment. Please try again.';
    });
  }

  void _reset() {
    _formKey.currentState?.reset();
    _searchMobileCtrl.clear();
    _searchEpicCtrl.clear();
    _searchReferenceCtrl.clear();
    _agendaBriefCtrl.clear();
    _profileCtrl.clear();
    _schemeTypeCtrl.clear();
    _projectNameCtrl.clear();
    _estimatedCostCtrl.clear();
    _communityContributionCtrl.clear();
    _justificationCtrl.clear();
    setState(() {
      _agendaType = 'A4';
      _location = 'SHILLONG';
      _isOrganisation = false;
      _includeSchemeDetails = false;
      _applicationType = 'NEW';
      _projectCategory = 'Community Infrastructure';
      _beneficiaryType = 'Community';
      _beneficiaryCount = '1-50';
      _submitted = false;
      _submittedAppId = null;
      _submittedToken = null;
      _contextError = null;
      _consentAccepted = false;
      if (!widget.isPublic) _selectedVisitor = null;
    });
  }

  Future<void> _searchVisitor() async {
    setState(() {
      _searching = true;
      _contextError = null;
      _selectedVisitor = null;
      _searchResults = [];
    });
    final results = await ApiService.searchVisitors(
      mobile: _searchMobileCtrl.text,
      epic: _searchEpicCtrl.text,
      referenceId: _searchReferenceCtrl.text,
    );
    if (!mounted) return;
    setState(() {
      _searching = false;
      if (results.isEmpty) {
        _contextError =
            'No visitor found. Register the visitor first, then create the appointment.';
      } else {
        _searchResults = results
            .whereType<Map>()
            .map((row) => Map<String, dynamic>.from(row))
            .toList();
        if (_searchResults.length == 1) {
          _selectedVisitor = _searchResults.first;
        }
      }
    });
  }

  Future<void> _openVisitorRegistration() async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const VisitorRegistrationScreen()),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (_submitted) return _buildSuccess(context);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (widget.isWalkIn || widget.isPublic) _buildInfoBanner(),
            _buildSection(
              widget.isPublic ? 'Citizen Details' : 'Visitor Search',
              widget.isPublic
                  ? _buildSelectedVisitorSummary()
                  : _buildVisitorSearch(),
            ),
            const SizedBox(height: 16),
            if (_contextError != null) ...[
              _buildErrorBanner(_contextError!),
              const SizedBox(height: 16),
            ],
            if (_selectedVisitor != null)
              _buildSection('Appointment Details', _buildAppointmentFields()),
            if (widget.isPublic && _selectedVisitor != null) ...[
              const SizedBox(height: 16),
              _buildSection('Privacy Consent', _buildConsentNotice()),
            ],
            const SizedBox(height: 24),
            if (_selectedVisitor != null)
              SizedBox(
                height: 50,
                child: ElevatedButton.icon(
                  icon: _loading
                      ? const SizedBox(
                          height: 20,
                          width: 20,
                          child: CircularProgressIndicator(
                              strokeWidth: 2, color: Colors.white),
                        )
                      : const Icon(Icons.send),
                  label: Text(
                    widget.isPublic
                        ? 'Submit Appointment'
                        : 'Create Appointment',
                    style: const TextStyle(fontSize: 16),
                  ),
                  onPressed: _loading ? null : _submit,
                ),
              ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  Widget _buildInfoBanner() {
    final isWalkIn = widget.isWalkIn;
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isWalkIn ? const Color(0xFFD1FAE5) : const Color(0xFFE8EAF6),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: isWalkIn ? const Color(0xFF6EE7B7) : const Color(0xFF9FA8DA),
        ),
      ),
      child: Row(
        children: [
          Icon(
            isWalkIn ? Icons.login : Icons.info_outline,
            color: isWalkIn ? const Color(0xFF065F46) : const Color(0xFF1A237E),
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              isWalkIn
                  ? 'Walk-in Counter: Register an in-person visitor for a direct appointment with the Chief Minister.'
                  : 'Your registered MeghaConnect profile will be used. Add only appointment-specific details.',
              style: TextStyle(
                color: isWalkIn
                    ? const Color(0xFF065F46)
                    : const Color(0xFF1A237E),
                fontSize: 12,
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildErrorBanner(String text) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF2F2),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFECACA)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.error_outline, color: Color(0xFF991B1B), size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(color: Color(0xFF991B1B), fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildConsentNotice() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const Text(
          'MeghaConnect will process your registered profile and appointment details for citizen service, appointment review, security, audit, and governance workflow purposes.',
          style: TextStyle(fontSize: 13, height: 1.4),
        ),
        const SizedBox(height: 6),
        Text(
          'Privacy: ${AppConfig.privacyPolicyUrl}\nTerms: ${AppConfig.termsUrl}',
          style: TextStyle(fontSize: 12, color: Colors.grey.shade700),
        ),
        CheckboxListTile(
          contentPadding: EdgeInsets.zero,
          value: _consentAccepted,
          onChanged: (value) =>
              setState(() => _consentAccepted = value ?? false),
          title: const Text(
            'I consent to appointment data processing for MeghaConnect services.',
            style: TextStyle(fontSize: 13),
          ),
          controlAffinity: ListTileControlAffinity.leading,
        ),
      ],
    );
  }

  Widget _buildSelectedVisitorSummary() {
    if (_visitorLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: CircularProgressIndicator(),
        ),
      );
    }
    final v = _selectedVisitor;
    if (v == null) {
      return const Text(
        'Visitor profile could not be loaded.',
        style: TextStyle(color: Color(0xFF991B1B)),
      );
    }
    return _VisitorSummary(visitor: v);
  }

  Widget _buildVisitorSearch() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextFormField(
          controller: _searchMobileCtrl,
          keyboardType: TextInputType.phone,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(10),
          ],
          decoration: const InputDecoration(
            labelText: 'Mobile Number',
            prefixIcon: Icon(Icons.phone_outlined),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _searchEpicCtrl,
          textCapitalization: TextCapitalization.characters,
          decoration: const InputDecoration(
            labelText: 'EPIC / Voter ID',
            prefixIcon: Icon(Icons.credit_card_outlined),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _searchReferenceCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(
            labelText: 'Visitor Reference ID',
            prefixIcon: Icon(Icons.tag_outlined),
          ),
        ),
        const SizedBox(height: 12),
        ElevatedButton.icon(
          icon: _searching
              ? const SizedBox(
                  width: 18,
                  height: 18,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Icon(Icons.search),
          label: Text(_searching ? 'Searching...' : 'Search Visitor'),
          onPressed: _searching ? null : _searchVisitor,
        ),
        if (_selectedVisitor != null) ...[
          const SizedBox(height: 14),
          _VisitorSummary(visitor: _selectedVisitor!),
        ],
        if (_searchResults.length > 1 && _selectedVisitor == null) ...[
          const SizedBox(height: 14),
          Text(
            'Select Visitor',
            style: TextStyle(
              color: Colors.grey[700],
              fontWeight: FontWeight.w700,
              fontSize: 12,
            ),
          ),
          const SizedBox(height: 8),
          ..._searchResults.map((visitor) {
            final name = visitor['fullName']?.toString() ?? 'Visitor';
            final phone = visitor['phoneNumber']?.toString() ?? '';
            final epic = visitor['epicNumber']?.toString() ?? '';
            return Card(
              margin: const EdgeInsets.only(bottom: 8),
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: const Color(0xFFE8EAF6),
                  foregroundColor: const Color(0xFF1A237E),
                  child: Text(name.isEmpty ? 'V' : name[0].toUpperCase()),
                ),
                title: Text(name),
                subtitle: Text([
                  if (phone.isNotEmpty) phone,
                  if (epic.isNotEmpty) epic,
                ].join(' · ')),
                trailing: const Icon(Icons.chevron_right),
                onTap: () {
                  setState(() {
                    _selectedVisitor = visitor;
                    _contextError = null;
                  });
                },
              ),
            );
          }),
        ],
        if (_contextError != null && _selectedVisitor == null) ...[
          const SizedBox(height: 12),
          OutlinedButton.icon(
            icon: const Icon(Icons.person_add_alt_1_outlined),
            label: const Text('Register Visitor First'),
            onPressed: _openVisitorRegistration,
          ),
        ],
      ],
    );
  }

  Widget _buildSection(String title, Widget content) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 15,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1A237E),
              ),
            ),
            const SizedBox(height: 14),
            content,
          ],
        ),
      ),
    );
  }

  Widget _buildAppointmentFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _expandedDropdown(
          value: _agendaType,
          label: 'Agenda Type *',
          icon: Icons.category_outlined,
          values: _agendaTypes,
          labelFor: (t) => '$t - ${_agendaDescriptions[t]}',
          onChanged: (v) => setState(() => _agendaType = v ?? _agendaType),
        ),
        const SizedBox(height: 12),
        _expandedDropdown(
          value: _location,
          label: 'Preferred Location *',
          icon: Icons.place_outlined,
          values: _locations,
          onChanged: (v) => setState(() => _location = v ?? _location),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _agendaBriefCtrl,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Agenda / Purpose of Meeting *',
            prefixIcon: Icon(Icons.description_outlined),
            alignLabelWithHint: true,
          ),
          validator: (v) => (v == null || v.trim().isEmpty)
              ? 'Please describe the purpose'
              : null,
          textInputAction: TextInputAction.newline,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _profileCtrl,
          maxLines: 3,
          decoration: const InputDecoration(
            labelText: 'Brief Profile / Background',
            prefixIcon: Icon(Icons.notes_outlined),
            alignLabelWithHint: true,
            hintText: 'Optional: Any relevant background information',
          ),
          textInputAction: TextInputAction.done,
        ),
        const SizedBox(height: 12),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _isOrganisation,
          onChanged: (value) => setState(() => _isOrganisation = value),
          title: const Text('Applicant represents an organisation'),
          subtitle: const Text('Required when applying on behalf of a group.'),
        ),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _includeSchemeDetails,
          onChanged: (value) => setState(() => _includeSchemeDetails = value),
          title: const Text('Add CM scheme / project details'),
          subtitle:
              const Text('Skip when this appointment is not for a scheme.'),
        ),
        if (_includeSchemeDetails) ...[
          const SizedBox(height: 12),
          TextFormField(
            controller: _schemeTypeCtrl,
            decoration: const InputDecoration(
              labelText: 'Scheme Type',
              prefixIcon: Icon(Icons.workspace_premium_outlined),
            ),
          ),
          const SizedBox(height: 12),
          _expandedDropdown(
            value: _applicationType,
            label: 'Application Type',
            icon: Icons.assignment_outlined,
            values: _applicationTypes,
            onChanged: (v) =>
                setState(() => _applicationType = v ?? _applicationType),
          ),
          const SizedBox(height: 12),
          _expandedDropdown(
            value: _projectCategory,
            label: 'Project Category',
            icon: Icons.category_outlined,
            values: _projectCategories,
            onChanged: (v) =>
                setState(() => _projectCategory = v ?? _projectCategory),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _projectNameCtrl,
            decoration: const InputDecoration(
              labelText: 'Project Name',
              prefixIcon: Icon(Icons.drive_file_rename_outline),
            ),
          ),
          const SizedBox(height: 12),
          _expandedDropdown(
            value: _beneficiaryType,
            label: 'Beneficiary Type',
            icon: Icons.groups_outlined,
            values: _beneficiaryTypes,
            onChanged: (v) =>
                setState(() => _beneficiaryType = v ?? _beneficiaryType),
          ),
          const SizedBox(height: 12),
          _expandedDropdown(
            value: _beneficiaryCount,
            label: 'Beneficiary Count',
            icon: Icons.format_list_numbered_outlined,
            values: _beneficiaryCounts,
            onChanged: (v) =>
                setState(() => _beneficiaryCount = v ?? _beneficiaryCount),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _estimatedCostCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Estimated Cost',
              prefixIcon: Icon(Icons.currency_rupee),
            ),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _communityContributionCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Community Contribution',
              prefixIcon: Icon(Icons.savings_outlined),
            ),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _justificationCtrl,
            maxLines: 3,
            decoration: const InputDecoration(
              labelText: 'Project Justification',
              prefixIcon: Icon(Icons.fact_check_outlined),
              alignLabelWithHint: true,
            ),
          ),
        ],
      ],
    );
  }

  DropdownButtonFormField<String> _expandedDropdown({
    required String value,
    required String label,
    required IconData icon,
    required List<String> values,
    String Function(String value)? labelFor,
    required ValueChanged<String?> onChanged,
  }) {
    return DropdownButtonFormField<String>(
      value: values.contains(value) ? value : null,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: label,
        prefixIcon: Icon(icon),
      ),
      selectedItemBuilder: (context) => values
          .map(
            (item) => Text(
              labelFor?.call(item) ?? item,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          )
          .toList(),
      items: values.map((item) {
        return DropdownMenuItem(
          value: item,
          child: Text(
            labelFor?.call(item) ?? item,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        );
      }).toList(),
      onChanged: onChanged,
    );
  }

  Widget _buildSuccess(BuildContext context) {
    final appId = _submittedAppId ??
        'MC-${DateTime.now().year}-${(DateTime.now().millisecondsSinceEpoch % 90000 + 10000).toString()}';

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(20),
              decoration: const BoxDecoration(
                color: Color(0xFFD1FAE5),
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.check_circle_outline,
                  size: 72, color: Color(0xFF065F46)),
            ),
            const SizedBox(height: 24),
            const Text(
              'Appointment Registered!',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF065F46),
              ),
            ),
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFE8EAF6),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                'Application ID: $appId',
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A237E),
                  fontSize: 16,
                ),
              ),
            ),
            if (_submittedToken != null && _submittedToken!.isNotEmpty) ...[
              const SizedBox(height: 10),
              Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                decoration: BoxDecoration(
                  color: const Color(0xFFD1FAE5),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  'Token Number: $_submittedToken',
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    color: Color(0xFF065F46),
                    fontSize: 16,
                  ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            Text(
              widget.isPublic
                  ? 'Your application has been submitted. You will be notified once it is reviewed by the CMO.'
                  : 'The appointment has been registered in the system and is pending CMO review.',
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600], fontSize: 14),
            ),
            const SizedBox(height: 32),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    icon: const Icon(Icons.add),
                    label: const Text('New Entry'),
                    onPressed: _reset,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: const Color(0xFF1A237E),
                      side: const BorderSide(color: Color(0xFF1A237E)),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                ),
                if (!widget.isPublic) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      icon: const Icon(Icons.list_alt),
                      label: const Text('View All'),
                      onPressed: widget.onViewAppointments ??
                          () => context
                              .read<NavigationService>()
                              .navigateTo('appointments'),
                    ),
                  ),
                ],
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _VisitorSummary extends StatelessWidget {
  final Map<String, dynamic> visitor;

  const _VisitorSummary({required this.visitor});

  @override
  Widget build(BuildContext context) {
    final name = visitor['fullName']?.toString() ?? 'Visitor';
    final phone = visitor['phoneNumber']?.toString() ?? '—';
    final epic = visitor['epicNumber']?.toString() ?? '—';
    final district = visitor['district']?.toString() ?? '—';
    final constituency = visitor['constituency']?.toString() ?? '—';
    final booth = visitor['booth']?.toString() ??
        visitor['boothVillage']?.toString() ??
        '—';
    final part = visitor['partNumber']?.toString() ??
        visitor['pollingPartNo']?.toString() ??
        '—';
    final kyc = visitor['kycStatus']?.toString() ??
        (visitor['kycVerified'] == true ? 'VERIFIED' : 'PENDING');

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFE2E8F0)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CircleAvatar(
                backgroundColor: const Color(0xFF1A237E),
                foregroundColor: Colors.white,
                child: Text(name.isEmpty ? 'V' : name[0].toUpperCase()),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      name,
                      style: const TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 14,
                      ),
                    ),
                    Text(
                      'Profile details are read-only for appointment booking',
                      style: TextStyle(fontSize: 11, color: Colors.grey[600]),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _SummaryPill(label: 'Mobile', value: phone),
              _SummaryPill(label: 'EPIC', value: epic),
              _SummaryPill(label: 'District', value: district),
              _SummaryPill(label: 'Constituency', value: constituency),
              _SummaryPill(label: 'Booth', value: booth),
              _SummaryPill(label: 'Part', value: part),
              _SummaryPill(label: 'KYC', value: kyc),
            ],
          ),
        ],
      ),
    );
  }
}

class _SummaryPill extends StatelessWidget {
  final String label;
  final String value;

  const _SummaryPill({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Container(
      constraints: const BoxConstraints(minWidth: 128),
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFE5E7EB)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(
            label.toUpperCase(),
            style: TextStyle(
              color: Colors.grey[600],
              fontSize: 10,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            value.isEmpty ? '—' : value,
            style: const TextStyle(
              color: Color(0xFF111827),
              fontSize: 12,
              fontWeight: FontWeight.w600,
            ),
          ),
        ],
      ),
    );
  }
}
