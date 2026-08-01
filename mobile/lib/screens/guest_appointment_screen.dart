import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import '../services/notification_service.dart';
import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:provider/provider.dart';

import '../core/config/app_config.dart';
import '../services/api_service.dart';
import '../services/connectivity_service.dart';
import '../services/offline_repository.dart';
import '../services/sync_service.dart';

class GuestAppointmentScreen extends StatefulWidget {
  const GuestAppointmentScreen({super.key});

  @override
  State<GuestAppointmentScreen> createState() => _GuestAppointmentScreenState();
}

class _GuestAppointmentScreenState extends State<GuestAppointmentScreen> {
  final _formKey = GlobalKey<FormState>();
  final _fullNameCtrl = TextEditingController();
  final _mobileCtrl = TextEditingController();
  final _addressCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();
  final _organizationCtrl = TextEditingController();
  final _designationCtrl = TextEditingController();
  final _referredByCtrl = TextEditingController();
  final _reasonCtrl = TextEditingController();
  final _remarksCtrl = TextEditingController();

  final _imagePicker = ImagePicker();
  List<Map<String, String>> _referredOffices = [];
  List<Map<String, String>> _visitorCategories = [];
  String? _referredOffice;
  String? _visitorCategory;
  DateTime? _preferredDate;
  XFile? _guestPhoto;
  PlatformFile? _supportingDocument;
  bool _useFrontCamera = true;
  bool _loadingRefs = true;
  bool _submitting = false;
  bool _consentAccepted = false;
  String? _error;
  String? _successReference;

  @override
  void initState() {
    super.initState();
    _loadReferenceData();
  }

