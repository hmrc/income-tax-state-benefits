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

package models.taskList

import models.tasklist.TaskTitle
import models.tasklist.TaskTitle.{ESA, JSA}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsPath, JsSuccess, JsValue, Json}

class TaskTitleSpec extends AnyFreeSpec with Matchers {

  "TaskTitle" - {

    "must contain the correct values" in {
      TaskTitle.values mustEqual Seq[TaskTitle](
        ESA,
        JSA
      )
    }

    "must parse each element to jsValue successfully" in {
      val underTest: Seq[JsValue] = TaskTitle.values.map(x => Json.toJson(x))
      underTest.isInstanceOf[Seq[JsValue]] mustBe true
    }
  }

  "ESA" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(ESA)

      underTest.toString() mustBe s"\"$ESA\""
      underTest.validate[TaskTitle] mustBe JsSuccess(ESA, JsPath())
    }
  }

  "JSA" - {

    "must parse to and from json" in {
      val underTest = Json.toJson(JSA)

      underTest.toString() mustBe s"\"$JSA\""
      underTest.validate[TaskTitle] mustBe JsSuccess(JSA, JsPath())
    }
  }
}