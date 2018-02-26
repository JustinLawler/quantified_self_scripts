package testing;

import org.apache.commons.io.IOUtils
import org.apache.commons.io.output.CountingOutputStream
import org.apache.commons.io.output.NullOutputStream

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.xml.bind.annotation.adapters.HexBinaryAdapter
import java.text.SimpleDateFormat
import java.util.logging.Logger
import java.util.logging.Level

import groovy.json.JsonSlurper

import groovyx.net.http.HTTPBuilder
import static groovyx.net.http.ContentType.BINARY
import static groovyx.net.http.ContentType.TEXT
import static groovyx.net.http.Method.GET
import static groovyx.net.http.Method.POST

/**
 * Dependencies:
 *      + HTTP Builder -            https://github.com/jgritman/httpbuilder
 *      + Apache Commons IO -       https://commons.apache.org/proper/commons-io/
 *
 * Uploading to production:
 * TODO:
 *      + add 2-way ssh to enable running in production
 *      + Make configurable from the command line
 */
@Grapes([
    @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version='0.6'),
    @Grab(group='commons-io', module='commons-io', version='2.4'),
    @GrabConfig(systemClassLoader=true)
])
class TestConnectYounited
{
    static def logger = Logger.getLogger("TestConnectYounited")

    // Skyblue APIs
    static def younitedUrl_API
    static def younitedUrl_Data
    static def younitedUrl_Ticket
    static def younitedUrl_Admin

    static def PRE_SHARED_KEY
    static def PSK_USER_NAME

    // Prod APIs
//    static def younitedUrl_API = 'https://UCM01-PROD-DATCHL-ucm.att.com:8443'
//    static def younitedUrl_Data = 'https://UCM01-PROD-DATCHL-ucm.att.com:8443'
//    static def younitedUrl_Ticket = 'https://UCM01-PROD-DATCHL-ucm.att.com:8443'
//    static def younitedUrl_Admin = 'https://UCM01-PROD-DATCHL-ucm.att.com:8443'

//    static def PRE_SHARED_KEY = '3SUCMSharedKey'
//    static def PSK_USER_NAME = 'SOAfoFbtUGzGglx6'


    // should be /adimin_l2 to change user state
    static def pskUrl = '/v2/token/psk1/user'
    static def userUrl = '/v2/content/me/account'
    static def contactsUrl = '/v2/content/me/contacts'
    static def userDetailsAdminUrl = '/v2/admin/operators/888/users/'
    static def devicesUrl = '/v2/content/me/devices'
    static def filesUrl = '/v2/content/me/files'
    static def journalUrl = '/v2/content/me/journal'
    static def collectionsURL = '/v2/content/me/lists'
    static def fileUrl = '/v2/data/me/files'

    static def UUID_FILE = '/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/skyblue_fsio_users.txt'
    static def UUID_FILE_WITH_CONTACTS = '/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/skyblue-all-users-with-contacts.txt'
    static def LOG_FILE = '/tmp/user_extract.log'
    static def TEST_UUID = 'bb84ab4a-e8c3-11e4-835e-080027a14a3c'

    static def uuidsWithDeviceTypesFile = new File("/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/uuidsWithDeviceTypes.log")
    static def uuidsWithDevicesFile = new File("/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/uuidsWithDevices.log")
    static def deviceNamesFile = new File("/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/deviceNames.log")
    static def deviceFolderTypes = new File("/home/jlawler/Documents/projects/F-Secure_Migration/environments/skyblue/deviceFolderTypes.log")
    static def uuidsWithContacts = new File(UUID_FILE_WITH_CONTACTS)




