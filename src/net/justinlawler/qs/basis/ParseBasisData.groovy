package net.justinlawler.qs.basis

import java.text.SimpleDateFormat
import java.util.logging.Logger

import org.joda.time.*
import org.joda.time.format.*

import groovy.io.FileType;
import groovy.json.JsonSlurper

@Grapes([
	@Grab(group='commons-io', module='commons-io', version='2.4'),
	@Grab(group='commons-lang', module='commons-lang', version='2.6'),
	@Grab(group='joda-time', module='joda-time', version='2.9.2'),
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6')
])
class ParseBasisData {
	
	static def logger = Logger.getLogger("ParseBasisData")
	
	static def DATE_FORMATTER_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	static def DATE_FORMATTER_DATE = new SimpleDateFormat("yyyy-MM-dd");
	static def DATE_FORMATTER_TIME = new SimpleDateFormat("HH:mm:ss");

	public static final String BASIS_FILE_ROOT = "basis-data-"
	//public static final String BASIS_DATA_DIR = "/Users/justinlawler/Dropbox/projects/basis_band/basis-data-export-master/data";
	public static final String BASIS_DATA_DIR = "/Users/justinlawler/Tools/basis-data-export-master/data"
	private static final String SLEEP_JSON_FILE = "basis-data-2015-12-30-sleep.json"
	private static final String METRICS_JSON_FILE = "basis-data-2015-12-17-metrics.json"
	
	private static final long SLEEP_TOLLERANCE = 65 * 60 * 1000
	
	
	/**
	 */
	public static void main(String[] args) {
		
//		def activities = parseSleep(new File(BASIS_DATA_DIR + "/" + SLEEP_JSON_FILE))
//		activities = consolodateSleepActivities(activities, SLEEP_TOLLERANCE)
//		printSleepActivities(activities)
		//parseMetrics(new File(BASIS_DATA_DIR + "/" + METRICS_JSON_FILE))
		
		iterateOverAllSleepFiles(BASIS_DATA_DIR)
		//iterateOverAllMetricFiles(BASIS_DATA_DIR)
	}
	
	/**
	 */
	private static def iterateOverAllSleepFiles(dirStr) {
		
		def sleepFiles = getFilesInDir(dirStr, ".*sleep.json")
		def sleepActivities = []
		sleepFiles.each {
			sleepActivities.addAll(parseSleep(new File(dirStr, it)))
		}
		sleepActivities = consolodateSleepActivities(sleepActivities, SLEEP_TOLLERANCE)
		printSleepActivities(sleepActivities)
	}
	
	/**
	 */
	private static def iterateOverAllMetricFiles(dirStr) {
		
		def metricFiles = getFilesInDir(dirStr, ".*metrics.json")
		metricFiles.each {
			parseMetrics(new File(dirStr, it))
		}
	}
	
	/**
	 */
	private static def getFilesInDir(dirStr, matchingString) {

		File dirFile = new File(dirStr)
		def files = []
		dirFile.eachFile FileType.FILES, {
			 if (it.name.matches(matchingString)) {
				 files << it.name
			 }
		}
		
		files.sort()
		
		return files
	}
	
	/**
	 */
	private static def parseSleep(File file) {
		
		def slurper = new JsonSlurper();
		def result = slurper.parse(file.newInputStream())
		
		def sleepActivities = []
		
		for (def activity : result.content.activities) {
			
			def startTime = parseDateStr(activity.start_time.iso).getMillis()
			def endTime = parseDateStr(activity.end_time.iso).getMillis()
			
			if (startTime > 0) {
				sleepActivities << [
					start_time: startTime,
					end_time: endTime,
					rem: activity.sleep.rem_minutes,
					light: activity.sleep.light_minutes,
					deep: activity.sleep.deep_minutes,
					interruptions: activity.sleep.interruptions,
					interupt_mins:  (activity.sleep.interruption_minutes + activity.sleep.unknown_minutes),
					toss: activity.sleep.toss_and_turn,
					avg_heart_rate: activity.heart_rate.avg
				]
			}
			else {
				logger.warning("No sleep data for file " + file.getName())
			}
		}
		
		return sleepActivities
	}
	
