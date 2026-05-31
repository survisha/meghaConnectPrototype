import 'dart:convert';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/api_service.dart';
import '../services/auth_service.dart';

class SchemeFormScreen extends StatefulWidget {
  const SchemeFormScreen({super.key});

  @override
  State<SchemeFormScreen> createState() => _SchemeFormScreenState();
}

class _SchemeFormScreenState extends State<SchemeFormScreen> {
  final _formKey = GlobalKey<FormState>();
  final _projectNameCtrl = TextEditingController();
  final _estimatedCostCtrl = TextEditingController();
  final _communityContributionCtrl = TextEditingController();
  final _justificationCtrl = TextEditingController();

  int _step = 0;
  bool _loading = false;
  bool _loadingSchemes = true;
  bool _submitted = false;
  bool _mlaMdcApproved = false;
  String? _error;
  String? _submittedId;
  String? _schemeType;
  String _projectCategory = _projectCategories.first;
  String _beneficiaryType = _beneficiaryTypes.first;
  String _beneficiaryCount = _beneficiaryCounts.first;

  final List<_Ref> _schemeTypes = [];
  final List<_SchemeItem> _items = [_SchemeItem()];
  final List<String> _schemeHistory = [];
  final List<_SchemeDocument> _documents = _defaultDocuments();

  static const _steps = [
    'Scheme',
    'Project',
    'Financial',
    'Documents',
    'Submit',
  ];

  static const _projectCategories = [
    'Electricity',
    'Road',
    'House',
    'School',
    'Community hall',
    'Retaining wall',
    'Office',
    'Travel',
    'Medical',
    'Musical instrument',
    'Sports Equipment',
    'Buses',
    'Pickup Van',
    'Computer lab upgradation',
    'Repair',
    'Others',
  ];
  static const _beneficiaryTypes = [
    'Individual',
    'Community/Society',
    'School/Youth Organisation',
    'All of the above',
    'Others',
  ];
  static const _beneficiaryCounts = [
    '1 TO 100',
    '101 TO 500',
    '501 TO 1000',
    'Above 1000',
  ];
  static const _schemeHistoryOptions = [
    'CMSDF',
    'CMSG',
    'CM Care',
    'CM Connect',
    'CM Elevate',
    'Focus+',
  ];

  @override
  void initState() {
    super.initState();
    _loadSchemeTypes();
  }

  @override
  void dispose() {
    _projectNameCtrl.dispose();
    _estimatedCostCtrl.dispose();
    _communityContributionCtrl.dispose();
    _justificationCtrl.dispose();
    for (final item in _items) {
      item.dispose();
    }
    super.dispose();
  }

  Future<void> _loadSchemeTypes() async {
    setState(() {
      _loadingSchemes = true;
      _error = null;
    });
    debugPrint('Scheme form API: GET /api/v1/reference/CM_SCHEME');
    final rows = await ApiService.getReferenceData('CM_SCHEME');
    if (!mounted) return;
    setState(() {
      _schemeTypes
        ..clear()
        ..addAll(rows.map((row) => _Ref(
              code: row['code'] ?? '',
              label: row['value'] ?? row['code'] ?? '',
            )));
      _schemeType = _schemeTypes.isEmpty ? null : _schemeTypes.first.code;
      _loadingSchemes = false;
      if (_schemeTypes.isEmpty) {
        _error = 'Failed to load scheme types. Please try again.';
      }
    });
  }

  bool get _isCmCare {
    final value = (_schemeType ?? '').trim().toUpperCase();
    return value == 'CM_CARE' || value == 'CMCARE';
  }

  double get _totalCost {
    return _items.fold<double>(0, (sum, item) {
      final quantity = double.tryParse(item.quantity.text) ?? 0;
      final unitCost = double.tryParse(item.unitCost.text) ?? 0;
      return sum + (quantity * unitCost);
    });
  }

  int? _applicantId() {
    final user = context.read<AuthService>().user;
    final visitorId = user?.visitorId;
    return visitorId != null && visitorId > 0 ? visitorId : null;
  }

  void _next() {
    if (!_validateStep()) return;
    setState(() => _step = (_step + 1).clamp(0, _steps.length - 1));
  }

  void _previous() {
    setState(() {
      _error = null;
      _step = (_step - 1).clamp(0, _steps.length - 1);
    });
  }

