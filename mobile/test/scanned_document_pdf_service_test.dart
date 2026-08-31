import 'dart:convert';
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart' as img;
import 'package:megha_connect/services/scanned_document_pdf_service.dart';

void main() {
  test('creates one ordered PDF containing every captured page', () async {
    final directory = await Directory.systemTemp.createTemp('megha-scan-test-');
    addTearDown(() => directory.delete(recursive: true));
    final paths = <String>[];
    for (var index = 0; index < 3; index++) {
      final image = img.Image(width: 120 + index, height: 180 + index);
      img.fill(image, color: img.ColorRgb8(240 - index, 240, 240));
      final file =
          File('${directory.path}${Platform.pathSeparator}page-$index.jpg');
      await file.writeAsBytes(img.encodeJpg(image));
      paths.add(file.path);
    }

    final pdf = await ScannedDocumentPdfService.create(paths,
        outputDirectory: directory);
    final bytes = await pdf.readAsBytes();
    final structure = latin1.decode(bytes, allowInvalid: true);

    expect(ascii.decode(bytes.take(8).toList()), '%PDF-1.4');
    expect(structure, contains('/Type /Pages /Count 3'));
    expect(RegExp(r'/Subtype /Image').allMatches(structure).length, 3);
  });

  test('rejects empty and over-limit scans', () async {
    expect(
        () => ScannedDocumentPdfService.create(const []), throwsArgumentError);
    expect(
        () => ScannedDocumentPdfService.create(
            List.filled(ScannedDocumentPdfService.maxPages + 1, 'unused')),
        throwsArgumentError);
  });
}