	/**
	 * @param activities
	 * @param cutOff
	 * @return
	 */
	private static def consolodateSleepActivities(activities, cutOff) {
		
		def consolidatedActivities = []
		def lastActivity = null
		for (def activity : activities) {
			def consolidateWithLast = false
			def diff = 0
			if (lastActivity != null) {
				diff = activity.start_time - lastActivity.end_time
				if (diff < SLEEP_TOLLERANCE) {
					consolidateWithLast = true
				}
			}
			if (consolidateWithLast) {
				def startTime = lastActivity.start_time
				def endTime = activity.end_time
				def oldMins = lastActivity.rem + lastActivity.light + lastActivity.deep + lastActivity.interupt_mins
				def newMins = activity.rem + activity.light + activity.deep + activity.interupt_mins
				def rem = lastActivity.rem + activity.rem
				def light = lastActivity.light + activity.light
				def deep = lastActivity.deep + activity.deep
				def interupt_mins = lastActivity.interupt_mins + activity.interupt_mins + (diff / 1000 / 60)
				def interruptions = lastActivity.interruptions + activity.interruptions + 1
				def toss = lastActivity.toss + activity.toss
				
				consolidatedActivities.remove(lastActivity)
				activity = [
					start_time: startTime,
					end_time: endTime,
					rem: rem,
					light:light,
					deep: deep,
					interruptions: interruptions,
					interupt_mins: interupt_mins,
					toss: toss
				]
			}
			consolidatedActivities << activity
			lastActivity = activity
		}
		
		return consolidatedActivities
	}
	
	/**
	 * @param activities
	 * @return
	 */
	private static def printSleepActivities(activities) {
	
		activities.each { activity -> 
			def metrics = getMetricsForActivity(activity.start_time, activity.end_time)
			printSleepActivity(activity, metrics)
		}
	}
	
	/**
	 */
	private static def printSleepActivity(activity, metrics) {
		
		def separator = ','
				
		def strBldr = new StringBuilder("")
		strBldr.append(ISODateTimeFormat.date().print(activity.end_time))
		strBldr.append(separator)
		strBldr.append(ISODateTimeFormat.hourMinuteSecond().print(activity.start_time))
		strBldr.append(separator)
		strBldr.append(ISODateTimeFormat.hourMinuteSecond().print(activity.end_time))
		strBldr.append(separator)
		strBldr.append(activity.rem)
		strBldr.append(separator)
		strBldr.append(activity.light)
		strBldr.append(separator)
		strBldr.append(activity.deep)
		strBldr.append(separator)
		strBldr.append(activity.interupt_mins)
		strBldr.append(separator)
		strBldr.append(activity.toss)
		strBldr.append(separator)
		strBldr.append(activity.interruptions)
		strBldr.append(separator)
		strBldr.append(sprintf('%.3f', metrics.avg_heart_rate))
		strBldr.append(separator)
		strBldr.append(sprintf('%.3f', metrics.calories))
		strBldr.append(separator)
		strBldr.append(sprintf('%.3f', metrics.avg_temp))
		strBldr.append(separator)
		strBldr.append(sprintf('%.5f', metrics.avg_gsr))
		
		println strBldr
	}
	
	/**
	 * @param startTime		In milliseconds
	 * @param endTime		In milliseconds
	 */
	private static def getMetricsForActivity(startTime, endTime) {

		def startMetricsFile = getFileForDate(startTime, "metrics")
		def endMetricsFile = getFileForDate(endTime, "metrics")
		
		if (startMetricsFile != endMetricsFile) {
			def startDateTime = new DateTime(startTime)
			def endDateTime = new DateTime(endTime)
			
			def endDayOne = new DateTime(
					startDateTime.getYear(), startDateTime.getMonthOfYear(), startDateTime.getDayOfMonth(), 23, 59, 59)
			def startMetrics = parseMetricsFromFile(startTime, endDayOne.getMillis(), startMetricsFile)
			
			def startDayTwo = new DateTime(
					endDateTime.getYear(), endDateTime.getMonthOfYear(), endDateTime.getDayOfMonth(), 0, 0, 0)
			def endMetrics = parseMetricsFromFile(startDayTwo.getMillis(), endTime, endMetricsFile)
			
			return sumMetrics( [startMetrics, endMetrics])
		}
		else {
			return parseMetricsFromFile(startTime, endTime, startMetricsFile)
		}
	}
	
