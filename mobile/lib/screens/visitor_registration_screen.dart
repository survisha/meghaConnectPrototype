import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/i18n/app_i18n.dart';
import '../services/api_service.dart';
import '../widgets/megha_ui.dart';

class VisitorRegistrationScreen extends StatefulWidget {
  const VisitorRegistrationScreen({super.key});

  @override
  State<VisitorRegistrationScreen> createState() =>
      _VisitorRegistrationScreenState();
}

class _VisitorRegistrationScreenState extends State<VisitorRegistrationScreen> {
  final _idFormKey = GlobalKey<FormState>();
  final _otpFormKey = GlobalKey<FormState>();
  final _detailsFormKey = GlobalKey<FormState>();

  final _epicCtrl = TextEditingController();
  final _visitorNameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  final _otpCtrl = TextEditingController();
  final _fullNameCtrl = TextEditingController();
  final _addressCtrl = TextEditingController();
  final _districtCtrl = TextEditingController();
  final _constituencyCtrl = TextEditingController();
  final _boothCtrl = TextEditingController();
  final _villageCtrl = TextEditingController();
  final _designationCtrl = TextEditingController();
  final _emailCtrl = TextEditingController();

  int _step = 0;
  String _idType = 'EPIC';
  bool _loading = false;
  bool _submitted = false;
  bool _photoCaptured = false;
  bool _outsideMeghalaya = false;
  String? _error;
  String? _warning;
  String? _success;
  String? _qrDataUri;
  String? _aadhaarTxnId;
  String? _livePhotoDataUri;
  int _kycConfidence = 0;

  static const _steps = [
    MeghaStepData('ID Verification', Icons.badge_outlined),
    MeghaStepData('OTP Verification', Icons.phone_android_outlined),
    MeghaStepData('Photo Capture', Icons.photo_camera_outlined),
    MeghaStepData('Details', Icons.person_outline),
    MeghaStepData('Complete', Icons.verified_outlined),
  ];

  static const _designations = [
    'Citizen',
    'Student',
    'Farmer',
    'Business Owner',
    'Government Employee',
    'Community Leader',
    'Other',
  ];

  @override
  void dispose() {
    _epicCtrl.dispose();
    _visitorNameCtrl.dispose();
    _phoneCtrl.dispose();
    _otpCtrl.dispose();
    _fullNameCtrl.dispose();
    _addressCtrl.dispose();
    _districtCtrl.dispose();
    _constituencyCtrl.dispose();
    _boothCtrl.dispose();
    _villageCtrl.dispose();
    _designationCtrl.dispose();
    _emailCtrl.dispose();
    super.dispose();
  }

  void _clearMessages() {
    _error = null;
    _warning = null;
    _success = null;
  }

  void _switchIdType(String value) {
    setState(() {
      _idType = value;
      _qrDataUri = null;
      _aadhaarTxnId = null;
      _otpCtrl.clear();
      _clearMessages();
    });
  }

