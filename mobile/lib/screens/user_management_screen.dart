import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../core/security/secure_app_storage.dart';
import '../models/user.dart';
import '../services/api_service.dart';
import '../services/auth_service.dart';

class _UserEntry {
  final int id;
  final String username;
  final String fullName;
  final UserRole role;
  final String phoneNumber;
  final String email;
  final String department;
  final bool active;
  final bool locked;
  final String createdAt;
  final String lastLogin;

  const _UserEntry({
    required this.id,
    required this.username,
    required this.fullName,
    required this.role,
    required this.phoneNumber,
    required this.email,
    required this.department,
    required this.active,
    required this.locked,
    required this.createdAt,
    required this.lastLogin,
  });

  factory _UserEntry.fromJson(Map<String, dynamic> m) {
    return _UserEntry(
      id: (m['id'] as num?)?.toInt() ?? 0,
      username: _text(m['username']),
      fullName: _text(m['fullName'], '-'),
      role: _parseRole(_text(m['role'], 'PUBLIC')),
      phoneNumber: _text(m['phoneNumber']),
      email: _text(m['email']),
      department:
          _firstText([m['departmentName'], m['department'], m['designation']]),
      active: m['active'] != false,
      locked: m['locked'] == true,
      createdAt: _formatDateTime(m['createdAt']),
      lastLogin: _formatDateTime(m['lastLogin']),
    );
  }
}

class UserManagementScreen extends StatefulWidget {
  const UserManagementScreen({super.key});

  @override
  State<UserManagementScreen> createState() => _UserManagementScreenState();
}

class _UserManagementScreenState extends State<UserManagementScreen> {
  final _searchCtrl = TextEditingController();
  List<_UserEntry> _users = [];
  bool _loading = true;
  String _roleFilter = '';
  String _activeFilter = '';
  String _lockedFilter = '';
  String _currentUsername = '';

  static const _pageSize = 20;
  int _visibleCount = _pageSize;

