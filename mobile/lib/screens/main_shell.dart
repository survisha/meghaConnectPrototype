import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/navigation_service.dart';
import 'dashboard_screen.dart';
import 'appointments_screen.dart';
import 'new_appointment_screen.dart';
import 'user_management_screen.dart';

class _NavItem {
  final String label;
  final IconData icon;
  final String? route;
  final List<UserRole> roles;
  final List<_NavItem>? children;

  const _NavItem({
    required this.label,
    required this.icon,
    this.route,
    required this.roles,
    this.children,
  });
}

const _allRoles = [
  UserRole.HCM,
  UserRole.ADMIN,
  UserRole.SAIDUL_OSD,
  UserRole.APPROVER_JT_SECY,
  UserRole.CMO_OFFICER,
  UserRole.DATA_ENTRY_OPERATOR,
];

const _fullControl = [UserRole.HCM, UserRole.ADMIN, UserRole.SAIDUL_OSD];

final _navTree = <_NavItem>[
  _NavItem(
    label: 'Dashboard',
    icon: Icons.dashboard_outlined,
    route: 'dashboard',
    roles: _allRoles,
  ),
  _NavItem(
    label: 'Calendar / Schedule',
    icon: Icons.calendar_month_outlined,
    route: 'calendar',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.APPROVER_JT_SECY,
      UserRole.CMO_OFFICER,
    ],
  ),
  _NavItem(
    label: 'Appointments',
    icon: Icons.people_outline,
    route: 'appointments',
    roles: _allRoles,
    children: [
      _NavItem(
        label: 'All Appointments',
        icon: Icons.list_alt_outlined,
        route: 'appointments',
        roles: _allRoles,
      ),
      _NavItem(
        label: 'New Appointment',
        icon: Icons.add_circle_outline,
        route: 'new_appointment',
        roles: [UserRole.ADMIN, UserRole.SAIDUL_OSD, UserRole.DATA_ENTRY_OPERATOR],
      ),
      _NavItem(
        label: 'Walk-in Counter',
        icon: Icons.login_outlined,
        route: 'walkin',
        roles: [UserRole.ADMIN, UserRole.SAIDUL_OSD, UserRole.DATA_ENTRY_OPERATOR],
      ),
    ],
  ),
  _NavItem(
    label: 'CM Schemes',
    icon: Icons.workspace_premium_outlined,
    route: 'schemes',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.APPROVER_JT_SECY,
      UserRole.CMO_OFFICER,
    ],
  ),
  _NavItem(
    label: 'Public Identification',
    icon: Icons.badge_outlined,
    route: 'identify',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.DATA_ENTRY_OPERATOR,
    ],
  ),
  _NavItem(
    label: 'Reports',
    icon: Icons.bar_chart_outlined,
    route: 'reports',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.SAIDUL_OSD,
      UserRole.APPROVER_JT_SECY,
      UserRole.CMO_OFFICER,
    ],
    children: [
      _NavItem(
        label: 'Analytics',
        icon: Icons.pie_chart_outline,
        route: 'reports',
        roles: [
          UserRole.HCM,
          UserRole.ADMIN,
          UserRole.SAIDUL_OSD,
          UserRole.APPROVER_JT_SECY,
          UserRole.CMO_OFFICER,
        ],
      ),
      _NavItem(
        label: 'Pending Follow-ups',
        icon: Icons.access_time_outlined,
        route: 'followups',
        roles: [
          UserRole.HCM,
          UserRole.ADMIN,
          UserRole.SAIDUL_OSD,
          UserRole.APPROVER_JT_SECY,
          UserRole.CMO_OFFICER,
        ],
      ),
      _NavItem(
        label: 'Audit Trail',
        icon: Icons.history,
        route: 'audit',
        roles: [UserRole.ADMIN],
      ),
    ],
  ),
  _NavItem(
    label: 'User Management',
    icon: Icons.manage_accounts_outlined,
    route: 'users',
    roles: _fullControl,
  ),
];

class MainShell extends StatefulWidget {
  const MainShell({super.key});

  @override
  State<MainShell> createState() => _MainShellState();
}

class _MainShellState extends State<MainShell> {
  String _currentRoute = 'dashboard';
  final Set<String> _expandedItems = {};

  static const _primaryBlue = Color(0xFF1A237E);

  Widget _buildBody(String route) {
    switch (route) {
      case 'dashboard':
        return const DashboardScreen();
      case 'appointments':
        return const AppointmentsScreen();
      case 'new_appointment':
      case 'walkin':
        return NewAppointmentScreen(isWalkIn: route == 'walkin');
      case 'users':
        return const UserManagementScreen();
      default:
        return _PlaceholderScreen(route: route);
    }
  }

