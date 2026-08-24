package com.survisha.meghaconnect.legacy.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.survisha.meghaconnect.legacy.dto.*;
import com.survisha.meghaconnect.legacy.dto.LegacyPersonSearchResponse.*;
import com.survisha.meghaconnect.legacy.entity.*;
import com.survisha.meghaconnect.legacy.repository.*;
import com.survisha.meghaconnect.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor
public class LegacyPersonSearchService {
    private final LegacyPersonIndexRepository personRepository;
    private final LegacyDatasetRecordRepository recordRepository;
    private final LegacyDatasetDefinitionRepository datasetRepository;
    private final ObjectMapper mapper;
    private final AuditLogService audit;

    @Transactional(transactionManager="legacyTransactionManager",readOnly=true)
    public LegacyPersonSearchResponse search(LegacyPersonSearchRequest q,String actor){
        Query query=normalize(q);if(query.empty())throw new IllegalArgumentException("At least one meaningful legacy search criterion is required.");
        int page=Math.max(0,Optional.ofNullable(q.getPage()).orElse(0));int limit=Math.min(query.nameOnly()?10:50,Math.max(1,Optional.ofNullable(q.getLimit()).orElse(20)));
        Page<LegacyPersonIndex> candidates=personRepository.findAll(spec(query),PageRequest.of(0,200));
        List<Scored> scored=candidates.stream().map(p->score(query,p)).filter(s->s.score>=minimum(query)).sorted(Comparator.comparingInt((Scored s)->s.score).reversed()).collect(Collectors.toList());
        LinkedHashMap<String,Candidate> grouped=new LinkedHashMap<>();for(Scored s:scored){String key=groupKey(s.person);Candidate candidate=grouped.computeIfAbsent(key,k->candidate(s));candidate.getDatasets().add(dataset(s.person));}
        List<Candidate> all=new ArrayList<>(grouped.values());int from=Math.min(page*limit,all.size()),to=Math.min(from+limit,all.size());List<Candidate> result=all.subList(from,to);
        audit.log("LegacyPersonSearch",null,"LEGACY_PERSON_SEARCH","Criteria="+query.criteria()+", results="+all.size(),actor);
        return LegacyPersonSearchResponse.builder().page(page).limit(limit).totalMatches(all.size()).matches(result).build();
    }