  @override
  void initState() {
    super.initState();
    _loadCurrentUser();
    _loadUsers();
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadCurrentUser() async {
    final raw = await SecureAppStorage.readUserJson();
    if (raw == null) return;
    try {
      final data = jsonDecode(raw) as Map<String, dynamic>;
      if (mounted) setState(() => _currentUsername = _text(data['username']));
    } catch (_) {}
  }

  Future<void> _loadUsers() async {
    setState(() => _loading = true);
    final list = await ApiService.getUsers();
    if (!mounted) return;
    setState(() {
      _users = list
          .whereType<Map>()
          .map((e) => _UserEntry.fromJson(Map<String, dynamic>.from(e)))
          .where((u) => u.role != UserRole.PUBLIC)
          .toList();
      _loading = false;
      _visibleCount = _pageSize;
    });
  }

  List<_UserEntry> get _filtered {
    final query = _searchCtrl.text.trim().toLowerCase();
    return _users.where((u) {
      final haystack = [
        u.fullName,
        u.username,
        u.phoneNumber,
        u.email,
        u.role.displayName,
      ].join(' ').toLowerCase();
      return (query.isEmpty || haystack.contains(query)) &&
          (_roleFilter.isEmpty || u.role.name == _roleFilter) &&
          (_activeFilter.isEmpty || u.active.toString() == _activeFilter) &&
          (_lockedFilter.isEmpty || u.locked.toString() == _lockedFilter);
    }).toList();
  }

  List<_UserEntry> get _visible => _filtered.take(_visibleCount).toList();

  @override
  Widget build(BuildContext context) {
    final filtered = _filtered;
    return Column(
      children: [
        _buildHeader(context),
        Expanded(
          child: _loading
              ? const Center(child: CircularProgressIndicator())
              : RefreshIndicator(
                  onRefresh: _loadUsers,
                  child: ListView.separated(
                    padding: const EdgeInsets.all(12),
                    itemCount: _visible.length +
                        (_visible.length < filtered.length ? 1 : 0),
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, i) {
                      if (i >= _visible.length) {
                        return Center(
                          child: OutlinedButton.icon(
                            onPressed: () =>
                                setState(() => _visibleCount += _pageSize),
                            icon: const Icon(Icons.expand_more),
                            label: const Text('Load more'),
                          ),
                        );
                      }
                      final user = _visible[i];
                      return _UserCard(
                        user: user,
                        isSelf: user.username == _currentUsername,
                        onEdit: () => _showUserDialog(context, user),
                        onToggleActive: () => _toggleActive(user),
                        onUnlock: user.locked ? () => _unlockUser(user) : null,
                        onDelete: () => _deleteUser(user),
                      );
                    },
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildHeader(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(12, 12, 12, 4),
      child: Column(
        children: [
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchCtrl,
                  decoration: InputDecoration(
                    hintText: 'Search name, login ID, mobile, email',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _searchCtrl.text.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear),
                            onPressed: () {
                              _searchCtrl.clear();
                              setState(() => _visibleCount = _pageSize);
                            },
                          )
                        : null,
                  ),
                  onChanged: (_) => setState(() => _visibleCount = _pageSize),
                ),
              ),
              const SizedBox(width: 10),
              IconButton.filled(
                icon: const Icon(Icons.person_add_outlined),
                onPressed: () => _showUserDialog(context, null),
                tooltip: 'Add User',
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              Expanded(child: _roleDropdown()),
              const SizedBox(width: 8),
              Expanded(child: _statusDropdown()),
              const SizedBox(width: 8),
              Expanded(child: _lockedDropdown()),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _statChip('Total', '${_users.length}', const Color(0xFF1A237E)),
              const SizedBox(width: 8),
              _statChip('Active', '${_users.where((u) => u.active).length}',
                  const Color(0xFF16A34A)),
              const SizedBox(width: 8),
              _statChip('Locked', '${_users.where((u) => u.locked).length}',
                  const Color(0xFFB45309)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _roleDropdown() {
    final roles = UserRole.values.where((r) => r != UserRole.PUBLIC).toList();
    return DropdownButtonFormField<String>(
      value: _roleFilter,
      isExpanded: true,
      decoration: const InputDecoration(labelText: 'Role'),
      items: [
        const DropdownMenuItem(value: '', child: Text('All')),
        for (final role in roles)
          DropdownMenuItem(value: role.name, child: Text(role.badgeLabel)),
      ],
      onChanged: (value) => setState(() => _roleFilter = value ?? ''),
    );
  }

  Widget _statusDropdown() {
    return DropdownButtonFormField<String>(
      value: _activeFilter,
      isExpanded: true,
      decoration: const InputDecoration(labelText: 'Active'),
      items: const [
        DropdownMenuItem(value: '', child: Text('All')),
        DropdownMenuItem(value: 'true', child: Text('Active')),
        DropdownMenuItem(value: 'false', child: Text('Inactive')),
      ],
      onChanged: (value) => setState(() => _activeFilter = value ?? ''),
    );
  }

  Widget _lockedDropdown() {
    return DropdownButtonFormField<String>(
      value: _lockedFilter,
      isExpanded: true,
      decoration: const InputDecoration(labelText: 'Locked'),
      items: const [
        DropdownMenuItem(value: '', child: Text('All')),
        DropdownMenuItem(value: 'true', child: Text('Locked')),
        DropdownMenuItem(value: 'false', child: Text('Unlocked')),
      ],
      onChanged: (value) => setState(() => _lockedFilter = value ?? ''),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Expanded(
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: color.withAlpha(20),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: color.withAlpha(51)),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Flexible(
              child: Text(label,
                  overflow: TextOverflow.ellipsis,
                  style: TextStyle(color: color, fontSize: 12)),
            ),
            const SizedBox(width: 6),
            Text(value,
                style: TextStyle(
                    color: color, fontWeight: FontWeight.bold, fontSize: 13)),
          ],
        ),
      ),
    );
  }

  Future<void> _showUserDialog(BuildContext context, _UserEntry? existing) {
    final nameCtrl = TextEditingController(text: existing?.fullName ?? '');
    final userCtrl = TextEditingController(text: existing?.username ?? '');
    final phoneCtrl = TextEditingController(text: existing?.phoneNumber ?? '');
    final passwordCtrl = TextEditingController();
    final actorRole = context.read<AuthService>().user?.role;
    final allowedRoles = actorRole == UserRole.SUPER_ADMIN
        ? [UserRole.DEPARTMENT_ADMIN]
        : actorRole == UserRole.DEPARTMENT_ADMIN
            ? [UserRole.DEO, UserRole.DEPARTMENT_PA, UserRole.HEAD_DEPARTMENT]
            : UserRole.values.where((role) => role != UserRole.PUBLIC).toList();
    UserRole selectedRole = existing?.role ?? allowedRoles.first;
    bool active = existing?.active ?? true;
    bool locked = existing?.locked ?? false;
    bool saving = false;

    return showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) => AlertDialog(
          title: Text(existing == null ? 'Add User' : 'Edit User'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Full Name *',
                    prefixIcon: Icon(Icons.person_outline),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: userCtrl,
                  enabled: existing == null,
                  decoration: const InputDecoration(
                    labelText: 'Username / Login ID *',
                    prefixIcon: Icon(Icons.account_circle_outlined),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: phoneCtrl,
                  keyboardType: TextInputType.phone,
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(10),
                  ],
                  decoration: const InputDecoration(
                    labelText: 'Mobile',
                    prefixIcon: Icon(Icons.phone_outlined),
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<UserRole>(
                  value: selectedRole,
                  isExpanded: true,
                  decoration: const InputDecoration(
                    labelText: 'Role *',
                    prefixIcon: Icon(Icons.shield_outlined),
                  ),
                  items: allowedRoles
                      .map((r) => DropdownMenuItem(
                          value: r, child: Text(r.displayName)))
                      .toList(),
                  onChanged: (v) =>
                      setLocalState(() => selectedRole = v ?? selectedRole),
                ),
                if (existing == null) ...[
                  const SizedBox(height: 12),
                  TextField(
                    controller: passwordCtrl,
                    obscureText: true,
                    decoration: const InputDecoration(
                      labelText: 'Password *',
                      prefixIcon: Icon(Icons.lock_outline),
                    ),
                  ),
                ],
                if (existing != null) ...[
                  const SizedBox(height: 12),
                  SwitchListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text('Active user'),
                    value: active,
                    onChanged: existing.username == _currentUsername
                        ? null
                        : (value) => setLocalState(() => active = value),
                  ),
                  ListTile(
                    contentPadding: EdgeInsets.zero,
                    title: Text(locked ? 'Locked' : 'Unlocked'),
                    subtitle: const Text('Locked users cannot sign in.'),
                    trailing: locked
                        ? OutlinedButton(
                            onPressed: () =>
                                setLocalState(() => locked = false),
                            child: const Text('Unlock'),
                          )
                        : const Icon(Icons.lock_open, color: Color(0xFF16A34A)),
                  ),
                ],
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: saving ? null : () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: saving
                  ? null
                  : () async {
                      final message = _validateUserForm(
                        existing: existing,
                        fullName: nameCtrl.text,
                        username: userCtrl.text,
                        password: passwordCtrl.text,
                        phone: phoneCtrl.text,
                      );
                      if (message != null) {
                        _showSnack(message, success: false);
                        return;
                      }
                      setLocalState(() => saving = true);
                      final ok = await _saveUser(
                        existing: existing,
                        fullName: nameCtrl.text.trim(),
                        username: userCtrl.text.trim(),
                        password: passwordCtrl.text.trim(),
                        phone: phoneCtrl.text.trim(),
                        role: selectedRole,
                        active: active,
                        locked: locked,
                      );
                      if (!ctx.mounted) return;
                      if (ok) Navigator.pop(ctx);
                      setLocalState(() => saving = false);
                    },
              child: Text(saving
                  ? 'Saving...'
                  : existing == null
                      ? 'Create'
                      : 'Save'),
            ),
          ],
        ),
      ),
    );
  }

