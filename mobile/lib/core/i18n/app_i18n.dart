import 'dart:convert';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart' show rootBundle;
import 'package:shared_preferences/shared_preferences.dart';

import '../config/app_config.dart';

/// Lightweight runtime i18n using JSON assets (similar to the Angular setup).
///
/// - Loads language files from `assets/i18n/{lang}.json`
/// - Supports runtime switching
/// - Persists selection in SharedPreferences (key: AppConfig.languageStorageKey)
/// - Falls back to English for missing keys
class AppI18n extends ChangeNotifier {
  static const supported = <String, String>{
    'en': 'English',
    'kh': 'Khasi',
    'gr': 'Garo',
    'hi': 'Hindi',
  };

  String _lang = 'en';
  Map<String, dynamic> _strings = const {};
  Map<String, dynamic> _fallbackEn = const {};

  String get lang => _lang;

  Future<void> init() async {
    final prefs = await SharedPreferences.getInstance();
    final stored = prefs.getString(AppConfig.languageStorageKey);
    final initial =
        (stored != null && supported.containsKey(stored)) ? stored : 'en';

    _fallbackEn = await _loadLang('en');
    _lang = initial;
    _strings = _lang == 'en' ? _fallbackEn : await _loadLang(_lang);
  }

  Future<void> setLang(String code) async {
    if (!supported.containsKey(code)) return;
    if (code == _lang) return;

    final nextStrings = code == 'en' ? _fallbackEn : await _loadLang(code);
    _lang = code;
    _strings = nextStrings;

    final prefs = await SharedPreferences.getInstance();
    await prefs.setString(AppConfig.languageStorageKey, code);
    notifyListeners();
  }

  String t(String key) {
    final v = _strings[key];
    if (v is String && v.isNotEmpty) return v;
    final fb = _fallbackEn[key];
    if (fb is String && fb.isNotEmpty) return fb;
    return key; // last resort
  }

  Future<Map<String, dynamic>> _loadLang(String code) async {
    try {
      final raw = await rootBundle.loadString('assets/i18n/$code.json');
      final json = jsonDecode(raw);
      if (json is Map<String, dynamic>) return json;
    } catch (e) {
      if (kDebugMode) {
        // ignore: avoid_print
        print('i18n load failed for $code: $e');
      }
    }
    return <String, dynamic>{};
  }
}
