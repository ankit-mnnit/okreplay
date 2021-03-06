package okreplay

import com.google.common.io.Files
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.ClassRule
import spock.lang.*

import static com.google.common.net.HttpHeaders.VIA
import static java.net.HttpURLConnection.HTTP_OK
import static okreplay.TapeMode.READ_WRITE

@OkReplay(mode = READ_WRITE)
@Unroll
@Timeout(10)
class RequestMethodsSpec extends Specification {
  @Shared @AutoCleanup("deleteDir") def tapeRoot = Files.createTempDir()
  @Shared def configuration = new OkReplayConfig.Builder()
      .tapeRoot(tapeRoot)
      .interceptor(new OkReplayInterceptor())
      .build()
  @Shared @ClassRule RecorderRule recorder = new RecorderRule(configuration)
  @Shared def endpoint = new MockWebServer()

  def client = new OkHttpClient.Builder()
      .addInterceptor(configuration.interceptor())
      .build()

  void setupSpec() {
    endpoint.start()
  }

  void "proxy handles #method requests"() {
    def mockResponse = new MockResponse().setBody("OK")
    if (method == 'HEAD') {
      mockResponse.clearHeaders()
    }
    when:
    endpoint.enqueue(mockResponse)
    MediaType mediaType = MediaType.parse("text/plain")
    def body = method == "GET" || method == 'HEAD' ? null : RequestBody.create(mediaType, "")
    def request = new Request.Builder()
        .url(endpoint.url("/"))
        .method(method, body)
        .build()
    def response = client.newCall(request).execute()

    then:
    response.code() == HTTP_OK
    response.header(VIA) == "OkReplay"

    where:
    method << ["GET", "POST", "PUT", "HEAD", "DELETE", "OPTIONS"]
  }
}
