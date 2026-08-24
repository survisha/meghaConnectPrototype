package com.survisha.meghaconnect.legacy.controller;

import com.survisha.meghaconnect.legacy.dto.LegacyImportDtos.*;
import com.survisha.meghaconnect.legacy.service.LegacyImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/v1/legacy-import")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','DEO')")
public class LegacyImportController {
    private final LegacyImportService service;

    @PostMapping(value="/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public BatchSummary upload(@RequestParam("file") MultipartFile file, Authentication auth){return service.uploadAndAnalyze(file,auth.getName());}
    @GetMapping("/{batchId}") public BatchSummary batch(@PathVariable Long batchId,Authentication auth){return service.getBatch(batchId,auth.getName(),admin(auth));}
    @GetMapping("/{batchId}/sheets") public List<SheetSummary> sheets(@PathVariable Long batchId,Authentication auth){return service.sheets(batchId,auth.getName(),admin(auth));}
    @GetMapping("/{batchId}/sheets/{sheetId}") public SheetSummary sheet(@PathVariable Long batchId,@PathVariable Long sheetId,Authentication auth){return service.sheets(batchId,auth.getName(),admin(auth)).stream().filter(s->s.getId().equals(sheetId)).findFirst().orElseThrow();}
    @GetMapping("/{batchId}/sheets/{sheetId}/preview") public Preview preview(@PathVariable Long batchId,@PathVariable Long sheetId,@RequestParam(defaultValue="20") int limit,Authentication auth){return service.preview(batchId,sheetId,limit,auth.getName(),admin(auth));}
    @GetMapping("/{batchId}/sheets/{sheetId}/columns") public List<ColumnInfo> columns(@PathVariable Long batchId,@PathVariable Long sheetId,Authentication auth){return sheet(batchId,sheetId,auth).getColumns();}
    @PostMapping("/{batchId}/sheets/{sheetId}/mapping") public SheetSummary mapping(@PathVariable Long batchId,@PathVariable Long sheetId,@RequestBody MappingRequest request,Authentication auth){return service.applyMapping(batchId,sheetId,request,auth.getName(),admin(auth));}
    @PostMapping("/{batchId}/validate") public BatchSummary validate(@PathVariable Long batchId,Authentication auth){return service.validate(batchId,auth.getName(),admin(auth));}
    @PostMapping("/{batchId}/execute") public BatchSummary execute(@PathVariable Long batchId,Authentication auth){return service.execute(batchId,auth.getName(),admin(auth));}
    @PostMapping("/{batchId}/sheets/{sheetId}/retry") public SheetSummary retry(@PathVariable Long batchId,@PathVariable Long sheetId,Authentication auth){return service.retry(batchId,sheetId,auth.getName(),admin(auth));}
    @PostMapping("/{batchId}/sheets/{sheetId}/skip") public SheetSummary skip(@PathVariable Long batchId,@PathVariable Long sheetId,Authentication auth){return service.skip(batchId,sheetId,auth.getName(),admin(auth));}
    @GetMapping("/{batchId}/errors") public Page<ErrorInfo> errors(@PathVariable Long batchId,@PageableDefault(size=50) Pageable pageable,Authentication auth){return service.errors(batchId,pageable,auth.getName(),admin(auth));}
    @GetMapping(value="/{batchId}/errors/export",produces="text/csv") public ResponseEntity<byte[]> errorExport(@PathVariable Long batchId,Authentication auth){return download(service.errorCsv(batchId,auth.getName(),admin(auth)),"legacy-import-errors-"+batchId+".csv");}
    @GetMapping(value="/{batchId}/summary/export",produces="text/csv") public ResponseEntity<byte[]> summaryExport(@PathVariable Long batchId,Authentication auth){return download(service.summaryCsv(batchId,auth.getName(),admin(auth)),"legacy-import-summary-"+batchId+".csv");}
    @GetMapping("/history") public Page<BatchSummary> history(@PageableDefault(size=20,sort="uploadedAt",direction=Sort.Direction.DESC) Pageable pageable,Authentication auth){return service.history(pageable,auth.getName(),admin(auth));}
    private boolean admin(Authentication auth){return auth.getAuthorities().stream().anyMatch(a->"ROLE_ADMIN".equals(a.getAuthority()));}
    private ResponseEntity<byte[]> download(byte[] bytes,String name){return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(bytes);}
}
