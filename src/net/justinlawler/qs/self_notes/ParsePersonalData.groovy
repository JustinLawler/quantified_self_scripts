package net.justinlawler.qs.self_notes

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
class ParsePersonalData {

	static def logger = Logger.getLogger("ParsePersonalData")
		
	private static final String FILES_DIR = "/Users/justinlawler/Dropbox/personal/notes"
	
	//   2016/01/03 07:33:01.480380
	private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

	/**
	 */
	public static void main(String[] args) {
		
		def testResults = parseFile("scratchpad-2015_December")
		printParsedValues("Sleep", "Slept", testResults)
	}
	
	/**
	  	# Monday - 01
	  
	    ## Sleep
	 	+ Slept - 4
	 	+ Eye  - 6
		+ Focus - 5
		+ Gut - 4
		+ Well - 5
		+ Brain_Clean - 5
		+ Made bed - true
		+ Static discharge - true
		
		
		## Early Morning
		+ Self Quantisation:
			+ before dump
		+ Gratitude - 9
	 */
	 private static parseFile(selfLogFile) {
		 
		 def fileContents = [:]
		 def currentBlock = null
		 def currentBlockName = null
		 if (!selfLogFile.exists()) {
			 println "File does not exist " + selfLogFile
		 }
		 else {
			 selfLogFile.eachLine { line ->
				 if (line.trim().size() != 0) {
					if (line.startsWith("# ")) {
						// date = TODO:  
					}
					else if (line.startsWith("## ")) {
						if (currentBlock != null) {
							fileContents[currentBlockName] = currentBlock
						}
						currentBlock = [:]
						currentBlockName = line.substring(4)
					} else if ((line.startsWith("+ ") && line.contains(" - ")) {
						def name = line.substring(3, line.indexOf(" - "))
						def val = line.substring(line.indexOf(" - ") + 3)
						currentBlock[name] = val
					}
				 }
			 }
		 }
		 
		 return fileContents
	 }
	 
	 /**
	  */
	 private static printParsedValues(category, name, parsedFile) {
		 
		 
	 }
}
