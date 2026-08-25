import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:pdfrx/pdfrx.dart';

class DocumentViewerScreen extends StatelessWidget {
  const DocumentViewerScreen({
    super.key,
    required this.fileName,
    required this.bytes,
  });

  final String fileName;
  final Uint8List bytes;

  bool get _isPdf => fileName.toLowerCase().endsWith('.pdf');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(fileName, overflow: TextOverflow.ellipsis)),
      body: SafeArea(
        child: _isPdf
            ? PdfViewer.data(bytes, sourceName: fileName)
            : InteractiveViewer(
                minScale: 0.5,
                maxScale: 5,
                child: Center(
                  child: Image.memory(
                    bytes,
                    fit: BoxFit.contain,
                    errorBuilder: (_, __, ___) => const Padding(
                      padding: EdgeInsets.all(24),
                      child: Text('Unable to display this image.'),
                    ),
                  ),
                ),
              ),
      ),
    );
  }
}
