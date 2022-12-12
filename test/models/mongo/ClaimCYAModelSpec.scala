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
import support.builders.mongo.ClaimCYAModelBuilder.{aClaimCYAModel, aClaimCYAModelJson}
import utils.{EncryptedValue, SecureGCMCipher}

class ClaimCYAModelSpec extends UnitTest
  with MockFactory {

  private implicit val secureGCMCipher: SecureGCMCipher = mock[SecureGCMCipher]
  private implicit val textAndKey: TextAndKey = TextAndKey("some-associated-text", "some-aes-key")

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
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.benefitId.get.toString, textAndKey).returning(encryptedBenefitId)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.startDate.toString, textAndKey).returning(encryptedStartDate)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.endDateQuestion.get.toString, textAndKey).returning(encryptedEndDateQuestion)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.endDate.get.toString, textAndKey).returning(encryptedEndDate)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.dateIgnored.get.toString, textAndKey).returning(encryptedDateIgnored)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.submittedOn.get.toString, textAndKey).returning(encryptedSubmittedOn)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.amount.get.toString, textAndKey).returning(encryptedAmount)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.taxPaidQuestion.get.toString, textAndKey).returning(encryptedTaxPaidQuestion)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.taxPaid.get.toString, textAndKey).returning(encryptedTaxPaid)
      (secureGCMCipher.encrypt(_: String)(_: TextAndKey)).expects(aClaimCYAModel.isHmrcData.toString, textAndKey).returning(encryptedIsHmrcData)

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
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedBenefitId.value, encryptedBenefitId.nonce, textAndKey).returning(aClaimCYAModel.benefitId.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedStartDate.value, encryptedStartDate.nonce, textAndKey).returning(aClaimCYAModel.startDate.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedEndDateQuestion.value, encryptedEndDateQuestion.nonce, textAndKey).returning(aClaimCYAModel.endDateQuestion.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedEndDate.value, encryptedEndDate.nonce, textAndKey).returning(aClaimCYAModel.endDate.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedDateIgnored.value, encryptedDateIgnored.nonce, textAndKey).returning(aClaimCYAModel.dateIgnored.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedSubmittedOn.value, encryptedSubmittedOn.nonce, textAndKey).returning(aClaimCYAModel.submittedOn.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedAmount.value, encryptedAmount.nonce, textAndKey).returning(aClaimCYAModel.amount.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedTaxPaidQuestion.value, encryptedTaxPaidQuestion.nonce, textAndKey).returning(aClaimCYAModel.taxPaidQuestion.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedTaxPaid.value, encryptedTaxPaid.nonce, textAndKey).returning(aClaimCYAModel.taxPaid.get.toString)
      (secureGCMCipher.decrypt(_: String, _: String)(_: TextAndKey))
        .expects(encryptedIsHmrcData.value, encryptedIsHmrcData.nonce, textAndKey).returning(aClaimCYAModel.isHmrcData.toString)

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
