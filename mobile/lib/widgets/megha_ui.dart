import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/i18n/app_i18n.dart';

class MeghaColors {
  static const primary = Color(0xFF1A237E);
  static const primary2 = Color(0xFF3949AB);
  static const accent = Color(0xFF1565C0);
  static const publicGreen = Color(0xFF00695C);
  static const publicGreen2 = Color(0xFF00897B);
  static const pageBg = Color(0xFFF0F2F5);
  static const panelBg = Color(0xFFF8FAFC);
  static const text = Color(0xFF1F2937);
  static const muted = Color(0xFF6B7280);
  static const border = Color(0xFFE5E7EB);
  static const success = Color(0xFF166534);
  static const warning = Color(0xFF92400E);
  static const danger = Color(0xFFB91C1C);
}

class MeghaLanguageSelector extends StatelessWidget {
  final bool dark;
  final bool compact;

  const MeghaLanguageSelector({
    super.key,
    this.dark = false,
    this.compact = false,
  });

  @override
  Widget build(BuildContext context) {
    final i18n = context.watch<AppI18n>();
    final foreground = dark ? Colors.white : MeghaColors.text;
    final border = dark ? Colors.white.withAlpha(89) : const Color(0xFFD1D5DB);
    final bg = dark ? Colors.white.withAlpha(26) : Colors.white;

    return Container(
      height: compact ? 34 : 36,
      padding: EdgeInsets.only(left: compact ? 8 : 10, right: compact ? 4 : 6),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: border),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<String>(
          value: i18n.lang,
          isDense: true,
          icon: Icon(Icons.keyboard_arrow_down, size: 18, color: foreground),
          dropdownColor: Colors.white,
          selectedItemBuilder: (ctx) => [
            for (final e in AppI18n.supported.entries)
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.language, size: 16, color: foreground),
                  const SizedBox(width: 6),
                  Text(
                    compact ? e.value.split(' ').first : e.value,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: foreground,
                      fontSize: compact ? 12 : 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ],
              ),
          ],
          items: [
            for (final e in AppI18n.supported.entries)
              DropdownMenuItem<String>(
                value: e.key,
                child: Text(
                  e.value,
                  style: const TextStyle(color: MeghaColors.text, fontSize: 13),
                ),
              ),
          ],
          onChanged: (value) {
            if (value == null) return;
            context.read<AppI18n>().setLang(value);
          },
        ),
      ),
    );
  }
}

class MeghaStatusBanner extends StatelessWidget {
  final String message;
  final IconData icon;
  final Color background;
  final Color border;
  final Color foreground;

  const MeghaStatusBanner({
    super.key,
    required this.message,
    required this.icon,
    required this.background,
    required this.border,
    required this.foreground,
  });

  factory MeghaStatusBanner.error(String message) => MeghaStatusBanner(
        message: message,
        icon: Icons.error_outline,
        background: const Color(0xFFFEF2F2),
        border: const Color(0xFFFECACA),
        foreground: MeghaColors.danger,
      );

  factory MeghaStatusBanner.warning(String message) => MeghaStatusBanner(
        message: message,
        icon: Icons.warning_amber_outlined,
        background: const Color(0xFFFFFBEB),
        border: const Color(0xFFFDE68A),
        foreground: MeghaColors.warning,
      );

  factory MeghaStatusBanner.success(String message) => MeghaStatusBanner(
        message: message,
        icon: Icons.check_circle_outline,
        background: const Color(0xFFF0FDF4),
        border: const Color(0xFFBBF7D0),
        foreground: MeghaColors.success,
      );

  factory MeghaStatusBanner.info(String message) => MeghaStatusBanner(
        message: message,
        icon: Icons.info_outline,
        background: const Color(0xFFEFF6FF),
        border: const Color(0xFFBFDBFE),
        foreground: const Color(0xFF1E40AF),
      );

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: border),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: foreground, size: 18),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              message,
              style: TextStyle(color: foreground, fontSize: 13, height: 1.35),
            ),
          ),
        ],
      ),
    );
  }
}

class MeghaSectionCard extends StatelessWidget {
  final String title;
  final IconData icon;
  final Widget child;
  final Widget? trailing;

  const MeghaSectionCard({
    super.key,
    required this.title,
    required this.icon,
    required this.child,
    this.trailing,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(10),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withAlpha(20),
            blurRadius: 8,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      clipBehavior: Clip.antiAlias,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: const BoxDecoration(
              gradient: LinearGradient(
                colors: [MeghaColors.primary, MeghaColors.primary2],
              ),
            ),
            child: Row(
              children: [
                Icon(icon, color: Colors.white, size: 18),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    title,
                    style: const TextStyle(
                      color: Colors.white,
                      fontWeight: FontWeight.w700,
                      fontSize: 14,
                    ),
                  ),
                ),
                if (trailing != null) trailing!,
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(16),
            child: child,
          ),
        ],
      ),
    );
  }
}