  bool _validateStep() {
    setState(() => _error = null);
    if (_step == 0 && (_schemeType == null || _schemeType!.isEmpty)) {
      setState(() => _error = 'Please select a scheme type.');
      return false;
    }
    if (_step == 1 && !_formKey.currentState!.validate()) return false;
    if (_step == 3) {
      final missing = _visibleDocuments()
          .where((doc) => doc.required && doc.file == null)
          .map((doc) => doc.label)
          .join(', ');
      if (missing.isNotEmpty) {
        setState(
            () => _error = 'Please upload all required documents: $missing');
        return false;
      }
    }
    return true;
  }

  Future<void> _pickDocument(_SchemeDocument doc) async {
    final result = await FilePicker.platform.pickFiles(withData: false);
    if (result == null || result.files.isEmpty) return;
    setState(() => doc.file = result.files.first);
  }

  Future<void> _submit() async {
    if (!_validateStep()) return;
    final applicantId = _applicantId();
    if (applicantId == null) {
      setState(() {
        _error =
            'Visitor context is missing. Please login as a visitor or open this form with a selected visitor.';
      });
      return;
    }
    if ((_schemeType ?? '').isEmpty) {
      setState(() => _error = 'Please select a scheme type.');
      return;
    }
    if (_projectNameCtrl.text.trim().isEmpty) {
      setState(() => _error = 'Please enter the project name.');
      return;
    }

    final items = _items
        .where((item) => item.description.text.trim().isNotEmpty)
        .map((item) => {
              'description': item.description.text.trim(),
              'quantity': int.tryParse(item.quantity.text) ?? 1,
              'unitCost': double.tryParse(item.unitCost.text) ?? 0,
            })
        .toList();
    final fields = {
      'applicantId': applicantId.toString(),
      'schemeType': _schemeType ?? '',
      'projectName': _projectNameCtrl.text.trim(),
      'projectCategory': _projectCategory,
      'beneficiaryType': _beneficiaryType,
      'beneficiaryCount': _beneficiaryCount,
      'estimatedCost': (_totalCost > 0
              ? _totalCost
              : double.tryParse(_estimatedCostCtrl.text) ?? 0)
          .toString(),
      'communityContribution':
          (double.tryParse(_communityContributionCtrl.text) ?? 0).toString(),
      'justification': _justificationCtrl.text.trim(),
      'itemsJson': jsonEncode(items),
    };
    final documents = _visibleDocuments()
        .where((doc) => doc.file?.path?.isNotEmpty == true)
        .map((doc) => {
              'fieldName': 'documents_${doc.type}',
              'path': doc.file!.path!,
              'fileName': doc.file!.name,
            })
        .toList();

    setState(() => _loading = true);
    final result = await ApiService.createSchemeApplicationMultipart(
      fields: fields,
      documents: documents,
    );
    if (!mounted) return;
    if (result['success'] == false) {
      setState(() {
        _loading = false;
        _error = result['message']?.toString() ??
            'Failed to submit scheme application.';
      });
      return;
    }
    setState(() {
      _loading = false;
      _submitted = true;
      _submittedId =
          result['id'] == null ? '' : 'SC-${result['id'].toString()}';
    });
  }

