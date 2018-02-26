package net.justinlawler.qs.taplog

import java.text.DateFormat
import java.text.ParsePosition
import java.text.SimpleDateFormat

@Grapes([
	@Grab(group='commons-io', module='commons-io', version='2.4'),
	@Grab(group='commons-lang', module='commons-lang', version='2.6'),
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6')
])
class ParseTapLogData {

	public static final String TAP_LOG_DATA_DIR = '/Users/justinlawler/Dropbox/projects/life_logging/data/taplog/'
	private static final String TAPLOG_DATA_FILE = 'Tap Log Export_test_data.csv'
	
	private DateFormat dateForamtter = new SimpleDateFormat('yyyy-MM-dd')
	

	/**
	 */
	public static void main(String[] args) {
		def tapLogFile = new File(TAP_LOG_DATA_DIR + TAPLOG_DATA_FILE)
		
		def glucoseVals = ["Glucose - Left ", "Glucose - Right ", "Glucose"]
		def mergedContents = mergeValues_SinglePointPerDay(tapLogFile, glucoseVals)
		printValues(mergedContents, glucoseVals, '%.3f')
	}
	
	/**
	 */
	private static printValues(mergedContents, valsToPrint, formatStr)
	{
		def firstDate = mergedContents.keySet().iterator().next()
		def namesStrBldr = new StringBuilder("Date\t")
		for (def valName : valsToPrint) {
			namesStrBldr.append('\t')
			namesStrBldr.append(valName)
		}
		println namesStrBldr
		
		for (def entryDate : mergedContents.keySet()) {
			def entry = mergedContents.get(entryDate)
			def strBldr = new StringBuilder(entryDate + "\t")
			for (def valName : valsToPrint) {
				strBldr.append('\t')
				def val = entry.get(valName)
				if (val != null) {
					strBldr.append(sprintf(formatStr, val))
				}
				else {
					strBldr.append(' ')
				}
			}
			println strBldr
		}
	}
	
	/**
	 * @returns
	 * [
	 *		'2016-11-01': ["Glucose" : 5.0, "Glucose-Left" : 4.8, "Glucose-Right" : 5.2 ], 
	 * 		'2016-11-02': ["Glucose" : 5.1, "Glucose-Left" : 4.9, "Glucose-Right" : 5.3 ],
	 *      '2016-11-04': ["Glucose" : 5.2, "Glucose-Left" : 5.1, "Glucose-Right" : 5.5 ]
	 * ]
	 */
	private static mergeValues_SinglePointPerDay(taplogFile, valsToPrint)
	{
		def fileContents = parseTapLogData(taplogFile)
		
		def mergedFileContents = [:]
		for (def entry : fileContents) {
			if (valsToPrint.indexOf(entry.action) >= 0) {
				def dateStr = entry.date.format('yyyy-MM-dd')
				def mergedEntry = mergedFileContents[dateStr]
				if (mergedEntry == null) {
					mergedEntry = [:]
					mergedFileContents[dateStr] = mergedEntry
				}
				mergedEntry[entry.action] = entry.value
			}
			else {
				if (entry.action.indexOf('Glucose') >= 0) {
					println entry
				}
			}
		}
		
		return mergedFileContents.sort { it.key }
	}
	
	
	/**
	"Milliseconds","timezoneOffset","timestamp","DayOfYear","DayOfMonth","DayOfWeek","TimeOfDay","_id","cat1","cat2","cat3","cat4","cat5","cat6","cat7","cat8","cat9","number","note","gpstime","gpsMilliseconds","name","latitude","longitude","accuracy","altitude","speed","bearing","lat_text","lon_text","street","city","state","country","zip"
	"1470726056024","3600000","2016-08-09T08:00:56.024+01:00","222","9","Tuesday","8.015555555555556","2099","Morning Tracking","Glucose - Left ",,,,,,,,"5.4",,,,,,,,,,,,,,,,,
	"1479196836627","0","2016-11-15T08:00:36.627+00:00","320","15","Tuesday","8.01","3891","Diet","Food",,,,,,,,,,,,,,,,,,,,,,,,,
	*/
	private static parseTapLogData(taplogFile) {
		
		def fileContents = []
		if (!taplogFile.exists()) {
			println "File does not exist " + taplogFile
		}
		else {
			taplogFile.eachLine { line ->
				   	if (line.trim().size() == 0) {
					   throw new RuntimeException("null line")
				   	}
					else if (line.trim().indexOf('Milliseconds') > 0) {
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
	 * @param tapLogLine
	 * 	"1470726056024","3600000","2016-08-09T08:00:56.024+01:00","222","9","Tuesday","8.015555555555556","2099","Morning Tracking","Glucose - Left ",,,,,,,,"5.4",,,,,,,,,,,,,,,,,
     *
	 * @returns
	 * [
	 *		action: "Glucose",
	 * 		timeStr: "2016-11-15T06:17:35.601+00:00",
	 *      date: 2016-11-15T06:17:35.601+00:00,
	 *      value: 4.8
	 * ]
	 */
	private static parseFileLine(tapLogLine) {
		
		def tokens = tapLogLine.tokenize(',')
		def timeStr = tokens[2].replaceAll('^\"|\"$', '')
		java.util.Calendar cal = javax.xml.bind.DatatypeConverter.parseDateTime(timeStr)
		java.util.Date date = cal.getTime()
		def action = tokens[9].replaceAll('^\"|\"$', '')
		def value = tokens[10] != null ? Double.parseDouble(tokens[10].replaceAll('^\"|\"$', '')) : 0 
		
		//println " " + date + " " + timeStr + " " + action + " " + value
		
		return [
			action: action,
			timeStr: timeStr,
			date: date,
			value: value
			]
	}
}