class MeghaKycStepper extends StatelessWidget {
  final int currentStep;
  final List<MeghaStepData> steps;

  const MeghaKycStepper({
    super.key,
    required this.currentStep,
    required this.steps,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 12),
      decoration: BoxDecoration(
        color: MeghaColors.panelBg,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          for (var i = 0; i < steps.length; i++) ...[
            Expanded(
              child: _StepDot(
                data: steps[i],
                active: i == currentStep,
                completed: i < currentStep,
              ),
            ),
            if (i != steps.length - 1)
              SizedBox(
                width: 12,
                child: Container(
                  height: 2,
                  margin: const EdgeInsets.only(top: 18),
                  color: i < currentStep
                      ? const Color(0xFF16A34A)
                      : const Color(0xFFE5E7EB),
                ),
              ),
          ],
        ],
      ),
    );
  }
}

class MeghaStepData {
  final String label;
  final IconData icon;

  const MeghaStepData(this.label, this.icon);
}

class _StepDot extends StatelessWidget {
  final MeghaStepData data;
  final bool active;
  final bool completed;

  const _StepDot({
    required this.data,
    required this.active,
    required this.completed,
  });

  @override
  Widget build(BuildContext context) {
    final color = completed
        ? const Color(0xFF16A34A)
        : active
            ? MeghaColors.primary
            : const Color(0xFFE5E7EB);
    final fg = active || completed ? Colors.white : const Color(0xFF9CA3AF);
    final labelColor = completed
        ? const Color(0xFF16A34A)
        : active
            ? MeghaColors.primary
            : MeghaColors.muted;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: color,
            shape: BoxShape.circle,
            boxShadow: active
                ? [
                    BoxShadow(
                      color: MeghaColors.primary.withAlpha(31),
                      blurRadius: 0,
                      spreadRadius: 4,
                    ),
                  ]
                : null,
          ),
          child: Icon(completed ? Icons.check : data.icon, color: fg, size: 17),
        ),
        const SizedBox(height: 6),
        Text(
          data.label,
          textAlign: TextAlign.center,
          maxLines: 2,
          overflow: TextOverflow.ellipsis,
          style: TextStyle(
            color: labelColor,
            fontSize: 10.5,
            fontWeight: active || completed ? FontWeight.w700 : FontWeight.w500,
            height: 1.15,
          ),
        ),
      ],
    );
  }
}

class MeghaBrandHeader extends StatelessWidget {
  final bool publicTone;
  final Widget? trailing;
  final String? description;

  const MeghaBrandHeader({
    super.key,
    this.publicTone = false,
    this.trailing,
    this.description,
  });

  @override
  Widget build(BuildContext context) {
    final i18n = context.watch<AppI18n>();
    final colors = publicTone
        ? const [Color(0xFF004D40), Color(0xFF00695C), Color(0xFF00897B)]
        : const [MeghaColors.primary, Color(0xFF0D47A1), MeghaColors.accent];

    return Container(
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: colors,
        ),
      ),
      child: Stack(
        children: [
          Positioned.fill(
            child: Opacity(
              opacity: 0.06,
              child: Image.asset('assets/state_map.png', fit: BoxFit.cover),
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 18, 18, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Image.asset(
                      'assets/logo-small.png',
                      width: 54,
                      height: 54,
                      fit: BoxFit.contain,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'MeghaConnect',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 22,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 9,
                              vertical: 4,
                            ),
                            decoration: BoxDecoration(
                              color: const Color(0xFF22C55E).withAlpha(46),
                              borderRadius: BorderRadius.circular(999),
                              border: Border.all(
                                color: const Color(0xFF22C55E).withAlpha(77),
                              ),
                            ),
                            child: Row(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                const Icon(
                                  Icons.verified_user_outlined,
                                  color: Color(0xFF86EFAC),
                                  size: 14,
                                ),
                                const SizedBox(width: 5),
                                Flexible(
                                  child: Text(
                                    i18n.t('OFFICIAL_GOVERNMENT_PORTAL'),
                                    overflow: TextOverflow.ellipsis,
                                    style: const TextStyle(
                                      color: Color(0xFFDCFCE7),
                                      fontSize: 11,
                                      fontWeight: FontWeight.w700,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                    if (trailing != null) trailing!,
                  ],
                ),
                if (description != null) ...[
                  const SizedBox(height: 14),
                  Text(
                    description!,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 13,
                      height: 1.45,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}