  List<_SchemeDocument> _visibleDocuments() {
    return _documents.where((doc) {
      if (doc.type == 'MLA_APPROVAL_LETTER') return _mlaMdcApproved;
      if (doc.type == 'CM_CARE_HOSPITAL') return _isCmCare;
      return doc.visible;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    if (_submitted) return _buildSuccess();
    return Form(
      key: _formKey,
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const Text(
              'Scheme Application',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: Color(0xFF1A237E),
              ),
            ),
            const SizedBox(height: 12),
            _buildStepper(),
            const SizedBox(height: 12),
            if (_error != null) ...[
              _errorBanner(_error!),
              const SizedBox(height: 12)
            ],
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: _loadingSchemes
                    ? const Center(child: CircularProgressIndicator())
                    : _buildStep(),
              ),
            ),
            const SizedBox(height: 16),
            _navigation(),
          ],
        ),
      ),
    );
  }

  Widget _buildStepper() {
    return Row(
      children: List.generate(_steps.length, (index) {
        final active = index <= _step;
        return Expanded(
          child: Container(
            height: 4,
            margin: EdgeInsets.only(right: index == _steps.length - 1 ? 0 : 4),
            decoration: BoxDecoration(
              color: active ? const Color(0xFF1A237E) : const Color(0xFFE5E7EB),
              borderRadius: BorderRadius.circular(2),
            ),
          ),
        );
      }),
    );
  }

  Widget _buildStep() {
    switch (_step) {
      case 0:
        return _schemeStep();
      case 1:
        return _projectStep();
      case 2:
        return _financialStep();
      case 3:
        return _documentsStep();
      case 4:
        return _reviewStep();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _schemeStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _dropdown(
          value: _schemeType,
          label: 'Scheme Type *',
          values: _schemeTypes.map((item) => item.code).toList(),
          labelFor: (code) =>
              _schemeTypes.firstWhere((item) => item.code == code).label,
          onChanged: (value) => setState(() => _schemeType = value),
        ),
        const SizedBox(height: 12),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: _schemeHistoryOptions.map((scheme) {
            final selected = _schemeHistory.contains(scheme);
            return FilterChip(
              label: Text(scheme),
              selected: selected,
              onSelected: (_) => setState(() {
                selected
                    ? _schemeHistory.remove(scheme)
                    : _schemeHistory.add(scheme);
              }),
            );
          }).toList(),
        ),
      ],
    );
  }

  Widget _projectStep() {
    return Column(
      children: [
        TextFormField(
          controller: _projectNameCtrl,
          decoration: const InputDecoration(labelText: 'Project Name *'),
          validator: (value) => (value == null || value.trim().isEmpty)
              ? 'Please enter the project name.'
              : null,
        ),
        const SizedBox(height: 12),
        _dropdown(
          value: _projectCategory,
          label: 'Project Category',
          values: _projectCategories,
          onChanged: (value) => setState(() => _projectCategory = value!),
        ),
        const SizedBox(height: 12),
        _dropdown(
          value: _beneficiaryType,
          label: 'Who will benefit',
          values: _beneficiaryTypes,
          onChanged: (value) => setState(() => _beneficiaryType = value!),
        ),
        const SizedBox(height: 12),
        _dropdown(
          value: _beneficiaryCount,
          label: 'Total People Benefiting',
          values: _beneficiaryCounts,
          onChanged: (value) => setState(() => _beneficiaryCount = value!),
        ),
      ],
    );
  }

  Widget _financialStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextFormField(
          controller: _estimatedCostCtrl,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Estimated Cost'),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _communityContributionCtrl,
          keyboardType: TextInputType.number,
          decoration:
              const InputDecoration(labelText: 'Community Contribution'),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _justificationCtrl,
          maxLines: 3,
          decoration: const InputDecoration(labelText: 'Justification'),
        ),
        const SizedBox(height: 12),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _mlaMdcApproved,
          onChanged: (value) => setState(() => _mlaMdcApproved = value),
          title: const Text('MLA / MDC letter available'),
        ),
        const SizedBox(height: 8),
        ..._items.asMap().entries.map((entry) => _itemRow(entry.key)),
        OutlinedButton.icon(
          icon: const Icon(Icons.add),
          label: const Text('Add Item'),
          onPressed: () => setState(() => _items.add(_SchemeItem())),
        ),
      ],
    );
  }

  Widget _itemRow(int index) {
    final item = _items[index];
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      child: Row(
        children: [
          Expanded(
            flex: 3,
            child: TextField(
              controller: item.description,
              decoration: const InputDecoration(labelText: 'Item'),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: TextField(
              controller: item.quantity,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Qty'),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: TextField(
              controller: item.unitCost,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Cost'),
            ),
          ),
          if (_items.length > 1)
            IconButton(
              icon: const Icon(Icons.delete_outline),
              onPressed: () => setState(() {
                final removed = _items.removeAt(index);
                removed.dispose();
              }),
            ),
        ],
      ),
    );
  }

  Widget _documentsStep() {
    return Column(
      children: _visibleDocuments().map((doc) {
        return ListTile(
          contentPadding: EdgeInsets.zero,
          leading: const Icon(Icons.attach_file, color: Color(0xFF1A237E)),
          title: Text(doc.required ? '${doc.label} *' : doc.label),
          subtitle: Text(doc.file?.name ?? 'No file selected'),
          trailing: TextButton(
            onPressed: () => _pickDocument(doc),
            child: Text(doc.file == null ? 'Upload' : 'Change'),
          ),
        );
      }).toList(),
    );
  }

  Widget _reviewStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _review('Scheme', _schemeLabel(_schemeType)),
        _review('Project', _projectNameCtrl.text),
        _review('Category', _projectCategory),
        _review('Beneficiaries', _beneficiaryCount),
        _review('Estimated Cost',
            'Rs ${(_totalCost > 0 ? _totalCost : double.tryParse(_estimatedCostCtrl.text) ?? 0).toStringAsFixed(0)}'),
        _review('MLA/MDC Letter', _mlaMdcApproved ? 'Yes' : 'No'),
      ],
    );
  }

  Widget _navigation() {
    final last = _step == _steps.length - 1;
    return Row(
      children: [
        if (_step > 0)
          Expanded(
            child: OutlinedButton.icon(
              icon: const Icon(Icons.chevron_left),
              label: const Text('Previous'),
              onPressed: _loading ? null : _previous,
            ),
          ),
        if (_step > 0) const SizedBox(width: 12),
        Expanded(
          child: ElevatedButton.icon(
            icon: _loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(
                      strokeWidth: 2,
                      color: Colors.white,
                    ),
                  )
                : Icon(last ? Icons.send : Icons.chevron_right),
            label: Text(last ? 'Submit Application' : 'Next'),
            onPressed: _loading ? null : (last ? _submit : _next),
          ),
        ),
      ],
    );
  }

  Widget _dropdown({
    required String? value,
    required String label,
    required List<String> values,
    String Function(String value)? labelFor,
    required ValueChanged<String?> onChanged,
  }) {
    return DropdownButtonFormField<String>(
      value: values.contains(value) ? value : null,
      isExpanded: true,
      decoration: InputDecoration(labelText: label),
      items: values
          .map((item) => DropdownMenuItem(
                value: item,
                child: Text(
                  labelFor?.call(item) ?? item,
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                ),
              ))
          .toList(),
      onChanged: onChanged,
    );
  }

  String _schemeLabel(String? code) {
    if (code == null) return '-';
    return _schemeTypes
        .firstWhere((item) => item.code == code,
            orElse: () => _Ref(code: code, label: code))
        .label;
  }

  Widget _review(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(label,
                style: const TextStyle(fontWeight: FontWeight.w700)),
          ),
          Expanded(child: Text(value.isEmpty ? '-' : value)),
        ],
      ),
    );
  }

  Widget _errorBanner(String message) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFEF2F2),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFFECACA)),
      ),
      child: Text(message, style: const TextStyle(color: Color(0xFF991B1B))),
    );
  }

  Widget _buildSuccess() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.check_circle_outline,
                color: Color(0xFF16A34A), size: 80),
            const SizedBox(height: 16),
            const Text(
              'Scheme application submitted successfully',
              textAlign: TextAlign.center,
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800),
            ),
            if ((_submittedId ?? '').isNotEmpty) ...[
              const SizedBox(height: 10),
              Text(_submittedId!,
                  style: const TextStyle(color: Color(0xFF1A237E))),
            ],
          ],
        ),
      ),
    );
  }
}

