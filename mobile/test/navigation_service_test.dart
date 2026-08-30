import 'package:flutter_test/flutter_test.dart';
import 'package:megha_connect/services/navigation_service.dart';

void main() {
  test('pilot appointment report and scanner menu routes are registered', () {
    final navigation = NavigationService();

    for (final route in const [
      AppRoutes.completedAppointments,
      AppRoutes.rejectedAppointments,
      AppRoutes.qrScanner,
    ]) {
      navigation.navigateTo(route);
      expect(navigation.currentRoute, route);
    }
  });
}
