package net.justinlawler.qs.elitehrv

@Grapes([
	@Grab(group='commons-io', module='commons-io', version='2.4'),
	@Grab(group='commons-lang', module='commons-lang', version='2.6'),
	@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6')
])
class ParseEliteHRVData {
	
	public static final String HRV_DATA_DIR = "/Users/justinlawler/Dropbox/projects/life_logging/data/EliteHRV/";
	private static final String HRV_DATA_FILE = '2016-02-02 06-20-43.txt'
	

	/**
	 */
	public static void main(String[] args) {
		parseHeartRatesInFolder(new File(HRV_DATA_DIR))
	}
	
	/**
	 */
	public static void parseHeartRatesInFolder(hrvDir)
	{
		hrvDir.eachFileRecurse(groovy.io.FileType.FILES) {
			if (it.name.endsWith('.txt') && it.name.startsWith('201')) {
				def hrvData = readHRVData(it)
				def heartRate = calcAvgHeartRate(hrvData)
				printf (it.getName() + " %.2f\n", heartRate)
			}
		}
	}
	

	/**
	 * Input format:
	 	1189
		1169
		1144
		1128
		1136
		1177
		1210
	 */
	private static readHRVData(hrvFile) {
		
		def fileContents = []
		if (!hrvFile.exists()) {
			println "File does not exist"
		}
		else {
			hrvFile.eachLine { line ->
		   		if (line.trim().size() == 0) {
				   throw new RuntimeException("null line")
				}
				else {
					fileContents << Integer.parseInt(line)
				}
			}
		}
		
		return fileContents
	}
		
	/**
	 */
	private static calcAvgHeartRate(hrvData) {
		
		def sum = 0
		def count = 0
		for (def beatInterval : hrvData) {
			count++
			sum += beatInterval
		}
		
		if (count > 0) {
			def avgInterval = sum / count
			def pulse = (60 / avgInterval) * 1000
		}
		else {
			return 0.0f
		}
	}
}
