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
class ParseWithingsJsonFiles {
	
	static def logger = Logger.getLogger("ParseWithingsJsonFiles")
	
	private static final String DATA_DIRECTORY = "/Users/justinlawler/Tools/withings/data/"
	
	private static final int START_HOUR = 3
	private static final int END_HOUR = 6
		
	private static final String QM_SESSIONS_STR = "http://www.quantified-mind.com/query"
	private static final String COFFEE_EXPERIMENT = '5951537408901120'
	private static final String COMVITA_EXPERIMENT = '6421288643985408'
	
	//   2016/01/03 07:33:01.480380
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd");

	/**
	 */
	public static void main(String[] args) {
		
		DATE_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		def files = getListOfFiles(DATA_DIRECTORY)
		for (def file : files) {
			printAvgValsForFile(file)
		}
	}
	
	/**
	 * Returns sorted list of json files in the directory
	 */
	private static getListOfFiles(dataDirectory) {
		
		def baseDir = new File(dataDirectory);
		def files = []
		baseDir.eachFileMatch(~/.*.json/) { file ->
		    files.add(file)
		}
		
		
		files.sort{ a,b -> a.name <=> b.name }
		
		return files
	}
	
	/**
	 */
	private static printAvgValsForFile(jsonFile) {
		
		def dataSeries = parseJsonFile(jsonFile)
		def avgTemp = getAvgTime(jsonFile, dataSeries.temp, START_HOUR, END_HOUR)
		def avgCo2 = getAvgTime(jsonFile, dataSeries.co2, START_HOUR, END_HOUR)
		
		long timeSecs = dataSeries.temp.keySet().iterator().next()
		Calendar cal = Calendar.getInstance()
		cal.setTimeInMillis(timeSecs * 1000)
		//println avgTemp + " " + avgCo2
		printf(DATE_FORMATTER.format(cal.getTime()) + ", %.2f, %.2f \n", avgTemp, avgCo2)
	}
	
	
	private static getAvgTime(jsonFile, dataSeries, startHour, endHour) {
		
		if (dataSeries == null) {
			throw new RuntimeException("no data for file " + jsonFile)
		}
		
		def numVals = 0
		def sumVals = 0.0
		Calendar cal = Calendar.getInstance()
		for (def dateMillis : dataSeries.keySet()) {
			cal.setTimeInMillis(dateMillis * 1000l)
			int hour = cal.get(Calendar.HOUR)
			if (hour >= startHour && hour < endHour) {
				numVals++
				sumVals += dataSeries.get(dateMillis)
			}
		}
		
		if (numVals == 0) {
			return 0.0f
		}
		else {
			return sumVals / numVals
		}
	}
	
	/**
	 */
	private static parseJsonFile(jsonFile) {
		
		if (!jsonFile.exists()) {
			throw new RuntimeException("File does not exist " + jsonFile)
		}
		else {
			def slurper = new JsonSlurper();
			def result = slurper.parse(jsonFile)
			
			def tempSeries = null
			def co2Series = null
			for (def dataSeries : result.body.series) {
				if (dataSeries.type == 12) {
					tempSeries = parseDataSeries(dataSeries)
				}
				else if (dataSeries.type == 35) {
					co2Series = parseDataSeries(dataSeries)
				}
				else {
					throw new RuntimeException("Unknown data series type " + dataSeries.type)
				}
			}
			
			return [ "temp" : tempSeries, "co2" : co2Series]
		}
	}
	
	/**
	 */
	private static parseDataSeries(tempJsonSeries) {
		
		def tempSeries = [:]
		for (def dataPoint : tempJsonSeries.data) {
			tempSeries.put(dataPoint.date, dataPoint.value)
		}
		
		return tempSeries.sort { it.key }
	}
}
