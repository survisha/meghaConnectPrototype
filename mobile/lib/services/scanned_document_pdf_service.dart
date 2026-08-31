import 'dart:convert';
import 'dart:io';
import 'dart:typed_data';

import 'package:image/image.dart' as img;
import 'package:path_provider/path_provider.dart';

class ScannedDocumentPdfService {
  static const int maxPages = 10;
  static const int _maxImageDimension = 1800;

  static Future<File> create(List<String> imagePaths,
      {Directory? outputDirectory}) async {
    if (imagePaths.isEmpty || imagePaths.length > maxPages) {
      throw ArgumentError('A scanned document must contain 1-$maxPages pages.');
    }
    final pages = <_PdfImage>[];
    for (final path in imagePaths) {
      final decoded = img.decodeImage(await File(path).readAsBytes());
      if (decoded == null) {
        throw StateError('A captured page could not be decoded.');
      }
      var page = img.bakeOrientation(decoded);
      if (page.width > _maxImageDimension || page.height > _maxImageDimension) {
        page = img.copyResize(page,
            width: page.width >= page.height ? _maxImageDimension : null,
            height: page.height > page.width ? _maxImageDimension : null,
            interpolation: img.Interpolation.linear);
      }
      pages.add(_PdfImage(Uint8List.fromList(img.encodeJpg(page, quality: 82)),
          page.width, page.height));
    }
    final directory = outputDirectory ?? await getTemporaryDirectory();
    final file = File(
        '${directory.path}${Platform.pathSeparator}appointment-document-${DateTime.now().millisecondsSinceEpoch}.pdf');
    await file.writeAsBytes(_buildPdf(pages), flush: true);
    if (await file.length() == 0) throw StateError('Generated PDF is empty.');
    return file;
  }

  static Uint8List _buildPdf(List<_PdfImage> pages) {
    final objectCount = 2 + pages.length * 3;
    final objects = List<Uint8List?>.filled(objectCount + 1, null);
    objects[1] = _ascii('<< /Type /Catalog /Pages 2 0 R >>');
    final pageIds = <int>[];
    for (var i = 0; i < pages.length; i++) {
      final pageId = 3 + i * 3;
      final contentId = pageId + 1;
      final imageId = pageId + 2;
      pageIds.add(pageId);
      final image = pages[i];
      final landscape = image.width > image.height;
      final pageWidth = landscape ? 841.89 : 595.28;
      final pageHeight = landscape ? 595.28 : 841.89;
      const margin = 24.0;
      final scale = ((pageWidth - margin * 2) / image.width)
          .clamp(0.0, (pageHeight - margin * 2) / image.height);
      final drawWidth = image.width * scale;
      final drawHeight = image.height * scale;
      final x = (pageWidth - drawWidth) / 2;
      final y = (pageHeight - drawHeight) / 2;
      final imageName = 'Im${i + 1}';
      final content =
          'q\n${drawWidth.toStringAsFixed(2)} 0 0 ${drawHeight.toStringAsFixed(2)} ${x.toStringAsFixed(2)} ${y.toStringAsFixed(2)} cm\n/$imageName Do\nQ\n';
      objects[pageId] = _ascii(
          '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${pageWidth.toStringAsFixed(2)} ${pageHeight.toStringAsFixed(2)}] /Resources << /XObject << /$imageName $imageId 0 R >> >> /Contents $contentId 0 R >>');
      objects[contentId] = _stream(_ascii(content), '');
      objects[imageId] = _stream(image.bytes,
          '/Type /XObject /Subtype /Image /Width ${image.width} /Height ${image.height} /ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode ');
    }
    objects[2] = _ascii(
        '<< /Type /Pages /Count ${pages.length} /Kids [${pageIds.map((id) => '$id 0 R').join(' ')}] >>');

    final output = BytesBuilder(copy: false)..add(_ascii('%PDF-1.4\n%âãÏÓ\n'));
    final offsets = List<int>.filled(objectCount + 1, 0);
    for (var id = 1; id <= objectCount; id++) {
      offsets[id] = output.length;
      output
        ..add(_ascii('$id 0 obj\n'))
        ..add(objects[id]!)
        ..add(_ascii('\nendobj\n'));
    }
    final xref = output.length;
    output.add(_ascii('xref\n0 ${objectCount + 1}\n0000000000 65535 f \n'));
    for (var id = 1; id <= objectCount; id++) {
      output
          .add(_ascii('${offsets[id].toString().padLeft(10, '0')} 00000 n \n'));
    }
    output.add(_ascii(
        'trailer\n<< /Size ${objectCount + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n'));
    return output.takeBytes();
  }

  static Uint8List _stream(Uint8List bytes, String dictionary) {
    final builder = BytesBuilder(copy: false)
      ..add(_ascii('<< $dictionary/Length ${bytes.length} >>\nstream\n'))
      ..add(bytes)
      ..add(_ascii('\nendstream'));
    return builder.takeBytes();
  }

  static Uint8List _ascii(String value) =>
      Uint8List.fromList(latin1.encode(value));
}

class _PdfImage {
  final Uint8List bytes;
  final int width;
  final int height;
  const _PdfImage(this.bytes, this.width, this.height);
}
