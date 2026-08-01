import 'package:flutter/material.dart';
import '../services/api_service.dart';
import '../services/notification_service.dart';

class DepartmentAccessRequestScreen extends StatefulWidget {
  const DepartmentAccessRequestScreen({super.key});
  @override
  State<DepartmentAccessRequestScreen> createState() =>
      _DepartmentAccessRequestScreenState();
}

class _DepartmentAccessRequestScreenState
    extends State<DepartmentAccessRequestScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _mobile = TextEditingController();
  final _purpose = TextEditingController();
  final _users = TextEditingController(text: '1');
  final _remarks = TextEditingController();
  List<Map<String, String>> _departments = [];
  String? _departmentCode;
  bool _loading = true;
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _loadDepartments();
  }

  Future<void> _loadDepartments() async {
    setState(() => _loading = true);
    final rows = await ApiService.getReferenceData('DEPARTMENT');
    if (!mounted) return;
    setState(() {
      _departments = rows;
      _loading = false;
    });
    if (rows.isEmpty) {
      AppNotificationService.error(
          'Unable to load departments. Please try again.');
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate() || _submitting) return;
    setState(() => _submitting = true);
    final result = await ApiService.submitDepartmentAccessRequest({
      'departmentCode': _departmentCode,
      'nodalOfficerName': _name.text.trim(),
      'officialEmail': _email.text.trim(),
      'officialMobile': _mobile.text.trim(),
      'requestPurpose': _purpose.text.trim(),
      'expectedUserCount': int.parse(_users.text),
      'remarks': _remarks.text.trim().isEmpty ? null : _remarks.text.trim(),
    });
    if (!mounted) return;
    setState(() => _submitting = false);
    if (result['success'] == true) {
      AppNotificationService.success(
          'Your application access request has been submitted successfully.');
      Navigator.pop(context);
    } else if (result['duplicate'] == true) {
      AppNotificationService.warning(
          'A request for the selected department is already pending.');
    } else {
      AppNotificationService.error(
          result['message']?.toString() ?? 'Unable to submit the request.');
    }
  }

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _mobile.dispose();
    _purpose.dispose();
    _users.dispose();
    _remarks.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Request for Application')),
        body: SafeArea(
            child: Form(
                key: _formKey,
                child: ListView(padding: const EdgeInsets.all(20), children: [
                  const Text('Department access request',
                      style:
                          TextStyle(fontSize: 24, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  const Text(
                      'Submit an onboarding request for your department.'),
                  const SizedBox(height: 20),
                  if (_loading)
                    const Center(child: CircularProgressIndicator())
                  else
                    DropdownButtonFormField<String>(
                        value: _departmentCode,
                        decoration: const InputDecoration(
                            labelText: 'Department *',
                            prefixIcon: Icon(Icons.business_outlined)),
                        items: _departments
                            .map((d) => DropdownMenuItem(
                                value: d['code'], child: Text(d['value']!)))
                            .toList(),
                        onChanged: (v) => setState(() => _departmentCode = v),
                        validator: (v) =>
                            v == null ? 'Select a department' : null),
                  const SizedBox(height: 14),
                  _field(_name, 'Nodal officer name *', Icons.badge_outlined,
                      (v) => v.trim().length < 2 ? 'Enter a valid name' : null),
                  const SizedBox(height: 14),
                  _field(
                      _email,
                      'Official email *',
                      Icons.email_outlined,
                      (v) => RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(v)
                          ? null
                          : 'Enter a valid email',
                      keyboard: TextInputType.emailAddress),
                  const SizedBox(height: 14),
                  _field(
                      _mobile,
                      'Official mobile *',
                      Icons.phone_outlined,
                      (v) => RegExp(r'^[6-9][0-9]{9}$').hasMatch(v)
                          ? null
                          : 'Enter a valid 10-digit mobile',
                      keyboard: TextInputType.phone,
                      maxLength: 10),
                  const SizedBox(height: 14),
                  _field(_users, 'Expected users *', Icons.groups_outlined,
                      (v) {
                    final n = int.tryParse(v);
                    return n == null || n < 1 || n > 10000
                        ? 'Enter 1 to 10,000'
                        : null;
                  }, keyboard: TextInputType.number),
                  const SizedBox(height: 14),
                  _field(
                      _purpose,
                      'Purpose *',
                      Icons.assignment_outlined,
                      (v) => v.trim().length < 10
                          ? 'Enter at least 10 characters'
                          : null,
                      maxLines: 4,
                      maxLength: 500),
                  const SizedBox(height: 14),
                  _field(
                      _remarks,
                      'Remarks (optional)',
                      Icons.notes_outlined,
                      (v) =>
                          v.length > 1000 ? 'Maximum 1,000 characters' : null,
                      maxLines: 3,
                      maxLength: 1000),
                  const SizedBox(height: 24),
                  ElevatedButton.icon(
                      onPressed: _loading || _departments.isEmpty || _submitting
                          ? null
                          : _submit,
                      icon: _submitting
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                  strokeWidth: 2, color: Colors.white))
                          : const Icon(Icons.send),
                      label:
                          Text(_submitting ? 'Submitting…' : 'Submit request')),
                ]))),
      );

  Widget _field(TextEditingController controller, String label, IconData icon,
          String? Function(String) validator,
          {TextInputType? keyboard, int maxLines = 1, int? maxLength}) =>
      TextFormField(
          controller: controller,
          keyboardType: keyboard,
          maxLines: maxLines,
          maxLength: maxLength,
          decoration: InputDecoration(labelText: label, prefixIcon: Icon(icon)),
          validator: (v) => validator(v?.trim() ?? ''));
}
