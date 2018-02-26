package net.justinlawler.qs.withings

import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.text.SimpleDateFormat
import java.util.logging.Logger
import java.util.logging.Level

import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.commons.io.output.NullOutputStream

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
class ParseWithingsWeightCVSFiles {

	static def logger = Logger.getLogger("ParseWithingsWeightCVSFiles")
	
	private static final String DATA_DIRECTORY = "/Users/justinlawler/Tools/withings/data/"
	
	// Time ranges for dates
	private static final int NIGHT_MIN = 20
	private static final int NIGHT_MAX = 5
	private static final int MORNING_MIN = 5
	private static final int MORNING_MAX = 12
		
	public static final String PROJECT_DIR = '/Users/justinlawler/Dropbox/projects/biohacking_justin/parsing_stats'
	private static final String DATA_FILE = '/src/test_data/withings/test_withings_data.csv'
	private static final String LIVE_DATA_DIR = '/Users/justinlawler/Tools/withings/weight/'
	private static final String DATA_FILE_2016 = 'Withings - Weight Justin_2016.csv'
	
	//   2016/01/03 07:33:01.480380
	private static final SimpleDateFormat DATE_TIME_FORMATTER = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 */
	public static void main(String[] args)
	{
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
		
//		def files = getListOfFiles(DATA_DIRECTORY)
//		for (def file : files) {
//			printAvgValsForFile(file)
//		}
		
		//File file = new File(PROJECT_DIR + DATA_FILE)
		File file = new File(LIVE_DATA_DIR, DATA_FILE_2016)
		def weights = parseAllWeights(file)
		def dailyData = parsePerDay(weights)
		printDailyData(dailyData)
	}
	
	/**
	  @param list of dates and weight breakdowns
	  [
		    2017/01/13 : [
		  		morning_weight: 63.9,
		  		morning_fatMass: 8.08,
		  		morning_leanMass: 55.33,
		        night_weight: 64.5,
		        night_fatMass: 8.5,
		  		night_leanMass: 56.4,
		    ]
		 ]
		 Prints out:
		 	- 2017/01/13, 63.9, 8.08, 55.33, 64.5, 8.5, 56.4
	 */
	private static void printDailyData(dailyData) {
		
		println "###### Daily Data"
		println "Date, Morning Weight, Morning Fat Mass, Morning Lean Mass, Evening Weight, Evening Fat Mass, Evening Lean Mass"
		def dateKeys = dailyData.sort()*.key
		for (def dateStr in dateKeys) {
			def vals = dailyData.get(dateStr)
			
			def strBldr = new StringBuilder("")
			strBldr.append(dateStr)
			strBldr.append(",")
			strBldr.append(vals.morning_weight != null ? sprintf('%.2f',  vals.morning_weight) : "")
			strBldr.append(",")
			strBldr.append(vals.morning_fatMass != null ? sprintf('%.2f',  vals.morning_fatMass) : "")
			strBldr.append(",")
			strBldr.append(vals.morning_leanMass != null ? sprintf('%.2f',  vals.morning_leanMass) : "")
			strBldr.append(",")
			strBldr.append(vals.night_weight != null ? sprintf('%.2f',  vals.night_weight) : "")
			strBldr.append(",")
			strBldr.append(vals.night_fatMass != null ? sprintf('%.2f',  vals.night_fatMass) : "")
			strBldr.append(",")
			strBldr.append(vals.night_leanMass != null ? sprintf('%.2f',  vals.night_leanMass) : "")
			println strBldr
		}
	}
	
