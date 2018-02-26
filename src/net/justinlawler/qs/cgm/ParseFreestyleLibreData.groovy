package net.justinlawler.qs.cgm

import javax.xml.bind.annotation.adapters.HexBinaryAdapter

import java.beans.MetaData.java_util_AbstractCollection_PersistenceDelegate
import java.text.SimpleDateFormat
import java.util.logging.Logger
import java.util.logging.Level

import org.apache.commons.lang.StringUtils;
import org.codehaus.groovy.runtime.dgmimpl.arrays.ArrayGetAtMetaMethod
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.commons.io.output.NullOutputStream
import com.opencsv.CSVReader
import com.opencsv.CSVParser
import com.opencsv.CSVWriter

import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

@Grapes([
	@Grab(group='commons-io', module='commons-io', version='2.4'),
	@Grab(group='commons-lang', module='commons-lang', version='2.6'),
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6')
])
class ParseFreestyleLibreData {
	static def logger = Logger.getLogger("ParseFreestyleLibreData")
	
	private static final String LIVE_DATA_DIR = '/Users/justinlawler/Dropbox/projects/Quantified_Self/2017_QS_Amsterdam/Blood_Glucose_Sleep_workshop/analysis/'
	private static final String DATA_FILE = 'CGM_values.csv'
	//private static final String DATA_FILE = 'CGM_values_test.csv'
	
	private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("Europe/London");
	
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd");
	private static final SimpleDateFormat TIME_FORMATTER = new SimpleDateFormat("HH:mm:ss");
	private static final SimpleDateFormat HOUR_FORMATTER = new SimpleDateFormat("HH");
	
	// format: 31/05/17 13:48:00
	private static final SimpleDateFormat LIBRE_DATE_TIME_FORMATTER = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
	private static final SimpleDateFormat INTERNAL_DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	/**
	 */
	public static void main(String[] args)
	{
//		DATE_FORMATTER.setTimeZone(TIME_ZONE);
//		TIME_FORMATTER.setTimeZone(TIME_ZONE);
//		HOUR_FORMATTER.setTimeZone(TIME_ZONE);
//		DATE_TIME_FORMATTER.setTimeZone(TIME_ZONE);
//		INTERNAL_DATE_TIME_FORMATTER.setTimeZone(TIME_ZONE);
		
//		def files = getListOfFiles(DATA_DIRECTORY)
//		for (def file : files) {
//			printAvgValsForFile(file)
//		}
		
		//File file = new File(PROJECT_DIR + DATA_FILE)
		File file = new File(LIVE_DATA_DIR, DATA_FILE)
		def dateTimeVals = readLibreCSVFile(file);
		
		println("num vals = " + dateTimeVals.size());
		
		def avgsPerHour = calAvgsPerHour(dateTimeVals)
		printAvgsPerHourPerDay(avgsPerHour)
	}
	
	/**
	 */
	public static final calAvgsPerHour(dateTimeVals) throws Exception
	{
		println "########################";
		println "### breakDownResults";
		def bucketsVals = [:]
		def bucketsCounts = [:]
		
		dateTimeVals.each{ dateTimeStr, v ->
			//println INTERNAL_DATE_TIME_FORMATTER.format(dateTime.getTime()) + " - " + v;
			println dateTimeStr + " - " + v;
			def origDateTime = INTERNAL_DATE_TIME_FORMATTER.parse(dateTimeStr)
			
			def hour = HOUR_FORMATTER.format(origDateTime.getTime());
			def dateTimeOnHourStr = DATE_FORMATTER.format(origDateTime.getTime()) + " " + hour + ":00:00";
			double totalVal = bucketsVals.containsKey(dateTimeOnHourStr) ? bucketsVals.get(dateTimeOnHourStr) : 0.0f;
			int totalCount = bucketsCounts.containsKey(dateTimeOnHourStr) ? bucketsCounts.get(dateTimeOnHourStr) : 0;
			bucketsVals.put(dateTimeOnHourStr, totalVal + v);
			bucketsCounts.put(dateTimeOnHourStr, totalCount + 1);
		}
		
		// getting the averages across the hours
		println "###############";
		def avgsPerHour = new java.util.LinkedHashMap();
		bucketsVals.each { dateTimeOnHourStr, v ->
			int count = bucketsCounts.get(dateTimeOnHourStr)
			double avg = v / count;
			avgsPerHour.put(dateTimeOnHourStr, avg)
		}
		
		return avgsPerHour;
	}
	
