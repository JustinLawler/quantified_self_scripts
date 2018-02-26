package testing

class TestUnicode {

	public static void main(String[] args) {
		
		def str = "Your scoreYour 10-year QRISK®2 score6.9%"
		
		println str.indexOf("QRISK®")
		println str.indexOf("®")
		println str.indexOf("\u00ae")
		println "matches = " + str.matches(".*QRISK.*")
		println "matches = " + str.matches(".*QRISK?.*")
		println "matches = " + str.matches(".*QRISK?2.*")
		println "matches = " + str.matches(".*QRISK??2.*")
	}
}