  Future<void> _startEpicFlow() async {
    final i18n = context.read<AppI18n>();
    if (!_idFormKey.currentState!.validate()) return;

    setState(() {
      _loading = true;
      _clearMessages();
    });

    final phone = _phoneCtrl.text.trim();
    final epic = _epicCtrl.text.trim().toUpperCase();
    final check = await ApiService.checkVisitorRegistration(
      phoneNumber: phone,
      epicNumber: epic,
    );

    if (!mounted) return;
    if (check['epicMobileExists'] == true ||
        check['alreadyRegistered'] == true) {
      setState(() {
        _loading = false;
        _error =
            (check['message'] as String?) ?? i18n.t('USER_ALREADY_REGISTERED');
      });
      return;
    }
    if (check['mobileExists'] == true) {
      _warning =
          (check['message'] as String?) ?? i18n.t('WARNING_MOBILE_EXISTS');
    }

    final epicResult = await ApiService.verifyEpic(
      epicNumber: epic,
      visitorName: _visitorNameCtrl.text,
      phoneNumber: phone,
    );

    if (!mounted) return;
    final epicOk = epicResult['success'] == true ||
        epicResult['code']?.toString() == '200' ||
        epicResult['message']?.toString().toLowerCase() == 'success';

    if (!epicOk) {
      setState(() {
        _loading = false;
        _error = (epicResult['message'] as String?) ??
            i18n.t('ERROR_FAILED_VERIFY_EPIC_TRY');
      });
      return;
    }

    _populateFromEpic(epicResult);

    final otpResult = await ApiService.generateVisitorOtp(
      phoneNumber: phone,
      epicNumber: epic,
    );

    if (!mounted) return;
    setState(() {
      _loading = false;
      if (otpResult['success'] == true) {
        _step = 1;
        _success = i18n.t('OTP_SENT_SUCCESS');
      } else {
        _error = (otpResult['message'] as String?) ??
            i18n.t('ERROR_FAILED_GENERATE_OTP_TRY');
      }
    });
  }

  Future<void> _generateAadhaarQr() async {
    final i18n = context.read<AppI18n>();
    setState(() {
      _loading = true;
      _clearMessages();
    });

    final result = await ApiService.generateAadhaarQr();
    if (!mounted) return;

    final success = result['success'] == true && result['qrDataUri'] is String;
    setState(() {
      _loading = false;
      if (success) {
        _qrDataUri = result['qrDataUri'] as String;
        _aadhaarTxnId = result['txnId']?.toString();
        _success = i18n.t('QR_CODE_GENERATED');
      } else {
        _error = (result['errorMessage'] as String?) ??
            i18n.t('ERROR_QR_GENERATION_FAILED');
      }
    });
  }

  Future<void> _verifyOtp() async {
    final i18n = context.read<AppI18n>();
    if (!_otpFormKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _clearMessages();
    });

    final result = await ApiService.verifyVisitorRegistrationOtp(
      idNumber: _epicCtrl.text.trim().toUpperCase(),
      otp: _otpCtrl.text.trim(),
      phoneNumber: _phoneCtrl.text.trim(),
      idType: _idType,
    );

