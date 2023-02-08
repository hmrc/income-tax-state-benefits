/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors

import config.AppConfig
import org.scalamock.scalatest.MockFactory
import support.UnitTest
import support.providers.AppConfigStubProvider
import support.stubs.AppConfigStub
import uk.gov.hmrc.http.HeaderNames.{authorisation, xRequestChain, xSessionId}
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, SessionId}

import java.net.URL

class DESConnectorSpec extends UnitTest
  with MockFactory
  with AppConfigStubProvider {

  private val underTest = new DESConnector {
    override protected val appConfig: AppConfig = appConfigStub
  }

  ".baseUrl" should {
    "return the app config value" in {
      val underTest = new DESConnector {
        override protected val appConfig: AppConfig = new AppConfigStub().config("not-test")
      }

      underTest.baseUrl shouldBe appConfigStub.desBaseUrl
    }
  }

  ".desHeaderCarrier" should {
    "return correct HeaderCarrier when internal host" in {
      val internalHost = new URL("http://localhost")

      val result = underTest.desHeaderCarrier(internalHost)(HeaderCarrier())

      result.authorization shouldBe Some(Authorization(s"Bearer ${appConfigStub.desAuthorisationToken}"))
      result.extraHeaders shouldBe Seq("Environment" -> appConfigStub.desEnvironment)
    }

    "return correct HeaderCarrier when external host" in {
      val externalHost = new URL("http://127.0.0.1")
      val hc = HeaderCarrier(sessionId = Some(SessionId("sessionIdHeaderValue")))

      val result = underTest.desHeaderCarrier(externalHost)(hc)

      result.extraHeaders.size shouldBe 4
      result.extraHeaders.contains(xSessionId -> "sessionIdHeaderValue") shouldBe true
      result.extraHeaders.contains(authorisation -> s"Bearer ${appConfigStub.desAuthorisationToken}") shouldBe true
      result.extraHeaders.contains("Environment" -> appConfigStub.desEnvironment) shouldBe true
      result.extraHeaders.exists(x => x._1.equalsIgnoreCase(xRequestChain)) shouldBe true
    }
  }
}