    /**
     * @param args
     */
    public static void main(String[] args)
    {
        println "Groovy version = " + GroovySystem.version


        java.util.logging.FileHandler fh = new java.util.logging.FileHandler(LOG_FILE )
        logger.addHandler(fh)
        java.util.logging.SimpleFormatter formatter = new java.util.logging.SimpleFormatter()
        fh.setFormatter(formatter)

        logger.setLevel(Level.INFO)
        logger.info("main()")

        setSkyblueEndpoints()
        //setFrontierProd()

        if (args.length > 0) {
            for (def uuid : args) {
                runFlow(uuid)
            }
        }

        //pingAPIs()


//
//        runFlow('017cc391-cc07-4989-ba9e-983253a6e0a9')

//        runFlow('ae5abca7-a90f-4765-8422-852e17082774')
//        runFlow('679472eb-0e0a-4207-8576-c9a855d07436')
//
//        runFlow('04ed3fbf-6542-4cd3-bd91-82f77488167a')
//        runFlow('bb84ab4a-e8c3-11e4-835e-080027a14a3c')               // 0 devices, 181 journal item count, 108 files, 75 collections
//        runFlow('6704a661-9d07-4561-af69-2e7b516de837')                // UI credentials - jdpl28@yahoo.co.uk
//        runFlow('d5395faf-bc4f-49bb-8710-923a3fb2be1a')                 // 0 devices
//        runFlow('00127f2d-ce2b-46ac-89e5-8421146885aa')               // 2 devices - web & mac. journal item count,  0 files, 0 collections
//        runFlow('013ed8ed-a008-42b4-8175-d83542cdfec6')                 // 5 devices, journal item count,  0 files, 0 collections

//        runFlow('6af3eb04-ea95-48f3-bd48-da519e872882')               // 2 devices, 13 journal item count, 1 files, 0 collections
//        //runFlow('7698ab24-3d19-45e0-ae58-474815d5f8f3')               // 0 devices, 0 journal item count, 0 files, 0 collections
//        runFlow('89627dbb-8701-479a-b9ef-4d730e6bf4db')               // 0 devices, 0  journal item count, 203 files, 5 collections
//        runFlow('91757fbc-654e-4ea3-9fe8-56a9ba8c82aa')               // 0 devices, 0  journal item count, 30  files, 2 collections
//        runFlow('885fbeba-6dc6-43fa-8602-35b85fe62498')               // 1 device, 34  journal item count, 30  files, 1 collections
//        runFlow('6704a661-9d07-4561-af69-2e7b516de837')               // jdpl28@yahoo.co.uk
//
//        // has contacts
//        runFlow('a9d3b039-e2b8-3d49-8fec-6cb7bb40ba3a')
//        runFlow('a5b9b200-e247-368e-877a-847065d584aa')
//        runFlow('b566306b-f2c1-4e30-bef2-162b871ceb92')
//        runFlow('8e21affb-ed54-4d06-8be0-a04340ac344b')
        // runFlow('055ec85b-4de7-4199-870b-7c01613e0acb')
        // runFlow('11c353a2-15fa-4518-abd0-faa51f914ce4')
        runFlow('12cc6d71-6a4a-4f13-8538-f9219433f052')                // 4 contacts 'my contacts.5' & 'my contacts.6'
        //runFlow('12dbcb6a-7e42-415c-b9d4-1a6eaaa33949')

//      UUIDs don't exist
//        runFlow('4a5030ab-4571-408b-8215-9439ab6d9f6e')


//       TODO:  DODGY!!!
        //runFlow('65343364-a756-4876-8ab0-e90e17847aff')
        //runFlow('38175265-b107-4b8a-af25-e9728b00207a')                 // Takes forever
        //runFlow('00ccb03b-0dc8-47ca-924f-f464cfed0629')
        //runFlow('c31092fa-f91c-4703-a99d-0fa060bc5a11')         // hangs on download journal. Its fucking big
//        runFlow('1805504d-54bf-4d8b-972d-de5a0d21f651')             // huge files
//        runFlow('7653e507-48a9-4490-a8ad-1fa715b9ec7f')            // quarintined files
        //runFlow('6d15878f-8524-4a91-b82d-fa8d118c1dca')            // quarintined files

//        authenticate('4a5030ab-4571-408b-8215-9439ab6d9f6e', PRE_SHARED_KEY, PSK_USER_NAME)
//        authenticate('89627dbb-8701-479a-b9ef-4d730e6bf4db', PRE_SHARED_KEY, PSK_USER_NAME)
//        runFlow('89627dbb-8701-479a-b9ef-4d730e6bf4db')
//        authenticate('00127f2d-ce2b-46ac-89e5-8421146885aa', PRE_SHARED_KEY, PSK_USER_NAME)
//        authenticate('6af3eb04-ea95-48f3-bd48-da519e872882', PRE_SHARED_KEY, PSK_USER_NAME)
//        authenticate('91757fbc-654e-4ea3-9fe8-56a9ba8c82aa', PRE_SHARED_KEY, PSK_USER_NAME)

        iterateOverUUIDFile(UUID_FILE)
    }

