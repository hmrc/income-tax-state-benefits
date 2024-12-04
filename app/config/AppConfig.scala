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

import com.google.inject.ImplementedBy
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration

@ImplementedBy(classOf[AppConfigImpl])
trait AppConfig {
  def ifBaseUrl: String
  def ifEnvironment: String
  def submissionBaseUrl: String
  def stateBenefitsFrontendUrl: String
  def mongoTTL: Int
  def encryptionKey: String
  def useEncryption: Boolean
  def mongoJourneyAnswersTTL: Int
  def replaceJourneyAnswersIndexes: Boolean
  def sectionCompletedQuestionEnabled: Boolean
  def authorisationTokenFor(apiVersion: String): String
  def emaSupportingAgentsEnabled: Boolean
}

@Singleton
class AppConfigImpl @Inject()(servicesConfig: ServicesConfig) extends AppConfig {

  private lazy val authorisationTokenKey: String = "microservice.services.integration-framework.authorisation-token"

  lazy val ifBaseUrl: String = servicesConfig.baseUrl(serviceName = "integration-framework")
  lazy val ifEnvironment: String = servicesConfig.getString(key = "microservice.services.integration-framework.environment")

  lazy val submissionBaseUrl: String = s"${servicesConfig.baseUrl(serviceName = "income-tax-submission")}/income-tax-submission-service"

  lazy val stateBenefitsFrontendUrl: String =
    s"${servicesConfig.getString("microservice.services.income-tax-state-benefits-frontend.url")}/update-and-submit-income-tax-return/state-benefits"

  //User data Mongo config
  lazy val mongoTTL: Int = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt
  lazy val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  lazy val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")

  //Journey answers Mongo config
  lazy val mongoJourneyAnswersTTL: Int = Duration(servicesConfig.getString("mongodb.journeyAnswersTimeToLive")).toDays.toInt
  lazy val replaceJourneyAnswersIndexes: Boolean = servicesConfig.getBoolean("mongodb.replaceJourneyAnswersIndexes")

  lazy val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.sectionCompletedQuestionEnabled")

  def authorisationTokenFor(apiVersion: String): String = servicesConfig.getString(authorisationTokenKey + s".$apiVersion")

  lazy val emaSupportingAgentsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.ema-supporting-agents-enabled")
}
