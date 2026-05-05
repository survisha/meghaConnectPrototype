import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';
import '../services/navigation_service.dart';
import 'visitor_registration_screen.dart';

class NewAppointmentScreen extends StatefulWidget {
  final bool isWalkIn;
  final bool isPublic;

  const NewAppointmentScreen({
    super.key,
    this.isWalkIn = false,
    this.isPublic = false,
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
  final _agendaBriefCtrl = TextEditingController();
  final _profileCtrl = TextEditingController();

  bool _submitted = false;
  bool _loading = false;
  bool _searching = false;
  bool _visitorLoading = false;
  String? _contextError;
  String? _submittedAppId;
  Map<String, dynamic>? _selectedVisitor;

  static const _agendaTypes = ['A1', 'A2', 'A3', 'A4', 'B1', 'B2'];
  static const _locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];

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
    Future.microtask(_loadPublicVisitor);
  }

  @override
  void dispose() {
    _searchMobileCtrl.dispose();
    _searchEpicCtrl.dispose();
    _searchReferenceCtrl.dispose();
    _agendaBriefCtrl.dispose();
    _profileCtrl.dispose();
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

    setState(() => _loading = true);
    final result = await ApiService.createAppointment({
      'applicantId': visitorId,
      'eventType': _agendaType,
      'agendaType': _agendaDescriptions[_agendaType] ?? _agendaType,
      'agendaBrief': _agendaBriefCtrl.text.trim(),
      'requestedLocation': _location,
      'isWalkIn': widget.isWalkIn,
    });
    if (!mounted) return;
    setState(() {
      _loading = false;
      if (result == null) {
        _contextError = 'Unable to submit appointment. Please try again.';
      } else {
        _submittedAppId =
            result['applicationId'] as String? ?? result['id']?.toString();
        _submitted = true;
      }
    });
  }

  void _reset() {
    _formKey.currentState?.reset();
    _searchMobileCtrl.clear();
    _searchEpicCtrl.clear();
    _searchReferenceCtrl.clear();
    _agendaBriefCtrl.clear();
    _profileCtrl.clear();
    setState(() {
      _agendaType = 'A4';
      _location = 'SHILLONG';
      _submitted = false;
      _submittedAppId = null;
      _contextError = null;
      if (!widget.isPublic) _selectedVisitor = null;
    });
  }

  Future<void> _searchVisitor() async {
    setState(() {
      _searching = true;
      _contextError = null;
      _selectedVisitor = null;
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
        _selectedVisitor = results.first as Map<String, dynamic>;
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
        DropdownButtonFormField<String>(
          value: _agendaType,
          decoration: const InputDecoration(
            labelText: 'Agenda Type *',
            prefixIcon: Icon(Icons.category_outlined),
          ),
          items: _agendaTypes.map((t) {
            return DropdownMenuItem(
              value: t,
              child: Text('$t – ${_agendaDescriptions[t]}'),
            );
          }).toList(),
          onChanged: (v) => setState(() => _agendaType = v ?? _agendaType),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: _location,
          decoration: const InputDecoration(
            labelText: 'Preferred Location *',
            prefixIcon: Icon(Icons.place_outlined),
          ),
          items: _locations
              .map((l) => DropdownMenuItem(value: l, child: Text(l)))
              .toList(),
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
      ],
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
                      onPressed: () => context
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
