import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';
import '../models/user.dart';

class _Grievance {
  final String ticketId;
  final String applicantName;
  final String district;
  final String category;
  final String subject;
  final String description;
  String status;
  final String submittedAt;
  String? assignedDepartment;
  String? remarks;

  _Grievance({
    required this.ticketId,
    required this.applicantName,
    required this.district,
    required this.category,
    required this.subject,
    required this.description,
    required this.status,
    required this.submittedAt,
    this.assignedDepartment,
    this.remarks,
  });
}

final _mockGrievances = <_Grievance>[
  _Grievance(
    ticketId: 'GRV-2024-001',
    applicantName: 'Ramsing Marak',
    district: 'West Garo Hills',
    category: 'Infrastructure',
    subject: 'Road repair request – Dalu village',
    description:
        'The main road connecting Dalu village to the district headquarters has been damaged since the last monsoon.',
    status: 'UNDER_REVIEW',
    submittedAt: '10 Mar 2024',
    assignedDepartment: 'PWD',
    remarks: 'Forwarded to PWD for inspection.',
  ),
  _Grievance(
    ticketId: 'GRV-2024-002',
    applicantName: 'Sunita Sangma',
    district: 'East Khasi Hills',
    category: 'Education',
    subject: 'School infrastructure improvement',
    description: 'Classroom walls are crumbling and there is no proper drinking water facility.',
    status: 'FORWARDED',
    submittedAt: '11 Mar 2024',
    assignedDepartment: 'Education Dept',
  ),
  _Grievance(
    ticketId: 'GRV-2024-003',
    applicantName: 'Bijoy Momin',
    district: 'South Garo Hills',
    category: 'Health',
    subject: 'PHC non-functional for 2 months',
    description: 'The Primary Health Centre at Baghmara Block has been without a doctor for 2 months.',
    status: 'RESOLVED',
    submittedAt: '20 Feb 2024',
    assignedDepartment: 'Health Dept',
    remarks: 'Doctor posted. PHC operational.',
  ),
  _Grievance(
    ticketId: 'GRV-2024-004',
    applicantName: 'Deibok Lyngdoh',
    district: 'Ri Bhoi',
    category: 'Welfare Scheme',
    subject: 'Delay in CM Care disbursement',
    description: 'My CM Care application was approved 3 months ago but the amount has not been credited.',
    status: 'SUBMITTED',
    submittedAt: '14 Mar 2024',
  ),
];

Color _statusColor(String status) {
  switch (status) {
    case 'SUBMITTED':
      return const Color(0xFF1565C0);
    case 'ACKNOWLEDGED':
      return const Color(0xFF0288D1);
    case 'UNDER_REVIEW':
      return const Color(0xFFB45309);
    case 'FORWARDED':
      return const Color(0xFF7C3AED);
    case 'RESOLVED':
      return const Color(0xFF065F46);
    case 'CLOSED':
      return const Color(0xFF6B7280);
    default:
      return const Color(0xFF6B7280);
  }
}

class GrievanceScreen extends StatefulWidget {
  const GrievanceScreen({super.key});

  @override
  State<GrievanceScreen> createState() => _GrievanceScreenState();
}

class _GrievanceScreenState extends State<GrievanceScreen> {
  final List<_Grievance> _grievances = List.from(_mockGrievances);
  String _search = '';
  String _filterStatus = '';

  List<_Grievance> get _filtered => _grievances.where((g) {
        final matchSearch = _search.isEmpty ||
            g.applicantName.toLowerCase().contains(_search.toLowerCase()) ||
            g.ticketId.toLowerCase().contains(_search.toLowerCase()) ||
            g.subject.toLowerCase().contains(_search.toLowerCase());
        final matchStatus = _filterStatus.isEmpty || g.status == _filterStatus;
        return matchSearch && matchStatus;
      }).toList();

