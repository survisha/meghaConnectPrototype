package com.survisha.meghaconnect.legacy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.legacy.dto.LegacyImportDtos.*;
import com.survisha.meghaconnect.legacy.entity.*;
import com.survisha.meghaconnect.legacy.repository.*;
import com.survisha.meghaconnect.service.AuditLogService;
import com.survisha.meghaconnect.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegacyImportService {
    private static final Set<String> EXCEL_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel", "application/octet-stream");
    private static final Set<String> TYPES = Set.of("STRING", "INTEGER", "DECIMAL", "DATE", "BOOLEAN");
    private static final Set<String> IDENTIFIERS = Set.of("EPIC", "NAME", "MOBILE", "VILLAGE", "DISTRICT", "CONSTITUENCY", "SCHEME", "ADDRESS", "OTHER");
    private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{2,79}");
    private static final Pattern SAFE_FIELD = Pattern.compile("[a-z][a-z0-9_]{1,79}");
    private static final int PREVIEW_LIMIT = 20;

    private final LegacyDatasetDefinitionRepository datasetRepository;
    private final LegacyImportBatchRepository batchRepository;
    private final LegacyImportSheetRepository sheetRepository;
    private final LegacyImportColumnRepository columnRepository;
    private final LegacyImportErrorRepository errorRepository;
    private final LegacyDatasetRecordRepository recordRepository;
    private final LegacyPersonIndexRepository personRepository;
    private final AuditLogService audit;
    private final ObjectMapper objectMapper;
    @PersistenceContext(unitName="legacy")
    private EntityManager entityManager;

    @Value("${legacy.import.storage-path:${java.io.tmpdir}/meghaconnect-legacy-import}") private String storagePath;
    @Value("${legacy.import.max-file-size-mb:100}") private long maxFileSizeMb;
    @Value("${legacy.import.max-sheets:100}") private int maxSheets;
    @Value("${legacy.import.max-columns:300}") private int maxColumns;
    @Value("${legacy.import.max-rows-per-sheet:1000000}") private long maxRowsPerSheet;
    @Value("${legacy.import.batch-size:500}") private int chunkSize;

    @Transactional(transactionManager="legacyTransactionManager")
    public BatchSummary uploadAndAnalyze(MultipartFile file, String actor) {
        validateUpload(file);
        String original = sanitizeFilename(file.getOriginalFilename());
        Path stored = store(file);
        LegacyImportBatch batch = batchRepository.save(LegacyImportBatch.builder()
                .originalFileName(original).storedFileName(stored.getFileName().toString())
                .fileHash(sha256(stored)).uploadedBy(actor).uploadedAt(now()).overallStatus("ANALYZING").build());
        audit.log("LegacyImportBatch", batch.getId(), "LEGACY_FILE_UPLOADED", "Workbook uploaded: " + original, actor);
        try (InputStream in = Files.newInputStream(stored); Workbook workbook = WorkbookFactory.create(in)) {
            if (workbook.getNumberOfSheets() > maxSheets) throw new IllegalArgumentException("Workbook exceeds the configured sheet limit.");
            List<LegacyDatasetDefinition> datasets = datasetRepository.findByActiveTrueAndApprovedTrueOrderByDatasetNameAsc();
            long totalRows = 0;
            int mappingRequired = 0, skipped = 0;
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet poiSheet = workbook.getSheetAt(i);
                boolean hidden = workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i);
                LegacyImportSheet sheet = LegacyImportSheet.builder().batch(batch).sheetIndex(i).sheetName(safeSheetName(poiSheet.getSheetName()))
                        .hidden(hidden).status("DETECTED").build();
                if (isEmpty(poiSheet)) {
                    sheet.setStatus("SKIPPED"); sheet.setStatusReason("EMPTY_SHEET"); skipped++;
                    sheetRepository.save(sheet); continue;
                }
                Header header = detectHeader(poiSheet, datasets);
                if (header == null) {
                    sheet.setStatus("MAPPING_REQUIRED"); sheet.setStatusReason("HEADER_NOT_DETECTED"); mappingRequired++;
                    sheetRepository.save(sheet); continue;
                }
                if (header.values.size() > maxColumns) throw new IllegalArgumentException("Sheet exceeds configured column limit: " + poiSheet.getSheetName());
                long rows = countDataRows(poiSheet, header.rowIndex);
                if (rows > maxRowsPerSheet) throw new IllegalArgumentException("Sheet exceeds configured row limit: " + poiSheet.getSheetName());
                sheet.setDetectedHeaderRow(header.rowIndex + 1); sheet.setTotalColumns(header.values.size()); sheet.setTotalRows(rows);
                Match match = matchDataset(header.values, datasets);
                if (match.dataset != null) {
                    sheet.setDetectedDataset(match.dataset); sheet.setMappingConfidence(BigDecimal.valueOf(match.score).setScale(2, RoundingMode.HALF_UP));
                    sheet.setTargetTable(match.dataset.getTargetTable());
                }
                if (hidden) {
                    sheet.setStatus("MAPPING_REQUIRED"); sheet.setStatusReason("HIDDEN_SHEET_REQUIRES_REVIEW"); mappingRequired++;
                } else if (match.dataset == null || match.score < 60) {
                    sheet.setStatus("MAPPING_REQUIRED"); sheet.setStatusReason("NO_CONFIDENT_DATASET_MATCH"); mappingRequired++;
                } else {
                    sheet.setStatus(match.score >= 85 ? "READY" : "MAPPING_REQUIRED");
                    if (match.score < 85) { sheet.setStatusReason("DATASET_CONFIRMATION_REQUIRED"); mappingRequired++; }
                    else sheet.setConfirmedDataset(match.dataset);
                }
                sheetRepository.save(sheet);
                persistColumns(sheet, poiSheet, header, match.dataset);
                totalRows += rows;
            }
            batch.setTotalSheets(workbook.getNumberOfSheets()); batch.setAnalyzedSheets(workbook.getNumberOfSheets());
            batch.setSkippedSheets(skipped); batch.setMappingRequiredSheets(mappingRequired); batch.setTotalRows(totalRows);
            batch.setOverallStatus(mappingRequired > 0 ? "READY_FOR_MAPPING" : "READY_FOR_MAPPING");
            batchRepository.save(batch);
            audit.log("LegacyImportBatch", batch.getId(), "LEGACY_WORKBOOK_ANALYZED", "Sheets=" + batch.getTotalSheets() + ", rows=" + totalRows, actor);
            return summary(batch, true);
        } catch (Exception e) {
            batch.setOverallStatus("FAILED"); batch.setCompletedAt(now()); batchRepository.save(batch);
            throw e instanceof RuntimeException ? (RuntimeException)e : new IllegalArgumentException("Unable to analyze workbook.", e);
        }
    }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public BatchSummary getBatch(Long id, String actor, boolean admin) { return summary(ownedBatch(id, actor, admin), true); }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public List<SheetSummary> sheets(Long id, String actor, boolean admin) {
        ownedBatch(id, actor, admin); return sheetRepository.findByBatchIdOrderBySheetIndex(id).stream().map(s -> sheetDto(s, true)).collect(Collectors.toList());
    }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public Preview preview(Long batchId, Long sheetId, int requestedLimit, String actor, boolean admin) {
        LegacyImportBatch batch = ownedBatch(batchId, actor, admin); LegacyImportSheet sheet = ownedSheet(batchId, sheetId);
        int limit = Math.min(Math.max(1, requestedLimit), PREVIEW_LIMIT);
        try (InputStream in=Files.newInputStream(resolveStored(batch)); Workbook wb=WorkbookFactory.create(in)) {
            Sheet ps=wb.getSheetAt(sheet.getSheetIndex()); int header=(sheet.getDetectedHeaderRow()==null ? 1 : sheet.getDetectedHeaderRow())-1;
            Row hr=ps.getRow(header); List<String> columns=readRow(hr, lastCell(hr)); List<List<String>> rows=new ArrayList<>();
            for(int r=header+1;r<=ps.getLastRowNum() && rows.size()<limit;r++) if(!empty(ps.getRow(r))) rows.add(readRow(ps.getRow(r),columns.size()));
            return Preview.builder().sheetId(sheetId).sheetName(sheet.getSheetName()).totalRows(sheet.getTotalRows()).columns(columns).rows(rows).build();
        } catch(IOException e){ throw new IllegalStateException("Unable to preview workbook.",e); }
    }

    @Transactional(transactionManager="legacyTransactionManager")
    public SheetSummary applyMapping(Long batchId, Long sheetId, MappingRequest request, String actor, boolean admin) {
        ownedBatch(batchId, actor, admin); LegacyImportSheet sheet=ownedSheet(batchId,sheetId);
        LegacyDatasetDefinition dataset=datasetRepository.findById(request.getDatasetId()).filter(d->d.isActive()&&d.isApproved())
                .orElseThrow(()->new IllegalArgumentException("Approved dataset definition not found."));
        if(request.getHeaderRow()!=null && request.getHeaderRow()>0) sheet.setDetectedHeaderRow(request.getHeaderRow());
        Map<String,LegacyDatasetColumn> targets=dataset.getColumns().stream().filter(LegacyDatasetColumn::isActive).collect(Collectors.toMap(LegacyDatasetColumn::getTargetFieldName,c->c));
        Map<Integer,ColumnMapping> incoming=request.getColumns()==null?Map.of():request.getColumns().stream().collect(Collectors.toMap(ColumnMapping::getSourceColumnIndex,c->c,(a,b)->b));
        for(LegacyImportColumn c:columnRepository.findBySheetIdOrderBySourceColumnIndex(sheetId)){
            ColumnMapping m=incoming.get(c.getSourceColumnIndex()); if(m==null) continue;
            if(Boolean.TRUE.equals(m.getIgnored())){c.setIgnored(true);c.setMappedTargetField(null);c.setMappedIdentifierType(null);c.setMappingStatus("IGNORED");}
            else {LegacyDatasetColumn target=targets.get(m.getTargetField());if(target==null)throw new IllegalArgumentException("Target field is not allowed by the selected dataset: "+m.getTargetField());
                c.setIgnored(false);c.setMappedTargetField(target.getTargetFieldName());c.setMappedIdentifierType(target.getIdentifierType());c.setMandatory(target.isMandatory());c.setMappingStatus("MANUAL_MAPPING");}
            columnRepository.save(c);
        }
        ensureMandatoryMapped(dataset,columnRepository.findBySheetIdOrderBySourceColumnIndex(sheetId));
        sheet.setConfirmedDataset(dataset);sheet.setTargetTable(dataset.getTargetTable());sheet.setStatus("READY");sheet.setStatusReason(null);sheetRepository.save(sheet);
        recalculateBatch(batchId); audit.log("LegacyImportSheet",sheetId,"LEGACY_SHEET_MAPPED","Batch="+batchId+", sheet="+sheet.getSheetName()+", dataset="+dataset.getDatasetCode(),actor);
        return sheetDto(sheet,true);
    }

    @Transactional(transactionManager="legacyTransactionManager")
    public BatchSummary validate(Long batchId,String actor,boolean admin){
        LegacyImportBatch batch=ownedBatch(batchId,actor,admin);batch.setOverallStatus("VALIDATING");batchRepository.save(batch);
        for(LegacyImportSheet sheet:sheetRepository.findByBatchIdOrderBySheetIndex(batchId)){
            if(!"READY".equals(sheet.getStatus())&&!"PARTIAL_SUCCESS".equals(sheet.getStatus())&&!"FAILED".equals(sheet.getStatus()))continue;
            validateSheet(batch,sheet,false);
        }
        recalculateBatch(batchId);audit.log("LegacyImportBatch",batchId,"LEGACY_VALIDATION_COMPLETED","Workbook validation completed",actor);
        return summary(batchRepository.findById(batchId).orElseThrow(),true);
    }

    @Transactional(transactionManager="legacyTransactionManager")
    public BatchSummary execute(Long batchId,String actor,boolean admin){
        LegacyImportBatch batch=ownedBatch(batchId,actor,admin);batch.setOverallStatus("IMPORTING");batch.setStartedAt(now());batchRepository.save(batch);
        audit.log("LegacyImportBatch",batchId,"LEGACY_IMPORT_STARTED","Workbook import started",actor);
        for(LegacyImportSheet sheet:sheetRepository.findByBatchIdOrderBySheetIndex(batchId)){
            if(!Set.of("READY","PARTIAL_SUCCESS","FAILED").contains(sheet.getStatus())||sheet.getConfirmedDataset()==null)continue;
            try{validateSheet(batch,sheet,true);audit.log("LegacyImportSheet",sheet.getId(),"LEGACY_SHEET_COMPLETED","Sheet="+sheet.getSheetName()+", imported="+sheet.getImportedRows(),actor);}
            catch(RuntimeException e){sheet.setStatus("FAILED");sheet.setStatusReason(safeMessage(e));sheet.setCompletedAt(now());sheetRepository.save(sheet);audit.log("LegacyImportSheet",sheet.getId(),"LEGACY_SHEET_FAILED","Sheet="+sheet.getSheetName(),actor);}
            entityManager.clear();
        }
        recalculateBatch(batchId);batch=batchRepository.findById(batchId).orElseThrow();batch.setCompletedAt(now());batchRepository.save(batch);
        audit.log("LegacyImportBatch",batchId,"LEGACY_IMPORT_COMPLETED","Status="+batch.getOverallStatus()+", imported="+batch.getImportedRows(),actor);
        return summary(batch,true);
    }

    @Transactional(transactionManager="legacyTransactionManager")
    public SheetSummary retry(Long batchId,Long sheetId,String actor,boolean admin){
        LegacyImportBatch batch=ownedBatch(batchId,actor,admin);LegacyImportSheet sheet=ownedSheet(batchId,sheetId);
        if(sheet.getConfirmedDataset()==null)throw new IllegalStateException("Confirm a dataset mapping before retry.");
        audit.log("LegacyImportSheet",sheetId,"LEGACY_SHEET_RETRY","Batch="+batchId+", sheet="+sheet.getSheetName(),actor);
        validateSheet(batch,sheet,true);recalculateBatch(batchId);return sheetDto(sheet,true);
    }

    @Transactional(transactionManager="legacyTransactionManager")
    public SheetSummary skip(Long batchId,Long sheetId,String actor,boolean admin){
        ownedBatch(batchId,actor,admin);LegacyImportSheet sheet=ownedSheet(batchId,sheetId);
        if(Set.of("COMPLETED","PARTIAL_SUCCESS").contains(sheet.getStatus()))throw new IllegalStateException("An imported sheet cannot be skipped.");
        sheet.setStatus("SKIPPED");sheet.setStatusReason("SKIPPED_BY_USER");sheet.setSkippedRows(sheet.getTotalRows());sheet.setCompletedAt(now());sheetRepository.save(sheet);
        recalculateBatch(batchId);audit.log("LegacyImportSheet",sheetId,"LEGACY_SHEET_SKIPPED","Batch="+batchId+", sheet="+sheet.getSheetName(),actor);return sheetDto(sheet,true);
    }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public Page<ErrorInfo> errors(Long batchId,Pageable pageable,String actor,boolean admin){ownedBatch(batchId,actor,admin);return errorRepository.findByImportBatchIdOrderByImportSheetIdAscSourceRowNumberAsc(batchId,pageable).map(this::errorDto);}

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public Page<BatchSummary> history(Pageable pageable,String actor,boolean admin){
        Page<LegacyImportBatch> page=admin?batchRepository.findAllByOrderByUploadedAtDesc(pageable):batchRepository.findByUploadedByOrderByUploadedAtDesc(actor,pageable);
        return page.map(b->summary(b,false));
    }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public byte[] errorCsv(Long batchId,String actor,boolean admin){
        LegacyImportBatch batch=ownedBatch(batchId,actor,admin);List<LegacyImportError> errors=errorRepository.findByImportBatchIdOrderByImportSheetIdAscSourceRowNumberAsc(batchId,PageRequest.of(0,Integer.MAX_VALUE)).getContent();
        StringBuilder out=new StringBuilder("File Name,Sheet Name,Row Number,Column Name,Value,Error Code,Error Message,Import Batch ID\r\n");
        for(LegacyImportError e:errors)out.append(csv(batch.getOriginalFileName())).append(',').append(csv(e.getSheetName())).append(',').append(e.getSourceRowNumber()).append(',').append(csv(e.getColumnName())).append(',').append(csv(e.getRawValue())).append(',').append(csv(e.getErrorCode())).append(',').append(csv(e.getErrorMessage())).append(',').append(batchId).append("\r\n");
        return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public byte[] summaryCsv(Long batchId,String actor,boolean admin){BatchSummary b=getBatch(batchId,actor,admin);StringBuilder out=new StringBuilder("Sheet,Dataset,Columns,Rows,Valid,Imported,Failed,Duplicates,Status\r\n");for(SheetSummary s:b.getSheets())out.append(csv(s.getSheetName())).append(',').append(csv(s.getDataset())).append(',').append(s.getColumnCount()).append(',').append(s.getRowCount()).append(',').append(s.getValidRows()).append(',').append(s.getImportedRows()).append(',').append(s.getFailedRows()).append(',').append(s.getDuplicateRows()).append(',').append(s.getStatus()).append("\r\n");return out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);}

    @Transactional(transactionManager="legacyTransactionManager", readOnly=true)
    public List<DatasetInfo> datasets(){return datasetRepository.findByActiveTrueAndApprovedTrueOrderByDatasetNameAsc().stream().map(this::datasetDto).collect(Collectors.toList());}

    @Transactional(transactionManager="legacyTransactionManager")
    public DatasetInfo createDataset(DatasetRequest request,String actor){
        String code=upper(request.getDatasetCode());if(!SAFE_CODE.matcher(code).matches())throw new IllegalArgumentException("Dataset code must contain only uppercase letters, numbers and underscores.");
        if(datasetRepository.findByDatasetCodeIgnoreCase(code).isPresent())throw new IllegalArgumentException("Dataset code already exists.");
        if(request.getColumns()==null||request.getColumns().isEmpty())throw new IllegalArgumentException("At least one dataset column is required.");
        LegacyDatasetDefinition d=LegacyDatasetDefinition.builder().datasetCode(code).datasetName(required(request.getDatasetName(),"Dataset name"))
                .description(request.getDescription()).targetTable("legacy_dataset_record").category(request.getCategory()).duplicateKeyFields(request.getDuplicateKeyFields())
                .active(true).approved(Boolean.TRUE.equals(request.getApproved())).createdBy(actor).createdAt(now()).build();
        int order=0;Set<String> fields=new HashSet<>();
        for(DatasetColumnRequest c:request.getColumns()){
            String field=c.getTargetFieldName()==null?"":c.getTargetFieldName().trim();if(!SAFE_FIELD.matcher(field).matches()||!fields.add(field))throw new IllegalArgumentException("Invalid or duplicate target field: "+field);
            String type=upper(c.getTargetDataType());if(!TYPES.contains(type))throw new IllegalArgumentException("Unsupported data type: "+type);
            String identifier=upper(c.getIdentifierType());if(!IDENTIFIERS.contains(identifier))identifier="OTHER";
            LegacyDatasetColumn dc=LegacyDatasetColumn.builder().datasetDefinition(d).targetFieldName(field).targetDataType(type).mandatory(Boolean.TRUE.equals(c.getMandatory())).identifierType(identifier).displayOrder(c.getDisplayOrder()==null?order++:c.getDisplayOrder()).active(true).build();
            List<String> aliases=new ArrayList<>();aliases.add(field);if(c.getAliases()!=null)aliases.addAll(c.getAliases());Set<String> normalized=new HashSet<>();
            for(String alias:aliases){String n=normalizeHeader(alias);if(!n.isBlank()&&normalized.add(n))dc.getAliases().add(LegacyDatasetColumnAlias.builder().datasetColumn(dc).sourceColumnAlias(alias.trim()).normalizedAlias(n).build());}
            d.getColumns().add(dc);
        }
        datasetRepository.save(d);audit.log("LegacyDatasetDefinition",d.getId(),"LEGACY_DATASET_CREATED","Dataset="+code,actor);return datasetDto(d);
    }

    private void validateSheet(LegacyImportBatch batch,LegacyImportSheet sheet,boolean importRows){
        errorRepository.deleteByImportSheetId(sheet.getId());sheet.setStatus(importRows?"IMPORTING":"VALIDATING");sheet.setStartedAt(now());sheet.setValidRows(0);sheet.setFailedRows(0);sheet.setDuplicateRows(0);if(importRows)sheet.setImportedRows(0);sheetRepository.save(sheet);
        List<LegacyImportColumn> columns=columnRepository.findBySheetIdOrderBySourceColumnIndex(sheet.getId());LegacyDatasetDefinition dataset=sheet.getConfirmedDataset();ensureMandatoryMapped(dataset,columns);
        Map<String,LegacyDatasetColumn> definitions=dataset.getColumns().stream().collect(Collectors.toMap(LegacyDatasetColumn::getTargetFieldName,c->c));
        List<LegacyDatasetRecord> pending=new ArrayList<>();long valid=0,failed=0,duplicates=0,imported=0;
        try(InputStream in=Files.newInputStream(resolveStored(batch));Workbook wb=WorkbookFactory.create(in)){
            Sheet ps=wb.getSheetAt(sheet.getSheetIndex());int header=sheet.getDetectedHeaderRow()-1;
            for(int r=header+1;r<=ps.getLastRowNum();r++){
                Row row=ps.getRow(r);if(empty(row))continue;Map<String,String> data=new LinkedHashMap<>();List<LegacyImportError> rowErrors=new ArrayList<>();
                for(LegacyImportColumn c:columns){if(c.isIgnored()||c.getMappedTargetField()==null)continue;String raw=cell(row,c.getSourceColumnIndex());LegacyDatasetColumn def=definitions.get(c.getMappedTargetField());String normalized=normalizeValue(raw,def,rowErrors,batch,sheet,r+1,c.getSourceColumnName());data.put(def.getTargetFieldName(),normalized);}
                if(!rowErrors.isEmpty()){errorRepository.saveAll(rowErrors);failed++;continue;}
                String fingerprint=fingerprint(dataset,data);boolean duplicate=recordRepository.existsByDatasetDefinitionIdAndRecordFingerprint(dataset.getId(),fingerprint)||recordRepository.existsByImportBatchIdAndSourceSheetAndSourceRowNumberAndDatasetDefinitionId(batch.getId(),sheet.getSheetName(),r+1,dataset.getId());
                if(duplicate){duplicates++;continue;}valid++;
                if(importRows){LegacyDatasetRecord record=LegacyDatasetRecord.builder().datasetDefinition(dataset).datasetCode(dataset.getDatasetCode()).recordFingerprint(fingerprint).recordData(json(data)).sourceFile(batch.getOriginalFileName()).sourceSheet(sheet.getSheetName()).sourceRowNumber(r+1).importBatchId(batch.getId()).importedBy(batch.getUploadedBy()).importedAt(now()).build();pending.add(record);
                    if(pending.size()>=Math.max(1,chunkSize)){imported+=persistChunk(pending,dataIdentifierColumns(dataset),batch,sheet);pending.clear();entityManager.flush();entityManager.clear();}}
            }
            if(importRows&&!pending.isEmpty())imported+=persistChunk(pending,dataIdentifierColumns(dataset),batch,sheet);
        }catch(IOException e){throw new IllegalStateException("Unable to read stored workbook.",e);}
        sheet.setValidRows(valid);sheet.setFailedRows(failed);sheet.setDuplicateRows(duplicates);if(importRows)sheet.setImportedRows(imported);
        sheet.setStatus(failed>0?(valid>0?"PARTIAL_SUCCESS":"FAILED"):(importRows?"COMPLETED":"READY"));sheet.setCompletedAt(now());sheetRepository.save(sheet);
    }

    private long persistChunk(List<LegacyDatasetRecord> records,Map<String,String> ids,LegacyImportBatch batch,LegacyImportSheet sheet){
        List<LegacyDatasetRecord> saved=recordRepository.saveAll(records);recordRepository.flush();for(LegacyDatasetRecord record:saved){Map<String,String> data=readJson(record.getRecordData());String name=valueByIdentifier(data,ids,"NAME"),epic=normalizeEpic(valueByIdentifier(data,ids,"EPIC")),mobile=normalizeMobile(valueByIdentifier(data,ids,"MOBILE")),village=valueByIdentifier(data,ids,"VILLAGE"),address=valueByIdentifier(data,ids,"ADDRESS");if(blank(name)&&blank(epic)&&blank(mobile))continue;personRepository.save(LegacyPersonIndex.builder().sourceDatasetCode(record.getDatasetCode()).sourceTable("legacy_dataset_record").sourceRecordId(record.getId()).name(name).normalizedName(normalizeName(name)).epic(epic).normalizedEpic(epic).mobile(mobile).normalizedMobile(mobile).village(village).normalizedVillage(normalizeText(village)).address(address).normalizedAddress(normalizeText(address)).district(valueByIdentifier(data,ids,"DISTRICT")).constituency(valueByIdentifier(data,ids,"CONSTITUENCY")).schemeCode(valueByIdentifier(data,ids,"SCHEME")).identityBasis(!blank(epic)?"EPIC":!blank(mobile)?"MOBILE":"NAME_CANDIDATE").sourceFile(batch.getOriginalFileName()).sourceSheet(sheet.getSheetName()).sourceRowNumber(record.getSourceRowNumber()).importBatchId(batch.getId()).build());}return saved.size();
    }

    private void persistColumns(LegacyImportSheet sheet,Sheet ps,Header header,LegacyDatasetDefinition dataset){for(int i=0;i<header.values.size();i++){String source=header.values.get(i),normalized=normalizeHeader(source);LegacyDatasetColumn mapped=findColumn(dataset,normalized);columnRepository.save(LegacyImportColumn.builder().sheet(sheet).sourceColumnIndex(i).sourceColumnName(source).normalizedColumnName(normalized).detectedDataType(inferType(ps,header.rowIndex,i)).mappedTargetField(mapped==null?null:mapped.getTargetFieldName()).mappedIdentifierType(mapped==null?null:mapped.getIdentifierType()).mandatory(mapped!=null&&mapped.isMandatory()).ignored(false).mappingStatus(mapped==null?"UNMAPPED":"MATCHED").build());}}
    private Match matchDataset(List<String> headers,List<LegacyDatasetDefinition> datasets){Set<String> source=headers.stream().map(this::normalizeHeader).collect(Collectors.toSet());LegacyDatasetDefinition best=null;double bestScore=0;for(LegacyDatasetDefinition d:datasets){List<LegacyDatasetColumn> active=d.getColumns().stream().filter(LegacyDatasetColumn::isActive).collect(Collectors.toList());if(active.isEmpty())continue;long matched=active.stream().filter(c->aliases(c).stream().anyMatch(source::contains)).count();long mandatory=active.stream().filter(LegacyDatasetColumn::isMandatory).count();long mandatoryMatched=active.stream().filter(c->c.isMandatory()&&aliases(c).stream().anyMatch(source::contains)).count();long identifiers=active.stream().filter(c->!"OTHER".equals(c.getIdentifierType())).count();long identifierMatched=active.stream().filter(c->!"OTHER".equals(c.getIdentifierType())&&aliases(c).stream().anyMatch(source::contains)).count();long exact=active.stream().filter(c->source.contains(normalizeHeader(c.getTargetFieldName()))).count();double score=(mandatory==0?40d:40d*mandatoryMatched/mandatory)+(identifiers==0?25d:25d*identifierMatched/identifiers)+25d*matched/active.size()+10d*exact/active.size();if(mandatory>0&&mandatoryMatched<mandatory)score=Math.min(score,84);if(score>bestScore){bestScore=score;best=d;}}return new Match(best,bestScore);}
    private Header detectHeader(Sheet sheet,List<LegacyDatasetDefinition> datasets){Set<String> aliases=datasets.stream().flatMap(d->d.getColumns().stream()).flatMap(c->aliases(c).stream()).collect(Collectors.toSet());Header best=null;double score=0;for(int r=sheet.getFirstRowNum();r<=Math.min(sheet.getLastRowNum(),sheet.getFirstRowNum()+19);r++){Row row=sheet.getRow(r);if(empty(row))continue;List<String> values=readRow(row,lastCell(row));long nonBlank=values.stream().filter(v->!blank(v)).count(),unique=values.stream().map(this::normalizeHeader).filter(v->!v.isBlank()).distinct().count(),known=values.stream().map(this::normalizeHeader).filter(aliases::contains).count();double candidate=nonBlank+unique*.5+known*3;if(nonBlank>=2&&candidate>score){score=candidate;best=new Header(r,values);}}return best;}
    private String inferType(Sheet sheet,int header,int col){int numeric=0,dates=0,bool=0,text=0;for(int r=header+1;r<=Math.min(sheet.getLastRowNum(),header+20);r++){Cell c=sheet.getRow(r)==null?null:sheet.getRow(r).getCell(col);if(c==null||c.getCellType()==CellType.BLANK)continue;if(DateUtil.isCellDateFormatted(c))dates++;else if(c.getCellType()==CellType.NUMERIC)numeric++;else {String v=cell(sheet.getRow(r),col);if(v.equalsIgnoreCase("true")||v.equalsIgnoreCase("false")||v.equalsIgnoreCase("yes")||v.equalsIgnoreCase("no"))bool++;else text++;}}if(dates>0&&text==0)return"DATE";if(numeric>0&&text==0)return"DECIMAL";if(bool>0&&text==0&&numeric==0)return"BOOLEAN";return"STRING";}
    private String normalizeValue(String raw,LegacyDatasetColumn def,List<LegacyImportError> errors,LegacyImportBatch batch,LegacyImportSheet sheet,long row,String column){String value=raw==null?"":raw.trim();if(value.isEmpty()&&def.isMandatory()){errors.add(error(batch,sheet,row,column,value,"MANDATORY_FIELD_MISSING","Mandatory value is missing."));return value;}if(value.isEmpty())return value;try{switch(def.getTargetDataType()){case"INTEGER":return new BigDecimal(value.replace(",","")).toBigIntegerExact().toString();case"DECIMAL":return new BigDecimal(value.replace(",","")).stripTrailingZeros().toPlainString();case"BOOLEAN":if(Set.of("TRUE","YES","1").contains(upper(value)))return"true";if(Set.of("FALSE","NO","0").contains(upper(value)))return"false";throw new IllegalArgumentException();case"DATE":return parseDate(value);default:break;}}catch(Exception e){errors.add(error(batch,sheet,row,column,value,"INVALID_"+def.getTargetDataType(),"Value is not a valid "+def.getTargetDataType().toLowerCase(Locale.ROOT)+"."));}if("EPIC".equals(def.getIdentifierType())){String epic=normalizeEpic(value);if(!epic.matches("[A-Z0-9]{5,20}"))errors.add(error(batch,sheet,row,column,value,"INVALID_EPIC_FORMAT","EPIC contains an invalid format."));return epic;}if("MOBILE".equals(def.getIdentifierType()))return normalizeMobile(value);return value;}
    private void ensureMandatoryMapped(LegacyDatasetDefinition d,List<LegacyImportColumn> mapped){Set<String> fields=mapped.stream().filter(c->!c.isIgnored()&&c.getMappedTargetField()!=null).map(LegacyImportColumn::getMappedTargetField).collect(Collectors.toSet());List<String> missing=d.getColumns().stream().filter(c->c.isActive()&&c.isMandatory()&&!fields.contains(c.getTargetFieldName())).map(LegacyDatasetColumn::getTargetFieldName).collect(Collectors.toList());if(!missing.isEmpty())throw new IllegalArgumentException("Mandatory target fields are not mapped: "+String.join(", ",missing));}
    private void recalculateBatch(Long id){LegacyImportBatch b=batchRepository.findById(id).orElseThrow();List<LegacyImportSheet>s=sheetRepository.findByBatchIdOrderBySheetIndex(id);b.setImportedSheets((int)s.stream().filter(x->Set.of("COMPLETED","PARTIAL_SUCCESS").contains(x.getStatus())).count());b.setFailedSheets((int)s.stream().filter(x->"FAILED".equals(x.getStatus())).count());b.setSkippedSheets((int)s.stream().filter(x->"SKIPPED".equals(x.getStatus())).count());b.setMappingRequiredSheets((int)s.stream().filter(x->"MAPPING_REQUIRED".equals(x.getStatus())).count());b.setTotalRows(s.stream().mapToLong(LegacyImportSheet::getTotalRows).sum());b.setValidRows(s.stream().mapToLong(LegacyImportSheet::getValidRows).sum());b.setImportedRows(s.stream().mapToLong(LegacyImportSheet::getImportedRows).sum());b.setFailedRows(s.stream().mapToLong(LegacyImportSheet::getFailedRows).sum());b.setDuplicateRows(s.stream().mapToLong(LegacyImportSheet::getDuplicateRows).sum());if(b.getImportedSheets()>0&&(b.getFailedSheets()>0||b.getMappingRequiredSheets()>0||b.getSkippedSheets()>0||b.getFailedRows()>0))b.setOverallStatus("PARTIAL_SUCCESS");else if(b.getImportedSheets()>0)b.setOverallStatus("COMPLETED");else if(b.getFailedSheets()>0)b.setOverallStatus("FAILED");else b.setOverallStatus("READY_FOR_MAPPING");batchRepository.save(b);}
    private BatchSummary summary(LegacyImportBatch b,boolean includeSheets){return BatchSummary.builder().batchId(b.getId()).fileName(b.getOriginalFileName()).status(b.getOverallStatus()).uploadedBy(b.getUploadedBy()).uploadedAt(b.getUploadedAt()).totalSheets(b.getTotalSheets()).analyzedSheets(b.getAnalyzedSheets()).importedSheets(b.getImportedSheets()).failedSheets(b.getFailedSheets()).skippedSheets(b.getSkippedSheets()).mappingRequiredSheets(b.getMappingRequiredSheets()).totalRows(b.getTotalRows()).validRows(b.getValidRows()).importedRows(b.getImportedRows()).failedRows(b.getFailedRows()).duplicateRows(b.getDuplicateRows()).sheets(includeSheets?sheetRepository.findByBatchIdOrderBySheetIndex(b.getId()).stream().map(s->sheetDto(s,true)).collect(Collectors.toList()):List.of()).build();}
    private SheetSummary sheetDto(LegacyImportSheet s,boolean cols){LegacyDatasetDefinition d=s.getConfirmedDataset()!=null?s.getConfirmedDataset():s.getDetectedDataset();return SheetSummary.builder().id(s.getId()).sheetIndex(s.getSheetIndex()).sheetName(s.getSheetName()).hidden(s.isHidden()).detectedHeaderRow(s.getDetectedHeaderRow()).columnCount(s.getTotalColumns()).rowCount(s.getTotalRows()).detectedDatasetId(s.getDetectedDataset()==null?null:s.getDetectedDataset().getId()).confirmedDatasetId(s.getConfirmedDataset()==null?null:s.getConfirmedDataset().getId()).dataset(d==null?null:d.getDatasetCode()).targetTable(s.getTargetTable()).confidence(s.getMappingConfidence()).validRows(s.getValidRows()).importedRows(s.getImportedRows()).failedRows(s.getFailedRows()).duplicateRows(s.getDuplicateRows()).skippedRows(s.getSkippedRows()).status(s.getStatus()).statusReason(s.getStatusReason()).columns(cols?columnRepository.findBySheetIdOrderBySourceColumnIndex(s.getId()).stream().map(this::columnDto).collect(Collectors.toList()):List.of()).build();}
    private ColumnInfo columnDto(LegacyImportColumn c){return ColumnInfo.builder().id(c.getId()).index(c.getSourceColumnIndex()).sourceHeader(c.getSourceColumnName()).normalizedHeader(c.getNormalizedColumnName()).detectedType(c.getDetectedDataType()).targetField(c.getMappedTargetField()).identifierType(c.getMappedIdentifierType()).mandatory(c.isMandatory()).ignored(c.isIgnored()).mappingStatus(c.getMappingStatus()).build();}
    private DatasetInfo datasetDto(LegacyDatasetDefinition d){return DatasetInfo.builder().id(d.getId()).code(d.getDatasetCode()).name(d.getDatasetName()).category(d.getCategory()).approved(d.isApproved()).columns(d.getColumns().stream().map(c->ColumnInfo.builder().targetField(c.getTargetFieldName()).detectedType(c.getTargetDataType()).identifierType(c.getIdentifierType()).mandatory(c.isMandatory()).build()).collect(Collectors.toList())).build();}
    private ErrorInfo errorDto(LegacyImportError e){return ErrorInfo.builder().id(e.getId()).sheetId(e.getImportSheetId()).sheetName(e.getSheetName()).rowNumber(e.getSourceRowNumber()).columnName(e.getColumnName()).rawValue(e.getRawValue()).errorCode(e.getErrorCode()).errorMessage(e.getErrorMessage()).build();}
    private LegacyImportError error(LegacyImportBatch b,LegacyImportSheet s,long row,String col,String raw,String code,String msg){return LegacyImportError.builder().importBatchId(b.getId()).importSheetId(s.getId()).sheetName(s.getSheetName()).sourceRowNumber(row).columnName(col).rawValue(limit(raw,500)).errorCode(code).errorMessage(msg).createdAt(now()).build();}
    private LegacyImportBatch ownedBatch(Long id,String actor,boolean admin){LegacyImportBatch b=batchRepository.findById(id).orElseThrow(()->new NoSuchElementException("Import batch not found."));if(!admin&&!b.getUploadedBy().equalsIgnoreCase(actor))throw new AccessDeniedException("You cannot access this import batch.");return b;}
    private LegacyImportSheet ownedSheet(Long b,Long s){return sheetRepository.findByIdAndBatchId(s,b).orElseThrow(()->new NoSuchElementException("Import sheet not found."));}
    private void validateUpload(MultipartFile f){if(f==null||f.isEmpty())throw new IllegalArgumentException("Select a non-empty Excel workbook.");if(f.getSize()>maxFileSizeMb*1024*1024)throw new IllegalArgumentException("Workbook exceeds "+maxFileSizeMb+" MB.");String name=Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);if(!(name.endsWith(".xlsx")||name.endsWith(".xls")))throw new IllegalArgumentException("Only .xls and .xlsx files are accepted.");if(f.getContentType()!=null&&!EXCEL_TYPES.contains(f.getContentType()))throw new IllegalArgumentException("The uploaded MIME type is not an Excel workbook.");}
    private Path store(MultipartFile file){try{Path dir=Paths.get(storagePath).toAbsolutePath().normalize();Files.createDirectories(dir);String ext=file.getOriginalFilename()!=null&&file.getOriginalFilename().toLowerCase().endsWith(".xls")?".xls":".xlsx";Path target=dir.resolve(UUID.randomUUID()+ext).normalize();if(!target.startsWith(dir))throw new IllegalArgumentException("Invalid storage path.");try(InputStream in=file.getInputStream()){Files.copy(in,target,StandardCopyOption.REPLACE_EXISTING);}return target;}catch(IOException e){throw new IllegalStateException("Unable to store workbook.",e);}}
    private Path resolveStored(LegacyImportBatch b){Path dir=Paths.get(storagePath).toAbsolutePath().normalize(),p=dir.resolve(b.getStoredFileName()).normalize();if(!p.startsWith(dir)||!Files.isRegularFile(p))throw new IllegalStateException("Stored workbook is unavailable.");return p;}
    private String sha256(Path p){try(InputStream in=Files.newInputStream(p)){MessageDigest d=MessageDigest.getInstance("SHA-256");byte[]buf=new byte[8192];for(int n;(n=in.read(buf))>0;)d.update(buf,0,n);return HexFormat.of().formatHex(d.digest());}catch(Exception e){throw new IllegalStateException("Unable to hash workbook.",e);}}
    private String fingerprint(LegacyDatasetDefinition d,Map<String,String> data){List<String>keys=csvFields(d.getDuplicateKeyFields());String basis;if(keys.isEmpty())basis=data.entrySet().stream().map(e->e.getKey()+"="+upper(e.getValue())).collect(Collectors.joining("|"));else basis=keys.stream().map(k->k+"="+upper(data.get(k))).collect(Collectors.joining("|"));try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(basis.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private Map<String,String> dataIdentifierColumns(LegacyDatasetDefinition d){return d.getColumns().stream().filter(c->!"OTHER".equals(c.getIdentifierType())).collect(Collectors.toMap(LegacyDatasetColumn::getTargetFieldName,LegacyDatasetColumn::getIdentifierType,(a,b)->a));}
    private String valueByIdentifier(Map<String,String>d,Map<String,String>ids,String type){return ids.entrySet().stream().filter(e->type.equals(e.getValue())).map(e->d.get(e.getKey())).filter(Objects::nonNull).findFirst().orElse(null);}
    private LegacyDatasetColumn findColumn(LegacyDatasetDefinition d,String normalized){if(d==null)return null;return d.getColumns().stream().filter(LegacyDatasetColumn::isActive).filter(c->aliases(c).contains(normalized)).findFirst().orElse(null);}
    private Set<String> aliases(LegacyDatasetColumn c){Set<String>s=c.getAliases().stream().map(LegacyDatasetColumnAlias::getNormalizedAlias).collect(Collectors.toSet());s.add(normalizeHeader(c.getTargetFieldName()));return s;}
    private boolean isEmpty(Sheet s){for(Row r:s)if(!empty(r))return false;return true;} private boolean empty(Row r){if(r==null)return true;for(Cell c:r)if(!cell(r,c.getColumnIndex()).isBlank())return false;return true;}
    private int lastCell(Row r){return r==null?0:Math.max(0,r.getLastCellNum());} private List<String> readRow(Row r,int n){List<String>v=new ArrayList<>();for(int i=0;i<n;i++)v.add(cell(r,i));while(!v.isEmpty()&&v.get(v.size()-1).isBlank())v.remove(v.size()-1);return v;}
    private String cell(Row r,int i){if(r==null)return"";Cell c=r.getCell(i,Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);if(c==null)return"";return new DataFormatter(Locale.ENGLISH).formatCellValue(c).trim();}
    private long countDataRows(Sheet s,int h){long n=0;for(int r=h+1;r<=s.getLastRowNum();r++)if(!empty(s.getRow(r)))n++;return n;}
    public String normalizeHeader(String v){if(v==null)return"";return java.text.Normalizer.normalize(v,java.text.Normalizer.Form.NFKC).trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+","_").replaceAll("^_+|_+$","").replaceAll("_+","_");}
    private String normalizeEpic(String v){return blank(v)?null:v.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+","");}private String normalizeMobile(String v){if(blank(v))return null;String d=v.replaceAll("\\D","");return d.length()>10?d.substring(d.length()-10):d;}private String normalizeName(String v){return blank(v)?null:v.trim().replaceAll("\\s+"," ").toUpperCase(Locale.ROOT);}
    private String normalizeText(String v){return blank(v)?null:v.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]+"," ").replaceAll("\\s+"," ");}
    private String parseDate(String v){for(DateTimeFormatter f:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("d/M/uuuu"),DateTimeFormatter.ofPattern("d-M-uuuu"),DateTimeFormatter.ofPattern("d.M.uuuu"))){try{return LocalDate.parse(v,f).toString();}catch(Exception ignored){}}throw new IllegalArgumentException();}
    private String json(Map<String,String>v){try{return objectMapper.writeValueAsString(v);}catch(JsonProcessingException e){throw new IllegalStateException(e);}}@SuppressWarnings("unchecked")private Map<String,String>readJson(String v){try{return objectMapper.readValue(v,LinkedHashMap.class);}catch(Exception e){throw new IllegalStateException(e);}}
    private List<String>csvFields(String v){return blank(v)?List.of():Arrays.stream(v.split(",")).map(String::trim).filter(SAFE_FIELD.asPredicate()).collect(Collectors.toList());}private String csv(String v){String x=v==null?"":v;return"\""+x.replace("\"","\"\"")+"\"";}private String sanitizeFilename(String v){String n=v==null?"workbook.xlsx":Paths.get(v).getFileName().toString();return limit(n.replaceAll("[\\r\\n]","_"),255);}private String safeSheetName(String v){return limit(v==null?"Sheet":v.replaceAll("[\\r\\n]","_"),255);}private String limit(String v,int n){return v==null?null:v.substring(0,Math.min(v.length(),n));}private String safeMessage(Exception e){return limit(Optional.ofNullable(e.getMessage()).orElse("Sheet import failed."),255);}private String upper(String v){return v==null?"":v.trim().toUpperCase(Locale.ROOT);}private boolean blank(String v){return v==null||v.isBlank();}private String required(String v,String label){if(blank(v))throw new IllegalArgumentException(label+" is required.");return v.trim();}private LocalDateTime now(){return DateTimeUtil.nowIST();}
    private static final class Header{final int rowIndex;final List<String>values;Header(int r,List<String>v){rowIndex=r;values=v;}}private static final class Match{final LegacyDatasetDefinition dataset;final double score;Match(LegacyDatasetDefinition d,double s){dataset=d;score=s;}}
}
