/*
 * Copyright 2022 HM Revenue & Customs
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

package models.mongo

import models.encryption.TextAndKey
import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Json
import support.UnitTest
import support.builders.mongo.StateBenefitsUserDataBuilder.{aStateBenefitsUserData, aStateBenefitsUserDataJson}
import utils.SecureGCMCipher

class StateBenefitsUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

  private val claimCYAModel = mock[ClaimCYAModel]
  private val encryptedClaimCYAModel = mock[EncryptedClaimCYAModel]

  "StateBenefitsUserData.format" should {
    "write to Json correctly when using implicit formatter" in {
      val actualResult = Json.toJson(aStateBenefitsUserData)
      actualResult shouldBe aStateBenefitsUserDataJson
    }

    "read to Json correctly when using implicit read" in {
      val result = aStateBenefitsUserDataJson.as[StateBenefitsUserData]
      result shouldBe aStateBenefitsUserData
    }
  }

  "StateBenefitsUserData.encrypted" should {
    "return EncryptedStateBenefitsUserData" in {
      val underTest = aStateBenefitsUserData.copy(claim = Some(claimCYAModel))

      (claimCYAModel.encrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(encryptedClaimCYAModel)

      underTest.encrypted shouldBe EncryptedStateBenefitsUserData(
        sessionDataId = underTest.sessionDataId,
        sessionId = underTest.sessionId,
        mtdItId = underTest.mtdItId,
        nino = underTest.nino,
        taxYear = underTest.taxYear,
        isPriorSubmission = underTest.isPriorSubmission,
        claim = Some(encryptedClaimCYAModel),
        lastUpdated = underTest.lastUpdated,
      )
    }
  }

  "EncryptedStateBenefitsUserData.decrypted" should {
    "return StateBenefitsUserData" in {
      (encryptedClaimCYAModel.decrypted()(_: SecureGCMCipher, _: TextAndKey)).expects(*, *).returning(claimCYAModel)

      val encryptedData = EncryptedStateBenefitsUserData(
        sessionDataId = aStateBenefitsUserData.sessionDataId,
        sessionId = aStateBenefitsUserData.sessionId,
        mtdItId = aStateBenefitsUserData.mtdItId,
        nino = aStateBenefitsUserData.nino,
        taxYear = aStateBenefitsUserData.taxYear,
        isPriorSubmission = aStateBenefitsUserData.isPriorSubmission,
        claim = Some(encryptedClaimCYAModel),
        lastUpdated = aStateBenefitsUserData.lastUpdated
      )

      encryptedData.decrypted shouldBe StateBenefitsUserData(
        sessionDataId = aStateBenefitsUserData.sessionDataId,
        sessionId = aStateBenefitsUserData.sessionId,
        mtdItId = aStateBenefitsUserData.mtdItId,
        nino = aStateBenefitsUserData.nino,
        taxYear = aStateBenefitsUserData.taxYear,
        isPriorSubmission = aStateBenefitsUserData.isPriorSubmission,
        claim = Some(claimCYAModel),
        lastUpdated = aStateBenefitsUserData.lastUpdated
      )
    }
  }
}
