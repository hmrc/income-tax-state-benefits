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
import utils.TypeCaster.Converter
import utils.{EncryptedValue, SecureGCMCipher}

import java.time.{Instant, LocalDate}
import java.util.UUID

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
  private val encryptedTaxPaid = EncryptedValue("encryptedTaxPaid", "some-nonce")

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
      (secureGCMCipher.encrypt(_: UUID)(_: TextAndKey)).expects(aClaimCYAModel.benefitId.get, textAndKey).returning(encryptedBenefitId)
      (secureGCMCipher.encrypt(_: LocalDate)(_: TextAndKey)).expects(aClaimCYAModel.startDate, textAndKey).returning(encryptedStartDate)
      (secureGCMCipher.encrypt(_: Boolean)(_: TextAndKey)).expects(aClaimCYAModel.endDateQuestion.get, textAndKey).returning(encryptedEndDateQuestion)
      (secureGCMCipher.encrypt(_: LocalDate)(_: TextAndKey)).expects(aClaimCYAModel.endDate.get, textAndKey).returning(encryptedEndDate)
      (secureGCMCipher.encrypt(_: Instant)(_: TextAndKey)).expects(aClaimCYAModel.dateIgnored.get, textAndKey).returning(encryptedDateIgnored)
      (secureGCMCipher.encrypt(_: Instant)(_: TextAndKey)).expects(aClaimCYAModel.submittedOn.get, textAndKey).returning(encryptedSubmittedOn)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aClaimCYAModel.amount.get, textAndKey).returning(encryptedAmount)
      (secureGCMCipher.encrypt(_: BigDecimal)(_: TextAndKey)).expects(aClaimCYAModel.taxPaid.get, textAndKey).returning(encryptedTaxPaid)

      aClaimCYAModel.encrypted shouldBe EncryptedClaimCYAModel(
        benefitId = Some(encryptedBenefitId),
        startDate = encryptedStartDate,
        endDateQuestion = Some(encryptedEndDateQuestion),
        endDate = Some(encryptedEndDate),
        dateIgnored = Some(encryptedDateIgnored),
        submittedOn = Some(encryptedSubmittedOn),
        amount = Some(encryptedAmount),
        taxPaid = Some(encryptedTaxPaid)
      )
    }
  }

  "EncryptedClaimCYAModel.decrypted" should {
    "return ClaimCYAModel" in {
      (secureGCMCipher.decrypt[UUID](_: String, _: String)(_: TextAndKey, _: Converter[UUID]))
        .expects(encryptedBenefitId.value, encryptedBenefitId.nonce, textAndKey, *).returning(aClaimCYAModel.benefitId.get)
      (secureGCMCipher.decrypt[LocalDate](_: String, _: String)(_: TextAndKey, _: Converter[LocalDate]))
        .expects(encryptedStartDate.value, encryptedStartDate.nonce, textAndKey, *).returning(aClaimCYAModel.startDate)
      (secureGCMCipher.decrypt[Boolean](_: String, _: String)(_: TextAndKey, _: Converter[Boolean]))
        .expects(encryptedEndDateQuestion.value, encryptedEndDateQuestion.nonce, textAndKey, *).returning(aClaimCYAModel.endDateQuestion.get)
      (secureGCMCipher.decrypt[LocalDate](_: String, _: String)(_: TextAndKey, _: Converter[LocalDate]))
        .expects(encryptedEndDate.value, encryptedEndDate.nonce, textAndKey, *).returning(aClaimCYAModel.endDate.get)
      (secureGCMCipher.decrypt[Instant](_: String, _: String)(_: TextAndKey, _: Converter[Instant]))
        .expects(encryptedDateIgnored.value, encryptedDateIgnored.nonce, textAndKey, *).returning(aClaimCYAModel.dateIgnored.get)
      (secureGCMCipher.decrypt[Instant](_: String, _: String)(_: TextAndKey, _: Converter[Instant]))
        .expects(encryptedSubmittedOn.value, encryptedSubmittedOn.nonce, textAndKey, *).returning(aClaimCYAModel.submittedOn.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedAmount.value, encryptedAmount.nonce, textAndKey, *).returning(aClaimCYAModel.amount.get)
      (secureGCMCipher.decrypt[BigDecimal](_: String, _: String)(_: TextAndKey, _: Converter[BigDecimal]))
        .expects(encryptedTaxPaid.value, encryptedTaxPaid.nonce, textAndKey, *).returning(aClaimCYAModel.taxPaid.get)

      val encryptedData = EncryptedClaimCYAModel(
        benefitId = Some(encryptedBenefitId),
        startDate = encryptedStartDate,
        endDateQuestion = Some(encryptedEndDateQuestion),
        endDate = Some(encryptedEndDate),
        dateIgnored = Some(encryptedDateIgnored),
        submittedOn = Some(encryptedSubmittedOn),
        amount = Some(encryptedAmount),
        taxPaid = Some(encryptedTaxPaid)
      )

      encryptedData.decrypted shouldBe ClaimCYAModel(
        benefitId = aClaimCYAModel.benefitId,
        startDate = aClaimCYAModel.startDate,
        endDateQuestion = aClaimCYAModel.endDateQuestion,
        endDate = aClaimCYAModel.endDate,
        dateIgnored = aClaimCYAModel.dateIgnored,
        submittedOn = aClaimCYAModel.submittedOn,
        amount = aClaimCYAModel.amount,
        taxPaid = aClaimCYAModel.taxPaid
      )
    }
  }
}