  String? _validateUserForm({
    required _UserEntry? existing,
    required String fullName,
    required String username,
    required String password,
    required String phone,
  }) {
    if (fullName.trim().isEmpty ||
        (existing == null && username.trim().isEmpty)) {
      return 'Full name and username are required.';
    }
    if (existing == null && password.trim().isEmpty) {
      return 'Password is required for a new user.';
    }
    if (password.trim().isNotEmpty && password.trim().length < 6) {
      return 'Password must be at least 6 characters.';
    }
    if (phone.trim().isNotEmpty && phone.trim().length != 10) {
      return 'Mobile number must be exactly 10 digits.';
    }
    return null;
  }

  Future<bool> _saveUser({
    required _UserEntry? existing,
    required String fullName,
    required String username,
    required String password,
    required String phone,
    required UserRole role,
    required bool active,
    required bool locked,
  }) async {
    final payload = <String, dynamic>{
      'fullName': fullName,
      'role': _roleForApi(role),
      'phoneNumber': phone.isEmpty ? null : phone,
      'active': active,
      'locked': locked,
      'offlineAccess': false,
    };
    final result = existing == null
        ? await ApiService.createUser({
            ...payload,
            'username': username,
            'password': password,
          })
        : await ApiService.updateUser(existing.id, payload);
    if (result == null) {
      _showSnack(
          existing == null
              ? 'Failed to create user.'
              : 'Failed to update user.',
          success: false);
      return false;
    }
    _showSnack(existing == null
        ? 'User created successfully.'
        : locked
            ? 'User updated successfully.'
            : 'User updated successfully.');
    await _loadUsers();
    return true;
  }

