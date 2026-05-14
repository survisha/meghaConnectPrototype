import 'package:flutter/material.dart';

class StatusChip extends StatelessWidget {
  const StatusChip({
    required this.label,
    super.key,
  });

  final String label;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final color = _colorFor(label, scheme);

    return DecoratedBox(
      decoration: BoxDecoration(
        color: color.withOpacity(0.12),
        border: Border.all(color: color.withOpacity(0.35)),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        child: Text(
          label.replaceAll('_', ' '),
          style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: color,
                fontWeight: FontWeight.w700,
              ),
        ),
      ),
    );
  }

  Color _colorFor(String value, ColorScheme scheme) {
    final normalized = value.toUpperCase();
    if (normalized.contains('VALID') ||
        normalized.contains('CHECKED_IN') ||
        normalized.contains('SUCCESS')) {
      return Colors.teal.shade700;
    }
    if (normalized.contains('OUT')) {
      return Colors.indigo.shade700;
    }
    if (normalized.contains('EXPIRED') ||
        normalized.contains('CANCEL') ||
        normalized.contains('INVALID')) {
      return scheme.error;
    }
    return Colors.amber.shade800;
  }
}