  String _pageTitle(String route) {
    switch (route) {
      case 'dashboard':
        return 'Dashboard';
      case 'appointments':
        return 'Appointments';
      case 'new_appointment':
        return 'New Appointment';
      case 'walkin':
        return 'Walk-in Counter';
      case 'calendar':
        return 'Calendar / Schedule';
      case 'schemes':
        return 'CM Schemes';
      case 'identify':
        return 'Public Identification';
      case 'reports':
        return 'Reports & Analytics';
      case 'followups':
        return 'Pending Follow-ups';
      case 'audit':
        return 'Audit Trail';
      case 'users':
        return 'User Management';
      default:
        return 'MeghaConnect';
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final nav = context.watch<NavigationService>();
    final user = auth.user!;
    _currentRoute = nav.currentRoute;

    // Public users go directly to new appointment
    if (user.role == UserRole.PUBLIC) {
      return Scaffold(
        appBar: AppBar(
          title: const Text('🏛️ MeghaConnect'),
          actions: [
            IconButton(
              icon: const Icon(Icons.logout),
              onPressed: () => _confirmLogout(context, auth),
            ),
          ],
        ),
        body: NewAppointmentScreen(isWalkIn: false, isPublic: true),
      );
    }

    return Scaffold(
      appBar: AppBar(
        title: Text('🏛️ ${_pageTitle(_currentRoute)}'),
        actions: [
          _RoleBadge(role: user.role),
          const SizedBox(width: 8),
        ],
      ),
      drawer: _buildDrawer(context, auth, user),
      body: _buildBody(_currentRoute),
    );
  }

  Widget _buildDrawer(BuildContext context, AuthService auth, AuthUser user) {
    final visibleItems = _navTree.where((item) => item.roles.contains(user.role)).map((item) {
      if (item.children != null) {
        final visibleChildren = item.children!.where((c) => c.roles.contains(user.role)).toList();
        return _NavItem(
          label: item.label,
          icon: item.icon,
          route: item.route,
          roles: item.roles,
          children: visibleChildren.isEmpty ? null : visibleChildren,
        );
      }
      return item;
    }).toList();

    return Drawer(
      child: Column(
        children: [
          _buildDrawerHeader(user),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(vertical: 8),
              children: [
                for (final item in visibleItems)
                  if (item.children != null)
                    _buildExpandableItem(context, item)
                  else
                    _buildNavTile(context, item),
                const Divider(height: 1),
                ListTile(
                  leading: const Icon(Icons.logout, color: Color(0xFF991B1B)),
                  title: const Text(
                    'Logout',
                    style: TextStyle(color: Color(0xFF991B1B)),
                  ),
                  onTap: () {
                    Navigator.pop(context);
                    _confirmLogout(context, auth);
                  },
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDrawerHeader(AuthUser user) {
    return Container(
      padding: EdgeInsets.only(
        top: MediaQuery.of(context).padding.top + 16,
        left: 20,
        right: 20,
        bottom: 20,
      ),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFF1A237E), Color(0xFF1565C0)],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white.withAlpha(51),
              shape: BoxShape.circle,
            ),
            child: const Text('🏛️', style: TextStyle(fontSize: 28)),
          ),
          const SizedBox(height: 12),
          const Text(
            'MeghaConnect',
            style: TextStyle(
              color: Colors.white,
              fontSize: 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            user.fullName,
            style: TextStyle(
              color: Colors.white.withAlpha(204),
              fontSize: 13,
            ),
          ),
          const SizedBox(height: 6),
          _RoleBadge(role: user.role),
        ],
      ),
    );
  }

  Widget _buildNavTile(BuildContext context, _NavItem item, {bool isChild = false}) {
    final isActive = _currentRoute == item.route;
    return ListTile(
      dense: isChild,
      contentPadding: EdgeInsets.only(
        left: isChild ? 40 : 16,
        right: 16,
      ),
      leading: Icon(
        item.icon,
        color: isActive ? _primaryBlue : Colors.grey[600],
        size: isChild ? 20 : 24,
      ),
      title: Text(
        item.label,
        style: TextStyle(
          color: isActive ? _primaryBlue : Colors.grey[800],
          fontWeight: isActive ? FontWeight.w600 : FontWeight.normal,
          fontSize: isChild ? 14 : 15,
        ),
      ),
      tileColor: isActive ? _primaryBlue.withAlpha(20) : null,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      onTap: () {
              Navigator.pop(context);
              context.read<NavigationService>().navigateTo(item.route!);
            },
    );
  }

  Widget _buildExpandableItem(BuildContext context, _NavItem item) {
    final key = item.label;
    final isExpanded = _expandedItems.contains(key);
    final hasActiveChild = item.children?.any((c) => c.route == _currentRoute) ?? false;

    return Column(
      children: [
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 16),
          leading: Icon(
            item.icon,
            color: hasActiveChild ? _primaryBlue : Colors.grey[600],
          ),
          title: Text(
            item.label,
            style: TextStyle(
              color: hasActiveChild ? _primaryBlue : Colors.grey[800],
              fontWeight: hasActiveChild ? FontWeight.w600 : FontWeight.normal,
              fontSize: 15,
            ),
          ),
          trailing: AnimatedRotation(
            turns: isExpanded ? 0.5 : 0,
            duration: const Duration(milliseconds: 200),
            child: Icon(Icons.expand_more, color: Colors.grey[600]),
          ),
          onTap: () => setState(() {
            if (isExpanded) {
              _expandedItems.remove(key);
            } else {
              _expandedItems.add(key);
            }
          }),        ),
        AnimatedCrossFade(
          firstChild: const SizedBox.shrink(),
          secondChild: Column(
            children: item.children!
                .map((child) => _buildNavTile(context, child, isChild: true))
                .toList(),
          ),
          crossFadeState: isExpanded ? CrossFadeState.showSecond : CrossFadeState.showFirst,
          duration: const Duration(milliseconds: 200),
        ),
      ],
    );
  }