  @override
  Widget build(BuildContext context) {
    final auth = context.watch<AuthService>();
    final isPublic = auth.user?.role == UserRole.PUBLIC;

    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        title: const Text('Grievances'),
        actions: [
          IconButton(
            icon: const Icon(Icons.add),
            tooltip: 'Raise Grievance',
            onPressed: () => _showNewGrievanceForm(context),
          ),
        ],
      ),
      body: Column(
        children: [
          // Search + filter bar
          Container(
            color: Colors.white,
            padding: const EdgeInsets.all(12),
            child: Column(
              children: [
                TextField(
                  decoration: InputDecoration(
                    hintText: 'Search by name, ticket ID, subject…',
                    prefixIcon: const Icon(Icons.search, size: 20),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                    isDense: true,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  onChanged: (v) => setState(() => _search = v),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    const Text('Status: ', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
                    const SizedBox(width: 8),
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        value: _filterStatus.isEmpty ? null : _filterStatus,
                        decoration: InputDecoration(
                          contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 8),
                          isDense: true,
                          border: OutlineInputBorder(borderRadius: BorderRadius.circular(6)),
                        ),
                        hint: const Text('All Statuses', style: TextStyle(fontSize: 13)),
                        items: const [
                          DropdownMenuItem(value: '', child: Text('All Statuses')),
                          DropdownMenuItem(value: 'SUBMITTED', child: Text('Submitted')),
                          DropdownMenuItem(value: 'ACKNOWLEDGED', child: Text('Acknowledged')),
                          DropdownMenuItem(value: 'UNDER_REVIEW', child: Text('Under Review')),
                          DropdownMenuItem(value: 'FORWARDED', child: Text('Forwarded')),
                          DropdownMenuItem(value: 'RESOLVED', child: Text('Resolved')),
                          DropdownMenuItem(value: 'CLOSED', child: Text('Closed')),
                        ],
                        onChanged: (v) => setState(() => _filterStatus = v ?? ''),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
          // List
          Expanded(
            child: RefreshIndicator(
              onRefresh: () async {
                await Future.delayed(const Duration(milliseconds: 600));
              },
              child: _filtered.isEmpty
                  ? const Center(
                      child: Text('No grievances found.', style: TextStyle(color: Colors.grey)),
                    )
                  : ListView.builder(
                      padding: const EdgeInsets.all(12),
                      itemCount: _filtered.length,
                      itemBuilder: (ctx, i) => _GrievanceCard(
                        grievance: _filtered[i],
                        isStaff: !isPublic,
                        onTap: () => _showDetail(context, _filtered[i]),
                      ),
                    ),
            ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showNewGrievanceForm(context),
        icon: const Icon(Icons.add_comment_outlined),
        label: const Text('Raise Grievance'),
        backgroundColor: const Color(0xFF1A237E),
        foregroundColor: Colors.white,
      ),
    );
  }

  void _showDetail(BuildContext context, _Grievance g) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (_) => DraggableScrollableSheet(
        expand: false,
        initialChildSize: 0.75,
        maxChildSize: 0.95,
        builder: (_, controller) => _GrievanceDetailSheet(
          grievance: g,
          scrollController: controller,
          onStatusUpdate: (newStatus) {
            setState(() => g.status = newStatus);
            Navigator.pop(context);
          },
        ),
      ),
    );
  }

  void _showNewGrievanceForm(BuildContext context) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (_) => Padding(
        padding: EdgeInsets.only(
          bottom: MediaQuery.of(context).viewInsets.bottom,
        ),
        child: _NewGrievanceForm(
          onSubmit: (g) {
            setState(() => _grievances.insert(0, g));
            Navigator.pop(context);
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text('Grievance submitted! Ticket: ${g.ticketId}'),
                backgroundColor: const Color(0xFF065F46),
              ),
            );
          },
        ),
      ),
    );
  }
}

class _GrievanceCard extends StatelessWidget {
  final _Grievance grievance;
  final bool isStaff;
  final VoidCallback onTap;

  const _GrievanceCard({
    required this.grievance,
    required this.isStaff,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: const EdgeInsets.only(bottom: 10),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    grievance.ticketId,
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 12,
                      fontWeight: FontWeight.bold,
                      color: Color(0xFF1A237E),
                    ),
                  ),
                  const Spacer(),
                  _StatusBadge(status: grievance.status),
                ],
              ),
              const SizedBox(height: 6),
              Text(
                grievance.subject,
                style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 14),
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  Icon(Icons.person_outline, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(grievance.applicantName, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                  const SizedBox(width: 12),
                  Icon(Icons.location_on_outlined, size: 14, color: Colors.grey[600]),
                  const SizedBox(width: 4),
                  Text(grievance.district, style: TextStyle(fontSize: 12, color: Colors.grey[600])),
                ],
              ),
              const SizedBox(height: 6),
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: const Color(0xFFE8EAF6),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Text(
                      grievance.category,
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600, color: Color(0xFF1A237E)),
                    ),
                  ),
                  const Spacer(),
                  Text(grievance.submittedAt, style: TextStyle(fontSize: 11, color: Colors.grey[500])),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String status;
  const _StatusBadge({required this.status});

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: _statusColor(status).withOpacity(0.12),
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: _statusColor(status).withOpacity(0.3)),
      ),
      child: Text(
        status.replaceAll('_', ' '),
        style: TextStyle(fontSize: 11, fontWeight: FontWeight.w700, color: _statusColor(status)),
      ),
    );
  }
}

