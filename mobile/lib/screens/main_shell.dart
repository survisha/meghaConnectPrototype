import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../models/user.dart';
import '../services/auth_service.dart';
import '../services/connectivity_service.dart';
import '../services/navigation_service.dart';
import '../services/notification_service.dart';
import '../services/access_control_service.dart';
import '../core/i18n/app_i18n.dart';
import '../widgets/megha_ui.dart';
import '../widgets/app_footer.dart';
import 'dashboard_screen.dart';
import 'appointments_screen.dart';
import 'new_appointment_screen.dart';
import 'visitor_registration_screen.dart';
import 'user_management_screen.dart';
import 'calendar_screen.dart';
import 'schemes_screen.dart';
import 'scheme_form_screen.dart';
import 'public_identification_screen.dart';
import 'reports_screen.dart';
import 'heatmap_screen.dart';
import 'audit_trail_screen.dart';
import 'grievance_screen.dart';
import 'visitor_dashboard_screen.dart';
import 'approver_screen.dart';
import 'guest_appointment_screen.dart';
import 'pending_sync_screen.dart';
import 'qr_scanner_screen.dart';

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
  UserRole.APPROVER,
  UserRole.APPROVER,
  UserRole.APPROVER,
  UserRole.DEO,
  UserRole.SECURITY_POLICE,
];

const _fullControl = [UserRole.SUPER_ADMIN, UserRole.DEPARTMENT_ADMIN];

