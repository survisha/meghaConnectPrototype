import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/navigation_service.dart';
import '../core/i18n/app_i18n.dart';
import '../widgets/megha_ui.dart';
import 'dashboard_screen.dart';
import 'appointments_screen.dart';
import 'new_appointment_screen.dart';
import 'user_management_screen.dart';
import 'calendar_screen.dart';
import 'schemes_screen.dart';
import 'public_identification_screen.dart';
import 'reports_screen.dart';
import 'pending_followups_screen.dart';
import 'audit_trail_screen.dart';
import 'grievance_screen.dart';
import 'visitor_dashboard_screen.dart';
import 'approver_screen.dart';

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
  UserRole.OSD,
  UserRole.APPROVER,
  UserRole.CMO_OFFICER,
  UserRole.DATA_ENTRY_OPERATOR,
];

const _fullControl = [UserRole.HCM, UserRole.ADMIN, UserRole.OSD];

final _navTree = <_NavItem>[
  const _NavItem(
    label: 'Dashboard',
    icon: Icons.dashboard_outlined,
    route: 'dashboard',
    roles: _allRoles,
  ),
  const _NavItem(
    label: 'My Portal',
    icon: Icons.person_outline,
    route: 'visitor',
    roles: [UserRole.PUBLIC],
  ),
  const _NavItem(
    label: 'Calendar / Schedule',
    icon: Icons.calendar_month_outlined,
    route: 'calendar',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.APPROVER,
      UserRole.CMO_OFFICER,
    ],
  ),
  const _NavItem(
    label: 'Approver Review',
    icon: Icons.how_to_reg_outlined,
    route: 'approver',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.APPROVER,
    ],
  ),
  const _NavItem(
    label: 'Appointments',
    icon: Icons.people_outline,
    route: 'appointments',
    roles: [..._allRoles, UserRole.PUBLIC],
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
        roles: [
          UserRole.ADMIN,
          UserRole.OSD,
          UserRole.DATA_ENTRY_OPERATOR,
          UserRole.PUBLIC
        ],
      ),
      _NavItem(
        label: 'Walk-in Counter',
        icon: Icons.login_outlined,
        route: 'walkin',
        roles: [UserRole.ADMIN, UserRole.OSD, UserRole.DATA_ENTRY_OPERATOR],
      ),
    ],
  ),
  const _NavItem(
    label: 'CM Schemes',
    icon: Icons.workspace_premium_outlined,
    route: 'schemes',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.APPROVER,
      UserRole.CMO_OFFICER,
      UserRole.PUBLIC,
    ],
  ),
  const _NavItem(
    label: 'Grievances',
    icon: Icons.comment_outlined,
    route: 'grievances',
    roles: [..._allRoles, UserRole.PUBLIC],
  ),
  const _NavItem(
    label: 'Public Identification',
    icon: Icons.badge_outlined,
    route: 'identify',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.DATA_ENTRY_OPERATOR,
    ],
  ),
  const _NavItem(
    label: 'Reports',
    icon: Icons.bar_chart_outlined,
    route: 'reports',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.OSD,
      UserRole.APPROVER,
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
          UserRole.OSD,
          UserRole.APPROVER,
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
          UserRole.OSD,
          UserRole.APPROVER,
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
  const _NavItem(
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
      case 'visitor':
        return const VisitorDashboardScreen();
      case 'appointments':
        return const AppointmentsScreen();
      case 'new_appointment':
      case 'walkin':
        return NewAppointmentScreen(isWalkIn: route == 'walkin');
      case 'calendar':
        return const CalendarScreen();
      case 'approver':
        return const ApproverWorkflowScreen();
      case 'schemes':
        return const SchemesScreen();
      case 'grievances':
        return const GrievanceScreen();
      case 'identify':
        return const PublicIdentificationScreen();
      case 'reports':
        return const ReportsScreen();
      case 'followups':
        return const PendingFollowupsScreen();
      case 'audit':
        return const AuditTrailScreen();
      case 'users':
        return const UserManagementScreen();
      default:
        return _PlaceholderScreen(route: route);
    }
  }

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final nav = context.watch<NavigationService>();
    final i18n = context.watch<AppI18n>();
    final user = auth.user!;
    _currentRoute = nav.currentRoute;

    // Public users go to visitor dashboard
    if (user.role == UserRole.PUBLIC) {
      return const VisitorDashboardScreen();
    }

    return Scaffold(
      appBar: _buildAppBar(context, auth, user, i18n),
      drawer: _buildDrawer(context, auth, user),
      body: _buildBody(_currentRoute),
    );
  }

  PreferredSizeWidget _buildAppBar(
    BuildContext context,
    AuthService auth,
    AuthUser user,
    AppI18n i18n,
  ) {
    final width = MediaQuery.of(context).size.width;
    final showSubtitle = width >= 420;
    final showRole = width >= 360;
    return AppBar(
      toolbarHeight: 72,
      elevation: 2,
      backgroundColor: Colors.transparent,
      foregroundColor: Colors.white,
      titleSpacing: 0,
      flexibleSpace: const DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [MeghaColors.primary, MeghaColors.primary2],
          ),
        ),
      ),
      title: Row(
        children: [
          Image.asset('assets/logo.png', width: 38, height: 38),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'MeghaConnect',
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                if (showSubtitle)
                  Text(
                    i18n.t('CM_OFFICE_SCHEDULING'),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: Colors.white.withAlpha(214),
                      fontSize: 11,
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
      actions: [
        const MeghaLanguageSelector(dark: true, compact: true),
        if (showRole) ...[
          const SizedBox(width: 8),
          _HeaderRoleBadge(role: user.role),
        ],
        IconButton(
          tooltip: i18n.t('LOGOUT'),
          icon: const Icon(Icons.logout),
          onPressed: () => _confirmLogout(context, auth),
        ),
        const SizedBox(width: 4),
      ],
    );
  }

  Widget _buildDrawer(BuildContext context, AuthService auth, AuthUser user) {
    final visibleItems =
        _navTree.where((item) => item.roles.contains(user.role)).map((item) {
      if (item.children != null) {
        final visibleChildren =
            item.children!.where((c) => c.roles.contains(user.role)).toList();
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
      backgroundColor: _primaryBlue,
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
                Divider(height: 1, color: Colors.white.withAlpha(36)),
                ListTile(
                  leading: const Icon(Icons.logout, color: Color(0xFFFCA5A5)),
                  title: const Text(
                    'Logout',
                    style: TextStyle(color: Color(0xFFFCA5A5)),
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
          colors: [Color(0xFF1A237E), Color(0xFF3949AB)],
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Image.asset('assets/logo.png', width: 58, height: 58),
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
          _HeaderRoleBadge(role: user.role),
        ],
      ),
    );
  }

  Widget _buildNavTile(BuildContext context, _NavItem item,
      {bool isChild = false}) {
    final isActive = _currentRoute == item.route;
    final color = isActive ? Colors.white : Colors.white.withAlpha(204);
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(
        color: isActive ? Colors.white.withAlpha(31) : Colors.transparent,
        borderRadius: BorderRadius.circular(8),
        border: Border(
            left: BorderSide(
                color: isActive ? const Color(0xFF90CAF9) : Colors.transparent,
                width: 3)),
      ),
      child: ListTile(
        dense: isChild,
        contentPadding: EdgeInsets.only(
          left: isChild ? 36 : 14,
          right: 12,
        ),
        leading: Icon(item.icon, color: color, size: isChild ? 20 : 23),
        title: Text(
          item.label,
          style: TextStyle(
            color: color,
            fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
            fontSize: isChild ? 13 : 14,
          ),
        ),
        onTap: () {
          Navigator.pop(context);
          context.read<NavigationService>().navigateTo(item.route!);
        },
      ),
    );
  }

  Widget _buildExpandableItem(BuildContext context, _NavItem item) {
    final key = item.label;
    final isExpanded = _expandedItems.contains(key);
    final hasActiveChild =
        item.children?.any((c) => c.route == _currentRoute) ?? false;

    return Column(
      children: [
        ListTile(
          contentPadding: const EdgeInsets.symmetric(horizontal: 16),
          leading: Icon(
            item.icon,
            color: hasActiveChild ? Colors.white : Colors.white.withAlpha(204),
          ),
          title: Text(
            item.label,
            style: TextStyle(
              color:
                  hasActiveChild ? Colors.white : Colors.white.withAlpha(204),
              fontWeight: hasActiveChild ? FontWeight.w700 : FontWeight.w500,
              fontSize: 14,
            ),
          ),
          trailing: AnimatedRotation(
            turns: isExpanded ? 0.5 : 0,
            duration: const Duration(milliseconds: 200),
            child: Icon(Icons.expand_more, color: Colors.white.withAlpha(204)),
          ),
          onTap: () => setState(() {
            if (isExpanded) {
              _expandedItems.remove(key);
            } else {
              _expandedItems.add(key);
            }
          }),
        ),
        AnimatedCrossFade(
          firstChild: const SizedBox.shrink(),
          secondChild: Column(
            children: item.children!
                .map((child) => _buildNavTile(context, child, isChild: true))
                .toList(),
          ),
          crossFadeState:
              isExpanded ? CrossFadeState.showSecond : CrossFadeState.showFirst,
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
        content:
            const Text('Are you sure you want to log out of MeghaConnect?'),
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

class _HeaderRoleBadge extends StatelessWidget {
  final UserRole role;
  const _HeaderRoleBadge({required this.role});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withAlpha(46),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white.withAlpha(71)),
      ),
      child: Text(
        role.badgeLabel,
        style: const TextStyle(
          color: Colors.white,
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
      'calendar': (
        'Calendar / Schedule',
        Icons.calendar_month_outlined,
        'View and manage the CM\'s schedule, events, and appointments.'
      ),
      'schemes': (
        'CM Schemes',
        Icons.workspace_premium_outlined,
        'Manage and review Chief Minister scheme applications.'
      ),
      'identify': (
        'Public Identification',
        Icons.badge_outlined,
        'Search and identify visitors using phone, EPIC, or biometrics.'
      ),
      'reports': (
        'Reports & Analytics',
        Icons.bar_chart_outlined,
        'Appointment analytics, district heatmaps, and trend reports.'
      ),
      'followups': (
        'Pending Follow-ups',
        Icons.access_time_outlined,
        'Track pending directions and follow-up actions.'
      ),
      'audit': (
        'Audit Trail',
        Icons.history,
        'Full audit log of all actions performed in the system.'
      ),
    };

    final info = labels[route] ??
        ('Screen', Icons.web_outlined, 'This screen is under development.');

    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              padding: const EdgeInsets.all(24),
              decoration: const BoxDecoration(
                color: Color(0xFFE8EAF6),
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
              child: const Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.construction, color: Color(0xFFB45309), size: 18),
                  SizedBox(width: 8),
                  Text(
                    'Full implementation in progress',
                    style: TextStyle(
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
