import '../models/qr_action_result.dart';
import '../models/recent_scan.dart';
import '../models/visitor_details.dart';

class RecentScanStore {
  static const int _limit = 30;

  final List<RecentScan> _items = <RecentScan>[];

  List<RecentScan> get items => List<RecentScan>.unmodifiable(_items);

  void recordValidation(VisitorDetails details) {
    _add(RecentScan.fromValidation(details));
  }

  void recordAction(VisitorDetails details, QrActionResult action) {
    _add(RecentScan.fromAction(details, action));
  }

  void replaceFromBackend(List<RecentScan> scans) {
    _items
      ..clear()
      ..addAll(scans.take(_limit));
  }

  void clear() {
    _items.clear();
  }

  void _add(RecentScan scan) {
    _items.insert(0, scan);
    if (_items.length > _limit) {
      _items.removeRange(_limit, _items.length);
    }
  }
}