  void _confirmLogout(BuildContext context, AuthService auth) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Confirm Logout'),
        content: const Text('Are you sure you want to log out of MeghaConnect?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: const Color(0xFF991B1B),
            ),
            onPressed: () {
              Navigator.pop(ctx);
              auth.logout();
            },
            child: const Text('Logout'),
          ),
        ],
      ),
    );
  }
}

class _RoleBadge extends StatelessWidget {
  final UserRole role;
  const _RoleBadge({required this.role});

  Color get _color {
    switch (role) {
      case UserRole.HCM:
        return const Color(0xFF1A237E);
      case UserRole.ADMIN:
        return const Color(0xFF1565C0);
      case UserRole.SAIDUL_OSD:
        return const Color(0xFF0288D1);
      case UserRole.APPROVER_JT_SECY:
        return const Color(0xFF00838F);
      case UserRole.CMO_OFFICER:
        return const Color(0xFF2E7D32);
      case UserRole.DATA_ENTRY_OPERATOR:
        return const Color(0xFF558B2F);
      case UserRole.PUBLIC:
        return const Color(0xFFB45309);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: _color.withAlpha(51),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: _color.withAlpha(102)),
      ),
      child: Text(
        role.badgeLabel,
        style: TextStyle(
          color: _color,
          fontSize: 11,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

class _PlaceholderScreen extends StatelessWidget {
  final String route;
  const _PlaceholderScreen({required this.route});

  @override
  Widget build(BuildContext context) {
    final labels = {
      'calendar': ('Calendar / Schedule', Icons.calendar_month_outlined, 'View and manage the CM\'s schedule, events, and appointments.'),
      'schemes': ('CM Schemes', Icons.workspace_premium_outlined, 'Manage and review Chief Minister scheme applications.'),
      'identify': ('Public Identification', Icons.badge_outlined, 'Search and identify visitors using phone, EPIC, or biometrics.'),
      'reports': ('Reports & Analytics', Icons.bar_chart_outlined, 'Appointment analytics, district heatmaps, and trend reports.'),
      'followups': ('Pending Follow-ups', Icons.access_time_outlined, 'Track pending directions and follow-up actions.'),
      'audit': ('Audit Trail', Icons.history, 'Full audit log of all actions performed in the system.'),
    };

    final info = labels[route] ?? ('Screen', Icons.web_outlined, 'This screen is under development.');

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: BoxDecoration(
                color: const Color(0xFFE8EAF6),
                shape: BoxShape.circle,
              ),
              child: Icon(info.$2, size: 64, color: const Color(0xFF1A237E)),
            ),
            const SizedBox(height: 24),
            Text(
              info.$1,
              style: const TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: Color(0xFF1A237E),
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),
            Text(
              info.$3,
              style: TextStyle(fontSize: 15, color: Colors.grey[600]),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 32),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: const Color(0xFFFEF3C7),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: const Color(0xFFFCD34D)),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.construction, color: Color(0xFFB45309), size: 18),
                  const SizedBox(width: 8),
                  Text(
                    'Full implementation in progress',
                    style: const TextStyle(
                      color: Color(0xFFB45309),
                      fontWeight: FontWeight.w500,
                      fontSize: 13,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
