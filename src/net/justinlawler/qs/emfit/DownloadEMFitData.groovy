package net.justinlawler.qs.emfit

import java.text.SimpleDateFormat
import java.util.logging.Logger

import org.joda.time.*
import org.joda.time.format.*

import groovy.io.FileType;
import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

@Grapes([
	@Grab(group='commons-io', module='commons-io', version='2.4'),
	@Grab(group='commons-lang', module='commons-lang', version='2.6'),
	@Grab(group='joda-time', module='joda-time', version='2.9.2'),
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6')
])
class DownloadEMFitData {
	static def logger = Logger.getLogger("DownloadEMFitData")
	
	static def DATE_FORMATTER_ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
	static def DATE_FORMATTER_DATE = new SimpleDateFormat("yyyy-MM-dd");
	static def DATE_FORMATTER_TIME = new SimpleDateFormat("HH:mm:ss");
	
	/**
	 */
	public static void main(String[] args)
	{
		def jsonFile = new File(
				"/Users/justinlawler/Dropbox/projects/Quantified_Self/2017_QS_Amsterdam/Osteoperosis_speech/analysis/emfit_data.json")
		parseJSON(jsonFile)
	}
	
	
	/**
	 * Different data in the response:
	 * 		- hrv_rmssd_datapoints: The HRV
	 * 		- measured_datapoints:  The HR & breathing rates
	 */
	private static parseJSON(jsonFile)
	{
		def slurper = new JsonSlurper();
		def result = slurper.parse(jsonFile.newInputStream())
		
		def hrData = pullHRData(result.measured_datapoints)
		
	}
	
	/**
	 *  Format:
	 *  
	 *  "measured_datapoints": [
        [
            1493435766, 
            58, 
            null, 
            39
        ], 
        [
            1493435768, 
            58, 
            null, 
            34
        ], 
        [
            1493435770, 
            58, 
            null, 
            34
        ], 
        [
            1493435772, 
            58, 
            null, 
            32
        ], 
	 */
	private static pullHRData(measuredDataPoints)
	{
		def dataPoints = [:]
		for (def dataPoint : measuredDataPoints) {
			if (dataPoint[1] != null) {
				dataPoints[dataPoint[0]] = dataPoint[1]
				print dataPoint[0] + ", " + dataPoint[1] + "\n"
			}
		}
		
		return dataPoints;
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
}
