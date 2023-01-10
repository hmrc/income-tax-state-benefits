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

package support

import models.mongo.StateBenefitsUserData
import org.mongodb.scala.Document
import repositories.StateBenefitsUserDataRepositoryImpl

import java.util.UUID

trait DatabaseHelper {
  self: IntegrationTest =>

  lazy val stateBenefitsDatabase: StateBenefitsUserDataRepositoryImpl = app.injector.instanceOf[StateBenefitsUserDataRepositoryImpl]

  def dropStateBenefitsDB(): Unit = {
    await(stateBenefitsDatabase.collection.deleteMany(filter = Document()).toFuture())
    await(stateBenefitsDatabase.ensureIndexes)
  }

  def insertSessionData(stateBenefitsUserData: StateBenefitsUserData): Unit = {
    await(stateBenefitsDatabase.createOrUpdate(stateBenefitsUserData))
  }

  def findSessionData(nino: String, sessionDataId: UUID): Option[StateBenefitsUserData] = {
    await(stateBenefitsDatabase.find(nino, sessionDataId).map {
      case Left(_) => None
      case Right(userData) => Some(userData)
    })
  }
}
