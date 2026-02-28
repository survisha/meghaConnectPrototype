import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../services/navigation_service.dart';

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

  // Applicant Info
  final _fullNameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _epicCtrl = TextEditingController();
  final _designationCtrl = TextEditingController();

  // Appointment Details
  String _agendaType = 'A4';
  String _location = 'SHILLONG';
  final _agendaBriefCtrl = TextEditingController();
  final _profileCtrl = TextEditingController();

  String _district = 'East Khasi Hills';
  String _constituency = '';

  bool _submitted = false;
  bool _loading = false;

  static const _agendaTypes = ['A1', 'A2', 'A3', 'A4', 'B1', 'B2'];
  static const _locations = ['SHILLONG', 'TURA', 'DELHI', 'OTHERS'];
  static const _districts = [
    'East Khasi Hills',
    'West Khasi Hills',
    'South West Khasi Hills',
    'Ri Bhoi',
    'Jaintia Hills',
    'East Jaintia Hills',
    'East Garo Hills',
    'West Garo Hills',
    'South Garo Hills',
    'Eastern West Khasi Hills',
    'North Garo Hills',
    'Western South Garo Hills',
  ];

  static const _agendaDescriptions = {
    'A1': 'Cabinet/Flight/State Function',
    'A2': 'Events & Public Programs',
    'A3': 'File Clearing & Admin',
    'A4': 'Individual Appointment',
    'B1': 'Public Durbar',
    'B2': 'Walk-in',
  };

  @override
  void dispose() {
    _fullNameCtrl.dispose();
    _phoneCtrl.dispose();
    _epicCtrl.dispose();
    _designationCtrl.dispose();
    _agendaBriefCtrl.dispose();
    _profileCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _loading = true);
    await Future.delayed(const Duration(milliseconds: 800));
    if (!mounted) return;
    setState(() {
      _loading = false;
      _submitted = true;
    });
  }

  void _reset() {
    _formKey.currentState?.reset();
    _fullNameCtrl.clear();
    _phoneCtrl.clear();
    _epicCtrl.clear();
    _designationCtrl.clear();
    _agendaBriefCtrl.clear();
    _profileCtrl.clear();
    setState(() {
      _agendaType = 'A4';
      _location = 'SHILLONG';
      _district = 'East Khasi Hills';
      _constituency = '';
      _submitted = false;
    });
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
            if (widget.isWalkIn || widget.isPublic)
              _buildInfoBanner(),
            _buildSection('👤 Applicant Information', _buildApplicantFields()),
            const SizedBox(height: 16),
            _buildSection('📋 Appointment Details', _buildAppointmentFields()),
            const SizedBox(height: 24),
            SizedBox(
              height: 50,
              child: ElevatedButton.icon(
                icon: _loading
                    ? const SizedBox(
                        height: 20,
                        width: 20,
                        child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                      )
                    : const Icon(Icons.send),
                label: Text(
                  widget.isPublic ? 'Submit Application' : 'Register Appointment',
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
                  : 'Citizens: Submit your appointment request online. You will be contacted once reviewed.',
              style: TextStyle(
                color: isWalkIn ? const Color(0xFF065F46) : const Color(0xFF1A237E),
                fontSize: 12,
              ),
            ),
          ),
        ],
      ),
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

  Widget _buildApplicantFields() {
    return Column(
      children: [
        TextFormField(
          controller: _fullNameCtrl,
          decoration: const InputDecoration(
            labelText: 'Full Name *',
            prefixIcon: Icon(Icons.person_outline),
          ),
          textCapitalization: TextCapitalization.words,
          validator: (v) => (v == null || v.trim().isEmpty) ? 'Full name is required' : null,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _phoneCtrl,
          keyboardType: TextInputType.phone,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(10),
          ],
          decoration: const InputDecoration(
            labelText: 'Mobile Number *',
            prefixIcon: Icon(Icons.phone_outlined),
            prefixText: '+91 ',
          ),
          validator: (v) {
            if (v == null || v.isEmpty) return 'Mobile number is required';
            if (v.length != 10) return 'Enter valid 10-digit number';
            return null;
          },
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _epicCtrl,
          decoration: const InputDecoration(
            labelText: 'EPIC / Voter ID Number',
            prefixIcon: Icon(Icons.credit_card_outlined),
          ),
          textCapitalization: TextCapitalization.characters,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _designationCtrl,
          decoration: const InputDecoration(
            labelText: 'Designation / Occupation',
            prefixIcon: Icon(Icons.work_outline),
          ),
          textCapitalization: TextCapitalization.words,
          textInputAction: TextInputAction.next,
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: _district,
          decoration: const InputDecoration(
            labelText: 'District *',
            prefixIcon: Icon(Icons.location_city_outlined),
          ),
          items: _districts
              .map((d) => DropdownMenuItem(value: d, child: Text(d)))
              .toList(),
          onChanged: (v) => setState(() => _district = v ?? _district),
          validator: (v) => v == null ? 'Select district' : null,
        ),
        const SizedBox(height: 12),
        TextFormField(
          initialValue: _constituency,
          decoration: const InputDecoration(
            labelText: 'Constituency',
            prefixIcon: Icon(Icons.map_outlined),
          ),
          textCapitalization: TextCapitalization.words,
          onChanged: (v) => _constituency = v,
          textInputAction: TextInputAction.next,
        ),
      ],
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
          validator: (v) => (v == null || v.trim().isEmpty) ? 'Please describe the purpose' : null,
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
    final appId =
        'MC-2024-${(DateTime.now().millisecondsSinceEpoch % 90000 + 10000).toString()}';

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
              child: const Icon(Icons.check_circle_outline, size: 72, color: Color(0xFF065F46)),
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
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    ),
                  ),
                ),
                if (!widget.isPublic) ...[
                  const SizedBox(width: 12),
                  Expanded(
                    child: ElevatedButton.icon(
                      icon: const Icon(Icons.list_alt),
                      label: const Text('View All'),
                      onPressed: () =>
                          context.read<NavigationService>().navigateTo('appointments'),
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