    /**
     */
    private static setSkyblueEndpoints() {

        younitedUrl_API = 'https://api.sb.fsxt.net:443'
        younitedUrl_Data = 'https://data.sb.fsxt.net:443'
        younitedUrl_Ticket = 'https://ticket.skyblue.sb.fsxt.net:443'
        younitedUrl_Admin = 'https://api.sb.fsxt.net:443'

        PRE_SHARED_KEY = 'gchFmd4hXYodmbuhnbnZ3Y'
        PSK_USER_NAME = 'YOUNITED_CLIENT_TEST_888'
    }

    /**
     */
    private static setFrontierProd() {

        younitedUrl_API = 'https://api.us1.younitedapi.com:443'
        younitedUrl_Data = 'https://data-migration.us1.younitedcontent.com:443'
        younitedUrl_Ticket = 'https://ticket.us1.younitedapi.com:443'
        younitedUrl_Admin = null

        PRE_SHARED_KEY = 'xsJU0v9yDSMfR3mdqS6cgzRukxfqpGkHatWb6al2II9ORkeA'
        PSK_USER_NAME = 'FSIO_53784_SNCR_MIGRATION'
    }


    private static runFlow(uuid)
    {
        logger.info("runFlow - " + uuid)
        def downloadToken1 = authenticate(uuid, PRE_SHARED_KEY, PSK_USER_NAME)
        println downloadToken1
        def downloadToken2 = authenticate(uuid, PRE_SHARED_KEY, PSK_USER_NAME)
        println downloadToken2

        if (downloadToken1 != null) {
            //downloadToken = downloadToken.substring(0, downloadToken.length() - 1) + "b"      // uncomment to fail the request
            //getUserDetails(uuid, downloadToken)
            getUserContacts(uuid, downloadToken1, ["notes", "address", "organisations", "time_zone"])
            //getUserDetailsAdminAccess(uuid, downloadToken)
            //def devices1 = getAllDevices(uuid, downloadToken1)
            // def devices2 = getAllDevices(uuid, downloadToken2)
            //getAllRootFolders(uuid, downloadToken)
            countJournalEntries(uuid, downloadToken1)
            //def downloadInfo = getAllJournalBatches(uuid, downloadToken1)
            //println downloadInfo.downloadUrls
            //downloadFiles(downloadInfo.downloadUrls, downloadToken)
            //deltaSync(uuid, downloadToken1, downloadInfo.currentJournalId, downloadInfo.maxJournalId)
            //getAllCollections(uuid, downloadToken)
        }
    }

    /**
     */
    private static iterateOverUUIDFile(String uuidFile)
    {
        def lineNo = 0
        def minLineCount = 0
        def maxLineCount = 100000
        def file = new File(uuidFile)
        for (line in file.readLines())  {
            lineNo++
            if (lineNo < minLineCount) {
                continue
            }
            if (lineNo >= maxLineCount) {
                logger.info('breaking')
                return
            }
            runFlow(line)
        }
    }

