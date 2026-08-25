import 'package:flutter/material.dart';

class AppFooter extends StatelessWidget {
  const AppFooter({super.key});

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      top: false,
      child: Container(
        width: double.infinity,
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
        decoration: const BoxDecoration(
          color: Color(0xFFF8FAFC),
          border: Border(top: BorderSide(color: Color(0xFFDBE3EF))),
        ),
        child: const Text(
          'Design & Development by NITCON LIMITED',
          textAlign: TextAlign.center,
          softWrap: true,
          style: TextStyle(
            color: Color(0xFF475569),
            fontSize: 12,
            height: 1.35,
            fontWeight: FontWeight.w600,
          ),
        ),
      ),
    );
  }
}
