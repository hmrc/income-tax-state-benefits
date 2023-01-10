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

package models.mongo

import org.scalamock.scalatest.MockFactory
import play.api.libs.json.Json
import support.UnitTest
import support.builders.mongo.ClaimCYAModelBuilder.aClaimCYAModel
import support.builders.mongo.StateBenefitsUserDataBuilder.{aStateBenefitsUserData, aStateBenefitsUserDataJson}
import utils.AesGcmAdCrypto

import java.util.UUID

class StateBenefitsUserDataSpec extends UnitTest
  with MockFactory {

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val claimCYAModel = mock[ClaimCYAModel]
  private val encryptedClaimCYAModel = mock[EncryptedClaimCYAModel]

  ".isHmrcData" should {
    "return false when claim is None" in {
      aStateBenefitsUserData.copy(claim = None).isHmrcData shouldBe false
    }

    "return false when claim.isHmrcData is false" in {
      aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(isHmrcData = false))).isHmrcData shouldBe false
    }

    "return false when claim.isHmrcData is true" in {
      aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(isHmrcData = true))).isHmrcData shouldBe true
    }
  }

  ".isNewClaim" should {
    "return false when claim is None" in {
      aStateBenefitsUserData.copy(claim = None).isNewClaim shouldBe false
    }

    "return false when claim.benefitId is Some(...)" in {
      aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = Some(UUID.randomUUID())))).isNewClaim shouldBe false
    }

    "return false when claim.benefitId is None" in {
      aStateBenefitsUserData.copy(claim = Some(aClaimCYAModel.copy(benefitId = None))).isNewClaim shouldBe true
    }
  }

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

      (claimCYAModel.encrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(encryptedClaimCYAModel)

      underTest.encrypted shouldBe EncryptedStateBenefitsUserData(
        benefitType = underTest.benefitType,
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
      (encryptedClaimCYAModel.decrypted(_: AesGcmAdCrypto, _: String)).expects(*, *).returning(claimCYAModel)

      val encryptedData = EncryptedStateBenefitsUserData(
        benefitType = aStateBenefitsUserData.benefitType,
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
        benefitType = aStateBenefitsUserData.benefitType,
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