class _GrievanceDetailSheet extends StatelessWidget {
  final _Grievance grievance;
  final ScrollController scrollController;
  final void Function(String) onStatusUpdate;

  const _GrievanceDetailSheet({
    required this.grievance,
    required this.scrollController,
    required this.onStatusUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: ListView(
        controller: scrollController,
        padding: const EdgeInsets.all(20),
        children: [
          // Handle
          Center(
            child: Container(
              width: 36, height: 4,
              margin: const EdgeInsets.only(bottom: 16),
              decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
            ),
          ),
          Row(
            children: [
              Expanded(
                child: Text(
                  grievance.ticketId,
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.bold,
                    fontSize: 13,
                    color: Color(0xFF1A237E),
                  ),
                ),
              ),
              _StatusBadge(status: grievance.status),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            grievance.subject,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 17),
          ),
          const SizedBox(height: 16),
          _DetailRow(label: 'Applicant', value: grievance.applicantName),
          _DetailRow(label: 'District', value: grievance.district),
          _DetailRow(label: 'Category', value: grievance.category),
          _DetailRow(label: 'Submitted', value: grievance.submittedAt),
          if (grievance.assignedDepartment != null)
            _DetailRow(label: 'Assigned To', value: grievance.assignedDepartment!),
          const Divider(height: 24),
          const Text('Description', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: Colors.grey)),
          const SizedBox(height: 6),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.grey[50],
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: Colors.grey[200]!),
            ),
            child: Text(grievance.description, style: const TextStyle(fontSize: 14, height: 1.6)),
          ),
          if (grievance.remarks != null) ...[
            const SizedBox(height: 12),
            const Text('Remarks', style: TextStyle(fontSize: 12, fontWeight: FontWeight.w700, color: Colors.grey)),
            const SizedBox(height: 6),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: const Color(0xFFF0FDF4),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(grievance.remarks!, style: const TextStyle(fontSize: 13, color: Color(0xFF065F46))),
            ),
          ],
          if (grievance.status != 'RESOLVED' && grievance.status != 'CLOSED') ...[
            const Divider(height: 24),
            const Text(
              'Update Status',
              style: TextStyle(fontSize: 13, fontWeight: FontWeight.w700, color: Color(0xFF1A237E)),
            ),
            const SizedBox(height: 10),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                if (grievance.status == 'SUBMITTED')
                  _ActionChip(label: 'Acknowledge', onTap: () => onStatusUpdate('ACKNOWLEDGED')),
                _ActionChip(label: 'Forward to Dept', onTap: () => onStatusUpdate('FORWARDED'), outline: true),
                _ActionChip(label: 'Mark Resolved', onTap: () => onStatusUpdate('RESOLVED'), green: true),
              ],
            ),
          ],
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

class _DetailRow extends StatelessWidget {
  final String label;
  final String value;
  const _DetailRow({required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 110,
            child: Text(label, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: Colors.grey)),
          ),
          Expanded(child: Text(value, style: const TextStyle(fontSize: 14))),
        ],
      ),
    );
  }
}

class _ActionChip extends StatelessWidget {
  final String label;
  final VoidCallback onTap;
  final bool outline;
  final bool green;

  const _ActionChip({required this.label, required this.onTap, this.outline = false, this.green = false});

  @override
  Widget build(BuildContext context) {
    final color = green ? const Color(0xFF065F46) : const Color(0xFF1A237E);
    return OutlinedButton(
      onPressed: onTap,
      style: OutlinedButton.styleFrom(
        foregroundColor: color,
        side: BorderSide(color: color),
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      ),
      child: Text(label, style: const TextStyle(fontSize: 13)),
    );
  }
}

class _NewGrievanceForm extends StatefulWidget {
  final void Function(_Grievance) onSubmit;
  const _NewGrievanceForm({required this.onSubmit});

  @override
  State<_NewGrievanceForm> createState() => _NewGrievanceFormState();
}

class _NewGrievanceFormState extends State<_NewGrievanceForm> {
  int _step = 0;
  final _nameCtrl = TextEditingController();
  final _phoneCtrl = TextEditingController();
  String? _district;
  final _constituencyCtrl = TextEditingController();
  String? _category;
  final _subjectCtrl = TextEditingController();
  final _descCtrl = TextEditingController();

