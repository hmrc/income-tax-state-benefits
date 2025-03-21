/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import config.AppConfig
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex}
import org.mongodb.scala.model.{IndexModel, IndexOptions}

import java.util.concurrent.TimeUnit

object JourneyAnswersRepositoryIndexes {

  private val lookUpIndex: Bson = compoundIndex(
    ascending("mtdItId"),
    ascending("taxYear"),
    ascending("journey")
  )

  def indexes()(implicit appConfig: AppConfig): Seq[IndexModel] = Seq(
    IndexModel(lookUpIndex, IndexOptions().name("mtdItId-taxYear-journey")),
    IndexModel(
      ascending("lastUpdated"),
      IndexOptions()
        .expireAfter(appConfig.mongoJourneyAnswersTTL, TimeUnit.DAYS)
        .name("last-updated-index")
    )
  )

}