	/**
	 * 3 cases it needs to be able to handle
	 * 	- Morning & evening both on same day
	 * 	- Missing morning or evening weight 
	 *  - Evening weight after midnight
	 *  	- 2016-12-26 1:11 AM
	 *  	- Sun Dec 25 01:11:00 GMT 2016
	 *  	- Sat Feb 27 04:24:00 GMT 2016
	 * 
	   @param weights
	  	[
		  	[
		 		dateTime: java.util.Date,
		  		weight: 63.41,
		        fatMass: 8.08,
		        leanMass: 55.33
		    ],
		    [
		 		dateTime: java.util.Date,
		  		weight: 63.41,
		        fatMass: 8.08,
		        leanMass: 55.33
		    ]
		 ]
	  @return list of dates and weight breakdowns
	  [
		    2017/01/13 : [
		  		morning_weight: 63.9,
		  		morning_fatMass: 8.08,
		  		morning_leanMass: 55.33,
		        night_weight: 64.5,
		        night_fatMass: 8.5,
		  		night_leanMass: 56.4,
		    ]
		 ]
	 */
	private static parsePerDay(weights) {
		
		def parsedWeights = [:]
		weights.each {
			
			def date = it.dateTime
			def hour = date[Calendar.HOUR_OF_DAY]
			if (isNightTimeMeasurement(date) && (hour >=0 && hour < NIGHT_MAX)) {
				logger.info("weight after midnight")
				date = date.minus(1)
			}
			
			
			def dateStr = DATE_FORMATTER.format(it.dateTime)
			def parsedWeight = parsedWeights.get(dateStr)
			if (parsedWeight == null) {
				parsedWeight = [:]
				parsedWeights.put(dateStr, parsedWeight)
			}
			
			if (isMorningTimeMeasurement(date)) {
				// it's a morning time weight
				parsedWeight << [morning_weight: it.weight]
				parsedWeight << [morning_fatMass: it.fatMass]
				parsedWeight << [morning_leanMass: it.leanMass]
			}
			else if (isNightTimeMeasurement(date)) {
				// it's a night time weight
				parsedWeight << [night_weight: it.weight]
				parsedWeight << [night_fatMass: it.fatMass]
				parsedWeight << [night_leanMass: it.leanMass]
			}
			else {
				logger.warning("date time not recognised - " + date)
			}
		}
		
		return parsedWeights
	}
	
	/**
	 * Format:
	 * 
	    "Date","Weight (kg)","Fat mass (kg)","Lean mass (kg)","Comments"
		"2016-05-31 6:36 AM","62.77","8.37","54.4",""
		"2016-05-30 10:28 PM","63.82","8.39","55.43",""
		"2016-05-30 7:16 AM","63.24","8.48","54.76",""
		"2016-05-29 10:49 PM","64.35","8.58","55.77",""
		"2016-05-29 8:11 AM","64.16","8.52","55.64",""
		"2016-05-29 12:34 AM","65.67","","",""
		"2016-05-29 12:33 AM","65.7","","",""
		"2016-05-28 7:16 AM","63.1","8.19","54.91",""
		"2016-05-27 9:30 PM","64.66","8.46","56.2",""
		"2016-05-27 7:18 AM","63.41","8.08","55.33",""
	 */
	private static parseAllWeights(cvsFile) {
		
		def fileContents = []
		if (!cvsFile.exists()) {
			println "File does not exist " + cvsFile
		}
		else {
			cvsFile.eachLine { line ->
					   if (line.trim().size() == 0) {
					   throw new RuntimeException("null line")
					   }
					else if (line.trim().indexOf('Date') >= 0) {
						//
						println "Ignoring first line"
					}
					else {
						fileContents << parseFileLine(line)
					}
			}
		}
		
		return fileContents
	}
	
	/**
	 * @param line - Format
	 * 			"2016-05-28 7:16 AM","63.1","8.19","54.91",""
	 * 
	 * @returns
	 * [
	 *		dateTime: java.util.Date,
	 * 		weight: 63.41,
	 *      fatMass: 8.08,
	 *      leanMass: 55.33
	 * ]
	 */
	private static parseFileLine(line)
	{
		def tokens = line.tokenize(',')
		def timeStr = removeQuotes(tokens[0])
		java.util.Date dateTime = DATE_TIME_FORMATTER.parse(timeStr)
		def weight = returnDoubleValue(tokens[1])
		def fatMass = returnDoubleValue(tokens[2])
		def leanMass = returnDoubleValue(tokens[3])
		
		// println " " + DATE_FORMATTER.format(dateTime) + " " + weight + " " + fatMass + " " + leanMass
		
		return [
			dateTime: dateTime,
			weight: weight,
			fatMass: fatMass,
			value: leanMass
			]
	}
	
	private static final returnDoubleValue(quotedStr)
	{
		def strippedQuotesStr = removeQuotes(quotedStr)
		if (StringUtils.isBlank(strippedQuotesStr)) {
			return null
		}
		else {
			Double.parseDouble(removeQuotes(strippedQuotesStr))
		}		
	}
	
	/**
	 */
	private static final removeQuotes(str) {
		return str.replaceAll('^\"|\"$', '')
	}
	
	/**
	 */
	private static final boolean isMorningTimeMeasurement(date)
	{
		def hour = date[Calendar.HOUR_OF_DAY]
		return hour >= MORNING_MIN && hour < MORNING_MAX
	}
	
	/**
	 */
	private static final boolean isNightTimeMeasurement(date)
	{
		def hour = date[Calendar.HOUR_OF_DAY]
		return (hour >= NIGHT_MIN && hour <= 23) || (hour >=0 && hour < NIGHT_MAX)
	} 
}
