package net.justinlawler.qs.qm

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
class DownloadQuantifiedMindData {
	
	static def logger = Logger.getLogger("DownloadQuantifiedMindData")
	
	private static final String QM_URL = "http://www.quantified-mind.com"
	private static final String USERNAME_PARAM = "username"
	private static final String TOKEN_PARAM = "token"
	
	private static final String USER_NAME = "jdpl28"
	private static final String TOKEN_ID = "4386f571f1e26bd8690a0ee243f397eb"
	
	private static final String QM_SESSIONS_STR = "http://www.quantified-mind.com/query"
	private static final String COFFEE_EXPERIMENT = '5951537408901120'
	private static final String COMVITA_EXPERIMENT = '6421288643985408'
	
	//   2016/01/03 07:33:01.480380
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	/**
	 */
	public static void main(String[] args) {
		
		def testResults = downloadBatteryResults(COMVITA_EXPERIMENT, null)
		printTestResults(testResults)
	}
	
	/**
	 * Returns:
	 *	[
	 *		2016/01/03={start_time=07:23, overall_time=457, results={color_word=500.07597375354311, simple_n_back=607.63835167506966}),
	 *		2016/01/04={start_time=07:23, overall_time=457, results={color_word=515.22317098142094, simple_n_back=593.67043528490444}},
	 *		2016/01/16={start_time=07:23, overall_time=457, results={color_word=539.34532945424462, simple_n_back=644.06702466421348}}
	 *	]
	 */
	private static downloadBatteryResults(batteryId, start_date) {
		
		def sessionIds = downloadQMSessions(batteryId)		// All sessions of this experiment
		
		def allResults = [:]
		
		int count = 0
		int cutOff = -1
		for (def sessionId : sessionIds) {
			def sessionResults = downloadQMSession(batteryId, sessionId)
			def testResultsMap = [:]
			def startTimeStr = ""
			def totalTime = 0
			for (def testId : sessionResults.tests) {
				def testResults = downloadQMSessionTestResults(batteryId, sessionId, testId)
				startTimeStr = getBeforeDate(startTimeStr, testResults.start_time);
				totalTime += getTestTimeInSeconds(testResults.start_time, testResults.end_time)
				testResultsMap.put(
					testResults.test_name,
					testResults.score)
			}
			
			def key = sessionResults.date + " " + startTimeStr
			allResults.put(key, [
				"date" : sessionResults.date,
				"start_time" : startTimeStr,
				"overall_time" : totalTime,
				"results" : testResultsMap.sort { it.key }
			])
			count++
			if (cutOff > 0 && count >= cutOff) break
		}
		
		return allResults.sort { it.key }
	}
	
	/**
	 * Results in the format of:
	 * 
	 * 	[
	 *		2016/01/03={start_time=07:23, overall_time=457, results={color_word=500.07597375354311, simple_n_back=607.63835167506966}),
	 *		2016/01/04={start_time=07:23, overall_time=457, results={color_word=515.22317098142094, simple_n_back=593.67043528490444}},
	 *		2016/01/16={start_time=07:23, overall_time=457, results={color_word=539.34532945424462, simple_n_back=644.06702466421348}}
	 *	] 
     *
	 * Prints in the format of:
	 *      date 		Time 	Secs	color_word	simple_n_back 	Avg
	 *		2016/01/27	07:24	462 	500.075 	607.63 			553.422
	 */
	private static printTestResults(testResults) {
		
		// first print headers
		def firstDate = testResults.keySet().iterator().next()
		def testNames = testResults.get(firstDate).get("results")
		def namesStrBldr = new StringBuilder("Date\tTime\tSecs")
		for (def testName : testNames.keySet()) {
			namesStrBldr.append('\t')
			namesStrBldr.append(testName)
		}
		namesStrBldr.append("\tAvg")
		println namesStrBldr
		
		for (def dateTime : testResults.keySet()) {
			def session = testResults.get(dateTime)
			def startTime = session.get("start_time")
			def secs = session.get("overall_time")
			def results = session.get("results")
			def sum = 0.0
			def strBldr = new StringBuilder(session.date + "\t" + startTime + "\t")
			strBldr.append(sprintf('%.3f', secs))
			for (def testName : results.keySet()) {
				strBldr.append('\t')
				def val = results.get(testName)
				strBldr.append(sprintf('%.3f', val))
				sum += val
			}
			def avg = sum / results.size()
			strBldr.append('\t' + sprintf('%.3f', avg))
			println strBldr
		}
	}

	/**
	 * Returns all sessions for the battery
	 * 
	 * @param batteryId		The ID of the battery
	 * @returns
	 * [
	 * 		
	 * ]
	 */
	private static downloadQMSessions(batteryId) {
		
		logger.info("downloadQMSessions()")
		
		def http = new HTTPBuilder('http://www.quantified-mind.com/query/' + batteryId)

		http.request(GET, TEXT) {
			req ->
				uri.path = 'http://www.quantified-mind.com/query/' + batteryId
				uri.query = [ username: USER_NAME,
							  token: TOKEN_ID,
							  content: 'json' ]
				headers.'Accept' = 'application/json'
				headers.'Content-Type' = 'application/json'

				response.success = { resp, reader ->
					assert resp.status == 200
					logger.info('My response handler got response: ' + resp.statusLine)
					logger.info("Response length: " + resp.headers.'Content-Length')
//					System.out << reader // print response reader
					def slurper = new JsonSlurper();
					def result = slurper.parse(reader)
					return result.sessions[0]
				}

				response.failure = { resp, reader ->
					logger.warning('failure... - ' + resp.status)
					logger.warning('My response handler got response: ' + ${resp.statusLine})
					logger.warning('Response length: $' + {resp.headers.'Content-Length'})
					System.out << reader // print response reader
					throw new Exception("Failure on authentication")
				}
		}
	}
	