  static const _districts = [
    'East Khasi Hills', 'West Khasi Hills', 'Ri Bhoi',
    'East Jaintia Hills', 'West Jaintia Hills', 'East Garo Hills',
    'West Garo Hills', 'South Garo Hills', 'North Garo Hills',
  ];
  static const _categories = [
    'Public Services', 'Infrastructure', 'Health', 'Education',
    'Employment', 'Welfare Scheme', 'Law & Order', 'Others',
  ];

  @override
  void dispose() {
    _nameCtrl.dispose(); _phoneCtrl.dispose();
    _constituencyCtrl.dispose(); _subjectCtrl.dispose(); _descCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Center(
            child: Container(
              width: 36, height: 4,
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(2)),
            ),
          ),
          Text(
            'Raise a Grievance – Step ${_step + 1} of 3',
            style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFF1A237E)),
          ),
          const SizedBox(height: 16),
          if (_step == 0) ...[
            _field('Full Name *', _nameCtrl, 'Enter your full name'),
            const SizedBox(height: 12),
            _field('Mobile Number *', _phoneCtrl, '10-digit mobile number', keyboard: TextInputType.phone),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              value: _district,
              decoration: const InputDecoration(labelText: 'District *', isDense: true),
              items: _districts.map((d) => DropdownMenuItem(value: d, child: Text(d))).toList(),
              onChanged: (v) => setState(() => _district = v),
            ),
            const SizedBox(height: 12),
            _field('Constituency', _constituencyCtrl, 'Enter constituency'),
          ] else if (_step == 1) ...[
            DropdownButtonFormField<String>(
              value: _category,
              decoration: const InputDecoration(labelText: 'Category *', isDense: true),
              items: _categories.map((c) => DropdownMenuItem(value: c, child: Text(c))).toList(),
              onChanged: (v) => setState(() => _category = v),
            ),
            const SizedBox(height: 12),
            _field('Subject *', _subjectCtrl, 'Brief subject of grievance'),
            const SizedBox(height: 12),
            TextField(
              controller: _descCtrl,
              maxLines: 4,
              decoration: InputDecoration(
                labelText: 'Description *',
                hintText: 'Provide detailed description…',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                alignLabelWithHint: true,
              ),
            ),
          ] else ...[
            _reviewRow('Name', _nameCtrl.text),
            _reviewRow('Mobile', _phoneCtrl.text),
            _reviewRow('District', _district ?? '–'),
            _reviewRow('Category', _category ?? '–'),
            _reviewRow('Subject', _subjectCtrl.text),
            const Divider(height: 16),
            const Text('Description', style: TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w600)),
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.all(10),
              decoration: BoxDecoration(
                color: Colors.grey[50],
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: Colors.grey[200]!),
              ),
              child: Text(_descCtrl.text, style: const TextStyle(fontSize: 13)),
            ),
          ],
          const SizedBox(height: 20),
          Row(
            children: [
              if (_step > 0)
                OutlinedButton.icon(
                  onPressed: () => setState(() => _step--),
                  icon: const Icon(Icons.chevron_left),
                  label: const Text('Back'),
                ),
              const Spacer(),
              if (_step < 2)
                ElevatedButton.icon(
                  onPressed: () => setState(() => _step++),
                  icon: const Icon(Icons.chevron_right),
                  label: const Text('Next'),
                )
              else
                ElevatedButton.icon(
                  onPressed: _submit,
                  icon: const Icon(Icons.send),
                  label: const Text('Submit'),
                  style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF065F46)),
                ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _field(String label, TextEditingController ctrl, String hint, {TextInputType? keyboard}) {
    return TextField(
      controller: ctrl,
      keyboardType: keyboard,
      decoration: InputDecoration(
        labelText: label,
        hintText: hint,
        isDense: true,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
      ),
    );
  }

  Widget _reviewRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(width: 90, child: Text(label, style: const TextStyle(fontSize: 12, color: Colors.grey, fontWeight: FontWeight.w600))),
          Expanded(child: Text(value, style: const TextStyle(fontSize: 13))),
        ],
      ),
    );
  }

  void _submit() {
    final count = _mockGrievances.length + 1;
    final g = _Grievance(
      ticketId: 'GRV-2024-${count.toString().padLeft(3, '0')}',
      applicantName: _nameCtrl.text,
      district: _district ?? '',
      category: _category ?? 'Others',
      subject: _subjectCtrl.text,
      description: _descCtrl.text,
      status: 'SUBMITTED',
      submittedAt: 'Today',
    );
    widget.onSubmit(g);
  }
}
