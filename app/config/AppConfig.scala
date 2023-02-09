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

package config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig) {

  private lazy val authorisationTokenKey: String = "microservice.services.integration-framework.authorisation-token"

  lazy val ifBaseUrl: String = servicesConfig.baseUrl(serviceName = "integration-framework")
  lazy val ifEnvironment: String = servicesConfig.getString(key = "microservice.services.integration-framework.environment")

  lazy val submissionBaseUrl: String = s"${servicesConfig.baseUrl(serviceName = "income-tax-submission")}/income-tax-submission-service"

  //Mongo config
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt

  lazy val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")

  def authorisationTokenFor(apiVersion: String): String = servicesConfig.getString(authorisationTokenKey + s".$apiVersion")

  lazy val desBaseUrl: String = servicesConfig.baseUrl(serviceName = "des")
  lazy val desEnvironment: String = servicesConfig.getString(key = "microservice.services.des.environment")
  lazy val desAuthorisationToken: String = servicesConfig.getString(key = "microservice.services.des.authorisation-token")
}