	/**
	 */
	public static final printAvgsPerHourPerDay(avgsPerHour)
	{
		println "###############";
		def dailyHourlyStrs = new java.util.LinkedHashMap();
		avgsPerHour.each{ dateTimeOnHourStr, v ->
			def dateTimeOnHour = INTERNAL_DATE_TIME_FORMATTER.parse(dateTimeOnHourStr);
			def dateStr = DATE_FORMATTER.format(dateTimeOnHour.getTime());
			def hour = HOUR_FORMATTER.format(dateTimeOnHour.getTime());
			
			def hourlyMap = dailyHourlyStrs.get(dateStr);
			if (hourlyMap == null) {
				hourlyMap = generateHourlyMap();
				dailyHourlyStrs.put(dateStr, hourlyMap);
			}
			hourlyMap.put(hour, v);
		}
		
		dailyHourlyStrs.each { dateStr, v ->
			def line = dateStr;
			
			for (int i = 0; i < 24; i++) {
				line += ",";
				String formatted = String.format("%02d", i);
				if (v.containsKey(formatted)) {
					line += v.get(formatted);
				}
			}
			
			println line;
		}
	}
	
	private static generateHourlyMap()
	{
		def hourlyMap = new java.util.LinkedHashMap();
		for (int i = 0; i < 24; i++) {
			String formatted = String.format("%02d", i);
			hourlyMap.put(formatted, "");
		}
		
		return hourlyMap
	}
	
	/**
	    Date,Time,Combined,mmol/L
		25/05/17,18:13:00,25/05/17 18:13:00,1.1
		25/05/17,18:28:00,25/05/17 18:28:00,1.1
		25/05/17,18:43:00,25/05/17 18:43:00,1.1
		25/05/17,18:58:00,25/05/17 18:58:00,5.1
		25/05/17,19:06:00,25/05/17 19:06:00,4.8
		25/05/17,19:13:00,25/05/17 19:13:00,4.8
		25/05/17,19:26:00,25/05/17 19:26:00,5
		@returns Map of:
				yyyy/MM/dd HH:mm:ss -> 5.1
	 */
	public static final readLibreCSVFile(csvFile) throws Exception
	{
		def records = getRecords(csvFile)
		
		println "Reading file " + csvFile
	
		def numVals = 0
		def dateVals = [:]
		for (CSVRecord record : records) {
			def dateVal = record.getAt("Date");
			def timeVal = record.getAt("Time");
			double glucoseVal = Double.parseDouble(record.getAt("mmol/L"));
			
			def dateTimeStr = dateVal + " " + timeVal;
			Date dateTime = LIBRE_DATE_TIME_FORMATTER.parse(dateTimeStr);
			def formattedDateTimeStr = INTERNAL_DATE_TIME_FORMATTER.format(dateTime);
			if (dateVals.containsKey(formattedDateTimeStr)) {
				println("contains " + formattedDateTimeStr);
			}
			dateVals.put(formattedDateTimeStr, glucoseVal)
			numVals++
		}
		
		println "num vals = " + numVals;
				
		return dateVals
	}
	
	/**
	 */
	private static getRecords(csvFile)
	{
		Reader reader
		Iterable<CSVRecord> records = null
		try
		{
			reader = new FileReader(csvFile)
			records = CSVFormat.DEFAULT.withHeader().parse(reader) // header will be ignored
		}
		catch (IOException e) {
			throw new RuntimeException(e)
		}
		
		return records
	}
	
	private static filter(csvRow)
	{
		def status = csvRow.get("status")
		if ("dispatched" == status) {
			return true
		}
		else {
			return false
		}
	}
}