    /**
     * Questions:
     *  + how to get the PSK??
     *  + Any content passed up with 'POST' request?
     */
    private static authenticate(uuid, psk, dac)
    {
        logger.info("authenticate()");

        // Initialize a new builder and give a default URL
        def http = new HTTPBuilder(younitedUrl_Ticket)

        def date = getDate()
        def macKey = createMacKey(uuid, psk, date)
        //def macKey = '59d1cc50f892126e9f97d8eae9d84ea5d5b4fc1269a6523d985bf76ebc819825'
        logger.info("MAC Key: " + macKey)
        logger.info("Date: " + date)
        def downloadToken = null

        http.request(POST, TEXT) {
            req ->
                uri.path = pskUrl
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'x-authentication' = 'Uid ' + uuid + ':' + macKey
                headers.'x-dac' = dac
                headers.'x-date' = date

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine('My response handler got response: ${resp.statusLine}')
                    logger.fine("Response length: ${resp.headers.'Content-Length'}")
                    //System.out << reader // print response reader
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    logger.info('Token for user :                     ' + uuid + ' ' + result.token)
                    downloadToken = result.token
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning('My response handler got response: ${resp.statusLine}')
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                    throw new Exception("Failure on authentication")
                }

                // Invalid
                response.'400' = { resp, reader ->
                    logger.warning('Invalid')
                    System.out << reader // print response reader
                }

                // Can't authenticate user
                response.'401' = { resp, reader ->
                    logger.warning('Unauthorized')
                    System.out << reader // print response reader
                }

                // called only for a 403 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('Forbidden')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('Not found')
                    System.out << reader // print response reader
                }
        }

        return downloadToken
    }

    /**
     */
    private static getUserDetails(uuid, downloadToken)
    {
        logger.info("getUserDetails()");

        def http = new HTTPBuilder(younitedUrl_API)

        http.request(GET, TEXT) {
            req ->
                uri.path = userUrl
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    logger.fine("Response length: ${resp.headers.'Content-Length'}")
                    //System.out << reader // print response reader
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    logger.info('Quota           = ' + result.subscription_info.quota)
                    logger.info('Number of files: ' + uuid + ' ' + result.counts_including_history.all_files)
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // request not valid
                response.'400' = { resp, reader ->
                    logger.warning('400 - Bad request')
                    System.out << reader // print response reader
                }

                // download token is not valid
                response.'401' = { resp, reader ->
                    logger.warning('401 - Unauthorised')
                    System.out << reader // print response reader
                }

                response.'403' = { resp, reader ->
                    logger.warning('403 - Forbidden')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }
    }

    /**
     */
    private static getUserContacts(uuid, downloadToken, searchStrs)
    {
        logger.info("getUserContacts()");

        def http = new HTTPBuilder(younitedUrl_API)


        http.request(GET, TEXT) {
            req ->
                uri.path = contactsUrl
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    logger.fine("Response length: ${resp.headers.'Content-Length'}")
                    def resultStr = reader.getText()
                    if (searchStrs != null) {
                        for (str in searchStrs) {
                            if (resultStr.indexOf(str) > 0) {
                                println resultStr
                                println uuid
                            }
                        }
                    }
//                    System.out << reader // print response reader
//                    def slurper = new JsonSlurper()
//                    def result = slurper.parse(reader)
//                    logger.fine('Number contacts   = ' + result.total + ' last page = ' + result.last_page)
//                    if (result.total > 0) {
//                        logger.info('### Found ' + result.total + ' contacts for UUID ' + uuid)
//                        uuidsWithContacts << uuid + " " + result.total + "\n"
//                    }
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // request not valid
                response.'400' = { resp, reader ->
                    logger.warning('400 - Bad request')
                    System.out << reader // print response reader
                }

                // download token is not valid
                response.'401' = { resp, reader ->
                    logger.warning('401 - Unauthorised')
                    System.out << reader // print response reader
                }

                response.'403' = { resp, reader ->
                    logger.warning('403 - Forbidden')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }
    }


    /**
     */
    private static getUserDetailsAdminAccess(uuid, downloadToken)
    {
        logger.info("getUserDetailsAdminAccess()");

        def http = new HTTPBuilder(younitedUrl_API)

        http.request(GET, TEXT) {
            req ->
                uri.path = userDetailsAdminUrl + uuid
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    logger.fine("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
//                    def slurper = new JsonSlurper();
//                    def result = slurper.parse(reader)
//                    logger.info('Quota           = ' + result.subscription_info.quota)
//                    logger.info('Number of files: ' + uuid + ' ' + result.counts_including_history.all_files)
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }
        }
    }

    /**
     */
    private static getAllDevices(uuid, downloadToken)
    {
        logger.info("getAllDevices()")

        def http = new HTTPBuilder(younitedUrl_API)

        def devices = []
        http.request(GET, TEXT) {
            req ->
                uri.path = devicesUrl
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    def slurper = new JsonSlurper();
                    String str = reader.getText()

                    def result = slurper.parse(new StringReader(str))
                    result.items.each {
                        devices.add( [ it.name, it.key, it.metadata.category, it.metadata.platform, it.data_folder ] )
                        def category = it.metadata.category
                        def platform = it.metadata.platform

                        deviceFolderTypes << it.type + " " + category + " " + platform + "\n"
                        deviceNamesFile << it.name + "\n";

                        //List<String> ignoreCategories = ["web", "computer", "mobile"]
                        //List<String> ignoreCategories = ["web", "mobile"]
                        List<String> ignoreCategories = Collections.emptyList()
                        if (!ignoreCategories.contains(category)) {
                            if (category != null || !ignoreCategories.contains(platform)) {
                                def outputLogLine = "uuid=" + uuid + ', type=' + it.type + ', category=' + category + ", platform = " + platform
                                logger.info(outputLogLine)
                                uuidsWithDeviceTypesFile << uuid + ' ' + it.name + " " + it.type + ' ' + category + " " + platform + "\n"
                            }
                        }
                    }
                    logger.info('Dev count: ' + devices.size() + " " + devices)
                    if (devices.size() > 0) {
                        uuidsWithDevicesFile << devices.size() + " " + uuid + "\n"
                    }

                    return devices
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 401 (unauthorised) status code:
                response.'401' = { resp, reader ->
                    logger.warning('401 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }
    }

    /**
     * What works:
     *      + /v2/content/me/files/devices
     *      + /v2/content/me/files/
     *      +
     *
     * What doesn't work:
     *      + /files/devices/
     *      + /files/?recursive=True&visibility=visible
     *      + /files/?recursive=true&visibility=visible
     *      + /files?recursive=True&visibility=visible
     */
    private static getAllRootFolders(uuid, downloadToken)
    {
        logger.info("getAllRootFolders()")

        def http = new HTTPBuilder(younitedUrl_API)

        def rootFolders = []
        http.request(GET, TEXT) {
            req ->
                //uri.path = filesUrl + "/?recursive=true&visibility=visible"
                //uri.path = '/v2/content/me/files/?recursive=True&visibility=visible'
                //uri.path = '/v2/content/me/files/devices?recursive=true&object_type=file.image'
                uri.path = '/v2/content/me/files/'
                uri.rawQuery = ''
                headers.'User-Agent' = 'fsio-python-sdk/2.5'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    result.items.each {
                        rootFolders.add( it.name )
                    }
                    logger.info('Root folders: ' + rootFolders.size() + " " + rootFolders)
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - root folder for user ' + uuid + ' Not found')
                    System.out << reader // print response reader
                    throw new Exception ('404 - root folder for user ' + uuid + ' Not found')
                }
        }
    }

    /**
     */
    private static countJournalEntries(uuid, downloadToken)
    {
        logger.info("countJournalEntries()");

        def journalStatus = getJournalBatch(uuid, downloadToken, -1, -1, 100)
        logger.info("*****       total journal count = " + journalStatus.total)
    }

    /**
     */
    private static getAllJournalBatches(uuid, downloadToken)
    {
        logger.info("getAllJournalBatches()");

        def journalStatus = getJournalBatch(uuid, downloadToken, -1, -1, 100)
        logger.info("*****       total journal count = " + journalStatus.total)

        def currentJournalId = journalStatus.currentJournalId
        def maxJournalId = journalStatus.maxJournalId

        def downloadUrls = []
        downloadUrls.addAll(journalStatus.downloadUrls)
        while (journalStatus.itemCount > 0) {
            logger.fine("Current journal id = " + currentJournalId + " max journal id = " + maxJournalId)
            journalStatus = getJournalBatch(uuid, downloadToken, currentJournalId, maxJournalId, 100)
            if (journalStatus != null && journalStatus.itemCount > 0) {
                downloadUrls.addAll(journalStatus.downloadUrls)
                currentJournalId = journalStatus.currentJournalId
            }
        }

        logger.info("*****       num download urls = " + downloadUrls.size())

        return [ "downloadUrls" : downloadUrls, "currentJournalId" : currentJournalId, "maxJournalId" : maxJournalId ]
    }

    /**
     */
    private static getJournalBatch(uuid, downloadToken, journalId, maxJournalId, batchSize)
    {
        logger.info("getJournalBatch()");

        def http = new HTTPBuilder(younitedUrl_API)

        def total = 0
        def itemCount = 0;
        def highestJournelId = -1
        def maxJournelId = -1
        def downloadUrls = []
        http.request(GET, TEXT) {
            req ->
                uri.path = journalUrl
                if (journalId < 0) {
                    uri.query = [ initial_sync: 'True', related_objects: 'file', limit: batchSize]
                }
                else {
                    uri.query = [ initial_sync: 'True', related_objects: 'file', journal_id_gt : journalId, journal_id_lt : maxJournalId, limit: batchSize]
                }

                // For:
                //          + journal_max = 560832
                //          + last_journal_id = 560831
                //          +
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 656484]
                //uri.query = [ related_objects: 'file', journal_id_gt: 656484]
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 100]         // NOT working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 560829]         // NOT working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 560830]           // working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt : 560831]        // working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt : 560832]        // NOT working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt : 560831, journal_id_lt: 560832]             // Working
                //                                                                                                              // journal_id_lt only used for initial sync
                //                                                                                                              //
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 282318]          // NOT working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 282319]          // NOT working
                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 282310]


                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    def slurper = new JsonSlurper();
                    def journalStr = reader.text
                    if (journalStr.indexOf("starred") > 0) {
;                        println(journalStr)
                    }
                    def result = slurper.parse(new StringReader(journalStr))
                    def numFiles = 0
                    result.items.each {
                        highestJournelId = it.journal_id
                        if (it.related_file) {
                            downloadUrls.add( [
                                    "downloadUrl" : it.related_file.download_url,
                                    "sha256" : it.related_file.sha256,
                                    "size" : it.related_file.size
                            ])
                            numFiles++
                        }
                        else if (it.operation == "create") {
                            if (it.type == "folder") {
                                print it.key;
                            }
                            else if (it.type == "device") {
                                print it.key;
                            }
                            else if (it.type == "contact") {
                                print it.key;
                            }
                            else {
                                print it
                            }
                        }
                        else if (it.operation == "update") {
                            if (it.type == "contact") {
//                                print it.key;
                            }
                            else {
//                                print it.key;
                            }
                        }
                        else {
                            print it;
                        }
                    }
                    logger.info('Journal item count   = ' + result.count + ' num files = ' + numFiles)
                    maxJournelId = result.journal_max
                    itemCount = result.count
                    total = result.total
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }

        return [ currentJournalId : highestJournelId, maxJournalId : maxJournelId, itemCount : itemCount, downloadUrls : downloadUrls, total : total ]
    }

    /**
     */
    private static deltaSync(uuid, downloadToken, currentJournalId, maxJournalId)
    {
        logger.info("deltaSync()");

        def http = new HTTPBuilder(younitedUrl_API)

        def itemCount = 0;
        def highestJournelId = currentJournalId
        def downloadUrls = []
        http.request(GET, TEXT) {
            req ->
                uri.path = journalUrl
                //if (currentJournalId < 0) {
                if (maxJournalId < 0) {
                    uri.query = [ related_objects: 'file']
                }
                else {
                    // uri.query = [ related_objects: 'file', journal_id_gt : currentJournalId ]
                    uri.query = [ related_objects: 'file', journal_id_gt : maxJournalId - 1 ]
                }


                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    def numFiles = 0
                    result.items.each {
                        highestJournelId = it.journal_id
                        if (it.related_file) {
                            downloadUrls.add(it.related_file.download_url)
                            numFiles++
                        }
                    }
                    logger.info('Journal item count   = ' + result.count + ' num files = ' + numFiles)
                    itemCount = result.count
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }

        return [ currentJournalId : highestJournelId, itemCount : itemCount, downloadUrls : downloadUrls ]
    }

    /**
     */
    private static downloadFiles(downloadUrls, downloadToken)
    {
        logger.info("downloadFiles()");

        def currentTime = System.currentTimeMillis()
        def filesDownloaded = 0
        def bytesDownloaded = 0
        downloadUrls.each {
            bytesDownloaded += downloadFile(it.downloadUrl, downloadToken)
            filesDownloaded++
        }

        def totalTime = System.currentTimeMillis() - currentTime

        logger.info('## Downloaded ' + filesDownloaded + ' files at ' + bytesDownloaded + ' bytes in ' + (totalTime/1000) + ' seconds')

        return [ num_files : filesDownloaded, bytes_downloaded : bytesDownloaded ]
    }

    /**
     */
    private static downloadFile(download_url, downloadToken)
    {
        logger.info('downloadFile(' + download_url)

        def http = new HTTPBuilder(download_url)
        def bytesDownloaded = 0

        http.request(GET, BINARY) {
            req ->
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")

                    CountingOutputStream cos = new CountingOutputStream(new NullOutputStream())
                    IOUtils.copy(reader, cos)
                    logger.info("Downloaded " + cos.byteCount + " bytes for " + download_url)
                    bytesDownloaded = cos.byteCount
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 403 (unauthorised) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }

        logger.fine('         Downloaded')
        return bytesDownloaded
    }

    /**
     */
    private static getAllCollections(uuid, downloadToken)
    {
        logger.fine('getAllCollections()')

        def http = new HTTPBuilder(younitedUrl_API)

        http.request(GET, TEXT) {
            req ->
                uri.path = collectionsURL
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine( "My response handler got response: ${resp.statusLine}")
                    def journalStr = reader.text
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(new StringReader(journalStr))
                    def numFiles = 0
                    logger.info('Collection item count   = ' + result.count)
                    for (def result_item : result.items) {
                        logger.info('        \"' + result_item.name + '\" key ' + result_item.key + " size = " + result_item.size)
                        downloadCollection(uuid, downloadToken, result_item.key)
                    }
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }
    }

    private static downloadCollection(uuid, downloadToken, collectionKey) {

        def http = new HTTPBuilder(younitedUrl_API)

        http.request(GET, TEXT) {
            req ->
                uri.path = "/v2/content" + collectionKey + "/items"
                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine( "My response handler got response: ${resp.statusLine}")
                    def journalStr = reader.text
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(new StringReader(journalStr))
                    def numFiles = 0
                    logger.info('Collection item count   = ' + result.count)
                    for (def result_item : result.items) {
                        logger.info('        \"' + result_item.name + '\" key ' + result_item.key + " size = " + result_item.size)
                    }
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }
    }


    /**
     */
    private static getFileInfo(uuid, downloadToken, fileKey, fileVersion)
    {
        logger.info("getJournalBatch()");

        def http = new HTTPBuilder(younitedUrl_API)

        def itemCount = 0;
        def highestJournelId = -1
        def maxJournelId = -1
        def downloadUrls = []
        http.request(GET, TEXT) {
            req ->
                uri.path = journalUrl
//                if (journalId < 0) {
//                    uri.query = [ initial_sync: 'True', related_objects: 'file']
//                }
//                else {
//                    uri.query = [ initial_sync: 'True', related_objects: 'file', journal_id_gt : journalId, journal_id_lt : maxJournalId ]
//                }

                //uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 656484]
                //uri.query = [ related_objects: 'file', journal_id_gt: 656484]
                uri.query = [ related_objects: 'file', limit: 100, journal_id_gt: 656483]

                headers.'User-Agent' = 'Mozilla/5.0'
                headers.'authorization' = 'FsioToken ' + downloadToken
                headers.'date' = getDate()

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine("My response handler got response: ${resp.statusLine}")
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    def numFiles = 0
                    result.items.each {
                        highestJournelId = it.journal_id
                        if (it.related_file) {
                            downloadUrls.add(it.related_file.download_url)
                            numFiles++
                        }
                    }
                    logger.info('Journal item count   = ' + result.count + ' num files = ' + numFiles)
                    maxJournelId = result.journal_max
                    itemCount = result.count
                }

                response.failure = { resp, reader ->
                    logger.warning('failure... - ' + resp.status)
                    logger.warning("My response handler got response: ${resp.statusLine}")
                    logger.warning("Response length: ${resp.headers.'Content-Length'}")
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'403' = { resp, reader ->
                    logger.warning('403 - Unauthorised')
                    System.out << reader // print response reader
                }

                // called only for a 404 (not found) status code:
                response.'404' = { resp, reader ->
                    logger.warning('404 - User ' + uuid + ' Not found')
                    System.out << reader // print response reader
                }
        }

        return [ currentJournalId : highestJournelId, maxJournalId : maxJournelId, itemCount : itemCount, downloadUrls : downloadUrls ]
    }

    /**
     * Date format 2015-04-27T07:03:22.652883Z
     */
    private static getDate()
    {
        def dateObj = Calendar.getInstance().getTime()
        def date = new SimpleDateFormat("yyyy-MM-dd").format(dateObj) + 'T' + new SimpleDateFormat("HH:mm:ss.SSSSSS").format(dateObj) + 'Z'
        return date
    }

    /**
     */
    private static createMacKey(uuid, psk, date)
    {
        SecretKeySpec keySpec = new SecretKeySpec(psk.getBytes(), 'HmacSHA256')

        Mac mac = Mac.getInstance('HmacSHA256')
        mac.init(keySpec)

        // Format:
        //              bb84ab4a-e8c3-11e4-835e-080027a14a3c:POST:2015-04-27T11:59:53.488118Z:v2/token
        def data = uuid + ':POST:' + date + ':v2/token'
        logger.fine("data: " + data)

        byte[] result = mac.doFinal(data.getBytes())

        HexBinaryAdapter encoder = new HexBinaryAdapter()
        //BASE64Encoder encoder = new BASE64Encoder()
        return encoder.marshal(result).toLowerCase()
    }

    private static pingAPIs()
    {
        logger.info("pingAPIs()");

        def http = new HTTPBuilder('https://ticket.skyblue.sb.fsxt.net')
        //def http = new HTTPBuilder('https://api.sb.fsxt.net:443')

        http.request(POST, TEXT) {
            req ->
                uri.path = '/ticket/1_0_0/health'
                //uri.path = '/content/1_0_0/health'
                //headers.'User-Agent' = 'Mozilla/5.0'
                //setDoAuthentication(false)

                response.success = { resp, reader ->
                    assert resp.status == 200
                    logger.fine('My response handler got response: ${resp.statusLine}')
                    logger.fine("Response length: ${resp.headers.'Content-Length'}")
                    //System.out << reader // print response reader
                    def slurper = new JsonSlurper();
                    def result = slurper.parse(reader)
                    logger.info('Token for user :                     ' + uuid + ' ' + result.token)
                    downloadToken = result.token
                }

                response.failure = { resp, reader ->
                    logger.severe('failure... - ' + resp.status)
                    logger.severe('My response handler got response: ' + resp.statusLine)
                    logger.severe('Response length: ' + resp.headers)
                    System.err << reader // print response reader
                }
        }
    }
}
