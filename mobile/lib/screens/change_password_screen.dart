import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/auth_service.dart';

class ChangePasswordScreen extends StatefulWidget {
  const ChangePasswordScreen({super.key});
  @override
  State<ChangePasswordScreen> createState() => _ChangePasswordScreenState();
}

class _ChangePasswordScreenState extends State<ChangePasswordScreen> {
  final _formKey = GlobalKey<FormState>();
  final _current = TextEditingController();
  final _next = TextEditingController();
  final _confirm = TextEditingController();
  bool _loading = false;
  String? _error;
  @override
  void dispose() {
    _current.dispose();
    _next.dispose();
    _confirm.dispose();
    super.dispose();
  }

  bool _strong(String value) =>
      value.length >= 10 &&
      RegExp(r'[A-Z]').hasMatch(value) &&
      RegExp(r'[a-z]').hasMatch(value) &&
      RegExp(r'\d').hasMatch(value) &&
      RegExp(r'[^A-Za-z0-9]').hasMatch(value);
  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    final auth = context.read<AuthService>();
    final ok = await auth.changeTemporaryPassword(_current.text, _next.text);
    if (mounted && !ok) {
      setState(() {
        _loading = false;
        _error = auth.lastError;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Change temporary password'), actions: [
        IconButton(
            onPressed: () => context.read<AuthService>().logout(),
            icon: const Icon(Icons.logout),
            tooltip: 'Logout')
      ]),
      body: SafeArea(
          child: Center(
              child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: Form(
            key: _formKey,
            child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 440),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                        'A new password is required before you can use MeghaConnect.',
                        style: TextStyle(fontSize: 16)),
                    const SizedBox(height: 20),
                    TextFormField(
                        controller: _current,
                        obscureText: true,
                        decoration: const InputDecoration(
                            labelText: 'Current temporary password'),
                        validator: (v) =>
                            v == null || v.isEmpty ? 'Required' : null),
                    const SizedBox(height: 14),
                    TextFormField(
                        controller: _next,
                        obscureText: true,
                        decoration: const InputDecoration(
                            labelText: 'New password',
                            helperText:
                                '10+ characters with upper, lower, number and special'),
                        validator: (v) => !_strong(v ?? '')
                            ? 'Password does not meet the policy'
                            : null),
                    const SizedBox(height: 14),
                    TextFormField(
                        controller: _confirm,
                        obscureText: true,
                        decoration: const InputDecoration(
                            labelText: 'Confirm new password'),
                        validator: (v) =>
                            v != _next.text ? 'Passwords do not match' : null),
                    if (_error != null)
                      Padding(
                          padding: const EdgeInsets.only(top: 12),
                          child: Text(_error!,
                              style: const TextStyle(color: Colors.red))),
                    const SizedBox(height: 20),
                    ElevatedButton(
                        onPressed: _loading ? null : _submit,
                        child:
                            Text(_loading ? 'Changing…' : 'Change password')),
                  ],
                ))),
      ))),
    );
  }
}
