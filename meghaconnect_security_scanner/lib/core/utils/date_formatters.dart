import 'package:intl/intl.dart';

class DateFormatters {
  const DateFormatters._();

  static final DateFormat _dateTime = DateFormat('dd MMM yyyy, hh:mm a');

  static String dateTime(DateTime? value) {
    if (value == null) {
      return '-';
    }
    return _dateTime.format(value.toLocal());
  }
}
