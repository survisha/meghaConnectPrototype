import 'package:flutter/material.dart';
import '../models/user.dart';
import '../services/api_service.dart';

class _UserEntry {
  final int id;
  final String username;
  final String fullName;
  final UserRole role;
  final bool active;
  final String lastLogin;

  const _UserEntry({
    required this.id,
    required this.username,
    required this.fullName,
    required this.role,
    required this.active,
    required this.lastLogin,
  });
}

class UserManagementScreen extends StatefulWidget {
  const UserManagementScreen({super.key});

  @override
  State<UserManagementScreen> createState() => _UserManagementScreenState();
}

class _UserManagementScreenState extends State<UserManagementScreen> {
  String _searchQuery = '';
  List<_UserEntry> _users = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _loadUsers();
  }

  Future<void> _loadUsers() async {
    setState(() => _loading = true);
    final list = await ApiService.getUsers();
    if (!mounted) return;
    setState(() {
      _users = list.map((e) {
        final m = e as Map<String, dynamic>;
        final roleRaw = m['role'] as String? ?? 'PUBLIC';
        final normalized =
            roleRaw.startsWith('ROLE_') ? roleRaw.substring(5) : roleRaw;
        UserRole role;
        try {
          role = UserRole.values.byName(normalized);
        } catch (_) {
          role = UserRole.PUBLIC;
        }
        final ts = m['lastLogin'] as String? ?? '';
        String lastLoginLabel = ts;
        final dt = DateTime.tryParse(ts);
        if (dt != null) {
          lastLoginLabel =
              '${dt.year}-${dt.month.toString().padLeft(2, '0')}-${dt.day.toString().padLeft(2, '0')} '
              '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
        }
        return _UserEntry(
          id: (m['id'] as num?)?.toInt() ?? 0,
          username: m['username'] as String? ?? '',
          fullName: m['fullName'] as String? ?? '—',
          role: role,
          active: m['active'] as bool? ?? true,
          lastLogin: lastLoginLabel,
        );
      }).toList();
      _loading = false;
    });
  }

  List<_UserEntry> get _filtered => _users.where((u) {
        return _searchQuery.isEmpty ||
            u.fullName.toLowerCase().contains(_searchQuery.toLowerCase()) ||
            u.username.toLowerCase().contains(_searchQuery.toLowerCase()) ||
            u.role.displayName.toLowerCase().contains(_searchQuery.toLowerCase());
      }).toList();

  @override
  Widget build(BuildContext context) {
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
                    itemCount: _filtered.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 8),
                    itemBuilder: (_, i) => _UserCard(
                      user: _filtered[i],
                      onEdit: () => _showEditDialog(context, _filtered[i]),
                    ),
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
                  decoration: InputDecoration(
                    hintText: 'Search users...',
                    prefixIcon: const Icon(Icons.search),
                    suffixIcon: _searchQuery.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear),
                            onPressed: () => setState(() => _searchQuery = ''),
                          )
                        : null,
                    contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                  ),
                  onChanged: (v) => setState(() => _searchQuery = v),
                ),
              ),
              const SizedBox(width: 10),
              ElevatedButton.icon(
                icon: const Icon(Icons.person_add_outlined, size: 18),
                label: const Text('Add'),
                onPressed: () => _showAddDialog(context),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Row(
            children: [
              _statChip('Total', '${_users.length}', const Color(0xFF1A237E)),
              const SizedBox(width: 8),
              _statChip('Active', '${_users.where((u) => u.active).length}', const Color(0xFF16A34A)),
              const SizedBox(width: 8),
              _statChip('Inactive', '${_users.where((u) => !u.active).length}', const Color(0xFF6B7280)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _statChip(String label, String value, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: color.withAlpha(20),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: color.withAlpha(51)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text(label, style: TextStyle(color: color, fontSize: 12)),
          const SizedBox(width: 6),
          Text(value, style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 13)),
        ],
      ),
    );
  }

  void _showAddDialog(BuildContext context) {
    _showUserDialog(context, null);
  }

  void _showEditDialog(BuildContext context, _UserEntry user) {
    _showUserDialog(context, user);
  }

  void _showUserDialog(BuildContext context, _UserEntry? existing) {
    final nameCtrl = TextEditingController(text: existing?.fullName ?? '');
    final userCtrl = TextEditingController(text: existing?.username ?? '');
    UserRole selectedRole = existing?.role ?? UserRole.CMO_OFFICER;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setLocalState) => AlertDialog(
          title: Text(existing == null ? 'Add New User' : 'Edit User'),
          content: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Full Name',
                    prefixIcon: Icon(Icons.person_outline),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: userCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Username',
                    prefixIcon: Icon(Icons.account_circle_outlined),
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<UserRole>(
                  value: selectedRole,
                  decoration: const InputDecoration(
                    labelText: 'Role',
                    prefixIcon: Icon(Icons.shield_outlined),
                  ),
                  items: UserRole.values
                      .where((r) => r != UserRole.PUBLIC)
                      .map((r) => DropdownMenuItem(value: r, child: Text(r.displayName)))
                      .toList(),
                  onChanged: (v) => setLocalState(() => selectedRole = v ?? selectedRole),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Cancel'),
            ),
            ElevatedButton(
              onPressed: () {
                Navigator.pop(ctx);
                ScaffoldMessenger.of(context).showSnackBar(
                  SnackBar(
                    content: Text(existing == null
                        ? 'User "${nameCtrl.text}" created (demo)'
                        : 'User "${nameCtrl.text}" updated (demo)'),
                    backgroundColor: const Color(0xFF065F46),
                  ),
                );
              },
              child: Text(existing == null ? 'Create' : 'Save'),
            ),
          ],
        ),
      ),
    );
  }
}

class _UserCard extends StatelessWidget {
  final _UserEntry user;
  final VoidCallback onEdit;
  const _UserCard({required this.user, required this.onEdit});

  Color _roleColor(UserRole role) {
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
    final rc = _roleColor(user.role);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(14),
        child: Row(
          children: [
            CircleAvatar(
              backgroundColor: rc.withAlpha(26),
              child: Text(
                user.fullName[0].toUpperCase(),
                style: TextStyle(color: rc, fontWeight: FontWeight.bold),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          user.fullName,
                          style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 2),
                        decoration: BoxDecoration(
                          color: user.active
                              ? const Color(0xFFD1FAE5)
                              : Colors.grey.withAlpha(40),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          user.active ? 'Active' : 'Inactive',
                          style: TextStyle(
                            color: user.active ? const Color(0xFF065F46) : Colors.grey[600],
                            fontSize: 10,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 3),
                  Text(
                    '@${user.username}',
                    style: TextStyle(color: Colors.grey[500], fontSize: 12),
                  ),
                  const SizedBox(height: 5),
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                        decoration: BoxDecoration(
                          color: rc.withAlpha(20),
                          borderRadius: BorderRadius.circular(4),
                        ),
                        child: Text(
                          user.role.badgeLabel,
                          style: TextStyle(color: rc, fontSize: 10, fontWeight: FontWeight.bold),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Icon(Icons.access_time, size: 11, color: Colors.grey[400]),
                      const SizedBox(width: 3),
                      Text(
                        user.lastLogin,
                        style: TextStyle(color: Colors.grey[400], fontSize: 11),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            IconButton(
              icon: const Icon(Icons.edit_outlined, size: 20),
              color: const Color(0xFF1A237E),
              onPressed: onEdit,
              tooltip: 'Edit',
            ),
          ],
        ),
      ),
    );
  }
}