	/**
	 * @param metrics
	 * @return
	 */
	private static def sumMetrics(metrics) {
		
		def totalPeriods = 0
		def totalCals = 0.0d
		def totalHeartRate = 0.0d
		def totalTemp = 0.0d
		def totalGsr = 0.0d
		
		metrics.each {
			totalPeriods += it.num_periods
			totalCals += it.calories
			totalHeartRate += (it.num_periods * it.avg_heart_rate)
			totalTemp += (it.num_periods * it.avg_temp)
			totalGsr += (it.num_periods * it.avg_gsr)
		}

		return [
			num_periods: totalPeriods,
			calories: totalCals,
			avg_heart_rate: (totalHeartRate / totalPeriods),
			avg_temp: (totalTemp / totalPeriods),
			avg_gsr: (totalGsr / totalPeriods)
		]
	}
	
	/**
	 */
	private static def parseMetricsFromFile(startTime, endTime, metricsFile) {
		
		if (!metricsFile.exists()) {
			throw new RuntimeException("can't find file " + metricsFile)
		}
	
		def slurper = new JsonSlurper();
		def parsedJson = slurper.parse(metricsFile.newInputStream())
		
		long startDay = parsedJson.starttime * 1000l
		long interval = parsedJson.interval * 1000l
		
		def calories = sumElements(startTime, endTime, startDay, interval, parsedJson.metrics.calories.values)
		def heartRate = sumElements(startTime, endTime, startDay, interval, parsedJson.metrics.heartrate.values)
		def skinTemp = sumElements(startTime, endTime, startDay, interval, parsedJson.metrics.skin_temp.values)
		def gsr = sumElements(startTime, endTime, startDay, interval, parsedJson.metrics.gsr.values)
		
		return [
			num_periods: calories.periods,
			calories: calories.total,
			avg_heart_rate: heartRate.avg,
			avg_temp: (skinTemp.avg - 32) * (5.0 / 9.0),
			avg_gsr: gsr.avg
		]
	}
	
	/**
	 * @param startTime Start of the period to sum
	 * @param endTime   End of the period to sum
	 * @param startDay  Start of the day - in milliseconds
	 * @param interval  Between each element in array
	 * @param elements	Elements to work on
	 * @return
	 */
	private static def sumElements(startTime, endTime, startDay, interval, elements) {
		
		def currentTime = startDay
		int periods = 0
		double total = 0.0d
		elements.each {
			if (currentTime >= startTime && currentTime < endTime) {
				periods++
				if (it != null) {
					total += it
				}
			}
			currentTime += interval
		}
		double avg = total / periods
		
		return [
			total: total,
			avg: avg,
			periods: periods
		]
	}
	
	/**
	 */
	private static def parseMetrics(File file) {
		
		def slurper = new JsonSlurper();
		def result = slurper.parse(file.newInputStream())
		
		long dayTime = result.starttime * 1000l
		
		double totalCalories = 0.0;
		for (def calorie : result.metrics.calories.values) {
			if (calorie != null) {
				totalCalories += calorie
			}
			else {
				//println "null"
			}
		}
		
		def strBldr = new StringBuilder()
		strBldr.append(ISODateTimeFormat.date().print(dayTime))
		strBldr.append(" ")
		strBldr.append(sprintf('%.3f', totalCalories))
		
		println strBldr
	}
	
	/**
	 * 
	 * @param date - format 2015-12-11 2846.900
	 * @param fileType  - metrics, sleep, etc
	 * @return basis-data-2015-11-13-metrics.json
	 */
	private static def getFileForDate(date, fileType) {
		return new File(BASIS_DATA_DIR,
			BASIS_FILE_ROOT + ISODateTimeFormat.date().print(date) + "-" + fileType + ".json")
	}
	
	/**
	 * @param dateStr - format - 2015-11-14T06:35:00Z
	 * @return
	 */
	private static def parseDateStr(dateStr) {
		
		DateTimeFormatter parser    = ISODateTimeFormat.dateTimeParser()
		DateTime dateTime = parser.parseDateTime(dateStr)
		return dateTime
	}
}

