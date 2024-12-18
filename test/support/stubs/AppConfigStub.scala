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

package support.stubs

import config.{AppConfig, AppConfigImpl}
import org.scalamock.scalatest.MockFactory
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppConfigStub extends MockFactory {

  def config(environment: String = "test", encrypt: Boolean = true, useSectionCompletedQuestionEnabled: Boolean = true
            ): AppConfig = new AppConfigImpl(mock[ServicesConfig]) {
    private val wireMockPort = 11111

    private lazy val authorisationToken: String = "secret"

    override lazy val ifBaseUrl: String = s"http://localhost:$wireMockPort"
    override lazy val ifEnvironment: String = environment
    override lazy val stateBenefitsFrontendUrl: String = "http://localhost:9376"

    override lazy val submissionBaseUrl: String = s"http://localhost:$wireMockPort"

    override lazy val encryptionKey: String = "encryptionKey12345"
    override lazy val useEncryption: Boolean = encrypt

    override lazy val sectionCompletedQuestionEnabled: Boolean = useSectionCompletedQuestionEnabled

    override def authorisationTokenFor(apiVersion: String): String = authorisationToken + s".$apiVersion"

  }
}