final _navTree = <_NavItem>[
  const _NavItem(
    label: 'Dashboard',
    icon: Icons.dashboard_outlined,
    route: 'dashboard',
    roles: _allRoles,
  ),
  const _NavItem(
    label: 'Pending Sync',
    icon: Icons.sync_problem_outlined,
    route: 'pending_sync',
    roles: [..._allRoles, UserRole.PUBLIC],
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
      UserRole.APPROVER,
      UserRole.APPROVER,
      UserRole.APPROVER,
    ],
  ),
  const _NavItem(
    label: 'Approver Review',
    icon: Icons.how_to_reg_outlined,
    route: 'approver',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.APPROVER,
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
          UserRole.APPROVER,
          UserRole.DEO,
          UserRole.HCM,
          UserRole.PUBLIC
        ],
      ),
      _NavItem(
        label: 'Guest Registration',
        icon: Icons.person_add_alt_1_outlined,
        route: 'guest_registration',
        roles: [
          UserRole.ADMIN,
          UserRole.APPROVER,
          UserRole.DEO,
          UserRole.APPROVER,
        ],
      ),
      _NavItem(
        label: 'Walk-in Counter',
        icon: Icons.login_outlined,
        route: 'walkin',
        roles: [
          UserRole.SUPER_ADMIN,
          UserRole.ADMIN,
          UserRole.DEO,
          UserRole.APPROVER,
          UserRole.HCM
        ],
      ),
      _NavItem(
        label: 'Walk-in Appointments',
        icon: Icons.format_list_bulleted_outlined,
        route: 'walkin_appointments',
        roles: [
          UserRole.SUPER_ADMIN,
          UserRole.ADMIN,
          UserRole.DEO,
          UserRole.APPROVER,
          UserRole.HCM,
        ],
      ),
      _NavItem(
        label: 'Completed Appointments',
        icon: Icons.task_alt_outlined,
        route: 'completed_appointments',
        roles: [
          UserRole.SUPER_ADMIN,
          UserRole.DEO,
          UserRole.APPROVER,
          UserRole.HCM
        ],
      ),
      _NavItem(
        label: 'Rejected Appointments',
        icon: Icons.cancel_outlined,
        route: 'rejected_appointments',
        roles: [UserRole.SUPER_ADMIN, UserRole.APPROVER, UserRole.HCM],
      ),
      _NavItem(
        label: 'Closed Appointments',
        icon: Icons.verified_outlined,
        route: 'closed_appointments',
        roles: [UserRole.DEO, UserRole.APPROVER, UserRole.HCM],
      ),
      _NavItem(
        label: 'Register Visitor',
        icon: Icons.person_add_alt_1_outlined,
        route: 'register_visitor',
        roles: [UserRole.DEO, UserRole.APPROVER, UserRole.HCM],
      ),
      _NavItem(
        label: 'QR Scanner',
        icon: Icons.qr_code_scanner_outlined,
        route: 'qr_scanner',
        roles: [UserRole.DEO],
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
      UserRole.APPROVER,
      UserRole.APPROVER,
      UserRole.APPROVER,
      UserRole.PUBLIC,
    ],
  ),
  // Pilot 2026-09-01: Grievances navigation temporarily hidden.
  const _NavItem(
    label: 'Public Identification',
    icon: Icons.badge_outlined,
    route: 'identify',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.APPROVER,
      UserRole.APPROVER,
      UserRole.DEO,
    ],
  ),
  const _NavItem(
    label: 'Reports',
    icon: Icons.bar_chart_outlined,
    route: 'reports',
    roles: [
      UserRole.HCM,
      UserRole.ADMIN,
      UserRole.APPROVER,
      UserRole.APPROVER,
      UserRole.APPROVER,
    ],
    children: [
      _NavItem(
        label: 'Analytics',
        icon: Icons.pie_chart_outline,
        route: 'reports',
        roles: [
          UserRole.HCM,
          UserRole.ADMIN,
          UserRole.APPROVER,
          UserRole.APPROVER,
          UserRole.APPROVER,
        ],
      ),
      _NavItem(
        label: 'Heatmap',
        icon: Icons.map_outlined,
        route: 'heatmap',
        roles: [
          UserRole.HCM,
          UserRole.ADMIN,
          UserRole.APPROVER,
          UserRole.APPROVER,
          UserRole.APPROVER,
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
  bool _biometricPromptScheduled = false;

  static const _primaryBlue = Color(0xFF1A237E);

  void _scheduleBiometricPrompt(AuthService auth) {
    if (_biometricPromptScheduled) return;
    _biometricPromptScheduled = true;
    WidgetsBinding.instance.addPostFrameCallback((_) async {
      if (!mounted || await auth.hasBiometricPromptDecision) return;
      if (!mounted) return;
      final enable = await showDialog<bool>(
        context: context,
        builder: (dialogContext) => AlertDialog(
          title: const Text('Enable biometric login?'),
          content: const Text(
              'Use fingerprint, Face ID, or another enrolled device biometric to unlock your securely saved staff session.'),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogContext, false),
              child: const Text('Not Now'),
            ),
            FilledButton(
              onPressed: () => Navigator.pop(dialogContext, true),
              child: const Text('Enable'),
            ),
          ],
        ),
      );
      if (enable != true) {
        await auth.declineBiometricLogin();
        return;
      }
      final enabled = await auth.enableBiometricLogin();
      if (enabled) {
        AppNotificationService.success('Biometric login enabled successfully.');
      } else {
        AppNotificationService.warning(
            auth.lastError ?? 'Unable to enable biometric login.');
      }
    });
  }

  Widget _buildBody(String route) {
    final user = context.read<AuthService>().user!;
    if (!AccessControlService.canAccessRoute(user, route)) {
      return const Center(
          child: Text('You are not authorized to access this feature.'));
    }
    switch (route) {
      case 'dashboard':
        return const DashboardScreen();
      case 'visitor':
        return const VisitorDashboardScreen();
      case 'appointments':
        return const AppointmentsScreen();
      case 'new_appointment':
        if (context.read<AuthService>().user?.role == UserRole.PUBLIC) {
          return const NewAppointmentScreen();
        }
        return const PublicIdentificationScreen(walkInMode: true);
      case 'walkin':
        return const PublicIdentificationScreen(walkInMode: true);
      case 'walkin_appointments':
        return const AppointmentsScreen(walkInOnly: true);
      case 'completed_appointments':
        return const AppointmentsScreen(reportMode: 'completed');
      case 'rejected_appointments':
        return const AppointmentsScreen(reportMode: 'rejected');
      case 'closed_appointments':
        return const AppointmentsScreen(reportMode: 'closed');
      case 'register_visitor':
        return const VisitorRegistrationScreen(
            openAppointmentAfterSubmit: true);
      case 'qr_scanner':
        return const QrScannerScreen();
      case 'guest_registration':
        return const GuestAppointmentScreen();
      case 'pending_sync':
        return const PendingSyncScreen();
      case 'calendar':
        return const CalendarScreen();
      case 'approver':
        return const ApproverWorkflowScreen();
      case 'schemes':
        return const SchemesScreen();
      case 'scheme_form':
        return const SchemeFormScreen();
      case 'grievances':
        return const GrievanceScreen();
      case 'identify':
        return const PublicIdentificationScreen();
      case 'reports':
        return const ReportsScreen();
      case 'heatmap':
        if (kDebugMode) {
          debugPrint('MainShell destination=HeatmapScreen route=$route');
        }
        return const HeatmapScreen();
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
    _scheduleBiometricPrompt(auth);
    return Scaffold(
      backgroundColor: const Color(0xFFF4F6FB),
      appBar: _buildAppBar(context, auth, user, i18n),
      drawer: _buildDrawer(context, auth, user),
      body: Column(
        children: [
          if (context.watch<ConnectivityService>().isOffline)
            const _OfflineWorkBanner(),
          Expanded(child: _buildBody(_currentRoute)),
          const AppFooter(),
        ],
      ),
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
      automaticallyImplyLeading: false,
      leading: Builder(
        builder: (scaffoldContext) => IconButton(
          tooltip: 'Open navigation menu',
          icon: const Icon(Icons.menu),
          onPressed: () => Scaffold.of(scaffoldContext).openDrawer(),
        ),
      ),
      toolbarHeight: 72,
      elevation: 2,
      backgroundColor: MeghaColors.primary,
      systemOverlayStyle: SystemUiOverlayStyle.light.copyWith(
        statusBarColor: MeghaColors.primary,
      ),
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
          Image.asset(
            'assets/branding/meghaconnect-ai-logo-transparent.png',
            width: 44,
            height: 44,
            fit: BoxFit.contain,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                const Text(
                  'MEGHACONNECT AI',
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                if (showSubtitle)
                  Text(
                    'Connecting Citizens Through Intelligence',
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
        if (_currentRoute != 'dashboard')
          IconButton(
            tooltip: 'Back to Dashboard',
            icon: const Icon(Icons.arrow_back),
            onPressed: () =>
                context.read<NavigationService>().navigateTo('dashboard'),
          ),
        const _ConnectivityBadge(),
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
    final visibleItems = _navTree
        .where((item) => user.role == UserRole.DEPARTMENT_ADMIN
            ? AccessControlService.canAccessRoute(user, item.route)
            : item.roles.contains(user.role))
        .map((item) {
      if (item.children != null) {
        final visibleChildren = item.children!
            .where((c) => user.role == UserRole.DEPARTMENT_ADMIN
                ? AccessControlService.canAccessRoute(user, c.route)
                : c.roles.contains(user.role))
            .toList();
        if (item.route == 'reports' &&
            !visibleChildren.any((child) => child.route == 'heatmap')) {
          visibleChildren.insert(
            1,
            _NavItem(
              label: 'Heatmap',
              icon: Icons.map_outlined,
              route: 'heatmap',
              roles: item.roles,
            ),
          );
        }
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
        top: MediaQuery.of(context).padding.top + 14,
        left: 16,
        right: 16,
        bottom: 14,
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Image.asset(
                'assets/branding/meghaconnect-ai-logo-transparent.png',
                width: 52,
                height: 52,
                fit: BoxFit.contain,
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'MEGHACONNECT AI',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 17,
                        fontWeight: FontWeight.w900,
                      ),
                    ),
                    Text(
                      'Connecting Citizens Through Intelligence',
                      style: TextStyle(color: Color(0xFFC7D2FE), fontSize: 11),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),
          Text(
            user.fullName,
            style: TextStyle(
              color: Colors.white.withAlpha(235),
              fontSize: 15,
              fontWeight: FontWeight.w800,
            ),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 8),
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
          if (kDebugMode) {
            debugPrint(
              'MainShell menu clicked label=${item.label} route=${item.route} current=$_currentRoute',
            );
          }
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

class _OfflineWorkBanner extends StatelessWidget {
  const _OfflineWorkBanner();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      color: const Color(0xFFFEF3C7),
      child: const Text(
        'You are working in offline mode. Data will sync when internet is available.',
        textAlign: TextAlign.center,
        style: TextStyle(color: Color(0xFF92400E), fontSize: 12),
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

class _ConnectivityBadge extends StatelessWidget {
  const _ConnectivityBadge();

  @override
  Widget build(BuildContext context) {
    final online = context.watch<ConnectivityService>().isOnline;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 4),
      child: Tooltip(
        message: online
            ? 'Online'
            : 'You are working in offline mode. Data will sync when internet is available.',
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(
            color: online
                ? const Color(0xFF16A34A).withAlpha(46)
                : const Color(0xFFF59E0B).withAlpha(56),
            borderRadius: BorderRadius.circular(999),
            border: Border.all(
              color: online ? const Color(0xFF86EFAC) : const Color(0xFFFCD34D),
            ),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Icon(
                online ? Icons.cloud_done_outlined : Icons.cloud_off_outlined,
                color: Colors.white,
                size: 14,
              ),
              const SizedBox(width: 4),
              Text(
                online ? 'Online' : 'Offline',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 11,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
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