    private Specification<LegacyPersonIndex> spec(Query q){return(root,cq,cb)->{List<javax.persistence.criteria.Predicate>or=new ArrayList<>();if(q.epic!=null)or.add(cb.equal(root.get("normalizedEpic"),q.epic));if(q.mobile!=null)or.add(cb.equal(root.get("normalizedMobile"),q.mobile));if(q.name!=null){String token=q.name.length()>4?q.name.substring(0,Math.min(8,q.name.length())):q.name;or.add(cb.like(root.get("normalizedName"),"%"+token+"%"));}if(q.village!=null)or.add(cb.equal(root.get("normalizedVillage"),q.village));if(q.address!=null){String token=q.address.split(" ")[0];or.add(cb.like(root.get("normalizedAddress"),"%"+token+"%"));}if(q.district!=null)or.add(cb.equal(cb.upper(root.get("district")),q.district));if(q.constituency!=null)or.add(cb.equal(cb.upper(root.get("constituency")),q.constituency));return cb.or(or.toArray(new javax.persistence.criteria.Predicate[0]));};}
    private Scored score(Query q,LegacyPersonIndex p){int score=0;List<String>on=new ArrayList<>();if(eq(q.epic,p.getNormalizedEpic())){score=100;on.add("EPIC");}if(eq(q.mobile,p.getNormalizedMobile())){score=Math.max(score,90);on.add("MOBILE");}double name=q.name==null?0:similarity(q.name,p.getNormalizedName());if(name>=.98){score=Math.max(score,70);on.add("NAME");}else if(name>=.72){score=Math.max(score,(int)(45*name));on.add("NAME_SIMILAR");}if(eq(q.village,p.getNormalizedVillage())){score+=15;on.add("VILLAGE");}if(tokenSimilarity(q.address,p.getNormalizedAddress())>=.6){score+=15;on.add("ADDRESS");}if(eq(q.district,upper(p.getDistrict()))){score+=10;on.add("DISTRICT");}if(eq(q.constituency,upper(p.getConstituency()))){score+=10;on.add("CONSTITUENCY");}return new Scored(p,Math.min(100,score),on);}
    private Candidate candidate(Scored s){String level=s.score==100&&s.on.contains("EPIC")?"EXACT_EPIC":s.on.contains("MOBILE")?"EXACT_MOBILE":s.score>=75?"STRONG":s.on.equals(List.of("NAME"))||s.on.equals(List.of("NAME_SIMILAR"))?"NAME_ONLY":"POSSIBLE";LegacyPersonIndex p=s.person;return Candidate.builder().matchScore(s.score).matchLevel(level).manualVerificationRequired(Set.of("POSSIBLE","NAME_ONLY").contains(level)).matchedOn(s.on).legacyPerson(Person.builder().name(p.getName()).epic(p.getEpic()).mobile(p.getMobile()).village(p.getVillage()).address(p.getAddress()).district(p.getDistrict()).constituency(p.getConstituency()).build()).build();}
    private DatasetRecord dataset(LegacyPersonIndex p){LegacyDatasetRecord r=recordRepository.findById(p.getSourceRecordId()).orElse(null);String name=datasetRepository.findByDatasetCodeIgnoreCase(p.getSourceDatasetCode()).map(LegacyDatasetDefinition::getDatasetName).orElse(p.getSourceDatasetCode());Map<String,Object>details=Map.of();if(r!=null)try{details=mapper.readValue(r.getRecordData(),new TypeReference<Map<String,Object>>(){});}catch(Exception ignored){}return DatasetRecord.builder().datasetCode(p.getSourceDatasetCode()).datasetName(name).schemeName(p.getSchemeCode()).sourceRecordId(p.getSourceRecordId()).sourceFile(p.getSourceFile()).sourceSheet(p.getSourceSheet()).sourceRowNumber(p.getSourceRowNumber()).details(details).build();}
    private String groupKey(LegacyPersonIndex p){if(text(p.getNormalizedEpic()))return"E:"+p.getNormalizedEpic();if(text(p.getNormalizedMobile()))return"M:"+p.getNormalizedMobile();return"R:"+p.getId();}
    private Query normalize(LegacyPersonSearchRequest q){if(q==null)return new Query();Query n=new Query();n.epic=epic(q.getEpic());n.mobile=mobile(q.getMobile());n.name=norm(q.getName());n.village=norm(q.getVillage());n.address=norm(q.getAddress());n.district=norm(q.getDistrict());n.constituency=norm(q.getConstituency());return n;}
    private int minimum(Query q){return q.nameOnly()?30:35;}private boolean eq(String a,String b){return a!=null&&a.equals(b);}private boolean text(String v){return v!=null&&!v.isBlank();}private String upper(String v){return text(v)?v.trim().toUpperCase(Locale.ROOT):null;}private String epic(String v){return text(v)?v.toUpperCase(Locale.ROOT).replaceAll("\\s+",""):null;}private String mobile(String v){if(!text(v))return null;String d=v.replaceAll("\\D","");return d.length()>10?d.substring(d.length()-10):d;}private String norm(String v){return text(v)?v.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9 ]+"," ").trim().replaceAll("\\s+"," "):null;}
    private double tokenSimilarity(String a,String b){if(a==null||b==null)return 0;Set<String>x=new HashSet<>(Arrays.asList(a.split(" "))),y=new HashSet<>(Arrays.asList(b.split(" ")));Set<String>i=new HashSet<>(x);i.retainAll(y);Set<String>u=new HashSet<>(x);u.addAll(y);return u.isEmpty()?0:(double)i.size()/u.size();}
    private double similarity(String a,String b){if(a==null||b==null)return 0;int[]prev=new int[b.length()+1];for(int j=0;j<=b.length();j++)prev[j]=j;for(int i=1;i<=a.length();i++){int[]cur=new int[b.length()+1];cur[0]=i;for(int j=1;j<=b.length();j++)cur[j]=Math.min(Math.min(cur[j-1]+1,prev[j]+1),prev[j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));prev=cur;}return 1d-(double)prev[b.length()]/Math.max(a.length(),b.length());}
    private static final class Query{String epic,name,mobile,village,address,district,constituency;boolean empty(){return Arrays.asList(epic,name,mobile,village,address,district,constituency).stream().allMatch(Objects::isNull);}boolean nameOnly(){return name!=null&&epic==null&&mobile==null&&village==null&&address==null&&district==null&&constituency==null;}String criteria(){List<String>x=new ArrayList<>();if(epic!=null)x.add("EPIC");if(name!=null)x.add("NAME");if(mobile!=null)x.add("MOBILE");if(village!=null)x.add("VILLAGE");if(address!=null)x.add("ADDRESS");if(district!=null)x.add("DISTRICT");if(constituency!=null)x.add("CONSTITUENCY");return String.join("+",x);}}
    private static final class Scored{final LegacyPersonIndex person;final int score;final List<String>on;Scored(LegacyPersonIndex p,int s,List<String>o){person=p;score=s;on=o;}}
}
