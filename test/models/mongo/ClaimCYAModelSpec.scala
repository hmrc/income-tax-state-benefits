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
import support.builders.mongo.ClaimCYAModelBuilder.{aClaimCYAModel, aClaimCYAModelJson}
import uk.gov.hmrc.crypto.EncryptedValue
import utils.AesGcmAdCrypto

class ClaimCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val aesGcmAdCrypto: AesGcmAdCrypto = mock[AesGcmAdCrypto]
  private implicit val associatedText: String = "some-associated-text"

  private val encryptedBenefitId = EncryptedValue("encryptedBenefitId", "some-nonce")
  private val encryptedStartDate = EncryptedValue("encryptedStartDate", "some-nonce")
  private val encryptedEndDateQuestion = EncryptedValue("encryptedEndDateQuestion", "some-nonce")
  private val encryptedEndDate = EncryptedValue("encryptedEndDate", "some-nonce")
  private val encryptedDateIgnored = EncryptedValue("encryptedDateIgnored", "some-nonce")
  private val encryptedSubmittedOn = EncryptedValue("encryptedSubmittedOn", "some-nonce")
  private val encryptedAmount = EncryptedValue("encryptedAmount", "some-nonce")
  private val encryptedTaxPaidQuestion = EncryptedValue("encryptedTaxPaidQuestion", "some-nonce")
  private val encryptedTaxPaid = EncryptedValue("encryptedTaxPaid", "some-nonce")
  private val encryptedIsHmrcData = EncryptedValue("encryptedIsHmrcData", "some-nonce")

  "ClaimCYAModel.format" should {
    "write to Json correctly when using implicit formatter" in {
      val actualResult = Json.toJson(aClaimCYAModel)
      actualResult shouldBe aClaimCYAModelJson
    }

    "read to Json correctly when using implicit read" in {
      val result = aClaimCYAModelJson.as[ClaimCYAModel]
      result shouldBe aClaimCYAModel
    }
  }

  "ClaimCYAModel.encrypted" should {
    "return EncryptedClaimCYAModel" in {
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.benefitId.get.toString, associatedText).returning(encryptedBenefitId)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.startDate.toString, associatedText).returning(encryptedStartDate)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.endDateQuestion.get.toString, associatedText).returning(encryptedEndDateQuestion)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.endDate.get.toString, associatedText).returning(encryptedEndDate)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.dateIgnored.get.toString, associatedText).returning(encryptedDateIgnored)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.submittedOn.get.toString, associatedText).returning(encryptedSubmittedOn)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.amount.get.toString, associatedText).returning(encryptedAmount)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.taxPaidQuestion.get.toString, associatedText).returning(encryptedTaxPaidQuestion)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.taxPaid.get.toString, associatedText).returning(encryptedTaxPaid)
      (aesGcmAdCrypto.encrypt(_: String)(_: String)).expects(aClaimCYAModel.isHmrcData.toString, associatedText).returning(encryptedIsHmrcData)

      aClaimCYAModel.encrypted shouldBe EncryptedClaimCYAModel(
        benefitId = Some(encryptedBenefitId),
        startDate = encryptedStartDate,
        endDateQuestion = Some(encryptedEndDateQuestion),
        endDate = Some(encryptedEndDate),
        dateIgnored = Some(encryptedDateIgnored),
        submittedOn = Some(encryptedSubmittedOn),
        amount = Some(encryptedAmount),
        taxPaidQuestion = Some(encryptedTaxPaidQuestion),
        taxPaid = Some(encryptedTaxPaid),
        isHmrcData = encryptedIsHmrcData
      )
    }
  }

  "EncryptedClaimCYAModel.decrypted" should {
    "return ClaimCYAModel" in {
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedBenefitId, associatedText).returning(aClaimCYAModel.benefitId.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedStartDate, associatedText).returning(aClaimCYAModel.startDate.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEndDateQuestion, associatedText).returning(aClaimCYAModel.endDateQuestion.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedEndDate, associatedText).returning(aClaimCYAModel.endDate.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedDateIgnored, associatedText).returning(aClaimCYAModel.dateIgnored.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedSubmittedOn, associatedText).returning(aClaimCYAModel.submittedOn.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedAmount, associatedText).returning(aClaimCYAModel.amount.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTaxPaidQuestion, associatedText).returning(aClaimCYAModel.taxPaidQuestion.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedTaxPaid, associatedText).returning(aClaimCYAModel.taxPaid.get.toString)
      (aesGcmAdCrypto.decrypt(_: EncryptedValue)(_: String))
        .expects(encryptedIsHmrcData, associatedText).returning(aClaimCYAModel.isHmrcData.toString)

      val encryptedData = EncryptedClaimCYAModel(
        benefitId = Some(encryptedBenefitId),
        startDate = encryptedStartDate,
        endDateQuestion = Some(encryptedEndDateQuestion),
        endDate = Some(encryptedEndDate),
        dateIgnored = Some(encryptedDateIgnored),
        submittedOn = Some(encryptedSubmittedOn),
        amount = Some(encryptedAmount),
        taxPaidQuestion = Some(encryptedTaxPaidQuestion),
        taxPaid = Some(encryptedTaxPaid),
        isHmrcData = encryptedIsHmrcData
      )

      encryptedData.decrypted shouldBe ClaimCYAModel(
        benefitId = aClaimCYAModel.benefitId,
        startDate = aClaimCYAModel.startDate,
        endDateQuestion = aClaimCYAModel.endDateQuestion,
        endDate = aClaimCYAModel.endDate,
        dateIgnored = aClaimCYAModel.dateIgnored,
        submittedOn = aClaimCYAModel.submittedOn,
        amount = aClaimCYAModel.amount,
        taxPaidQuestion = aClaimCYAModel.taxPaidQuestion,
        taxPaid = aClaimCYAModel.taxPaid,
        isHmrcData = aClaimCYAModel.isHmrcData
      )
    }
  }
}