class _Ref {
  final String code;
  final String label;

  const _Ref({required this.code, required this.label});
}

class _SchemeItem {
  final description = TextEditingController();
  final quantity = TextEditingController(text: '1');
  final unitCost = TextEditingController(text: '0');

  void dispose() {
    description.dispose();
    quantity.dispose();
    unitCost.dispose();
  }
}

class _SchemeDocument {
  final String type;
  final String label;
  final bool required;
  final bool visible;
  PlatformFile? file;

  _SchemeDocument({
    required this.type,
    required this.label,
    required this.required,
    required this.visible,
  });
}

List<_SchemeDocument> _defaultDocuments() {
  return [
    _SchemeDocument(
      type: 'PLANS_ESTIMATES',
      label: 'Plans & Estimates (3 nos)',
      required: true,
      visible: true,
    ),
    _SchemeDocument(
      type: 'BANK_DETAILS',
      label: 'Bank Account Details',
      required: true,
      visible: true,
    ),
    _SchemeDocument(
      type: 'MLA_APPROVAL_LETTER',
      label: 'MLA / MDC Letter',
      required: false,
      visible: false,
    ),
    _SchemeDocument(
      type: 'CM_CARE_HOSPITAL',
      label: 'Hospital / Medical Docs (CM Care)',
      required: false,
      visible: false,
    ),
    _SchemeDocument(
      type: 'ORG_REGISTRATION_CERTIFICATE',
      label: 'Organisation Registration Certificate',
      required: false,
      visible: true,
    ),
  ];
}
