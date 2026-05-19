import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../services/auth_service.dart';
import 'appointments_screen.dart';
import 'visitor_registration_screen.dart';

class DeoHomeScreen extends StatelessWidget {
  const DeoHomeScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final user = auth.user;

    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: AppBar(
        title: const Text('DEO Counter'),
        actions: [
          IconButton(
            tooltip: 'Logout',
            icon: const Icon(Icons.logout),
            onPressed: () => context.read<AuthService>().logout(),
          ),
        ],
      ),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(18),
          children: [
            Text(
              'Welcome${user?.fullName.isNotEmpty == true ? ', ${user!.fullName}' : ''}',
              style: const TextStyle(
                color: Color(0xFF111827),
                fontSize: 22,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 6),
            const Text(
              'Fast entry flow for visitor registration and walk-in appointments.',
              style: TextStyle(color: Color(0xFF64748B), fontSize: 14),
            ),
            const SizedBox(height: 22),
            _ActionCard(
              icon: Icons.person_add_alt_1_outlined,
              title: 'Register Visitor',
              subtitle: 'Capture details, KYC/photo, then create appointment.',
              color: const Color(0xFF1A237E),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(
                    builder: (_) => const VisitorRegistrationScreen(
                      openAppointmentAfterSubmit: true,
                    ),
                  ),
                );
              },
            ),
            const SizedBox(height: 12),
            _ActionCard(
              icon: Icons.list_alt_outlined,
              title: 'Appointment List',
              subtitle: 'View DEO-created and assigned appointments.',
              color: const Color(0xFF1565C0),
              onTap: () {
                Navigator.of(context).push(
                  MaterialPageRoute(builder: (_) => const AppointmentsScreen()),
                );
              },
            ),
            const SizedBox(height: 12),
            _ActionCard(
              icon: Icons.logout,
              title: 'Logout',
              subtitle: 'Clear session and return to login.',
              color: const Color(0xFF991B1B),
              onTap: () => context.read<AuthService>().logout(),
            ),
          ],
        ),
      ),
    );
  }
}

class _ActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final Color color;
  final VoidCallback onTap;

  const _ActionCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.color,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      child: InkWell(
        borderRadius: BorderRadius.circular(10),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: color.withAlpha(24),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(icon, color: color),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        color: Color(0xFF111827),
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      subtitle,
                      style: const TextStyle(
                        color: Color(0xFF64748B),
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: Color(0xFF94A3B8)),
            ],
          ),
        ),
      ),
    );
  }
}
