import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/config/app_config.dart';
import '../services/api_service.dart';
import 'visitor_registration_screen.dart';
import '../services/auth_service.dart';
import '../services/connectivity_service.dart';
import '../services/navigation_service.dart';
import '../services/offline_repository.dart';
import '../services/scanned_document_pdf_service.dart';
import '../widgets/authenticated_photo.dart';
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
  final _offlineRepository = OfflineRepository();
  final _imagePicker = ImagePicker();

  final _searchMobileCtrl = TextEditingController();
  final _searchEpicCtrl = TextEditingController();
  final _searchReferenceCtrl = TextEditingController();
  final _agendaBriefCtrl = TextEditingController();
  final _profileCtrl = TextEditingController();
  final _projectNameCtrl = TextEditingController();
  final _estimatedCostCtrl = TextEditingController();
  final _communityContributionCtrl = TextEditingController();
  final _justificationCtrl = TextEditingController();
  final _associateSearchCtrl = TextEditingController();

  int _step = 0;
  bool _submitted = false;
  bool _loading = false;
  bool _loadingReferences = true;
  bool _referencesLoaded = false;
  bool _searching = false;
  bool _searchingAssociates = false;
  bool _visitorLoading = false;
  bool _consentAccepted = false;
  bool _isOrganisation = false;
  bool _includeSchemeDetails = false;
  bool _includeAssociates = false;
  bool _mlaMdcApproved = false;
  bool _scanningDocument = false;
  String? _documentStatus;

  String? _contextError;
  String? _referenceError;
  String? _submittedAppId;
  String? _submittedToken;
  String? _agendaType;
  String? _location;
  String? _organizationSubType;
  String? _schemeType;
  String? _applicationType;
  String? _projectCategory;
  String? _beneficiaryType;
  String? _beneficiaryCount;
  String? _schemeHistory;

  Map<String, dynamic>? _selectedVisitor;
  List<Map<String, dynamic>> _searchResults = [];
  List<Map<String, dynamic>> _associateResults = [];
  final List<Map<String, dynamic>> _associates = [];
  final List<_AppointmentDocument> _documents = _defaultDocuments();
  final Set<String> _generatedDocumentPaths = {};

  final Map<String, List<_ReferenceOption>> _references = {};

  static const _steps = [
    'Citizen',
    'Agenda',
    'Scheme',
    'Associates',
    'Documents',
    'Review',
  ];

  static const _locations = ['Shillong', 'Tura', 'Delhi', 'Others'];
  static const _schemeTypes = [
    'CMSDF',
    'CMSG',
    'CM Care',
    'CM Connect',
    'CM Elevate',
    'Others',
  ];
  static const _applicationTypes = ['NEW_APPLICATION', 'REMINDER'];
  static const _projectCategories = [
    'Electricity',
    'Road',
    'House',
    'School',
    'Community Hall',
    'Retaining Wall',
    'Office',
    'Travel',
    'Medical',
    'Musical Instrument',
    'Sports Equipment',
    'Buses',
    'Pickup Van',
    'Computer Lab Upgradation',
    'Repair',
    'Others',
  ];
  static const _beneficiaryTypes = [
    'Individual',
    'Community/Society',
    'School/Youth Organisation',
    'All of the Above',
    'Others',
  ];
  static const _beneficiaryCounts = [
    '1 to 100',
    '101 to 500',
    '501 to 1000',
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
    if (widget.initialVisitor != null) {
      _selectedVisitor = Map<String, dynamic>.from(widget.initialVisitor!);
      _agendaBriefCtrl.text =
          widget.initialVisitor!['briefDescription']?.toString() ?? '';
      _agendaType = widget.initialVisitor!['agendaType']?.toString();
    }
    Future.microtask(() async {
      await _loadReferences();
      await _loadPublicVisitor();
    });
  }

  @override
  void dispose() {
    if (!_loading) {
      for (final path in _generatedDocumentPaths) {
        unawaited(_deleteIfExists(path));
      }
    }
    _searchMobileCtrl.dispose();
    _searchEpicCtrl.dispose();
    _searchReferenceCtrl.dispose();
    _agendaBriefCtrl.dispose();
    _profileCtrl.dispose();
    _projectNameCtrl.dispose();
    _estimatedCostCtrl.dispose();
    _communityContributionCtrl.dispose();
    _justificationCtrl.dispose();
    _associateSearchCtrl.dispose();
    super.dispose();
  }

  int? get _selectedVisitorId {
    final id = _selectedVisitor?['id'] ?? _selectedVisitor?['serverId'];
    if (id is num) return id.toInt();
    return int.tryParse(id?.toString() ?? '');
  }

  List<_ReferenceOption> _options(String key) => _references[key] ?? const [];

  String _label(String key, String? code) {
    if (code == null || code.isEmpty) return '';
    return _options(key)
        .firstWhere(
          (option) => option.code == code,
          orElse: () => _ReferenceOption(code: code, label: code),
        )
        .label;
  }

  Future<void> _loadReferences({bool force = false}) async {
    if (_referencesLoaded && !force) return;
    setState(() {
      _loadingReferences = true;
      _referenceError = null;
    });

    final loaded = <String, List<_ReferenceOption>>{};
    loaded['agenda'] =
        await _loadFirstReference(const ['CM_AGENDA_MEETING'], 'agenda');
    loaded['organization'] =
        await _loadFirstReference(const ['ORGANIZATION_TYPE'], 'organization');

    if (!mounted) return;
    setState(() {
      _references
        ..clear()
        ..addAll(loaded);
      _agendaType = _pickExistingOrFirst('agenda', _agendaType);
      if (widget.isWalkIn) {
        _agendaType = _findCode('agenda', 'B2') ?? _agendaType;
      }
      _location = _pickStatic(_locations, _location) ?? _locations.first;
      _applicationType = _pickStatic(_applicationTypes, _applicationType) ??
          _applicationTypes.first;
      _projectCategory = _pickStatic(_projectCategories, _projectCategory) ??
          _projectCategories.first;
      _beneficiaryType = _pickStatic(_beneficiaryTypes, _beneficiaryType) ??
          _beneficiaryTypes.first;
      _beneficiaryCount = _pickStatic(_beneficiaryCounts, _beneficiaryCount) ??
          _beneficiaryCounts.first;
      _schemeHistory = _pickStatic(_schemeHistoryOptions, _schemeHistory) ??
          _schemeHistoryOptions.first;
      _loadingReferences = false;
      _referencesLoaded = true;
      if ((_options('agenda').isEmpty || _options('organization').isEmpty) &&
          !context.read<ConnectivityService>().isOffline) {
        _referenceError =
            'Some appointment dropdown data could not be loaded from the server. Tap retry after the backend reference data is available.';
      }
    });
  }

  String? _pickStatic(List<String> options, String? value) {
    if (options.isEmpty) return null;
    if (value != null && options.contains(value)) return value;
    return options.first;
  }

  Future<List<_ReferenceOption>> _loadFirstReference(
    List<String> types,
    String cacheKey,
  ) async {
    for (final type in types) {
      debugPrint('Appointment load API: GET /api/v1/reference/$type');
      final rows = await ApiService.getReferenceData(type);
      if (rows.isNotEmpty) {
        await _offlineRepository.cacheMasterData(cacheKey, rows);
        return rows.map(_ReferenceOption.fromMap).toList();
      }
    }

    final cached = await _offlineRepository.cachedMasterData(cacheKey);
    final cachedRows = cached is List
        ? cached
        : cached is Map<String, dynamic> && cached['data'] is List
            ? cached['data'] as List<dynamic>
            : <dynamic>[];
    return cachedRows
        .whereType<Map>()
        .map((row) => _ReferenceOption.fromMap(Map<String, dynamic>.from(row)))
        .toList();
  }

  String? _pickExistingOrFirst(String key, String? value) {
    final options = _options(key);
    if (options.isEmpty) return null;
    if (value != null && options.any((option) => option.code == value)) {
      return value;
    }
    return options.first.code;
  }

  String? _findCode(String key, String codeOrLabel) {
    final wanted = codeOrLabel.toLowerCase();
    for (final option in _options(key)) {
      if (option.code.toLowerCase() == wanted ||
          option.label.toLowerCase().contains(wanted)) {
        return option.code;
      }
    }
    return null;
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

    debugPrint(
        'Appointment load API: GET /api/v1/visitor/auth/profile/$visitorId');
    final profile = await ApiService.getVisitorProfileById(visitorId);
    if (!mounted) return;
    setState(() {
      _visitorLoading = false;
      if (profile != null) _selectedVisitor = profile;
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
        if (_searchResults.length == 1) _selectedVisitor = _searchResults.first;
      }
    });
  }

  Future<void> _searchAssociates() async {
    final query = _associateSearchCtrl.text.trim();
    if (query.length < 3) {
      setState(() => _contextError = 'Enter at least 3 characters to search.');
      return;
    }
    setState(() {
      _searchingAssociates = true;
      _contextError = null;
      _associateResults = [];
    });
    final isPhone = RegExp(r'^\d{3,}$').hasMatch(query);
    final results = isPhone
        ? await ApiService.searchVisitors(mobile: query)
        : await ApiService.searchPersonsByName(query);
    if (!mounted) return;
    setState(() {
      _searchingAssociates = false;
      _associateResults = results
          .whereType<Map>()
          .map((row) => Map<String, dynamic>.from(row))
          .where(
              (row) => row['id']?.toString() != _selectedVisitorId?.toString())
          .toList();
      if (_associateResults.isEmpty) {
        _contextError = 'No matching associate visitor found.';
      }
    });
  }

  Future<void> _registerAssociate() async {
    final visitor = await Navigator.of(context).push<Map<String, dynamic>>(
      MaterialPageRoute(
        builder: (_) => const VisitorRegistrationScreen(
          returnVisitorAfterSubmit: true,
        ),
      ),
    );
    if (!mounted || visitor == null) return;
    final id = visitor['id'] ?? visitor['visitorId'] ?? visitor['citizenId'];
    if (id == null) {
      setState(() => _contextError = 'Registration succeeded, but the citizen ID was not returned.');
      return;
    }
    final normalized = <String, dynamic>{...visitor, 'id': id};
    if (_associates.any((row) => row['id']?.toString() == id.toString())) {
      setState(() => _contextError = 'This citizen is already an associate.');
      return;
    }
    setState(() {
      _associates.add(normalized);
      _associateResults = [];
      _associateSearchCtrl.clear();
      _contextError = null;
    });
  }

  Future<void> _openVisitorRegistration() async {
    await Navigator.of(context).push(
      MaterialPageRoute(builder: (_) => const VisitorRegistrationScreen()),
    );
  }

  Future<void> _pickDocument(_AppointmentDocument document) async {
    final result = await FilePicker.platform.pickFiles(
      withData: false,
      type: FileType.custom,
      allowedExtensions: const ['pdf', 'jpg', 'jpeg', 'png'],
    );
    if (result == null || result.files.isEmpty) return;
    await _removeGeneratedDocument(document.file?.path);
    setState(() {
      document.file = result.files.first;
      _documentStatus = null;
    });
  }

  Future<void> _scanDocument(_AppointmentDocument document) async {
    if (_scanningDocument) return;
    final capturedPaths = <String>[];
    File? generatedPdf;
    try {
      var captureAnother = true;
      while (captureAnother &&
          capturedPaths.length < ScannedDocumentPdfService.maxPages) {
        final image = await _imagePicker.pickImage(
          source: ImageSource.camera,
          imageQuality: 90,
          maxWidth: 2200,
          maxHeight: 2200,
          requestFullMetadata: false,
        );
        if (image == null) {
          if (capturedPaths.isEmpty) return;
          break;
        }
        capturedPaths.add(image.path);
        if (!mounted) return;
        if (capturedPaths.length >= ScannedDocumentPdfService.maxPages) break;
        final action = await showDialog<String>(
          context: context,
          barrierDismissible: false,
          builder: (dialogContext) => AlertDialog(
            title: Text(
              '${capturedPaths.length} page${capturedPaths.length == 1 ? '' : 's'} captured',
            ),
            content: Image.file(File(image.path), fit: BoxFit.contain),
            actions: [
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, 'cancel'),
                child: const Text('Cancel'),
              ),
              TextButton(
                onPressed: () => Navigator.pop(dialogContext, 'retake'),
                child: const Text('Delete / Retake'),
              ),
              OutlinedButton(
                onPressed: () => Navigator.pop(dialogContext, 'next'),
                child: const Text('Add Page'),
              ),
              ElevatedButton(
                onPressed: () => Navigator.pop(dialogContext, 'finish'),
                child: const Text('Finish'),
              ),
            ],
          ),
        );
        if (action == 'cancel') return;
        if (action == 'retake') {
          final removed = capturedPaths.removeLast();
          await _deleteIfExists(removed);
          continue;
        }
        captureAnother = action == 'next';
      }
      if (capturedPaths.isEmpty) return;
      if (!mounted) return;
      setState(() {
        _scanningDocument = true;
        _documentStatus = 'Processing document...';
      });
      generatedPdf = await ScannedDocumentPdfService.create(capturedPaths);
      final size = await generatedPdf.length();
      await _removeGeneratedDocument(document.file?.path);
      _generatedDocumentPaths.add(generatedPdf.path);
      if (!mounted) return;
      setState(() {
        document.file = PlatformFile(
          name: generatedPdf!.uri.pathSegments.last,
          path: generatedPdf.path,
          size: size,
        );
        _documentStatus =
            'Document ready. It will upload automatically with the appointment.';
        _scanningDocument = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() {
        _scanningDocument = false;
        _documentStatus =
            'Camera permission is required to scan documents. You can enable camera permission or upload an existing PDF.';
      });
    } finally {
      for (final path in capturedPaths) {
        await _deleteIfExists(path);
      }
    }
  }

  Future<void> _removeGeneratedDocument(String? path) async {
    if (path == null || !_generatedDocumentPaths.remove(path)) return;
    await _deleteIfExists(path);
  }

  Future<void> _deleteIfExists(String path) async {
    try {
      final file = File(path);
      if (await file.exists()) await file.delete();
    } catch (_) {
      // Best-effort cleanup of sensitive temporary captures.
    }
  }

  bool _validateCurrentStep() {
    setState(() => _contextError = null);
    switch (_step) {
      case 0:
        if (_selectedVisitorId == null || _selectedVisitorId! <= 0) {
          setState(() {
            _contextError = widget.isPublic
                ? 'Visitor context is missing. Please login again before continuing.'
                : 'Search and select a visitor before continuing.';
          });
          return false;
        }
        return true;
      case 1:
      case 2:
        return _formKey.currentState?.validate() ?? false;
      case 3:
        if (_includeAssociates && _associates.length > 10) {
          setState(() => _contextError = 'Maximum 10 associates are allowed.');
          return false;
        }
        return true;
      case 4:
        final missing = _visibleDocuments()
            .where((document) => document.required && document.file == null)
            .map((document) => document.label)
            .join(', ');
        if (missing.isNotEmpty) {
          setState(() => _contextError = 'Please upload: $missing.');
          return false;
        }
        return true;
      case 5:
        if (widget.isPublic && !_consentAccepted) {
          setState(() {
            _contextError =
                'Please provide consent for appointment data processing.';
          });
          return false;
        }
        return _formKey.currentState?.validate() ?? false;
    }
    return true;
  }

  void _next() {
    if (!_validateCurrentStep()) return;
    setState(() => _step = (_step + 1).clamp(0, _steps.length - 1));
  }

  void _previous() {
    setState(() {
      _contextError = null;
      _step = (_step - 1).clamp(0, _steps.length - 1);
    });
  }

  Future<void> _submit() async {
    if (_loading) return;
    if (!_validateCurrentStep()) return;
    final visitorId = _selectedVisitorId;
    if (visitorId == null || visitorId <= 0) return;

    setState(() {
      _loading = true;
      if (_generatedDocumentPaths.isNotEmpty) {
        _documentStatus = 'Uploading document...';
      }
    });
    final fields = _buildSubmitFields(visitorId);
    final documents = _visibleDocuments()
        .where((document) => document.file?.path?.isNotEmpty == true)
        .map((document) => {
              'fieldName': 'documents_${document.type}',
              'path': document.file!.path!,
              'fileName': document.file!.name,
            })
        .toList();

    final offline = context.read<ConnectivityService>().isOffline;
    final result = offline
        ? {'success': false, 'message': 'Network error. Please try again.'}
        : await ApiService.createAppointmentMultipart(
            fields: fields,
            documents: documents,
          );
    if (!mounted) return;
    final serverAppointmentId =
        result?['applicationId']?.toString() ?? result?['id']?.toString();
    final serverWalkInToken = result?['walkInTokenNumber']?.toString() ??
        result?['tokenNumber']?.toString() ??
        result?['token']?.toString();
    final hasConfirmedServerReference =
        serverAppointmentId?.isNotEmpty == true &&
            (!widget.isWalkIn || serverWalkInToken?.isNotEmpty == true);
    if (result != null &&
        result['success'] != false &&
        hasConfirmedServerReference) {
      for (final path in _generatedDocumentPaths.toList()) {
        await _removeGeneratedDocument(path);
      }
      if (!mounted) return;
      setState(() {
        _loading = false;
        _submittedAppId = serverAppointmentId;
        _submittedToken = serverWalkInToken;
        _submitted = true;
      });
      return;
    }

    setState(() {
      _loading = false;
      _contextError = offline ||
              (result?['message']
                      ?.toString()
                      .toLowerCase()
                      .contains('network') ??
                  false)
          ? 'Unable to create the appointment. Your internet connection appears to be slow or unavailable. Please check the network and try again.'
          : result?['message']?.toString() ??
              'Unable to submit appointment. Please try again.';
    });
  }

  Map<String, String> _buildSubmitFields(int visitorId) {
    final agendaLabel = _label('agenda', _agendaType);
    final locationLabel = _location ?? '';
    final fields = <String, String>{
      'applicantId': visitorId.toString(),
      'eventType': widget.isWalkIn ? 'B2' : 'A1',
      'isWalkIn': widget.isWalkIn.toString(),
      'agendaType': agendaLabel,
      'agendaTypeCode': _agendaType ?? '',
      'registrationAgendaType': agendaLabel,
      'agendaBrief': _agendaBriefCtrl.text.trim(),
      'registrationBriefDescription': _agendaBriefCtrl.text.trim(),
      'requestedLocation': locationLabel.toUpperCase(),
      'isOrganisation': _isOrganisation.toString(),
      'organizationSubType':
          _isOrganisation ? _label('organization', _organizationSubType) : '',
      'organizationSubTypeCode':
          _isOrganisation ? _organizationSubType ?? '' : '',
      'mlaMdcApproved': _mlaMdcApproved.toString(),
      'aiPriorityLevel': 'MEDIUM',
      'aiSummary': _agendaBriefCtrl.text.trim(),
      'associates': jsonEncode(_associates.map((associate) {
        return {
          'citizenId': associate['id'],
          'remarks': associate['remarks']?.toString() ?? '',
        };
      }).toList()),
    };
    if (!widget.isPublic && _selectedVisitor != null) {
      fields.addAll({
        'applicantName': _selectedVisitor?['fullName']?.toString() ?? '',
        'applicantPhone': _selectedVisitor?['phoneNumber']?.toString() ?? '',
        'epicNumber': _selectedVisitor?['epicNumber']?.toString() ?? '',
      });
    }
    if (_includeSchemeDetails) {
      fields.addAll({
        'schemeType': _schemeType ?? '',
        'applicationType': _applicationType ?? '',
        'projectCategory': _projectCategory ?? '',
        'projectName': _projectNameCtrl.text.trim(),
        'beneficiaryType': _beneficiaryType ?? '',
        'beneficiaryCount': _beneficiaryCount ?? '',
        'estimatedCost': _estimatedCostCtrl.text.trim(),
        'communityContribution': _communityContributionCtrl.text.trim(),
        'justification': _justificationCtrl.text.trim(),
        'schemeHistoryList': jsonEncode([
          if (_schemeHistory != null) _schemeHistory,
        ]),
      });
    }
    if (widget.isPublic) {
      fields.addAll({
        'consentAccepted': _consentAccepted.toString(),
        'consentVersion': AppConfig.consentVersion,
        'consentTimestamp': DateTime.now().toUtc().toIso8601String(),
        'privacyPolicyUrl': AppConfig.privacyPolicyUrl,
        'termsUrl': AppConfig.termsUrl,
      });
    }
    return fields;
  }

  void _reset() {
    _formKey.currentState?.reset();
    _searchMobileCtrl.clear();
    _searchEpicCtrl.clear();
    _searchReferenceCtrl.clear();
    _agendaBriefCtrl.clear();
    _profileCtrl.clear();
    _projectNameCtrl.clear();
    _estimatedCostCtrl.clear();
    _communityContributionCtrl.clear();
    _justificationCtrl.clear();
    _associateSearchCtrl.clear();
    for (final document in _documents) {
      document.file = null;
    }
    setState(() {
      _step = 0;
      _agendaType = _pickExistingOrFirst('agenda', null);
      if (widget.isWalkIn) {
        _agendaType = _findCode('agenda', 'B2') ?? _agendaType;
      }
      _location = _locations.first;
      _organizationSubType = null;
      _schemeType = null;
      _applicationType = _applicationTypes.first;
      _projectCategory = _projectCategories.first;
      _beneficiaryType = _beneficiaryTypes.first;
      _beneficiaryCount = _beneficiaryCounts.first;
      _schemeHistory = _schemeHistoryOptions.first;
      _isOrganisation = false;
      _includeSchemeDetails = false;
      _includeAssociates = false;
      _mlaMdcApproved = false;
      _submitted = false;
      _submittedAppId = null;
      _submittedToken = null;
      _contextError = null;
      _consentAccepted = false;
      _associates.clear();
      _associateResults = [];
      if (!widget.isPublic) _selectedVisitor = null;
    });
  }

  void _viewAllAppointments() {
    final callback = widget.onViewAppointments;
    if (callback != null) {
      callback();
      return;
    }

    final navigator = Navigator.of(context);
    context
        .read<NavigationService>()
        .navigateTo(widget.isWalkIn ? 'walkin_appointments' : 'appointments');
    if (navigator.canPop()) navigator.pop();
  }

  List<_AppointmentDocument> _visibleDocuments() {
    return _documents;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      color: const Color(0xFFF4F6FB),
      child: _submitted
          ? _buildSuccess(context)
          : Form(
              key: _formKey,
              child: SingleChildScrollView(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    if (widget.isWalkIn || widget.isPublic) _buildInfoBanner(),
                    _buildStepHeader(),
                    const SizedBox(height: 12),
                    if (_referenceError != null) ...[
                      _buildWarningBanner(_referenceError!),
                      const SizedBox(height: 12),
                    ],
                    if (_contextError != null) ...[
                      _buildErrorBanner(_contextError!),
                      const SizedBox(height: 12),
                    ],
                    _buildSection(_steps[_step], _buildCurrentStep()),
                    const SizedBox(height: 16),
                    _buildNavigation(),
                    const SizedBox(height: 16),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildCurrentStep() {
    if (_loadingReferences && _step > 0) {
      return const Padding(
        padding: EdgeInsets.all(24),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    switch (_step) {
      case 0:
        return widget.isPublic
            ? _buildSelectedVisitorSummary()
            : _buildVisitorSearch();
      case 1:
        return _buildAgendaStep();
      case 2:
        return _buildSchemeStep();
      case 3:
        return _buildAssociateStep();
      case 4:
        return _buildDocumentStep();
      case 5:
        return _buildReviewStep();
      default:
        return const SizedBox.shrink();
    }
  }

  Widget _buildStepHeader() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Row(
          children: List.generate(_steps.length, (index) {
            final active = index == _step;
            final complete = index < _step;
            return Expanded(
              child: Container(
                height: 4,
                margin:
                    EdgeInsets.only(right: index == _steps.length - 1 ? 0 : 4),
                decoration: BoxDecoration(
                  color: active || complete
                      ? const Color(0xFF1A237E)
                      : const Color(0xFFE5E7EB),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            );
          }),
        ),
        const SizedBox(height: 8),
        Text(
          'Step ${_step + 1} of ${_steps.length}: ${_steps[_step]}',
          style: const TextStyle(
            color: Color(0xFF1A237E),
            fontWeight: FontWeight.w800,
            fontSize: 13,
          ),
        ),
      ],
    );
  }

  Widget _buildNavigation() {
    final isLast = _step == _steps.length - 1;
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
                : Icon(isLast ? Icons.send : Icons.chevron_right),
            label: Text(isLast
                ? (widget.isPublic
                    ? 'Submit Appointment'
                    : 'Create Appointment')
                : 'Next'),
            onPressed: _loading ? null : (isLast ? _submit : _next),
          ),
        ),
      ],
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
                  ? 'Walk-in Counter: register an in-person visitor for a direct appointment.'
                  : 'Your registered MeghaConnect profile will be used for this appointment.',
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

  Widget _buildWarningBanner(String text) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFFBEB),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: const Color(0xFFFDE68A)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(Icons.warning_amber_outlined,
              color: Color(0xFF92400E), size: 20),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              text,
              style: const TextStyle(color: Color(0xFF92400E), fontSize: 12),
            ),
          ),
          TextButton(
            onPressed: () => _loadReferences(force: true),
            child: const Text('Retry'),
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

  Widget _buildSelectedVisitorSummary() {
    if (_visitorLoading) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(16),
          child: CircularProgressIndicator(),
        ),
      );
    }
    final visitor = _selectedVisitor;
    if (visitor == null) {
      return const Text(
        'Visitor profile could not be loaded.',
        style: TextStyle(color: Color(0xFF991B1B)),
      );
    }
    return _VisitorSummary(visitor: visitor);
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
          ..._searchResults.map((visitor) => _visitorPickTile(
                visitor,
                onTap: () {
                  setState(() {
                    _selectedVisitor = visitor;
                    _contextError = null;
                  });
                },
              )),
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

  Widget _buildAgendaStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _referenceDropdown(
          keyName: 'agenda',
          value: _agendaType,
          label: 'Agenda Type *',
          icon: Icons.category_outlined,
          required: true,
          onChanged: (value) => setState(() => _agendaType = value),
        ),
        const SizedBox(height: 12),
        _referenceDropdown(
          value: _location,
          label: 'Preferred Location *',
          icon: Icons.place_outlined,
          values: _locations,
          required: true,
          onChanged: (value) => setState(() => _location = value),
        ),
        const SizedBox(height: 12),
        TextFormField(
          controller: _agendaBriefCtrl,
          maxLines: 4,
          decoration: const InputDecoration(
            labelText: 'Brief Description of Agenda *',
            prefixIcon: Icon(Icons.description_outlined),
            alignLabelWithHint: true,
            hintText: 'Describe your purpose of visit in detail...',
          ),
          validator: (value) => (value == null || value.trim().isEmpty)
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
          ),
        ),
        const SizedBox(height: 12),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _isOrganisation,
          onChanged: (value) => setState(() => _isOrganisation = value),
          title: const Text('Applicant represents an organisation'),
        ),
        if (_isOrganisation) ...[
          const SizedBox(height: 8),
          _referenceDropdown(
            keyName: 'organization',
            value: _organizationSubType,
            label: 'Organisation Type *',
            icon: Icons.business_outlined,
            required: true,
            onChanged: (value) => setState(() => _organizationSubType = value),
          ),
        ],
      ],
    );
  }

  Widget _buildSchemeStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _includeSchemeDetails,
          onChanged: (value) => setState(() => _includeSchemeDetails = value),
          title: const Text('Add CM scheme / project details'),
        ),
        if (_includeSchemeDetails) ...[
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _schemeType,
            label: 'Scheme Type *',
            icon: Icons.workspace_premium_outlined,
            values: _schemeTypes,
            required: true,
            onChanged: (value) => setState(() => _schemeType = value),
          ),
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _applicationType,
            label: 'Application Type *',
            icon: Icons.assignment_outlined,
            values: _applicationTypes,
            required: true,
            onChanged: (value) => setState(() => _applicationType = value),
          ),
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _projectCategory,
            label: 'Project Category *',
            icon: Icons.category_outlined,
            values: _projectCategories,
            required: true,
            onChanged: (value) => setState(() => _projectCategory = value),
          ),
          const SizedBox(height: 12),
          TextFormField(
            controller: _projectNameCtrl,
            decoration: const InputDecoration(
              labelText: 'Project Name *',
              prefixIcon: Icon(Icons.drive_file_rename_outline),
            ),
            validator: (value) => (value == null || value.trim().isEmpty)
                ? 'Please enter project name'
                : null,
          ),
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _beneficiaryType,
            label: 'Beneficiary Type',
            icon: Icons.groups_outlined,
            values: _beneficiaryTypes,
            onChanged: (value) => setState(() => _beneficiaryType = value),
          ),
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _beneficiaryCount,
            label: 'Beneficiary Count',
            icon: Icons.format_list_numbered_outlined,
            values: _beneficiaryCounts,
            onChanged: (value) => setState(() => _beneficiaryCount = value),
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
          const SizedBox(height: 12),
          _referenceDropdown(
            value: _schemeHistory,
            label: 'Scheme History',
            icon: Icons.history_outlined,
            values: _schemeHistoryOptions,
            onChanged: (value) => setState(() => _schemeHistory = value),
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            value: _mlaMdcApproved,
            onChanged: (value) => setState(() => _mlaMdcApproved = value),
            title: const Text('MLA/MDC approval received'),
          ),
        ],
      ],
    );
  }

  Widget _buildAssociateStep() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          value: _includeAssociates,
          onChanged: (value) => setState(() => _includeAssociates = value),
          title: const Text('Include associate visitors'),
        ),
        if (_includeAssociates) ...[
          const SizedBox(height: 8),
          TextField(
            controller: _associateSearchCtrl,
            decoration: const InputDecoration(
              labelText: 'Search associate by name or mobile',
              prefixIcon: Icon(Icons.search),
            ),
            onSubmitted: (_) => _searchAssociates(),
          ),
          const SizedBox(height: 10),
          OutlinedButton.icon(
            icon: _searchingAssociates
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.person_search_outlined),
            label: Text(_searchingAssociates ? 'Searching...' : 'Search'),
            onPressed: _searchingAssociates ? null : _searchAssociates,
          ),
          const SizedBox(height: 12),
          if (!_searchingAssociates &&
              _associateResults.isEmpty &&
              _associateSearchCtrl.text.trim().length >= 3) ...[
            const Text('Visitor not found'),
            const SizedBox(height: 8),
            FilledButton.icon(
              onPressed: _registerAssociate,
              icon: const Icon(Icons.person_add_alt_1_outlined),
              label: const Text('Register New Visitor'),
            ),
            const SizedBox(height: 12),
          ],
          ..._associateResults.map((visitor) {
            final id = visitor['id']?.toString();
            final added = id != null &&
                _associates.any((row) => row['id']?.toString() == id);
            return _visitorPickTile(
              visitor,
              trailing: added ? const Icon(Icons.check) : const Icon(Icons.add),
              onTap: added
                  ? null
                  : () {
                      setState(() {
                        _associates.add(visitor);
                        _associateResults = [];
                        _associateSearchCtrl.clear();
                      });
                    },
            );
          }),
          if (_associates.isNotEmpty) ...[
            const SizedBox(height: 8),
            ..._associates.map((visitor) => _visitorPickTile(
                  visitor,
                  trailing: IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () =>
                        setState(() => _associates.remove(visitor)),
                  ),
                )),
          ],
        ],
      ],
    );
  }

  Widget _buildDocumentStep() {
    final docs = _visibleDocuments();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Upload the primary application letter or project proposal. Accepted formats: PDF, JPG, JPEG, or PNG.',
          style: TextStyle(fontSize: 12, color: Color(0xFF6B7280)),
        ),
        const SizedBox(height: 10),
        if (_documentStatus != null) ...[
          Text(
            _documentStatus!,
            style: const TextStyle(fontSize: 12, color: Color(0xFF1A237E)),
          ),
          const SizedBox(height: 10),
        ],
        ...docs.map((document) {
          final fileName = document.file?.name;
          return Container(
            margin: const EdgeInsets.only(bottom: 10),
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              border: Border.all(color: const Color(0xFFE5E7EB)),
              borderRadius: BorderRadius.circular(8),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(children: [
                  const Icon(Icons.attach_file, color: Color(0xFF1A237E)),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          document.required
                              ? '${document.label} *'
                              : document.label,
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        Text(
                          fileName ?? 'No file selected',
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style:
                              TextStyle(fontSize: 12, color: Colors.grey[700]),
                        ),
                      ],
                    ),
                  ),
                ]),
                const SizedBox(height: 8),
                Row(children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: _scanningDocument
                          ? null
                          : () => _pickDocument(document),
                      icon: const Icon(Icons.upload_file),
                      label: Text(fileName == null ? 'Upload PDF' : 'Change'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: _scanningDocument
                          ? null
                          : () => _scanDocument(document),
                      icon: _scanningDocument
                          ? const SizedBox.square(
                              dimension: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.document_scanner_outlined),
                      label: const Text('Scan Document'),
                    ),
                  ),
                ]),
              ],
            ),
          );
        }),
      ],
    );
  }

  Widget _buildReviewStep() {
    final fields = _selectedVisitorId == null
        ? <String, String>{}
        : _buildSubmitFields(_selectedVisitorId!);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (_selectedVisitor != null)
          _VisitorSummary(visitor: _selectedVisitor!),
        const SizedBox(height: 12),
        _reviewRow('Agenda', fields['agendaType'] ?? ''),
        _reviewRow('Location', fields['requestedLocation'] ?? ''),
        _reviewRow('Purpose', fields['agendaBrief'] ?? ''),
        if (_includeSchemeDetails) ...[
          _reviewRow('Scheme', fields['schemeType'] ?? ''),
          _reviewRow('Project', fields['projectName'] ?? ''),
          _reviewRow('Category', fields['projectCategory'] ?? ''),
        ],
        _reviewRow('Associates', _associates.length.toString()),
        _reviewRow(
          'Documents',
          _visibleDocuments()
              .where((document) => document.file != null)
              .length
              .toString(),
        ),
        if (widget.isPublic) ...[
          const SizedBox(height: 12),
          _buildConsentNotice(),
        ],
      ],
    );
  }

  Widget _reviewRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(
              label,
              style: TextStyle(
                color: Colors.grey[700],
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value.isEmpty ? '-' : value,
              style: const TextStyle(fontSize: 13),
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

  Widget _visitorPickTile(
    Map<String, dynamic> visitor, {
    Widget? trailing,
    VoidCallback? onTap,
  }) {
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
        title: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis),
        subtitle: Text([
          if (phone.isNotEmpty) phone,
          if (epic.isNotEmpty) epic,
        ].join(' · ')),
        trailing: trailing ?? const Icon(Icons.chevron_right),
        onTap: onTap,
      ),
    );
  }

  DropdownButtonFormField<String> _referenceDropdown({
    String? keyName,
    List<String>? values,
    required String? value,
    required String label,
    required IconData icon,
    required ValueChanged<String?> onChanged,
    bool required = false,
  }) {
    final options = keyName == null
        ? (values ?? const [])
            .map((item) => _ReferenceOption(code: item, label: item))
            .toList()
        : _options(keyName);
    final current =
        options.any((option) => option.code == value) ? value : null;
    return DropdownButtonFormField<String>(
      value: current,
      isExpanded: true,
      decoration: InputDecoration(
        labelText: options.isEmpty ? '$label (not loaded)' : label,
        prefixIcon: Icon(icon),
      ),
      validator: required
          ? (selected) => selected == null || selected.isEmpty
              ? 'Please select $label'
              : null
          : null,
      selectedItemBuilder: (context) => options
          .map((option) => Text(
                option.label,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ))
          .toList(),
      items: options
          .map(
            (option) => DropdownMenuItem(
              value: option.code,
              child: Text(
                option.label,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
            ),
          )
          .toList(),
      onChanged: options.isEmpty ? null : onChanged,
    );
  }

  Widget _buildSuccess(BuildContext context) {
    final appId = _submittedAppId ??
        'MC-${DateTime.now().year}-${(DateTime.now().millisecondsSinceEpoch % 90000 + 10000).toString()}';

    return LayoutBuilder(
      builder: (context, constraints) => SingleChildScrollView(
        child: ConstrainedBox(
          constraints: BoxConstraints(minHeight: constraints.maxHeight),
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
                  padding:
                      const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
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
                    padding: const EdgeInsets.symmetric(
                        horizontal: 16, vertical: 10),
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
                      ),
                    ),
                    if (!widget.isPublic) ...[
                      const SizedBox(width: 12),
                      Expanded(
                        child: ElevatedButton.icon(
                          icon: const Icon(Icons.list_alt),
                          label: const Text('View All'),
                          onPressed: _viewAllAppointments,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _ReferenceOption {
  final String code;
  final String label;

  const _ReferenceOption({required this.code, required this.label});

  factory _ReferenceOption.fromMap(Map<dynamic, dynamic> row) {
    final code = row['code']?.toString() ??
        row['id']?.toString() ??
        row['value']?.toString() ??
        '';
    final label = row['value']?.toString() ??
        row['label']?.toString() ??
        row['name']?.toString() ??
        code;
    return _ReferenceOption(code: code, label: label);
  }
}

class _AppointmentDocument {
  final String type;
  final String label;
  final bool required;
  PlatformFile? file;

  _AppointmentDocument({
    required this.type,
    required this.label,
    this.required = false,
  });
}

List<_AppointmentDocument> _defaultDocuments() {
  return [
    _AppointmentDocument(
      type: 'APPLICATION_LETTER',
      label: 'Application Letter / Project Proposal',
      required: true,
    ),
  ];
}

class _VisitorSummary extends StatelessWidget {
  final Map<String, dynamic> visitor;

  const _VisitorSummary({required this.visitor});

  @override
  Widget build(BuildContext context) {
    final name = visitor['fullName']?.toString() ?? 'Visitor';
    final phone = visitor['phoneNumber']?.toString() ?? '-';
    final epic = visitor['epicNumber']?.toString() ?? '-';
    final district = visitor['district']?.toString() ?? '-';
    final constituency = visitor['constituency']?.toString() ?? '-';
    final booth = visitor['booth']?.toString() ??
        visitor['boothVillage']?.toString() ??
        '-';
    final part = visitor['partNumber']?.toString() ??
        visitor['pollingPartNo']?.toString() ??
        '-';
    final kyc = visitor['kycStatus']?.toString() ??
        (visitor['kycVerified'] == true ? 'VERIFIED' : 'PENDING');
    final photo = visitor['photoUrl']?.toString() ??
        visitor['photoStoragePath']?.toString() ??
        visitor['photoPath']?.toString() ??
        visitor['livePhotoBase64']?.toString();

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
              ClipOval(
                child: SizedBox.square(
                  dimension: 42,
                  child: AuthenticatedPhoto(
                    source: photo,
                    fallback: CircleAvatar(
                      backgroundColor: const Color(0xFF1A237E),
                      foregroundColor: Colors.white,
                      child: Text(name.isEmpty ? 'V' : name[0].toUpperCase()),
                    ),
                  ),
                ),
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
            value.isEmpty ? '-' : value,
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