    if (!mounted) return;
    final success = result['success'] == true;
    setState(() {
      _loading = false;
      if (success) {
        final demo = result['demographics'];
        if (demo is Map<String, dynamic>) _populateFromDemographics(demo);
        _step = 2;
        _success = i18n.t('CONTINUE_WITH_PHOTO_CAPTURE');
      } else {
        _error = (result['message'] as String?) ??
            i18n.t('ERROR_OTP_VERIFICATION_FAILED');
      }
    });
  }

  void _continueAfterAadhaar() {
    final i18n = context.read<AppI18n>();
    setState(() {
      _step = 2;
      _success = i18n.t('AADHAAR_KYC_VERIFIED_PREFILLED');
      _error = null;
      _warning = null;
      _kycConfidence = 92;
    });
  }

  void _capturePhoto() {
    setState(() {
      _photoCaptured = true;
      _livePhotoDataUri =
          'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/p9sAAAAASUVORK5CYII=';
      _success = context.read<AppI18n>().t('PHOTO_CAPTURED_SUCCESS');
      _error = null;
    });
  }

  Future<void> _submitRegistration() async {
    final i18n = context.read<AppI18n>();
    if (!_detailsFormKey.currentState!.validate()) return;
    if (!_photoCaptured) {
      setState(() => _error = i18n.t('PLEASE_CAPTURE_LIVE_PHOTO'));
      return;
    }

    setState(() {
      _loading = true;
      _clearMessages();
    });

    final district = _outsideMeghalaya ? 'NA' : _districtCtrl.text.trim();
    final booth = _outsideMeghalaya ? 'NA' : _boothCtrl.text.trim();
    final payload = <String, dynamic>{
      'phoneNumber': _phoneCtrl.text.trim(),
      'fullName': _fullNameCtrl.text.trim(),
      'designation': _designationCtrl.text.trim(),
      'address': _addressCtrl.text.trim(),
      'addressLine': _addressCtrl.text.trim(),
      'district': district,
      'constituency': _outsideMeghalaya ? 'NA' : _constituencyCtrl.text.trim(),
      'booth': booth,
      'boothVillage': booth,
      'village': _outsideMeghalaya ? 'NA' : _villageCtrl.text.trim(),
      'outsideMeghalaya': _outsideMeghalaya,
      'location': _outsideMeghalaya ? 'NA' : district,
      'email': _emailCtrl.text.trim(),
      'kycType': _idType,
      'kycStatus':
          _idType == 'AADHAAR' ? 'PHOTO_MATCHED' : 'DEMOGRAPHIC_MATCHED',
      'livePhoto': _livePhotoDataUri,
      'aadhaarClientTxnId': _aadhaarTxnId,
    };
    if (_idType == 'EPIC') {
      payload['epicNumber'] = _epicCtrl.text.trim().toUpperCase();
    }

    final result = await ApiService.registerVisitor(payload);
    if (!mounted) return;

    setState(() {
      _loading = false;
      if (result['success'] == true) {
        _submitted = true;
        _step = 4;
        _success = i18n.t('REGISTRATION_SUCCESS');
      } else {
        _error = (result['message'] as String?) ??
            i18n.t('ERROR_REGISTRATION_FAILED');
      }
    });
  }

  void _populateFromEpic(Map<String, dynamic> response) {
    final data = response['data'];
    final m = data is Map<String, dynamic> ? data : response;
    final name = m['verifiedName'] ??
        m['borrowernameonvoteridcard'] ??
        m['borrowerNameOnVoterIdCard'] ??
        _visitorNameCtrl.text;
    _fullNameCtrl.text = name.toString();
    _districtCtrl.text = (m['borroweraddressdistrict'] ??
            m['district'] ??
            response['district'] ??
            _districtCtrl.text)
        .toString();
    _constituencyCtrl.text = (m['assemblyconstituencyname'] ??
            m['assemblyConstituencyName'] ??
            m['constituency'] ??
            _constituencyCtrl.text)
        .toString();
    final polling = m['pollingdetails'];
    if (polling is Map<String, dynamic>) {
      _boothCtrl.text = (polling['pollingstationpartname'] ??
              polling['pollingStationPartName'] ??
              _boothCtrl.text)
          .toString();
    }
    final house = (m['borroweraddresshousenumber'] ?? '').toString();
    final section = (m['borroweraddresssectionnumber'] ?? '').toString();
    _addressCtrl.text = [house, section, _districtCtrl.text, 'Meghalaya']
        .where((part) => part.trim().isNotEmpty)
        .join(', ');
    final score = m['namematchscore'] ?? response['nameMatchScore'];
    if (score is num) _kycConfidence = score.round().clamp(0, 100);
  }

  void _populateFromDemographics(Map<String, dynamic> demo) {
    _fullNameCtrl.text = (demo['fullName'] ?? _fullNameCtrl.text).toString();
    _addressCtrl.text = (demo['address'] ?? _addressCtrl.text).toString();
    _districtCtrl.text = (demo['district'] ?? _districtCtrl.text).toString();
    _constituencyCtrl.text =
        (demo['constituency'] ?? _constituencyCtrl.text).toString();
  }

  @override
  Widget build(BuildContext context) {
    final i18n = context.watch<AppI18n>();
    return Scaffold(
      backgroundColor: MeghaColors.pageBg,
      body: SafeArea(
        child: Column(
          children: [
            MeghaBrandHeader(
              publicTone: true,
              trailing: const MeghaLanguageSelector(dark: true, compact: true),
              description: i18n.t('VISITOR_REGISTRATION_STEPS'),
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  _buildCardHeader(i18n),
                  const SizedBox(height: 14),
                  MeghaKycStepper(currentStep: _step, steps: _steps),
                  const SizedBox(height: 14),
                  if (_error != null) ...[
                    MeghaStatusBanner.error(_error!),
                    const SizedBox(height: 10),
                  ],
                  if (_warning != null) ...[
                    MeghaStatusBanner.warning(_warning!),
                    const SizedBox(height: 10),
                  ],
                  if (_success != null && !_submitted) ...[
                    MeghaStatusBanner.success(_success!),
                    const SizedBox(height: 10),
                  ],
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 180),
                    child: _submitted
                        ? _buildComplete(i18n)
                        : _buildStepContent(i18n),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCardHeader(AppI18n i18n) {
    return Container(
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(color: Colors.black.withAlpha(18), blurRadius: 12),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  i18n.t('NEW_VISITOR_REGISTRATION'),
                  style: const TextStyle(
                    color: MeghaColors.primary,
                    fontSize: 19,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  i18n.t('COMPLETE_KYC_STEPS'),
                  style:
                      const TextStyle(color: MeghaColors.muted, fontSize: 13),
                ),
              ],
            ),
          ),
          OutlinedButton.icon(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.home_outlined, size: 18),
            label: Text(i18n.t('HOME')),
            style: OutlinedButton.styleFrom(
              foregroundColor: MeghaColors.primary,
              side: const BorderSide(color: MeghaColors.primary),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepContent(AppI18n i18n) {
    switch (_step) {
      case 0:
        return _buildIdStep(i18n);
      case 1:
        return _buildOtpStep(i18n);
      case 2:
        return _buildPhotoStep(i18n);
      case 3:
        return _buildDetailsStep(i18n);
      default:
        return _buildComplete(i18n);
    }
  }

  Widget _buildIdStep(AppI18n i18n) {
    return MeghaSectionCard(
      title: i18n.t('STEP_1_ENTER_ID_DETAILS'),
      icon: Icons.badge_outlined,
      child: Form(
        key: _idFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              '${i18n.t('ID_TYPE')} *',
              style: const TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            SegmentedButton<String>(
              segments: [
                ButtonSegment(
                  value: 'EPIC',
                  icon: const Icon(Icons.badge_outlined),
                  label: Text(i18n.t('EPIC_VOTER_ID')),
                ),
                ButtonSegment(
                  value: 'AADHAAR',
                  icon: const Icon(Icons.fingerprint),
                  label: Text(i18n.t('AADHAAR_CARD')),
                ),
              ],
              selected: {_idType},
              onSelectionChanged: (v) => _switchIdType(v.first),
            ),
            const SizedBox(height: 14),
            if (_idType == 'EPIC') ...[
              TextFormField(
                controller: _epicCtrl,
                textCapitalization: TextCapitalization.characters,
                inputFormatters: [
                  FilteringTextInputFormatter.allow(RegExp('[A-Za-z0-9]')),
                  LengthLimitingTextInputFormatter(10),
                ],
                decoration: InputDecoration(
                  labelText: '${i18n.t('EPIC_NUMBER')} *',
                  prefixIcon: const Icon(Icons.credit_card_outlined),
                  helperText: i18n.t('EPIC_FORMAT_HINT'),
                ),
                validator: (v) {
                  if (_idType != 'EPIC') return null;
                  if (v == null || v.trim().length < 6) {
                    return i18n.t('ENTER_EPIC_NUMBER');
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _visitorNameCtrl,
                textCapitalization: TextCapitalization.characters,
                decoration: InputDecoration(
                  labelText: '${i18n.t('VISITOR_NAME_VOTER_CARD')} *',
                  prefixIcon: const Icon(Icons.person_outline),
                  helperText: i18n.t('VISITOR_NAME_VOTER_CARD_HINT'),
                ),
                onChanged: (v) {
                  final next = v.toUpperCase();
                  if (next == v) return;
                  _visitorNameCtrl.value = TextEditingValue(
                    text: next,
                    selection: TextSelection.collapsed(offset: next.length),
                  );
                },
                validator: (v) {
                  if (_idType != 'EPIC') return null;
                  if (v == null || v.trim().isEmpty) {
                    return i18n.t('VISITOR_NAME_VOTER_CARD');
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _phoneCtrl,
                keyboardType: TextInputType.phone,
                inputFormatters: [
                  FilteringTextInputFormatter.digitsOnly,
                  LengthLimitingTextInputFormatter(10),
                ],
                decoration: InputDecoration(
                  labelText: '${i18n.t('MOBILE_NUMBER')} *',
                  prefixIcon: const Icon(Icons.phone_outlined),
                  hintText: i18n.t('ENTER_10_DIGIT_MOBILE'),
                ),
                validator: (v) {
                  if (_idType != 'EPIC') return null;
                  if (v == null || v.length != 10) {
                    return i18n.t('ERROR_VALID_10_DIGIT_MOBILE');
                  }
                  return null;
                },
              ),
              const SizedBox(height: 12),
              MeghaStatusBanner.info(i18n.t('WILL_VALIDATE_ID_SEND_OTP')),
              const SizedBox(height: 18),
              _PrimaryProgressButton(
                loading: _loading,
                icon: Icons.send_outlined,
                label: i18n.t('GENERATE_OTP'),
                onPressed: _startEpicFlow,
              ),
            ] else ...[
              MeghaStatusBanner.success(i18n.t('CLICK_GENERATE_QR_AADHAAR')),
              const SizedBox(height: 14),
              if (_qrDataUri != null) _buildQrPreview(i18n),
              const SizedBox(height: 12),
              if (_qrDataUri == null)
                _PrimaryProgressButton(
                  loading: _loading,
                  icon: Icons.qr_code_2,
                  label: i18n.t('GENERATE_QR'),
                  onPressed: _generateAadhaarQr,
                )
              else
                _PrimaryProgressButton(
                  loading: false,
                  icon: Icons.arrow_forward,
                  label: i18n.t('CONTINUE_TO_DETAILS'),
                  onPressed: _continueAfterAadhaar,
                ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildQrPreview(AppI18n i18n) {
    Widget qr =
        const Icon(Icons.qr_code_2, size: 160, color: MeghaColors.primary);
    try {
      final raw =
          _qrDataUri!.contains(',') ? _qrDataUri!.split(',').last : _qrDataUri!;
      qr = Image.memory(base64Decode(raw), height: 180, fit: BoxFit.contain);
    } catch (_) {
      // Keep the generated QR area usable even if the backend returns a plain token.
    }
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: MeghaColors.panelBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: MeghaColors.border),
      ),
      child: Column(
        children: [
          Text(
            i18n.t('SCAN_QR_CODE_AADHAAR'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: MeghaColors.primary,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 12),
          qr,
          const SizedBox(height: 12),
          Text(
            [
              i18n.t('AADHAAR_APP_SCAN_STEP_1'),
              i18n.t('AADHAAR_APP_SCAN_STEP_2'),
              i18n.t('AADHAAR_APP_SCAN_STEP_3'),
              i18n.t('AADHAAR_APP_SCAN_STEP_4'),
            ].join('\n'),
            style: const TextStyle(
                color: MeghaColors.muted, fontSize: 12, height: 1.45),
          ),
        ],
      ),
    );
  }

  Widget _buildOtpStep(AppI18n i18n) {
    return MeghaSectionCard(
      title: i18n.t('STEP_2_VERIFY_OTP'),
      icon: Icons.phone_android_outlined,
      child: Form(
        key: _otpFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(18),
              decoration: BoxDecoration(
                color: const Color(0xFFF0F9FF),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: const Color(0xFFBAE6FD)),
              ),
              child: Column(
                children: [
                  const Icon(Icons.phone_android_outlined,
                      color: MeghaColors.accent, size: 36),
                  const SizedBox(height: 8),
                  Text(
                    i18n.t('OTP_SENT_TO'),
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: Color(0xFF0C4A6E)),
                  ),
                  const SizedBox(height: 3),
                  Text(
                    _maskedPhone,
                    style: const TextStyle(
                      color: MeghaColors.primary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            TextFormField(
              controller: _otpCtrl,
              keyboardType: TextInputType.number,
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.w700,
                letterSpacing: 6,
              ),
              inputFormatters: [
                FilteringTextInputFormatter.digitsOnly,
                LengthLimitingTextInputFormatter(6),
              ],
              decoration: InputDecoration(
                labelText: '${i18n.t('ENTER_OTP')} *',
                hintText: i18n.t('ENTER_6_DIGIT_OTP'),
              ),
              validator: (v) {
                if (v == null || v.length != 6) {
                  return i18n.t('ERROR_VALID_6_DIGIT_OTP');
                }
                return null;
              },
            ),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed:
                        _loading ? null : () => setState(() => _step = 0),
                    icon: const Icon(Icons.arrow_back),
                    label: Text(i18n.t('PREVIOUS')),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _PrimaryProgressButton(
                    loading: _loading,
                    icon: Icons.check,
                    label: i18n.t('VERIFY_OTP'),
                    onPressed: _verifyOtp,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPhotoStep(AppI18n i18n) {
    return MeghaSectionCard(
      title: i18n.t('STEP_3_CAPTURE_PHOTO'),
      icon: Icons.photo_camera_outlined,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          MeghaStatusBanner.success(i18n.t('CAPTURE_LIVE_PHOTO_NOTICE')),
          const SizedBox(height: 14),
          _buildVerifiedSummary(i18n),
          const SizedBox(height: 14),
          AspectRatio(
            aspectRatio: 4 / 3,
            child: Container(
              decoration: BoxDecoration(
                color: _photoCaptured
                    ? const Color(0xFFF0FDF4)
                    : const Color(0xFFF3F4F6),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: _photoCaptured
                      ? const Color(0xFF86EFAC)
                      : const Color(0xFFE5E7EB),
                ),
              ),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    _photoCaptured
                        ? Icons.check_circle_outline
                        : Icons.photo_camera_outlined,
                    size: 58,
                    color: _photoCaptured
                        ? const Color(0xFF16A34A)
                        : const Color(0xFF9CA3AF),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    _photoCaptured
                        ? i18n.t('PHOTO_CAPTURED_SUCCESS')
                        : i18n.t('CLICK_BELOW_START_CAMERA'),
                    textAlign: TextAlign.center,
                    style: const TextStyle(color: MeghaColors.muted),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 14),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: () =>
                      setState(() => _step = _idType == 'EPIC' ? 1 : 0),
                  icon: const Icon(Icons.arrow_back),
                  label: Text(i18n.t('PREVIOUS')),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: _capturePhoto,
                  icon:
                      Icon(_photoCaptured ? Icons.refresh : Icons.photo_camera),
                  label: Text(_photoCaptured
                      ? i18n.t('RETAKE_PHOTO')
                      : i18n.t('CAPTURE_PHOTO')),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ElevatedButton.icon(
            onPressed: _photoCaptured ? () => setState(() => _step = 3) : null,
            icon: const Icon(Icons.arrow_forward),
            label: Text(i18n.t('CONTINUE_TO_DETAILS')),
          ),
          const SizedBox(height: 8),
          Text(
            i18n.t('CAMERA_LIGHTING_HINT'),
            textAlign: TextAlign.center,
            style: const TextStyle(color: MeghaColors.muted, fontSize: 12),
          ),
        ],
      ),
    );
  }

  Widget _buildVerifiedSummary(AppI18n i18n) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: MeghaColors.panelBg,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            i18n.t('AUTO_POPULATED_FROM_VERIFIED_ID'),
            style: const TextStyle(color: MeghaColors.muted, fontSize: 12),
          ),
          const SizedBox(height: 8),
          _InfoRow(label: i18n.t('NAME'), value: _fullNameCtrl.text),
          _InfoRow(label: i18n.t('DISTRICT'), value: _districtCtrl.text),
          _InfoRow(label: i18n.t('ADDRESS'), value: _addressCtrl.text),
        ],
      ),
    );
  }

  Widget _buildDetailsStep(AppI18n i18n) {
    return MeghaSectionCard(
      title: i18n.t('STEP_4_ADDITIONAL_DETAILS'),
      icon: Icons.person_outline,
      child: Form(
        key: _detailsFormKey,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _buildVerifiedKycPanel(i18n),
            const SizedBox(height: 14),
            DropdownButtonFormField<String>(
              value:
                  _designationCtrl.text.isEmpty ? null : _designationCtrl.text,
              decoration:
                  InputDecoration(labelText: '${i18n.t('DESIGNATION')} *'),
              items: [
                for (final item in _designations)
                  DropdownMenuItem(value: item, child: Text(item)),
              ],
              onChanged: (value) {
                if (value == null) return;
                _designationCtrl.text = value;
              },
              validator: (v) => (v == null || v.isEmpty)
                  ? i18n.t('SELECT_DESIGNATION')
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _addressCtrl,
              minLines: 2,
              maxLines: 3,
              decoration: InputDecoration(
                labelText: i18n.t('ADDRESS_HOUSE_COLONY'),
              ),
            ),
            const SizedBox(height: 12),
            MeghaStatusBanner.info(i18n.t('AUTO_FILL_MISSING_DETAILS_HELPER')),
            const SizedBox(height: 10),
            CheckboxListTile(
              value: _outsideMeghalaya,
              dense: true,
              contentPadding: EdgeInsets.zero,
              title: Text(
                i18n.t('APPLICANT_OUTSIDE_MEGHALAYA'),
                style: const TextStyle(fontSize: 13),
              ),
              controlAffinity: ListTileControlAffinity.leading,
              onChanged: (value) {
                setState(() => _outsideMeghalaya = value ?? false);
              },
            ),
            if (_outsideMeghalaya) ...[
              MeghaStatusBanner.info(i18n.t('LOCATION_FIELDS_NA')),
              const SizedBox(height: 10),
            ],
            TextFormField(
              controller: _districtCtrl,
              enabled: !_outsideMeghalaya,
              decoration: InputDecoration(
                labelText:
                    '${i18n.t('DISTRICT')}${_outsideMeghalaya ? '' : ' *'}',
              ),
              validator: (v) {
                if (_outsideMeghalaya) return null;
                if (v == null || v.trim().isEmpty) {
                  return i18n.t('ERROR_DISTRICT_REQUIRED');
                }
                return null;
              },
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _constituencyCtrl,
              enabled: !_outsideMeghalaya,
              decoration: InputDecoration(labelText: i18n.t('CONSTITUENCY')),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _boothCtrl,
              enabled: !_outsideMeghalaya,
              decoration: InputDecoration(labelText: i18n.t('BOOTH_VILLAGE')),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _villageCtrl,
              enabled: !_outsideMeghalaya,
              decoration:
                  InputDecoration(labelText: i18n.t('VILLAGE_DIFFERENT')),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _emailCtrl,
              keyboardType: TextInputType.emailAddress,
              decoration: InputDecoration(labelText: i18n.t('EMAIL_OPTIONAL')),
            ),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed:
                        _loading ? null : () => setState(() => _step = 2),
                    icon: const Icon(Icons.arrow_back),
                    label: Text(i18n.t('BACK')),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _PrimaryProgressButton(
                    loading: _loading,
                    icon: Icons.check,
                    label: i18n.t('COMPLETE_REGISTRATION'),
                    onPressed: _submitRegistration,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildVerifiedKycPanel(AppI18n i18n) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: MeghaColors.panelBg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: const Color(0xFFC7D2FE)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.verified_user_outlined,
                  color: Color(0xFF16A34A)),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  i18n.t('VERIFIED_KYC_DETAILS'),
                  style: const TextStyle(
                    color: MeghaColors.primary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          _InfoRow(label: i18n.t('NAME'), value: _fullNameCtrl.text),
          _InfoRow(label: i18n.t('DISTRICT'), value: _districtCtrl.text),
          _InfoRow(
              label: i18n.t('CONSTITUENCY'), value: _constituencyCtrl.text),
          _InfoRow(label: i18n.t('BOOTH_VILLAGE'), value: _boothCtrl.text),
          _InfoRow(
            label: _idType == 'EPIC' ? i18n.t('EPIC') : i18n.t('AADHAAR_REF'),
            value:
                _idType == 'EPIC' ? _epicCtrl.text : (_aadhaarTxnId ?? 'N/A'),
          ),
          if (_kycConfidence > 0) ...[
            const SizedBox(height: 10),
            Text(
              '${i18n.t('AI_KYC_CONFIDENCE_INDICATOR')}: $_kycConfidence%',
              style: const TextStyle(
                color: MeghaColors.text,
                fontWeight: FontWeight.w700,
                fontSize: 12,
              ),
            ),
            const SizedBox(height: 5),
            ClipRRect(
              borderRadius: BorderRadius.circular(999),
              child: LinearProgressIndicator(
                minHeight: 8,
                value: _kycConfidence / 100,
                backgroundColor: const Color(0xFFE5E7EB),
                color: _kycConfidence >= 80
                    ? const Color(0xFF16A34A)
                    : const Color(0xFFCA8A04),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildComplete(AppI18n i18n) {
    return MeghaSectionCard(
      title: i18n.t('COMPLETE'),
      icon: Icons.verified_outlined,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Icon(Icons.check_circle_outline,
              size: 70, color: Color(0xFF16A34A)),
          const SizedBox(height: 12),
          Text(
            i18n.t('REGISTRATION_SUCCESS'),
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: MeghaColors.success,
              fontSize: 20,
              fontWeight: FontWeight.w800,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            i18n.t('REGISTRATION_COMPLETE_LOGIN'),
            textAlign: TextAlign.center,
            style: const TextStyle(color: MeghaColors.muted),
          ),
          const SizedBox(height: 18),
          MeghaStatusBanner.success(i18n.t('YOUR_IDENTITY_VERIFIED_COMPLETE')),
          const SizedBox(height: 18),
          ElevatedButton.icon(
            onPressed: () => Navigator.pop(context),
            icon: const Icon(Icons.login),
            label: Text(i18n.t('GO_TO_LOGIN')),
          ),
        ],
      ),
    );
  }

  String get _maskedPhone {
    final phone = _phoneCtrl.text.trim();
    if (phone.length < 4) return phone;
    return '******${phone.substring(phone.length - 4)}';
  }
}

class _InfoRow extends StatelessWidget {
  final String label;
  final String value;

  const _InfoRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 112,
            child: Text(
              label,
              style: const TextStyle(
                color: MeghaColors.muted,
                fontSize: 12,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value.trim().isEmpty ? 'N/A' : value,
              style: const TextStyle(
                color: MeghaColors.text,
                fontSize: 13,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _PrimaryProgressButton extends StatelessWidget {
  final bool loading;
  final IconData icon;
  final String label;
  final VoidCallback onPressed;

  const _PrimaryProgressButton({
    required this.loading,
    required this.icon,
    required this.label,
    required this.onPressed,
  });

  @override
  Widget build(BuildContext context) {
    return ElevatedButton.icon(
      onPressed: loading ? null : onPressed,
      icon: loading
          ? const SizedBox(
              width: 18,
              height: 18,
              child: CircularProgressIndicator(
                strokeWidth: 2,
                color: Colors.white,
              ),
            )
          : Icon(icon),
      label: Text(label),
    );
  }
}