  Future<void> _toggleActive(_UserEntry user) async {
    if (user.username == _currentUsername && user.active) {
      _showSnack('Cannot deactivate yourself.', success: false);
      return;
    }
    final result = await ApiService.setUserActive(user.id, !user.active);
    if (result == null) {
      _showSnack(
          user.active
              ? 'Failed to deactivate user.'
              : 'Failed to activate user.',
          success: false);
      return;
    }
    _showSnack(user.active
        ? 'User deactivated successfully.'
        : 'User activated successfully.');
    await _loadUsers();
  }

  Future<void> _unlockUser(_UserEntry user) async {
    final confirmed = await showDialog<bool>(
            context: context,
            builder: (context) => AlertDialog(
                  title: const Text('Unlock account?'),
                  content: Text(
                      'Unlock ${user.username} and reset failed login attempts?'),
                  actions: [
                    TextButton(
                        onPressed: () => Navigator.pop(context, false),
                        child: const Text('Cancel')),
                    FilledButton(
                        onPressed: () => Navigator.pop(context, true),
                        child: const Text('Unlock'))
                  ],
                )) ??
        false;
    if (!confirmed) return;
    final result = await ApiService.unlockUser(user.id);
    if (result == null) {
      _showSnack('Failed to unlock user.', success: false);
      return;
    }
    _showSnack('User unlocked successfully.');
    await _loadUsers();
  }

  Future<void> _deleteUser(_UserEntry user) async {
    if (user.username == _currentUsername) {
      _showSnack('Cannot delete yourself.', success: false);
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete User'),
        content: const Text('Are you sure you want to delete this user?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Delete'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;
    final ok = await ApiService.deleteUser(user.id);
    if (!ok) {
      _showSnack('Failed to delete user.', success: false);
      return;
    }
    _showSnack('User deleted successfully.');
    await _loadUsers();
  }

  void _showSnack(String message, {bool success = true}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor:
            success ? const Color(0xFF065F46) : const Color(0xFF991B1B),
      ),
    );
  }
}

class _UserCard extends StatelessWidget {
  final _UserEntry user;
  final bool isSelf;
  final VoidCallback onEdit;
  final VoidCallback onToggleActive;
  final VoidCallback? onUnlock;
  final VoidCallback onDelete;

  const _UserCard({
    required this.user,
    required this.isSelf,
    required this.onEdit,
    required this.onToggleActive,
    required this.onUnlock,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    final rc = _roleColor(user.role);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: rc.withAlpha(26),
                  child: Text(
                    (user.fullName.isEmpty ? '?' : user.fullName[0])
                        .toUpperCase(),
                    style: TextStyle(color: rc, fontWeight: FontWeight.bold),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        user.fullName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                            fontWeight: FontWeight.w700, fontSize: 14),
                      ),
                      Text('@${user.username}',
                          style:
                              TextStyle(color: Colors.grey[600], fontSize: 12)),
                    ],
                  ),
                ),
                PopupMenuButton<String>(
                  onSelected: (value) {
                    if (value == 'edit') onEdit();
                    if (value == 'toggle') onToggleActive();
                    if (value == 'unlock') onUnlock?.call();
                    if (value == 'delete') onDelete();
                  },
                  itemBuilder: (_) => [
                    const PopupMenuItem(value: 'edit', child: Text('Edit')),
                    PopupMenuItem(
                      value: 'toggle',
                      enabled: !(isSelf && user.active),
                      child: Text(user.active ? 'Deactivate' : 'Activate'),
                    ),
                    if (user.locked)
                      const PopupMenuItem(
                          value: 'unlock', child: Text('Unlock')),
                    PopupMenuItem(
                      value: 'delete',
                      enabled: !isSelf,
                      child: const Text('Delete'),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 6,
              children: [
                _Badge(user.role.badgeLabel, rc),
                _Badge(user.active ? 'Active' : 'Inactive',
                    user.active ? const Color(0xFF065F46) : Colors.grey),
                _Badge(
                    user.locked ? 'Locked' : 'Unlocked',
                    user.locked
                        ? const Color(0xFFB45309)
                        : const Color(0xFF065F46)),
              ],
            ),
            const SizedBox(height: 10),
            _MetaLine(Icons.phone_outlined,
                user.phoneNumber.isEmpty ? '-' : user.phoneNumber),
            if (user.email.isNotEmpty)
              _MetaLine(Icons.mail_outline, user.email),
            if (user.department.isNotEmpty)
              _MetaLine(Icons.business_outlined, user.department),
            _MetaLine(
                Icons.calendar_today_outlined,
                user.createdAt.isEmpty
                    ? 'Created: -'
                    : 'Created: ${user.createdAt}'),
          ],
        ),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  final String label;
  final Color color;
  const _Badge(this.label, this.color);

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: color.withAlpha(24),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Text(label,
          style: TextStyle(
              color: color, fontSize: 11, fontWeight: FontWeight.w700)),
    );
  }
}

