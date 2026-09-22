package com.onlymymoney.adapter.in.csv;
import com.onlymymoney.domain.model.*; import com.onlymymoney.domain.port.CsvReader; import com.opencsv.*; import org.springframework.stereotype.Component;
import java.io.*; import java.math.BigDecimal; import java.time.*; import java.time.format.*; import java.util.*;
@Component public class OpenCsvMovementReader implements CsvReader {
 public List<Movement> read(InputStream input,Long accountId){
  try {
   String content=new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
   try(Reader reader=new StringReader(content)){
   char sep=content.lines().findFirst().map(line->line.chars().filter(c->c==','||c==';'||c=='\t').count()).orElse(1L)>1 ? (content.lines().findFirst().get().contains(";")?';':content.lines().findFirst().get().contains("\t")?'\t':',') : ',';
   CSVReader csv=new CSVReaderBuilder(reader).withCSVParser(new CSVParserBuilder().withSeparator(sep).build()).build();
   List<String[]> rows=csv.readAll(); if(rows.isEmpty())throw new IllegalArgumentException("CSV is empty");
   String[] h=rows.get(0); Map<String,Integer> idx=new HashMap<>(); for(int i=0;i<h.length;i++)idx.put(norm(h[i]),i);
   int concept=col(idx,"concept","concepto","description","descripcion","merchant","comercio","name");
   int date=col(idx,"date","fecha","movement_date","transaction_date");
   int amount=col(idx,"amount","importe","valor","transaction_amount");
   int balance=col(idx,"available_balance","avaliable_balance","saldo_disponible","saldo","balance");
   List<Movement> out=new ArrayList<>();
   for(int i=1;i<rows.size();i++){String[] r=rows.get(i); if(r.length==0)continue;
    try{out.add(new Movement(null,accountId,cell(r,concept),parseDate(cell(r,date)),parseMoney(cell(r,amount)),parseMoney(cell(r,balance)),ImportStatus.CSV,null));}
    catch(Exception e){out.add(new Movement(null,accountId,cell(r,concept),null,null,null,ImportStatus.IGNORED,null));}
   }
   return out;
   }
  } catch(Exception e){throw new IllegalArgumentException("Unable to parse CSV: "+e.getMessage(),e);}
 }
 private int col(Map<String,Integer> m,String... names){for(String n:names)if(m.containsKey(norm(n)))return m.get(norm(n));throw new IllegalArgumentException("Missing required CSV column: "+Arrays.toString(names));}
 private String cell(String[] r,int i){return i<r.length?r[i].trim():null;}
 private String norm(String s){return s==null?"":s.trim().toLowerCase(Locale.ROOT).replace("\uFEFF","").replace(" ","_");}
 private BigDecimal parseMoney(String s){if(s==null||s.isBlank())return null;String x=s.replace("€","").replace(" ",""); if(x.contains(",")&&x.contains(".")){if(x.lastIndexOf(',')>x.lastIndexOf('.'))x=x.replace(".","").replace(",",".");else x=x.replace(",","");}else if(x.contains(","))x=x.replace(",",".");return new BigDecimal(x);}
 private LocalDate parseDate(String s){for(DateTimeFormatter f:List.of(DateTimeFormatter.ISO_LOCAL_DATE,DateTimeFormatter.ofPattern("dd/MM/yyyy"),DateTimeFormatter.ofPattern("dd-MM-yyyy"))){try{return LocalDate.parse(s,f);}catch(Exception ignored){}}throw new IllegalArgumentException("Unsupported date: "+s);}
}