  @override
  void dispose() {
    _fullNameCtrl.dispose();
    _mobileCtrl.dispose();
    _addressCtrl.dispose();
    _emailCtrl.dispose();
    _organizationCtrl.dispose();
    _designationCtrl.dispose();
    _referredByCtrl.dispose();
    _reasonCtrl.dispose();
    _remarksCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadReferenceData() async {
    final results = await Future.wait([
      ApiService.getReferenceData('GUEST_REFERRED_OFFICE'),
      ApiService.getReferenceData('GUEST_VISITOR_CATEGORY'),
    ]);
    if (!mounted) return;
    setState(() {
      _referredOffices = results[0];
      _visitorCategories = results[1];
      _loadingRefs = false;
      if (_referredOffices.length == 1) {
        _referredOffice = _referredOffices.first['code'];
      }
    });
  }

  Future<void> _capturePhoto() async {
    setState(() => _error = null);
    if (!await _confirmSensitiveAction(
      title: 'Camera access',
      message:
          'Camera access is required to capture the visitor photo for appointment verification, security, audit, and entry management.',
    )) {
      return;
    }
    final preferred = _useFrontCamera ? CameraDevice.front : CameraDevice.rear;
    XFile? photo;
    try {
      photo = await _imagePicker.pickImage(
        source: ImageSource.camera,
        preferredCameraDevice: preferred,
        imageQuality: 82,
        maxWidth: 1200,
      );
    } catch (_) {
      final fallback = _useFrontCamera ? CameraDevice.rear : CameraDevice.front;
      try {
        photo = await _imagePicker.pickImage(
          source: ImageSource.camera,
          preferredCameraDevice: fallback,
          imageQuality: 82,
          maxWidth: 1200,
        );
      } catch (_) {
        if (!mounted) return;
        setState(() {
          _error = 'Camera access was denied or is unavailable.';
        });
        return;
      }
    }
    if (!mounted || photo == null) return;
    setState(() {
      _guestPhoto = photo;
      _error = null;
    });
  }

  Future<void> _pickDocument() async {
    setState(() => _error = null);
    if (!await _confirmSensitiveAction(
      title: 'Document upload',
      message:
          'Selected documents will be uploaded as appointment support records and may be reviewed by authorized staff and document intelligence services.',
    )) {
      return;
    }
    try {
      final result = await FilePicker.platform.pickFiles(
        allowMultiple: false,
        type: FileType.custom,
        allowedExtensions: ['pdf', 'jpg', 'jpeg', 'png'],
      );
      if (!mounted || result == null || result.files.isEmpty) return;
      setState(() => _supportingDocument = result.files.single);
    } catch (_) {
      if (!mounted) return;
      setState(() => _error = 'Unable to select document.');
    }
  }

  Future<void> _pickPreferredDate() async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: _preferredDate ?? now,
      firstDate: DateTime(now.year, now.month, now.day),
      lastDate: DateTime(now.year + 2),
    );
    if (picked == null || !mounted) return;
    setState(() => _preferredDate = picked);
  }

  Future<void> _submit() async {
    setState(() => _error = null);
    if (!_formKey.currentState!.validate()) return;
    if (_guestPhoto == null) {
      setState(() => _error = 'Please capture guest photo before submitting.');
      return;
    }
    if (!_consentAccepted) {
      setState(() => _error =
          'Please provide consent for visitor, photo, document, and appointment data processing.');
      return;
    }

    final offline = context.read<ConnectivityService>().isOffline;
    setState(() => _submitting = true);
    final photoBytes = await _guestPhoto!.readAsBytes();
    final photoDataUri = 'data:image/jpeg;base64,${base64Encode(photoBytes)}';
    final fields = {
      'fullName': _fullNameCtrl.text,
      'mobileNumber': _mobileCtrl.text,
      'address': _addressCtrl.text,
      'email': _emailCtrl.text,
      'organizationName': _organizationCtrl.text,
      'designation': _designationCtrl.text,
      'visitorCategory': _visitorCategory ?? '',
      'referredOffice': _referredOffice ?? '',
      'referredByName': _referredByCtrl.text,
      'reasonForAppointment': _reasonCtrl.text,
      'preferredDate':
          _preferredDate == null ? '' : _dateParam(_preferredDate!),
      'remarks': _remarksCtrl.text,
      'consentAccepted': _consentAccepted.toString(),
      'consentVersion': AppConfig.consentVersion,
      'consentTimestamp': DateTime.now().toUtc().toIso8601String(),
      'privacyPolicyUrl': AppConfig.privacyPolicyUrl,
      'termsUrl': AppConfig.termsUrl,
    };
    final result = offline
        ? {'success': false, 'message': 'Network error. Please try again.'}
        : await ApiService.createGuestAppointment(
            fields: {
              ...fields,
            },
            livePhotoBase64: photoDataUri,
            supportingDocumentPath: _supportingDocument?.path,
            supportingDocumentName: _supportingDocument?.name,
          );
    if (!mounted) return;
    if (result['referenceId'] == null &&
        (offline ||
            (result['message']?.toString().toLowerCase().contains('network') ??
                false))) {
      final saved = await OfflineRepository().saveAppointmentOffline({
        ...fields,
        'livePhotoBase64': photoDataUri,
        'supportingDocumentName': _supportingDocument?.name,
        'appointmentSource': 'GUEST',
      });
      if (!mounted) return;
      context.read<SyncService>().syncNow();
      setState(() {
        _submitting = false;
        _successReference = saved.referenceNumber;
      });
      AppNotificationService.info('Appointment saved offline.');
      return;
    }
    setState(() {
      _submitting = false;
      if (result['referenceId'] != null) {
        _successReference = result['referenceId'].toString();
      } else {
        _error = result['message']?.toString() ??
            'Unable to submit guest appointment.';
      }
    });
  }

  void _reset() {
    _formKey.currentState?.reset();
    _fullNameCtrl.clear();
    _mobileCtrl.clear();
    _addressCtrl.clear();
    _emailCtrl.clear();
    _organizationCtrl.clear();
    _designationCtrl.clear();
    _referredByCtrl.clear();
    _reasonCtrl.clear();
    _remarksCtrl.clear();
    setState(() {
      _referredOffice =
          _referredOffices.length == 1 ? _referredOffices.first['code'] : null;
      _visitorCategory = null;
      _preferredDate = null;
      _guestPhoto = null;
      _supportingDocument = null;
      _error = null;
      _successReference = null;
    });
  }

  String _dateParam(DateTime date) {
    String pad(int value) => value.toString().padLeft(2, '0');
    return '${date.year}-${pad(date.month)}-${pad(date.day)}';
  }

  Future<bool> _confirmSensitiveAction({
    required String title,
    required String message,
  }) async {
    final result = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Continue'),
          ),
        ],
      ),
    );
    return result ?? false;
  }

  String _displayFor(List<Map<String, String>> rows, String? code) {
    for (final row in rows) {
      if (row['code'] == code) return row['value'] ?? code ?? '';
    }
    return code ?? '';
  }

  @override
  Widget build(BuildContext context) {
    if (_successReference != null) return _buildSuccess();

    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Form(
        key: _formKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildHeader(),
            const SizedBox(height: 14),
            if (_error != null) ...[
              _StatusBanner.error(_error!),
              const SizedBox(height: 12),
            ],
            _buildSection('Guest Details', _buildGuestFields()),
            const SizedBox(height: 14),
            _buildSection('Appointment Request', _buildAppointmentFields()),
            const SizedBox(height: 14),
            _buildSection('Photo & Document', _buildMediaFields()),
            const SizedBox(height: 18),
            _buildConsentNotice(),
            const SizedBox(height: 18),
            SizedBox(
              height: 50,
              child: ElevatedButton.icon(
                onPressed: _submitting ? null : _submit,
                icon: _submitting
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Icon(Icons.send_outlined),
                label: Text(_submitting
                    ? 'Submitting...'
                    : 'Submit Guest Registration'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConsentNotice() {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFCBD5E1)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Privacy consent',
            style: TextStyle(fontWeight: FontWeight.w800),
          ),
          const SizedBox(height: 6),
          const Text(
            'MeghaConnect will collect and process visitor name, mobile number, address, photo, appointment details, and uploaded documents for appointment review, verification, security, audit, and governance workflow purposes.',
            style: TextStyle(fontSize: 12, height: 1.35),
          ),
          const SizedBox(height: 8),
          const Text(
            '${AppConfig.privacyPolicyUrl}\n${AppConfig.termsUrl}',
            style: TextStyle(fontSize: 11, color: Color(0xFF1D4ED8)),
          ),
          CheckboxListTile(
            contentPadding: EdgeInsets.zero,
            dense: true,
            value: _consentAccepted,
            onChanged: (value) {
              setState(() => _consentAccepted = value ?? false);
            },
            controlAffinity: ListTileControlAffinity.leading,
            title: const Text(
              'I agree to the Privacy Policy and Terms & Conditions.',
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHeader() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: const Color(0xFFE8EAF6),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(Icons.person_add_alt_1_outlined,
                  color: Color(0xFF1A237E)),
            ),
            const SizedBox(width: 12),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Guest Registration',
                    style: TextStyle(
                      color: Color(0xFF1A237E),
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                  SizedBox(height: 3),
                  Text(
                    'Submit an appointment request for a guest visitor.',
                    style: TextStyle(color: Color(0xFF6B7280), fontSize: 12),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSection(String title, Widget child) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                color: Color(0xFF1A237E),
                fontSize: 15,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 14),
            child,
          ],
        ),
      ),
    );
  }

  Widget _buildGuestFields() {
    return Column(
      children: [
        TextFormField(
          controller: _fullNameCtrl,
          textCapitalization: TextCapitalization.words,
          decoration: const InputDecoration(
            labelText: 'Full Name *',
            prefixIcon: Icon(Icons.person_outline),
          ),
          validator: _required('Please enter guest name.'),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _mobileCtrl,
          keyboardType: TextInputType.phone,
          inputFormatters: [
            FilteringTextInputFormatter.digitsOnly,
            LengthLimitingTextInputFormatter(10),
          ],
          decoration: const InputDecoration(
            labelText: 'Mobile Number *',
            prefixIcon: Icon(Icons.phone_outlined),
          ),
          validator: (value) {
            final text = value?.trim() ?? '';
            if (!RegExp(r'^[6-9]\d{9}$').hasMatch(text)) {
              return 'Please enter a valid 10-digit mobile number.';
            }
            return null;
          },
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _addressCtrl,
          minLines: 2,
          maxLines: 3,
          decoration: const InputDecoration(
            labelText: 'Address *',
            prefixIcon: Icon(Icons.home_outlined),
            alignLabelWithHint: true,
          ),
          validator: _required('Please enter address.'),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _emailCtrl,
          keyboardType: TextInputType.emailAddress,
          decoration: const InputDecoration(
            labelText: 'Email',
            prefixIcon: Icon(Icons.email_outlined),
          ),
          validator: (value) {
            final text = value?.trim() ?? '';
            if (text.isEmpty || text.contains('@')) return null;
            return 'Please enter a valid email address.';
          },
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _organizationCtrl,
          decoration: const InputDecoration(
            labelText: 'Organization Name',
            prefixIcon: Icon(Icons.business_outlined),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _designationCtrl,
          decoration: const InputDecoration(
            labelText: 'Designation',
            prefixIcon: Icon(Icons.badge_outlined),
          ),
        ),
      ],
    );
  }

  Widget _buildAppointmentFields() {
    return Column(
      children: [
        DropdownButtonFormField<String>(
          value: _visitorCategory,
          decoration: const InputDecoration(
            labelText: 'Visitor Category',
            prefixIcon: Icon(Icons.category_outlined),
          ),
          items: _visitorCategories.map(_dropdownItem).toList(),
          onChanged: _loadingRefs
              ? null
              : (value) => setState(() => _visitorCategory = value),
        ),
        const SizedBox(height: 12),
        DropdownButtonFormField<String>(
          value: _referredOffice,
          decoration: InputDecoration(
            labelText:
                _loadingRefs ? 'Loading offices...' : 'Referred Office *',
            prefixIcon: const Icon(Icons.account_balance_outlined),
          ),
          items: _referredOffices.map(_dropdownItem).toList(),
          onChanged: _loadingRefs
              ? null
              : (value) => setState(() => _referredOffice = value),
          validator: (value) =>
              value == null || value.isEmpty ? 'Please select office.' : null,
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _referredByCtrl,
          decoration: const InputDecoration(
            labelText: 'Referred By',
            prefixIcon: Icon(Icons.person_search_outlined),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _reasonCtrl,
          minLines: 3,
          maxLines: 5,
          decoration: const InputDecoration(
            labelText: 'Reason for Appointment *',
            prefixIcon: Icon(Icons.description_outlined),
            alignLabelWithHint: true,
          ),
          validator: (value) {
            final text = value?.trim() ?? '';
            if (text.isEmpty) return 'Please enter appointment reason.';
            if (text.length < 10) {
              return 'Reason for appointment must be at least 10 characters.';
            }
            return null;
          },
        ),
        const SizedBox(height: 12),
        InkWell(
          borderRadius: BorderRadius.circular(8),
          onTap: _pickPreferredDate,
          child: InputDecorator(
            decoration: const InputDecoration(
              labelText: 'Preferred Date',
              prefixIcon: Icon(Icons.event_outlined),
            ),
            child: Text(
              _preferredDate == null
                  ? 'Select date'
                  : _dateParam(_preferredDate!),
              style: TextStyle(
                color: _preferredDate == null
                    ? const Color(0xFF6B7280)
                    : const Color(0xFF111827),
              ),
            ),
          ),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _remarksCtrl,
          minLines: 2,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Remarks',
            prefixIcon: Icon(Icons.notes_outlined),
            alignLabelWithHint: true,
          ),
        ),
      ],
    );
  }

  Widget _buildMediaFields() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AspectRatio(
          aspectRatio: 4 / 3,
          child: Container(
            decoration: BoxDecoration(
              color: const Color(0xFFF8FAFC),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: const Color(0xFFE5E7EB)),
            ),
            clipBehavior: Clip.antiAlias,
            child: _guestPhoto == null
                ? const Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.photo_camera_outlined,
                          size: 58, color: Color(0xFF9CA3AF)),
                      SizedBox(height: 10),
                      Text(
                        'Guest photo is required',
                        style: TextStyle(color: Color(0xFF6B7280)),
                      ),
                    ],
                  )
                : Image.file(File(_guestPhoto!.path), fit: BoxFit.cover),
          ),
        ),
        const SizedBox(height: 12),
        Row(
          children: [
            Expanded(
              child: OutlinedButton.icon(
                onPressed: () =>
                    setState(() => _useFrontCamera = !_useFrontCamera),
                icon: const Icon(Icons.cameraswitch_outlined),
                label: Text(_useFrontCamera ? 'Front Camera' : 'Back Camera'),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: ElevatedButton.icon(
                onPressed: _capturePhoto,
                icon: Icon(_guestPhoto == null
                    ? Icons.photo_camera_outlined
                    : Icons.refresh),
                label: Text(_guestPhoto == null ? 'Capture' : 'Retake'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: _pickDocument,
          icon: const Icon(Icons.attach_file),
          label: Text(_supportingDocument == null
              ? 'Upload Supporting Document'
              : _supportingDocument!.name),
        ),
      ],
    );
  }

  DropdownMenuItem<String> _dropdownItem(Map<String, String> row) {
    final code = row['code'] ?? '';
    return DropdownMenuItem(
      value: code,
      child: Text(row['value'] ?? code),
    );
  }

  FormFieldValidator<String> _required(String message) {
    return (value) {
      if (value == null || value.trim().isEmpty) return message;
      return null;
    };
  }

  Widget _buildSuccess() {
    return Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.check_circle_outline,
                size: 78, color: Color(0xFF16A34A)),
            const SizedBox(height: 18),
            const Text(
              'Guest Registration Submitted',
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Color(0xFF065F46),
                fontSize: 22,
                fontWeight: FontWeight.w800,
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
                'Reference ID: $_successReference',
                style: const TextStyle(
                  color: Color(0xFF1A237E),
                  fontWeight: FontWeight.w800,
                  fontSize: 15,
                ),
              ),
            ),
            if (_referredOffice != null) ...[
              const SizedBox(height: 8),
              Text(
                'Referred Office: ${_displayFor(_referredOffices, _referredOffice)}',
                textAlign: TextAlign.center,
                style: const TextStyle(color: Color(0xFF6B7280)),
              ),
            ],
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _reset,
              icon: const Icon(Icons.add),
              label: const Text('New Guest Registration'),
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusBanner extends StatelessWidget {
  final String text;
  final Color bg;
  final Color border;
  final Color fg;
  final IconData icon;

  const _StatusBanner.error(this.text)
      : bg = const Color(0xFFFEF2F2),
        border = const Color(0xFFFECACA),
        fg = const Color(0xFF991B1B),
        icon = Icons.error_outline;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: fg, size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(text, style: TextStyle(color: fg, fontSize: 12)),
          ),
        ],
      ),
    );
  }
}