class _MetaLine extends StatelessWidget {
  final IconData icon;
  final String text;
  const _MetaLine(this.icon, this.text);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(top: 4),
      child: Row(
        children: [
          Icon(icon, size: 14, color: Colors.grey[500]),
          const SizedBox(width: 6),
          Expanded(
            child: Text(
              text,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(color: Colors.grey[600], fontSize: 12),
            ),
          ),
        ],
      ),
    );
  }
}

UserRole _parseRole(String raw) {
  final normalized = raw.startsWith('ROLE_') ? raw.substring(5) : raw;
  final mapped = normalized == 'SECURITY'
      ? 'SECURITY_POLICE'
      : normalized == 'CMO'
          ? 'CMO_OFFICER'
          : normalized;
  return UserRole.values.firstWhere(
    (role) => role.name == mapped,
    orElse: () => UserRole.PUBLIC,
  );
}

String _roleForApi(UserRole role) {
  return role == UserRole.SECURITY_POLICE ? 'SECURITY' : role.name;
}

Color _roleColor(UserRole role) {
  switch (role) {
    case UserRole.SUPER_ADMIN:
      return const Color(0xFF7C2D12);
    case UserRole.DEPARTMENT_ADMIN:
      return const Color(0xFF4338CA);
    case UserRole.DEO:
      return const Color(0xFF558B2F);
    case UserRole.DEPARTMENT_PA:
      return const Color(0xFF0369A1);
    case UserRole.HEAD_DEPARTMENT:
      return const Color(0xFF0F766E);
    case UserRole.HCM:
      return const Color(0xFF1A237E);
    case UserRole.ADMIN:
      return const Color(0xFF1565C0);
    case UserRole.OSD:
      return const Color(0xFF0288D1);
    case UserRole.APPROVER:
      return const Color(0xFF00838F);
    case UserRole.CMO_OFFICER:
      return const Color(0xFF2E7D32);
    case UserRole.DATA_ENTRY_OPERATOR:
      return const Color(0xFF558B2F);
    case UserRole.SECURITY_POLICE:
      return const Color(0xFF7C3AED);
    case UserRole.PUBLIC:
      return const Color(0xFFB45309);
  }
}

String _text(dynamic value, [String fallback = '']) {
  final text = value?.toString().trim() ?? '';
  return text.isEmpty ? fallback : text;
}

String _firstText(List<dynamic> values) {
  for (final value in values) {
    final text = _text(value);
    if (text.isNotEmpty) return text;
  }
  return '';
}

String _formatDateTime(dynamic value) {
  final raw = value?.toString().trim() ?? '';
  if (raw.isEmpty) return '';
  final date = DateTime.tryParse(raw);
  if (date == null) return raw;
  final local = date.toLocal();
  return '${local.day.toString().padLeft(2, '0')}-${_month(local.month)}-${local.year} '
      '${local.hour.toString().padLeft(2, '0')}:${local.minute.toString().padLeft(2, '0')}';
}

String _month(int month) {
  const labels = [
    'Jan',
    'Feb',
    'Mar',
    'Apr',
    'May',
    'Jun',
    'Jul',
    'Aug',
    'Sep',
    'Oct',
    'Nov',
    'Dec',
  ];
  return labels[month - 1];
}