	/**
	 * Session information for the test session
	 * 
	 * Returns:
	 * [
	 * 		date: 2016-01-12,
	 * 		start_time: 10:38:53.663190,
	 * 		end_time: 10:39:47.385220,
	 * 		tests: [
	 *	        6214495884017664, 
	 *	        5198325143830528, 
	 *	        6147881343909888, 
	 *	        6625378628009984, 
	 *	        4819923761102848, 
	 *	        5086373868470272
	 *	    ]
	 * ]
	 */
	private static downloadQMSession(batteryId, sessionId) {
		
		logger.info("downloadQMSession()")
		
		def downloadUrl = QM_SESSIONS_STR + '/' + batteryId + '/' + sessionId
		logger.info("Downloading session info from " + downloadUrl);
		def http = new HTTPBuilder(downloadUrl)

		http.request(GET, TEXT) {
			req ->
				uri.path = downloadUrl
				uri.query = [ username: USER_NAME,
							  token: TOKEN_ID,
							  content: 'json' ]
				headers.'Accept' = 'application/json'
				headers.'Content-Type' = 'application/json'

				response.success = { resp, reader ->
					assert resp.status == 200
					logger.info('My response handler got response: ' + resp.statusLine)
					logger.info("Response length: " + resp.headers.'Content-Length')
//					System.out << reader // print response reader
					def slurper = new JsonSlurper();
					def result = slurper.parse(reader)
					println result.start_time + " " + result.end_time + " " + result.battery
					def date = result.start_time.split(" ")[0]
					return [
						"date" : date,
						"start_time": result.start_time,
						"end_time": result.end_time,
						"tests": result.tests ]
				}

				response.failure = { resp, reader ->
					logger.warning('failure... - ' + resp.status)
					logger.warning('My response handler got response: ' + resp.statusLine)
					logger.warning("Response length: " + resp.headers.'Content-Length')
					System.out << reader // print response reader
					throw new Exception("Failure on authentication")
				}
		}
	}
	
	/**
	 * Returns individual test result in a battery session
	 * [
	 * 		test_name=stroop,
	 * 		start_time=07:23:26,
	 * 		end_time=07:29:43,
	 * 		score=632.56,
	 * 		score_stderr=0.436
	 * ]
	 */
	private static downloadQMSessionTestResults(batteryId, sessionId, testId)
	{
		logger.info("downloadQMSessionTestResults()")

		// Initialize a new builder and give a default URL
		def downloadUrl = QM_SESSIONS_STR + '/' + batteryId + '/' + sessionId + '/' + testId
		logger.info("Downloading session test results from " + downloadUrl);
		def http = new HTTPBuilder(downloadUrl)

		http.request(GET, TEXT) {
			req ->
				//uri.path = downloadURL
			    uri.path = downloadUrl
				uri.query = [
					username: USER_NAME,
					token: TOKEN_ID,
					content: 'json'
				]
				headers.'Accept' = 'application/json'
				headers.'Content-Type' = 'application/json'

				response.success = { resp, reader ->
					assert resp.status == 200
					logger.info('My response handler got response: ' + resp.statusLine)
					logger.info("Response length: " + resp.headers.'Content-Length')
					//System.out << reader // print response reader
					def slurper = new JsonSlurper();
					def result = slurper.parse(reader)
					return [
						"test_name": result.test_name,
						"start_time": result.start_time,
						"end_time": result.end_time,
						"score": result.score,
						"score_stderr": result.score_stderr
					]
					println result.test_name + " " + result.score
				}

				response.failure = { resp, reader ->
					logger.warning('failure... - ' + resp.status)
					logger.warning('My response handler got response: ' + resp.statusLine)
					logger.warning("Response length: " + resp.headers.'Content-Length')
					System.out << reader // print response reader
					throw new Exception("Failure on authentication")
				}
		}
	}
	
	private static String generateURL(downloadURL, username, token) {
		return downloadURL + "?" + USERNAME_PARAM + "=" + username + "&" + TOKEN_PARAM + "=" + token
	}
	
	/**
	 * Date format:		2016/01/03 07:33:01.480380
	 */
	private static getTestTimeInSeconds(startDateStr, endDateStr) {
		
		def startDateObj = parseDateStr(startDateStr)
		def endDateObj = parseDateStr(endDateStr)
		
		def diff = (endDateObj.getTime() - startDateObj.getTime()) / 1000
		
		return diff
	}
	
	/**
	 */
	private static getBeforeDate(currentStartDate, testStartDate) {
		
		if (StringUtils.isBlank(currentStartDate)) {
			return testStartDate;
		}
		
		def currentStartDateTime = parseDateStr(currentStartDate)
		def testStartDateTime = parseDateStr(testStartDate)
		
		if (currentStartDateTime < testStartDateTime) {
			return currentStartDate
		}
		else {
			return testStartDate
		}
	}
	
	/**
	 * TODO: for some reason messing up the date when has milliseconds
	 */
	private static parseDateStr(dateStr) {
		def dateTrimStr = dateStr.substring(0, 19)
		return DATE_FORMATTER.parse(dateTrimStr)
	}
}
